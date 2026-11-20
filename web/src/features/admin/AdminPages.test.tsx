import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Schemas } from '@/data/types';
import { wideWindow } from '@/test/media';
import { renderApp } from '@/test/renderApp';
import { API, fail, ok, server, signedInHandlers } from '@/test/server';

/** Fixtures typed by the generated contract (api/openapi.json): a wrong shape fails the typecheck. */
const admin = { id: 1, email: 'admin@pathwise.vn', role: 'ADMIN', storeId: null, canDrive: false };
const page = <T,>(items: T[]) => ({ items, page: 0, size: 20, totalPages: 1, totalElements: items.length });

const day = (date: string, n: number): Schemas['Day'] => ({
  date,
  newStores: 0,
  plansCreated: n + 1,
  plansPublished: n,
  plansCompleted: n,
  stopsDelivered: n * 10,
  stopsFailed: 1,
  stopsSkipped: 0,
  optimizations: n + 2,
  optimizationsFailed: 0,
});
const business: Schemas['BusinessMetricsResponse'] = {
  from: '2026-10-16',
  to: '2026-11-14',
  timezone: 'Asia/Ho_Chi_Minh',
  computedAt: '2026-11-14T03:05:00Z',
  now: {
    stores: 12,
    activeStores7d: 8,
    activeStores30d: 10,
    lockedStores: 1,
    admins: 1,
    owners: 12,
    dispatchers: 9,
    drivers: 40,
  },
  totals: {
    newStores: 3,
    plansCreated: 210,
    plansPublished: 200,
    plansCompleted: 190,
    stopsDelivered: 4100,
    stopsFailed: 120,
    stopsSkipped: 30,
    deliveredRate: 0.9647,
    failedRate: 0.0282,
    skippedRate: 0.0071,
    optimizations: 260,
    optimizationsFailed: 4,
    optimizationFailureRate: null,
  },
  days: [day('2026-11-13', 7), day('2026-11-14', 6)],
};
const technical: Schemas['TechnicalMetricsResponse'] = {
  startedAt: '2026-11-14T01:00:00Z',
  uptimeSeconds: 7500,
  measuredAt: '2026-11-14T03:05:00Z',
  http: {
    requestsPerMinute: 12.4,
    requestsLastHour: 744,
    clientErrorRate: 0.012,
    serverErrorRate: 0,
    groups: [{ group: 'plans', requestsLastHour: 320, p95Millis: 85.3 }],
  },
  jobs: { unfinished: 0, failedLast24h: 2 },
  failuresLast24h: { email: 0, routing: 1, geocoding: 0, job: 2 },
  rateLimitedLast24h: 3,
  jvm: { heapUsedBytes: 180_000_000, heapMaxBytes: 402_653_184 },
  database: {
    activeConnections: 1,
    idleConnections: 9,
    maxConnections: 10,
    sizeBytes: 52_428_800,
    largestTables: [{ name: 'route_plan_stops', bytes: 20_971_520, rows: 41000 }],
  },
};
const summary: Schemas['AdminStoreSummaryResponse'] = {
  id: 3,
  name: 'Tiệm Hoa Hồng',
  ownerEmail: 'chu@tiemhoahong.vn',
  status: 'ACTIVE',
  createdAt: '2026-11-01T02:00:00Z',
  memberCounts: { owners: 1, dispatchers: 1, drivers: 1 },
  vehicles: 4,
  plans: 37,
  lastActivityAt: null,
};
const detail = (status: 'ACTIVE' | 'READ_ONLY' = 'ACTIVE'): Schemas['AdminStoreDetailResponse'] => ({
  ...summary,
  status,
  lockReason: status === 'READ_ONLY' ? 'Chưa thanh toán' : null,
  lockedAt: status === 'READ_ONLY' ? '2026-11-14T01:30:00Z' : null,
  timezone: 'Asia/Ho_Chi_Minh',
  last30Days: {
    since: '2026-10-15T03:00:00Z',
    plansCreated: 22,
    plansPublished: 20,
    plansCompleted: 19,
    stopsDelivered: 410,
    stopsFailed: 12,
    stopsSkipped: 3,
    deliveredRate: 0.9647,
  },
  members: [
    { id: 449, email: 'chu@tiemhoahong.vn', role: 'OWNER', status: 'ACTIVE', createdAt: '2026-11-01T02:00:00Z' },
    {
      id: 450,
      email: 'dieuphoi@tiemhoahong.vn',
      role: 'DISPATCHER',
      status: 'ACTIVE',
      createdAt: '2026-11-02T02:00:00Z',
    },
    { id: 451, email: 'taixe@tiemhoahong.vn', role: 'DRIVER', status: 'DISABLED', createdAt: '2026-11-02T02:00:00Z' },
  ],
});
const auditEvent: Schemas['AuditEventResponse'] = {
  id: 91,
  occurredAt: '2026-11-14T02:30:00Z',
  actorId: 1,
  actorEmail: 'admin@pathwise.vn',
  actorRole: 'ADMIN',
  storeId: 3,
  action: 'STORE_LOCKED',
  targetType: 'STORE',
  targetId: 3,
  details: { reason: 'Chưa thanh toán' },
  requestId: 'req-1',
  ip: '203.0.113.7',
};
const users: Schemas['AdminGetUserResponse'][] = [
  {
    id: 1,
    email: 'admin@pathwise.vn',
    phoneNumber: null,
    role: 'ADMIN',
    storeId: null,
    status: 'ACTIVE',
    createdAt: '2026-10-01T00:00:00Z',
    updatedAt: '2026-10-01T00:00:00Z',
  },
  {
    id: 449,
    email: 'chu@tiemhoahong.vn',
    phoneNumber: null,
    role: 'OWNER',
    storeId: 3,
    status: 'ACTIVE',
    createdAt: '2026-11-01T00:00:00Z',
    updatedAt: '2026-11-01T00:00:00Z',
  },
  {
    id: 512,
    email: 'moi@gas.vn',
    phoneNumber: null,
    role: 'OWNER',
    storeId: 14,
    status: 'PENDING_VERIFICATION',
    createdAt: '2026-11-13T00:00:00Z',
    updatedAt: '2026-11-13T00:00:00Z',
  },
];

