import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
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

describe('Thành viên (chủ cửa hàng)', () => {
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
    expect(within(mine).queryByRole('combobox')).not.toBeInTheDocument();
    expect(within(mine).queryByRole('button')).not.toBeInTheDocument();

    await user.click(within(mine).getByRole('checkbox', { name: 'Đi giao: Chưa đặt tên (bạn)' }));
    await waitFor(() => expect(within(rowOf('(bạn)')).getByRole('checkbox')).toBeChecked());
    expect(calls).toEqual([['me', { canDrive: true }]]);

    const driver = rowOf('Tài xế A');
    expect(within(driver).getByRole('checkbox')).toBeDisabled();
    expect(within(driver).getByRole('combobox', { name: 'Vai trò của Tài xế A' })).toHaveValue('DRIVER');
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

    await user.selectOptions(within(rowOf('Tài xế B')).getByRole('combobox'), 'DISPATCHER');
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
    expect(dialog).toHaveTextContent('bị đăng xuất ngay trên mọi thiết bị');
    await user.click(within(dialog).getByRole('button', { name: 'Quay lại' }));
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(state.members.find((member) => member.id === 451)?.status).toBe('ACTIVE');

    await user.click(within(rowOf('Tài xế A')).getByRole('button', { name: 'Vô hiệu hoá Tài xế A' }));
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Vô hiệu hoá' }));
    await waitFor(() => expect(rowOf('Tài xế A')).toHaveTextContent('ĐÃ VÔ HIỆU HOÁ'));
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();

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

describe('Lời mời', () => {
  it('gửi lời mời theo vai trò, danh sách tải lại; thu hồi lời mời đang chờ', async () => {
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

    const form = screen.getByRole('form', { name: 'Mời thành viên' });
    await user.type(within(form).getByLabelText(/^Email/), 'dieuphoi.moi@example.com');
    await user.selectOptions(within(form).getByLabelText(/^Vai trò/), 'DISPATCHER');
    await user.click(within(form).getByRole('button', { name: 'Gửi lời mời' }));

    expect(await screen.findByText('Đã gửi lời mời tới dieuphoi.moi@example.com.')).toBeInTheDocument();
    expect(body).toEqual({ email: 'dieuphoi.moi@example.com', role: 'DISPATCHER' });
    await waitFor(() => expect(within(list).getAllByRole('row')).toHaveLength(6));
    expect(screen.getByRole('tab', { name: 'Lời mời (5)' })).toBeInTheDocument();

    await user.click(within(list).getByRole('button', { name: 'Thu hồi lời mời của nguoimoi@example.com' }));
    await waitFor(() =>
      expect(
        within(list).queryByRole('button', { name: 'Thu hồi lời mời của nguoimoi@example.com' }),
      ).not.toBeInTheDocument(),
    );
    expect(state.invitations.find((item) => item.id === 14)?.status).toBe('REVOKED');
  });

  it('email đã có tài khoản thì báo dưới ô email', async () => {
    server.use(
      http.post(`${API}/store/invitations`, () => fail(409, 'EMAIL_EXISTED')),
      ...memberHandlers(storeState()),
    );
    renderApp('/members?tab=invitations');
    const user = userEvent.setup();
    const form = await screen.findByRole('form', { name: 'Mời thành viên' });

    await user.type(within(form).getByLabelText(/^Email/), 'taixe.a@example.com');
    await user.click(within(form).getByRole('button', { name: 'Gửi lời mời' }));

    expect(await within(form).findByText('Email này đã được dùng.')).toBeInTheDocument();
    expect(within(form).getByLabelText(/^Email/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('gửi quá nhiều thì khoá nút theo Retry-After', async () => {
    server.use(
      http.post(`${API}/store/invitations`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '600' })),
      ...memberHandlers(storeState()),
    );
    renderApp('/members?tab=invitations');
    const user = userEvent.setup();
    const form = await screen.findByRole('form', { name: 'Mời thành viên' });

    await user.type(within(form).getByLabelText(/^Email/), 'a@example.com');
    await user.click(within(form).getByRole('button', { name: 'Gửi lời mời' }));

    expect(await within(form).findByRole('alert')).toHaveTextContent('Thử lại sau 10 phút.');
    expect(within(form).getByRole('button', { name: /Thử lại sau/ })).toBeDisabled();
  });
});

describe('Thành viên (điều phối viên)', () => {
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
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Vô hiệu hoá|Kích hoạt lại/ })).not.toBeInTheDocument();
    screen.getAllByRole('checkbox').forEach((box) => expect(box).toBeDisabled());
    expect(rowOf('Điều phối (bạn)')).toBeInTheDocument();
    expect(invitationsLoaded).toBe(false);
  });
});
