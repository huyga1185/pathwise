import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { API, fail, ok, owner, server, signedInHandlers } from '@/test/server';

const noContent = () => new HttpResponse(null, { status: 204 });
const CHROME_LINUX =
  'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36';
const SAFARI_IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1';

/** Sessions shaped as API_SPEC §2 `SessionResponse` (no sample in docs/ux/samples yet). */
function sessionList() {
  return [
    {
      sessionId: 'b',
      userAgent: 'Dart/3.5 (dart:io)',
      ipAddress: '198.51.100.7',
      lastUsedAt: '2026-11-02T11:05:00Z',
      expiresAt: '2026-11-16T11:05:00Z',
      current: false,
    },
    {
      sessionId: 'a',
      userAgent: CHROME_LINUX,
      ipAddress: '203.0.113.24',
      lastUsedAt: '2026-11-03T09:20:00Z',
      expiresAt: '2026-11-17T09:20:00Z',
      current: true,
    },
    {
      sessionId: 'c',
      userAgent: SAFARI_IPHONE,
      ipAddress: '198.51.100.31',
      lastUsedAt: '2026-10-28T02:12:00Z',
      expiresAt: '2026-11-11T02:12:00Z',
      current: false,
    },
  ];
}

/** A signed-in OWNER on /profile whose sessions live in `state` (the server's view). */
function profileHandlers(state = { sessions: sessionList() }) {
  return [
    http.get(`${API}/auth/sessions`, () => ok(state.sessions)),
    http.delete(`${API}/auth/sessions/:id`, ({ params }) => {
      state.sessions = state.sessions.filter((item) => item.sessionId !== params.id);
      return noContent();
    }),
    http.post(`${API}/auth/sessions/revoke-others`, () => {
      state.sessions = state.sessions.filter((item) => item.current);
      return noContent();
    }),
    ...signedInHandlers({ ...owner, fullName: 'Chủ Lan', phoneNumber: '+84905123456' } as typeof owner),
  ];
}

const deviceRows = () =>
  within(screen.getByRole('region', { name: 'Thiết bị đang đăng nhập' })).getAllByRole('listitem');