function adminHandlers() {
  return [
    ...signedInHandlers(admin as never),
    http.get(`${API}/admin/metrics/business`, () => ok(business)),
    http.get(`${API}/admin/metrics/technical`, () => ok(technical)),
    http.get(`${API}/admin/audit`, () => ok(page([auditEvent]))),
  ];
}

describe('Khu quản trị (W11)', () => {
  it('ADMIN đăng nhập thì vào /admin; tổng quan hiện thẻ số, biểu đồ và khối kỹ thuật; đổi khoảng gửi from mới', async () => {
    const asked: string[] = [];
    server.use(
      http.get(`${API}/admin/metrics/business`, ({ request }) => {
        asked.push(new URL(request.url).searchParams.get('from') ?? '');
        return ok(business);
      }),
      ...adminHandlers(),
    );
    const { router } = renderApp('/login');

    expect(await screen.findByRole('heading', { name: 'Tổng quan nền tảng' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/admin');
    expect(await screen.findByText('96,5%')).toBeInTheDocument();
    expect(screen.getByText('12 chủ · 9 điều phối · 40 tài xế · 1 quản trị')).toBeInTheDocument();
    expect(screen.getByText('Tỉ lệ lỗi —')).toBeInTheDocument();
    expect(screen.getByRole('slider', { name: /kế hoạch giao việc mỗi ngày, 30 ngày, tổng 200/ })).toBeInTheDocument();
    expect(await screen.findByText(/tính từ 14\/11 08:00/)).toBeInTheDocument();
    expect(screen.getByText('2 lỗi trong 24 giờ')).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: /Thời gian phản hồi/ })).getByText('85 ms')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: '7 ngày' }));
    await waitFor(() => expect(asked).toHaveLength(2));
    expect(new Date(asked[1]!).getTime() - new Date(asked[0]!).getTime()).toBe(23 * 86_400_000);
  });

  it('người không phải ADMIN mở /admin thì thấy 403 ngay tại địa chỉ đó, trong khung cửa hàng (P-75)', async () => {
    server.use(...signedInHandlers());
    const { router } = renderApp('/admin/stores');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
    expect(screen.getByText('Về trang chính hoặc quay lại trang trước để tiếp tục.')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/admin/stores');
    expect(screen.getByRole('link', { name: 'Về trang chính' })).toHaveAttribute('href', '/plans');
  });

  it('ADMIN mở trang của cửa hàng thì thấy 403 trong khung quản trị (P-75)', async () => {
    server.use(...adminHandlers());
    renderApp('/plans');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
    expect(screen.getByText('Về trang chính hoặc quay lại trang trước để tiếp tục.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Về trang chính' })).toHaveAttribute('href', '/admin');
  });

  it('danh sách cửa hàng (điện thoại): hàng mở chi tiết; tìm khi gõ và bằng Enter; lọc trạng thái; xoá bộ lọc', async () => {
    const queries: URLSearchParams[] = [];
    server.use(
      http.get(`${API}/admin/stores`, ({ request }) => {
        queries.push(new URL(request.url).searchParams);
        return ok(page([summary, { ...summary, id: 7, name: 'Bánh mì Cô Ba', status: 'READ_ONLY' }]));
      }),
      ...adminHandlers(),
    );
    const { router } = renderApp('/admin/stores');
    const user = userEvent.setup();

    const rows = await screen.findByRole('list', { name: 'Cửa hàng của nền tảng' });
    expect(within(rows).getByRole('link', { name: /Tiệm Hoa Hồng/ })).toHaveAttribute('href', '/admin/stores/3');
    expect(within(rows).getByText('TẠM KHOÁ')).toBeInTheDocument();
    // No "Xoá bộ lọc" until something filters (G2.2).
    expect(screen.queryByRole('button', { name: 'Xoá bộ lọc' })).not.toBeInTheDocument();

    await user.type(screen.getByRole('searchbox', { name: 'Tìm cửa hàng' }), 'hoa{Enter}');
    await waitFor(() => expect(queries.at(-1)?.get('q')).toBe('hoa'));
    expect(router.state.location.search).toContain('q=hoa');
    // As people type: sent 0.3 s after the last key.
    await user.type(screen.getByRole('searchbox', { name: 'Tìm cửa hàng' }), ' hồng');
    await waitFor(() => expect(queries.at(-1)?.get('q')).toBe('hoa hồng'));

    await user.click(screen.getByRole('button', { name: /Trạng thái: Tất cả/ }));
    await user.click(screen.getByRole('menuitemradio', { name: 'Tạm khoá (chỉ đọc)' }));
    await waitFor(() => expect(queries.at(-1)?.get('status')).toBe('READ_ONLY'));

    await user.click(screen.getByRole('button', { name: 'Xoá bộ lọc' }));
    await waitFor(() => expect(router.state.location.search).toBe(''));
    expect(screen.getByRole('searchbox', { name: 'Tìm cửa hàng' })).toHaveValue('');
  });

  it('danh sách cửa hàng (màn rộng): sắp xếp bằng tiêu đề cột, bấm hàng mở chi tiết, trang quá cuối về trang cuối', async () => {
    wideWindow();
    const queries: URLSearchParams[] = [];
    server.use(
      http.get(`${API}/admin/stores/3`, () => ok(detail())),
      http.get(`${API}/admin/stores`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        queries.push(params);
        return ok({ ...page([summary]), page: Number(params.get('page')), totalPages: 2, totalElements: 21 });
      }),
      ...adminHandlers(),
    );
    const { router } = renderApp('/admin/stores?page=7');
    const user = userEvent.setup();

    await waitFor(() => expect(router.state.location.search).toBe('?page=1'));
    expect(screen.getByRole('navigation', { name: 'Trang danh sách cửa hàng' })).toHaveTextContent('Trang 2/2');
    // Until the last page's data arrives the old one stays, dimmed and busy; the result is then built anew. A click on the
    // old table as it is replaced was lost (F-77, CI): wait for the last page first.
    await waitFor(() => {
      expect(queries.at(-1)?.get('page')).toBe('1');
      expect(document.querySelector('.pw-list-result[aria-busy]')).toBeNull();
    });
    const table = await screen.findByRole('table', { name: 'Cửa hàng của nền tảng' });
    await user.click(within(table).getByRole('button', { name: /Cửa hàng/ }));
    await waitFor(() => expect(queries.at(-1)?.get('sort')).toBe('name,desc'));
    expect(router.state.location.search).toBe('?sort=name%2Cdesc');

    await user.click(
      within(screen.getByRole('table', { name: 'Cửa hàng của nền tảng' })).getByText('chu@tiemhoahong.vn'),
    );
    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/stores/3'));
    // Wait for the detail page: a request still on its way when the test ends is answered by the next test's handlers
    // and counted there as a second view (F-78).
    expect(await screen.findByRole('heading', { name: 'Tiệm Hoa Hồng' })).toBeInTheDocument();
  });

  it('chi tiết cửa hàng: khoá gửi lý do, hiện băng và mở khoá; chuyển chủ gửi userId', async () => {
    const bodies: unknown[] = [];
    let views = 0;
    server.use(
      http.get(`${API}/admin/stores/3`, () => {
        views += 1;
        return ok(detail());
      }),
      http.post(`${API}/admin/stores/3/lock`, async ({ request }) => {
        bodies.push(await request.json());
        return ok({ id: 3, status: 'READ_ONLY', lockReason: 'Chưa thanh toán', lockedAt: '2026-11-14T01:30:00Z' });
      }),
      http.post(`${API}/admin/stores/3/unlock`, () =>
        ok({ id: 3, status: 'ACTIVE', lockReason: null, lockedAt: null }),
      ),
      http.post(`${API}/admin/stores/3/owner`, async ({ request }) => {
        bodies.push(await request.json());
        return ok({
          storeId: 3,
          ownerId: 450,
          ownerEmail: 'dieuphoi@tiemhoahong.vn',
          ownerCanDrive: false,
          previousOwnerId: 449,
          previousOwnerEmail: 'chu@tiemhoahong.vn',
        });
      }),
      ...adminHandlers(),
    );
    renderApp('/admin/stores/3');
    const user = userEvent.setup();

    expect(await screen.findByRole('heading', { name: 'Tiệm Hoa Hồng' })).toBeInTheDocument();
    // The latest log entries: the whole sentence, who and when under it.
    expect(await screen.findByText('Tạm khoá cửa hàng: Chưa thanh toán')).toBeInTheDocument();
    // The store's actions are in the More menu (G7-Q3), locking last and destructive.
    await user.click(screen.getByRole('button', { name: 'Thao tác với cửa hàng' }));
    const items = screen.getAllByRole('menuitem');
    expect(items.map((item) => item.textContent)).toEqual(['Chuyển chủ cửa hàng…', 'Tạm khoá (chỉ đọc)…']);
    await user.click(items[1]!);
    const lockSheet = screen.getByRole('dialog', { name: 'Tạm khoá' });
    expect(within(lockSheet).getByRole('button', { name: 'Khoá' })).toBeDisabled();
    await user.type(within(lockSheet).getByLabelText(/Lý do/), '  Chưa thanh toán ');
    await user.click(within(lockSheet).getByRole('button', { name: 'Khoá' }));

    expect(await screen.findByText('Đã khoá cửa hàng.')).toBeInTheDocument();
    expect(bodies[0]).toEqual({ reason: 'Chưa thanh toán' });
    expect(screen.getByText('Đang tạm khoá (chỉ đọc)')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Thao tác với cửa hàng' }));
    await user.click(screen.getByRole('menuitem', { name: 'Mở khoá' }));
    await user.click(
      within(screen.getByRole('alertdialog', { name: /Mở khoá cửa hàng/ })).getByRole('button', { name: 'Mở khoá' }),
    );
    expect(await screen.findByText('Đã mở khoá cửa hàng.')).toBeInTheDocument();
    // Lock and unlock update the page from their response: the store is not read again (no extra STORE_VIEWED).
    expect(views).toBe(1);

    await user.click(screen.getByRole('button', { name: 'Thao tác với cửa hàng' }));
    await user.click(screen.getByRole('menuitem', { name: 'Chuyển chủ cửa hàng…' }));
    const transfer = screen.getByRole('dialog', { name: 'Chuyển chủ' });
    // Only ACTIVE members other than the OWNER can be chosen.
    expect(within(transfer).getAllByRole('option')).toHaveLength(1);
    await user.click(within(transfer).getByRole('button', { name: 'Chuyển chủ' }));
    expect(await screen.findByText('Đã chuyển chủ cửa hàng cho dieuphoi@tiemhoahong.vn.')).toBeInTheDocument();
    expect(bodies[1]).toEqual({ userId: 450 });
  });

  it('chi tiết cửa hàng không có thì báo không tìm thấy', async () => {
    server.use(
      http.get(`${API}/admin/stores/99`, () => fail(404, 'NOT_FOUND')),
      ...adminHandlers(),
    );
    renderApp('/admin/stores/99');
    expect(await screen.findByText('Không tìm thấy cửa hàng này.')).toBeInTheDocument();
  });

  it('người dùng: menu "…" của hàng có đúng hành động theo trạng thái; gửi link hỏi trước rồi gọi đúng API; không tự khoá mình', async () => {
    wideWindow();
    const posted: string[] = [];
    server.use(
      http.get(`${API}/admin/users`, () => ok(page(users))),
      http.post(`${API}/admin/users/:id/:action`, ({ params }) => {
        posted.push(`${String(params.id)}/${String(params.action)}`);
        return new HttpResponse(null, { status: 204 });
      }),
      ...adminHandlers(),
    );
    renderApp('/admin/users');
    const user = userEvent.setup();
    const menuOf = async (email: string) => {
      await user.click(await screen.findByRole('button', { name: `Thao tác với ${email}` }));
      const labels = screen.getAllByRole('menuitem').map((item) => item.textContent);
      return labels;
    };

    await screen.findByRole('table', { name: 'Người dùng của nền tảng' });
    // Oneself: no "Khoá".
    expect(await menuOf('admin@pathwise.vn')).not.toContain('Khoá');
    await user.keyboard('{Escape}');
    // Waiting for email: resend the verification, no reset link.
    const pendingItems = await menuOf('moi@gas.vn');
    expect(pendingItems).toContain('Gửi lại email xác thực');
    expect(pendingItems).not.toContain('Gửi link đặt lại mật khẩu');
    await user.keyboard('{Escape}');
    // Active: the reset link, the log, logging out, "Khoá" last (destructive).
    expect(await menuOf('chu@tiemhoahong.vn')).toEqual([
      'Gửi link đặt lại mật khẩu',
      'Xem nhật ký của người này',
      'Buộc đăng xuất',
      'Khoá',
    ]);
    await user.click(screen.getByRole('menuitem', { name: 'Gửi link đặt lại mật khẩu' }));
    const alert = screen.getByRole('alertdialog', { name: 'Gửi link đặt lại mật khẩu?' });
    expect(alert).toHaveTextContent('mở được trong 24 giờ');
    await user.click(within(alert).getByRole('button', { name: 'Gửi link' }));
    expect(await screen.findByText('Đã gửi link đặt lại mật khẩu cho chu@tiemhoahong.vn.')).toBeInTheDocument();
    expect(posted).toEqual(['449/password-reset-email']);
  });

  it('người dùng: cửa hàng chỉ đặt sẵn từ trang cửa hàng, hiện tên và bỏ được; F-76 không còn đếm "cửa hàng"', async () => {
    const queries: URLSearchParams[] = [];
    server.use(
      http.get(`${API}/admin/users`, ({ request }) => {
        queries.push(new URL(request.url).searchParams);
        return ok(page(users));
      }),
      ...adminHandlers(),
    );
    const { router } = renderApp('/admin/users?storeId=3&storeName=Ti%E1%BB%87m%20Hoa%20H%E1%BB%93ng');
    const user = userEvent.setup();

    await screen.findByRole('list', { name: 'Người dùng của nền tảng' });
    expect(queries[0]?.get('storeId')).toBe('3');
    expect(screen.getByText('Tiệm Hoa Hồng')).toBeInTheDocument();
    expect(screen.queryByText(/cửa hàng · trang/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Bỏ lọc cửa hàng' }));
    await waitFor(() => expect(router.state.location.search).toBe(''));
    await waitFor(() => expect(queries.at(-1)?.get('storeId')).toBeNull());
  });

  it('nhật ký: cửa hàng và người làm chỉ đặt sẵn từ trang khác (viên thuốc bỏ được), lọc hành động bằng pop-up', async () => {
    const queries: URLSearchParams[] = [];
    server.use(
      http.get(`${API}/admin/audit`, ({ request }) => {
        queries.push(new URL(request.url).searchParams);
        return ok(page([auditEvent]));
      }),
      ...adminHandlers(),
    );
    const { router } = renderApp(
      '/admin/audit?storeId=3&storeName=Ti%E1%BB%87m%20Hoa%20H%E1%BB%93ng&actorId=1&actor=admin%40pathwise.vn',
    );
    const user = userEvent.setup();

    expect(await screen.findByText('Tạm khoá cửa hàng: Chưa thanh toán')).toBeInTheDocument();
    expect(queries[0]?.get('storeId')).toBe('3');
    expect(queries[0]?.get('actorId')).toBe('1');
    // No field to type an id into (G7-Q2): the presets show names.
    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument();
    expect(screen.getByText('Tiệm Hoa Hồng')).toBeInTheDocument();
    const filters = screen.getByRole('group', { name: 'Lọc nhật ký' });
    expect(within(filters).getByText('admin@pathwise.vn')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Hành động: Tất cả/ }));
    await user.click(screen.getByRole('menuitemradio', { name: 'Khoá cửa hàng' }));
    await waitFor(() => expect(queries.at(-1)?.get('action')).toBe('STORE_LOCKED'));

    await user.click(screen.getByRole('button', { name: 'Bỏ lọc người làm' }));
    await waitFor(() => expect(queries.at(-1)?.get('actorId')).toBeNull());
    expect(queries.at(-1)?.get('storeId')).toBe('3');
    await user.click(screen.getByRole('button', { name: 'Bỏ lọc cửa hàng' }));
    await waitFor(() => expect(router.state.location.search).toBe('?action=STORE_LOCKED'));
  });

  it('nhật ký: không có tên trên link thì hiện mã; bấm một dòng mở sheet "Chi tiết" đủ chữ, có link sang cửa hàng', async () => {
    server.use(
      http.get(`${API}/admin/audit`, () => ok(page([auditEvent]))),
      ...adminHandlers(),
    );
    renderApp('/admin/audit?storeId=3&actorId=1');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Tạm khoá cửa hàng: Chưa thanh toán/ }));
    const filters = screen.getByRole('group', { name: 'Lọc nhật ký' });
    expect(within(filters).getByText('Cửa hàng #3')).toBeInTheDocument();
    expect(within(filters).getByText('Người dùng #1')).toBeInTheDocument();
    const sheet = await screen.findByRole('dialog', { name: 'Chi tiết' });
    expect(within(sheet).getByText('Tạm khoá cửa hàng: Chưa thanh toán')).toBeInTheDocument();
    expect(within(sheet).getByText('14/11/2026 09:30')).toBeInTheDocument();
    expect(within(sheet).getByText('admin@pathwise.vn')).toBeInTheDocument();
    expect(within(sheet).getByText('203.0.113.7')).toBeInTheDocument();
    expect(within(sheet).getByRole('link', { name: 'Cửa hàng #3' })).toHaveAttribute('href', '/admin/stores/3');
  });
});
