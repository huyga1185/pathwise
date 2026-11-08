import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { anonymousHandlers, API, driver, fail, ok, owner, server, signedInHandlers } from '@/test/server';

async function fillAndSubmit(email = 'chu.cuahang@example.com', password = 'matkhau123') {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/^Email/), email);
  await user.type(screen.getByLabelText(/^Mật khẩu/), password);
  await user.click(screen.getByRole('button', { name: /Đăng nhập/ }));
  return user;
}

describe('Phiên đăng nhập', () => {
  it('khôi phục phiên từ cookie refresh khi tải trang', async () => {
    server.use(...signedInHandlers());
    renderApp('/plans');

    expect(await screen.findByRole('heading', { name: 'Kế hoạch giao hàng' })).toBeInTheDocument();
    expect(await screen.findByText('Tiệm bánh Cô Lan')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Kế hoạch' })).toHaveAttribute('aria-current', 'page');
  });

  it('không có phiên thì về đăng nhập, đăng nhập xong quay lại trang định mở, đăng xuất thì về đăng nhập', async () => {
    let loggedOut = false;
    // msw uses the first matching handler: the test's own handlers come first.
    server.use(
      http.post(`${API}/auth/log-in`, () => ok({ accessToken: 'access-1', user: owner })),
      http.post(`${API}/auth/log-out`, () => {
        loggedOut = true;
        return new Response(null, { status: 204 });
      }),
      ...anonymousHandlers(),
    );
    const { router } = renderApp('/vehicles');

    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?next=%2Fvehicles');
    const user = await fillAndSubmit();
    expect(await screen.findByRole('heading', { name: 'Xe' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Đăng xuất' }));
    expect(await screen.findByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(loggedOut).toBe(true);
  });

  it('refresh thất bại giữa chừng thì về đăng nhập kèm câu hết phiên', async () => {
    let refreshes = 0;
    server.use(
      http.post(`${API}/auth/refresh`, () =>
        ++refreshes === 1 ? ok({ accessToken: 'access-1' }) : fail(401, 'INVALID_TOKEN'),
      ),
      http.get(`${API}/users/me`, () => ok(owner)),
      http.get(`${API}/store`, () => fail(401, 'INVALID_TOKEN')),
    );
    renderApp('/plans');

    expect(await screen.findByText('Phiên đăng nhập đã hết, vui lòng đăng nhập lại.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
  });

  it('tài xế đăng nhập web thì thấy trang dùng app tài xế', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () => ok({ accessToken: 'a', user: driver })),
    );
    renderApp('/login');

    await fillAndSubmit('taixe.a@example.com');
    expect(await screen.findByRole('heading', { name: 'Dùng app tài xế' })).toBeInTheDocument();
  });
});

describe('Lỗi đăng nhập (SCREENS §1.1)', () => {
  it('sai email hoặc mật khẩu', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () => fail(401, 'INVALID_EMAIL_OR_PASSWORD')),
    );
    renderApp('/login');
    await fillAndSubmit();
    expect(await screen.findByRole('alert')).toHaveTextContent('Email hoặc mật khẩu không đúng.');
  });

  it('email chưa xác thực có link sang màn xác thực, kèm email', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () => fail(403, 'EMAIL_NOT_VERIFIED')),
    );
    renderApp('/login');
    await fillAndSubmit('lan@example.com');
    expect(await screen.findByRole('alert')).toHaveTextContent('Email chưa được xác thực.');
    expect(screen.getByRole('link', { name: 'Nhập mã xác thực' })).toHaveAttribute(
      'href',
      '/verify-email?email=lan%40example.com',
    );
  });

  it('tài khoản bị vô hiệu hoá', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () => fail(403, 'ACCOUNT_DISABLED')),
    );
    renderApp('/login');
    await fillAndSubmit();
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Tài khoản đã bị vô hiệu hoá.');
    expect(alert).toHaveTextContent('Liên hệ chủ cửa hàng.');
  });

  it('bị giới hạn số lần thử thì khoá nút và đếm ngược theo Retry-After', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '252' })),
    );
    renderApp('/login');
    await fillAndSubmit();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Bạn thao tác quá nhiều lần. Thử lại sau 4 phút 12 giây.',
    );
    const button = screen.getByRole('button', { name: /Thử lại sau/ });
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent(/Thử lại sau 4 phút 1[12] giây/);
  });

  it('lỗi theo field hiện dưới đúng ô', async () => {
    server.use(
      ...anonymousHandlers(),
      http.post(`${API}/auth/log-in`, () =>
        fail(400, 'VALIDATION_ERROR', [{ field: 'email', code: 'INVALID_EMAIL_FORMAT' }]),
      ),
    );
    renderApp('/login');
    await fillAndSubmit('khong-phai-email');
    const field = await screen.findByLabelText(/^Email/);
    await waitFor(() => expect(field).toHaveAccessibleDescription('Email không đúng định dạng.'));
    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('nút hiện mật khẩu đổi kiểu ô và nhãn', async () => {
    server.use(...anonymousHandlers());
    renderApp('/login');
    const user = userEvent.setup();
    const password = await screen.findByLabelText(/^Mật khẩu/);
    expect(password).toHaveAttribute('type', 'password');
    await user.click(screen.getByRole('button', { name: 'Hiện mật khẩu' }));
    expect(password).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: 'Ẩn mật khẩu' })).toHaveAttribute('aria-pressed', 'true');
  });
});
