import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { wideWindow } from '@/test/media';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, owner, server, signedInHandlers } from '@/test/server';

type Member = { id: number; email: string; fullName: string | null; role: string; status: string; canDrive: boolean };
type Invitation = { id: number; email: string; role: string; status: string; expiresAt: string };

const noContent = () => new HttpResponse(null, { status: 204 });

/** The server's view of the store: members of the sample (the OWNER is 449, the signed-in user) and invitations. */
function storeState() {
  return {
    members: sample<{ data: Member[] }>('member-list').data,
    invitations: [
      { id: 14, email: 'nguoimoi@example.com', role: 'DRIVER', status: 'PENDING', expiresAt: '2026-11-10T09:17:25Z' },
      ...sample<{ data: Invitation[] }>('invitation-list').data,
    ],
  };
}

function memberHandlers(state: ReturnType<typeof storeState>, user: typeof owner = owner) {
  const patch = (id: number, changes: Partial<Member>) => {
    state.members = state.members.map((member) => (member.id === id ? { ...member, ...changes } : member));
    return ok(state.members.find((member) => member.id === id));
  };
  return [
    http.get(`${API}/store/members`, () => ok(state.members)),
    http.patch(`${API}/store/members/me/can-drive`, async ({ request }) =>
      patch(user.id, (await request.json()) as Partial<Member>),
    ),
    http.patch(`${API}/store/members/:id`, async ({ request, params }) =>
      patch(Number(params.id), (await request.json()) as Partial<Member>),
    ),
    http.get(`${API}/store/invitations`, () => ok(state.invitations)),
    http.post(`${API}/store/invitations/:id/revoke`, ({ params }) => {
      state.invitations = state.invitations.map((item) =>
        item.id === Number(params.id) ? { ...item, status: 'REVOKED' } : item,
      );
      return noContent();
    }),
    ...signedInHandlers(user),
  ];
}

const memberRows = () =>
  within(screen.getByRole('table', { name: 'Thành viên của cửa hàng' }))
    .getAllByRole('row')
    .slice(1);
const rowOf = (text: string) => memberRows().find((row) => row.textContent?.includes(text)) as HTMLElement;

afterEach(() => vi.restoreAllMocks());