describe('Hồ sơ và bảo mật', () => {
  it('hiện thông tin và thiết bị, thiết bị này đứng đầu, giờ theo timezone của cửa hàng', async () => {
    server.use(...profileHandlers());
    renderApp('/profile');

    expect(await screen.findByRole('heading', { name: 'Hồ sơ và bảo mật' })).toBeInTheDocument();
    expect(await screen.findByLabelText('Họ tên')).toHaveValue('Chủ Lan');
    expect(screen.getByLabelText('Số điện thoại')).toHaveValue('0905 123 456');
    expect(screen.getByText('Chủ cửa hàng')).toBeInTheDocument();

    await waitFor(() => expect(deviceRows()).toHaveLength(3));
    const [first, second, third] = deviceRows();
    expect(first).toHaveTextContent('Chrome trên Linux');
    expect(first).toHaveTextContent('Thiết bị này');
    expect(first).toHaveTextContent('IP 203.0.113.24 · dùng lần cuối 03/11 16:20 · hết hạn 17/11');
    expect(second).toHaveTextContent('App PathWise');
    expect(third).toHaveTextContent('Safari trên iOS');
  });

  it('lưu họ tên và số điện thoại; ô để trống được gửi null để xoá', async () => {
    let body: unknown;
    server.use(
      http.patch(`${API}/users/me`, async ({ request }) => {
        body = await request.json();
        return ok({ ...owner, fullName: 'Lan Nguyễn', phoneNumber: null });
      }),
      ...profileHandlers(),
    );
    renderApp('/profile');
    const user = userEvent.setup();

    const name = await screen.findByLabelText('Họ tên');
    await user.clear(name);
    await user.type(name, ' Lan Nguyễn ');
    await user.clear(screen.getByLabelText('Số điện thoại'));
    await user.click(screen.getByRole('button', { name: 'Lưu' }));

    expect(await screen.findByText('Đã lưu thông tin.')).toBeInTheDocument();
    expect(body).toEqual({ fullName: 'Lan Nguyễn', phoneNumber: null });
    expect(screen.getByRole('link', { name: 'Lan Nguyễn' })).toHaveAttribute('href', '/profile');
  });

  it('số điện thoại sai thì báo dưới ô', async () => {
    server.use(
      http.patch(`${API}/users/me`, () =>
        fail(400, 'VALIDATION_ERROR', [{ field: 'phoneNumber', code: 'PHONE_NUMBER_WRONG_REGION' }]),
      ),
      ...profileHandlers(),
    );
    renderApp('/profile');
    const user = userEvent.setup();

    const phone = await screen.findByLabelText('Số điện thoại');
    await user.clear(phone);
    await user.type(phone, '+14155550100');
    await user.click(screen.getByRole('button', { name: 'Lưu' }));

    expect(await screen.findByText('Chỉ nhận số Việt Nam.')).toBeInTheDocument();
    expect(phone).toHaveAttribute('aria-invalid', 'true');
  });

  it('đổi mật khẩu 2 bước; xong thì danh sách chỉ còn thiết bị này', async () => {
    const state = { sessions: sessionList() };
    const calls: unknown[] = [];
    server.use(
      http.post(`${API}/auth/password/change/otp`, async ({ request }) => {
        calls.push(await request.json());
        return noContent();
      }),
      http.put(`${API}/auth/password`, async ({ request }) => {
        calls.push(await request.json());
        state.sessions = state.sessions.filter((item) => item.current);
        return noContent();
      }),
      ...profileHandlers(state),
    );
    renderApp('/profile');
    const user = userEvent.setup();
    await waitFor(() => expect(deviceRows()).toHaveLength(3));

    await user.type(screen.getByLabelText(/^Mật khẩu hiện tại/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Gửi mã xác nhận' }));
    expect(await screen.findByText('Đã gửi mã 6 số tới chu.cuahang@example.com.')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Mã xác nhận'), '482913');
    await user.type(screen.getByLabelText(/^Mật khẩu mới/), 'matkhaumoi1');
    await user.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }));

    expect(await screen.findByText('Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.')).toBeInTheDocument();
    await waitFor(() => expect(deviceRows()).toHaveLength(1));
    expect(deviceRows()[0]).toHaveTextContent('Thiết bị này');
    expect(screen.getByRole('button', { name: 'Đăng xuất mọi thiết bị khác' })).toBeDisabled();
    expect(calls).toEqual([{ currentPassword: 'matkhau123' }, { otp: '482913', newPassword: 'matkhaumoi1' }]);
  });

  it('mật khẩu hiện tại sai thì báo dưới ô; xin mã quá sớm thì báo chờ', async () => {
    let call = 0;
    server.use(
      http.post(`${API}/auth/password/change/otp`, () =>
        ++call === 1 ? fail(400, 'INVALID_CURRENT_PASSWORD') : fail(429, 'OTP_COOLDOWN'),
      ),
      ...profileHandlers(),
    );
    renderApp('/profile');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Mật khẩu hiện tại/), 'sai');
    await user.click(screen.getByRole('button', { name: 'Gửi mã xác nhận' }));
    expect(await screen.findByText('Mật khẩu hiện tại không đúng.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Gửi mã xác nhận' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Vui lòng chờ trước khi xin mã mới.');
  });

  it('đăng xuất một thiết bị khác và mọi thiết bị khác', async () => {
    const state = { sessions: sessionList() };
    server.use(...profileHandlers(state));
    renderApp('/profile');
    const user = userEvent.setup();
    await waitFor(() => expect(deviceRows()).toHaveLength(3));

    await user.click(screen.getByRole('button', { name: 'Đăng xuất Safari trên iOS' }));
    await waitFor(() => expect(deviceRows()).toHaveLength(2));

    await user.click(screen.getByRole('button', { name: 'Đăng xuất mọi thiết bị khác' }));
    await waitFor(() => expect(deviceRows()).toHaveLength(1));
    expect(state.sessions.map((item) => item.sessionId)).toEqual(['a']);
  });

  it('đăng xuất ở dòng thiết bị này thì đăng xuất luôn', async () => {
    let loggedOut = false;
    server.use(
      http.post(`${API}/auth/log-out`, () => {
        loggedOut = true;
        return noContent();
      }),
      ...profileHandlers(),
    );
    renderApp('/profile');
    const user = userEvent.setup();
    await waitFor(() => expect(deviceRows()).toHaveLength(3));

    await user.click(within(deviceRows()[0] as HTMLElement).getByRole('button', { name: 'Đăng xuất' }));

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(loggedOut).toBe(true);
  });
});