describe('Thành viên (chủ cửa hàng, màn rộng)', () => {
  beforeEach(() => wideWindow());
  it('chủ cửa hàng chỉ đổi được "Đi giao" của chính mình; người khác có vai trò, đi giao, vô hiệu hoá', async () => {
    const state = storeState();
    const calls: Array<[string, unknown]> = [];
    server.use(
      http.patch(`${API}/store/members/me/can-drive`, async ({ request }) => {
        const body = (await request.json()) as Partial<Member>;
        calls.push(['me', body]);
        state.members = state.members.map((member) => (member.id === owner.id ? { ...member, ...body } : member));
        return ok(state.members.find((member) => member.id === owner.id));
      }),
      ...memberHandlers(state),
    );
    renderApp('/members');
    const user = userEvent.setup();

    await waitFor(() => expect(memberRows()).toHaveLength(4));
    const mine = rowOf('(bạn)');
    expect(mine).toHaveTextContent('Chưa đặt tên (bạn)');
    expect(mine).toHaveTextContent('Chủ cửa hàng');
    expect(within(mine).queryByRole('button')).not.toBeInTheDocument();

    await user.click(within(mine).getByRole('checkbox', { name: 'Đi giao: Chưa đặt tên (bạn)' }));
    await waitFor(() => expect(within(rowOf('(bạn)')).getByRole('checkbox')).toBeChecked());
    expect(calls).toEqual([['me', { canDrive: true }]]);

    const driver = rowOf('Tài xế A');
    expect(within(driver).getByRole('checkbox')).toBeDisabled();
    // The role is a pop-up button showing the current one.
    expect(within(driver).getByRole('button', { name: 'Vai trò của Tài xế A: Tài xế' })).toBeInTheDocument();
    expect(within(driver).getByRole('button', { name: 'Vô hiệu hoá Tài xế A' })).toBeInTheDocument();
  });

  it('đổi vai trò và bật đi giao của điều phối viên gửi PATCH và cập nhật dòng', async () => {
    const state = storeState();
    server.use(...memberHandlers(state));
    renderApp('/members');
    const user = userEvent.setup();
    await waitFor(() => expect(memberRows()).toHaveLength(4));

    await user.click(within(rowOf('Điều phối')).getByRole('checkbox', { name: 'Đi giao: Điều phối' }));
    await waitFor(() => expect(within(rowOf('Điều phối')).getByRole('checkbox')).toBeChecked());
    expect(state.members.find((member) => member.id === 450)?.canDrive).toBe(true);

    await user.click(within(rowOf('Tài xế B')).getByRole('button', { name: /^Vai trò của Tài xế B/ }));
    await user.click(screen.getByRole('menuitemradio', { name: 'Điều phối viên' }));
    await waitFor(() => expect(state.members.find((member) => member.id === 452)?.role).toBe('DISPATCHER'));
  });

  it('vô hiệu hoá có hộp xác nhận nêu bị đăng xuất ngay; kích hoạt lại được', async () => {
    const state = storeState();
    server.use(...memberHandlers(state));
    renderApp('/members');
    const user = userEvent.setup();
    await waitFor(() => expect(memberRows()).toHaveLength(4));

    await user.click(within(rowOf('Tài xế A')).getByRole('button', { name: 'Vô hiệu hoá Tài xế A' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Vô hiệu hoá Tài xế A?' });
    expect(dialog).toHaveTextContent('bị đăng xuất ngay');
    // An alert's way out is always titled Cancel (HIG Alerts).
    await user.click(within(dialog).getByRole('button', { name: 'Huỷ' }));
    // It fades out, then leaves the page.
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(state.members.find((member) => member.id === 451)?.status).toBe('ACTIVE');

    await user.click(within(rowOf('Tài xế A')).getByRole('button', { name: 'Vô hiệu hoá Tài xế A' }));
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Vô hiệu hoá' }));
    await waitFor(() => expect(rowOf('Tài xế A')).toHaveTextContent('ĐÃ VÔ HIỆU HOÁ'));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

    await user.click(within(rowOf('Tài xế A')).getByRole('button', { name: 'Kích hoạt lại Tài xế A' }));
    await waitFor(() => expect(rowOf('Tài xế A')).toHaveTextContent('ĐANG HOẠT ĐỘNG'));
  });

  it('thay đổi bị từ chối thì báo lỗi ở đầu bảng', async () => {
    const state = storeState();
    server.use(
      http.patch(`${API}/store/members/:id`, () => fail(422, 'CANNOT_MODIFY_OWNER')),
      ...memberHandlers(state),
    );
    renderApp('/members');
    const user = userEvent.setup();
    await waitFor(() => expect(memberRows()).toHaveLength(4));

    await user.click(within(rowOf('Điều phối')).getByRole('checkbox'));

    expect(await screen.findByRole('alert')).toHaveTextContent('Không thể thay đổi chủ cửa hàng.');
    expect(within(rowOf('Điều phối')).getByRole('checkbox')).not.toBeChecked();
  });
});

describe('Lời mời (màn rộng)', () => {
  beforeEach(() => wideWindow());

  it('nút "Mời" mở sheet: gửi theo vai trò, sang tab lời mời, danh sách tải lại; thu hồi lời mời đang chờ', async () => {
    const state = storeState();
    let body: unknown;
    server.use(
      http.post(`${API}/store/invitations`, async ({ request }) => {
        body = await request.json();
        const invitation = { id: 20, status: 'PENDING', expiresAt: '2026-11-11T03:00:00Z', ...(body as object) };
        state.invitations = [invitation as Invitation, ...state.invitations];
        return ok(invitation, { status: 201 });
      }),
      ...memberHandlers(state),
    );
    const { router } = renderApp('/members');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('tab', { name: /^Lời mời/ }));
    expect(router.state.location.search).toBe('?tab=invitations');
    const list = await screen.findByRole('table', { name: 'Lời mời đã gửi' });
    await waitFor(() => expect(within(list).getAllByRole('row')).toHaveLength(5));
    expect(within(list).getAllByRole('row')[1]).toHaveTextContent('CHỜ NHẬN');
    expect(within(list).getAllByRole('row')[1]).toHaveTextContent('10/11/2026');

    await user.click(screen.getByRole('button', { name: 'Mời' }));
    const sheet = screen.getByRole('dialog', { name: 'Mời thành viên' });
    await user.type(within(sheet).getByLabelText(/^Email/), 'dieuphoi.moi@example.com');
    await user.selectOptions(within(sheet).getByLabelText(/^Vai trò/), 'DISPATCHER');
    await user.click(within(sheet).getByRole('button', { name: 'Gửi' }));

    expect(await screen.findByText('Đã gửi lời mời tới dieuphoi.moi@example.com.')).toBeInTheDocument();
    expect(body).toEqual({ email: 'dieuphoi.moi@example.com', role: 'DISPATCHER' });
    await waitFor(() =>
      expect(within(screen.getByRole('table', { name: 'Lời mời đã gửi' })).getAllByRole('row')).toHaveLength(6),
    );
    expect(screen.getByRole('tab', { name: 'Lời mời (5)' })).toBeInTheDocument();

    const table = screen.getByRole('table', { name: 'Lời mời đã gửi' });
    await user.click(within(table).getByRole('button', { name: 'Thu hồi lời mời của nguoimoi@example.com' }));
    expect(await screen.findByText('Đã thu hồi lời mời của nguoimoi@example.com.')).toBeInTheDocument();
    expect(state.invitations.find((item) => item.id === 14)?.status).toBe('REVOKED');
  });

  it('email đã có tài khoản thì báo dưới ô email', async () => {
    server.use(
      http.post(`${API}/store/invitations`, () => fail(409, 'EMAIL_EXISTED')),
      ...memberHandlers(storeState()),
    );
    renderApp('/members?tab=invitations');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Mời' }));
    const sheet = screen.getByRole('dialog', { name: 'Mời thành viên' });

    await user.type(within(sheet).getByLabelText(/^Email/), 'taixe.a@example.com');
    await user.click(within(sheet).getByRole('button', { name: 'Gửi' }));

    expect(await within(sheet).findByText('Email này đã được dùng.')).toBeInTheDocument();
    expect(within(sheet).getByLabelText(/^Email/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('gửi quá nhiều thì khoá nút theo Retry-After', async () => {
    server.use(
      http.post(`${API}/store/invitations`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '600' })),
      ...memberHandlers(storeState()),
    );
    renderApp('/members?tab=invitations');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Mời' }));
    const sheet = screen.getByRole('dialog', { name: 'Mời thành viên' });

    await user.type(within(sheet).getByLabelText(/^Email/), 'a@example.com');
    await user.click(within(sheet).getByRole('button', { name: 'Gửi' }));

    expect(await within(sheet).findByRole('alert')).toHaveTextContent('Thử lại sau 10 phút.');
    expect(within(sheet).getByRole('button', { name: /Thử lại sau/ })).toBeDisabled();
  });
});

describe('Thành viên (điện thoại)', () => {
  it('segmented control đổi phần; bấm hàng mở sheet: switch "Đi giao", đổi vai trò, vô hiệu hoá qua alert', async () => {
    const state = storeState();
    server.use(...memberHandlers(state));
    const { router } = renderApp('/members');
    const user = userEvent.setup();

    const tabs = await screen.findByRole('tablist', { name: 'Thành viên và lời mời' });
    expect(within(tabs).getByRole('tab', { name: /^Thành viên/ })).toHaveAttribute('aria-selected', 'true');

    await user.click(await screen.findByRole('button', { name: 'Xem và đổi Điều phối' }));
    const sheet = screen.getByRole('dialog', { name: 'Điều phối' });
    const drive = within(sheet).getByRole('switch', { name: /^Đi giao/ });
    expect(drive).toHaveAttribute('aria-checked', 'false');
    await user.click(drive);
    await waitFor(() =>
      expect(within(screen.getByRole('dialog')).getByRole('switch')).toHaveAttribute('aria-checked', 'true'),
    );
    expect(state.members.find((member) => member.id === 450)?.canDrive).toBe(true);

    await user.selectOptions(within(sheet).getByLabelText('Vai trò'), 'DRIVER');
    await waitFor(() => expect(state.members.find((member) => member.id === 450)?.role).toBe('DRIVER'));

    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Vô hiệu hoá' }));
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Vô hiệu hoá' }));
    await waitFor(() => expect(state.members.find((member) => member.id === 450)?.status).toBe('DISABLED'));

    await user.click(within(tabs).getByRole('tab', { name: /^Lời mời/ }));
    expect(router.state.location.search).toBe('?tab=invitations');
    const invitations = await screen.findByRole('list', { name: 'Lời mời đã gửi' });
    expect(
      within(invitations).getByRole('button', { name: 'Thu hồi lời mời của nguoimoi@example.com' }),
    ).toBeInTheDocument();
  });
});

describe('Thành viên (điều phối viên)', () => {
  beforeEach(() => wideWindow());
  const dispatcher = { ...owner, id: 450, email: 'dieuphoi@example.com', role: 'DISPATCHER' } as typeof owner;

  it('chỉ xem: không có ô chọn vai trò, ô đi giao bị khoá, không có thao tác và tab lời mời', async () => {
    let invitationsLoaded = false;
    server.use(
      http.get(`${API}/store/invitations`, () => {
        invitationsLoaded = true;
        return ok([]);
      }),
      ...memberHandlers(storeState(), dispatcher),
    );
    renderApp('/members?tab=invitations');

    await waitFor(() => expect(memberRows()).toHaveLength(4));
    expect(screen.getByText(/Bạn đang xem với vai trò điều phối viên/)).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /^Lời mời/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Vai trò của/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Vô hiệu hoá|Kích hoạt lại/ })).not.toBeInTheDocument();
    screen.getAllByRole('checkbox').forEach((box) => expect(box).toBeDisabled());
    expect(rowOf('Điều phối (bạn)')).toBeInTheDocument();
    expect(invitationsLoaded).toBe(false);
  });
});
