import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Schemas } from '@/data/types';
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
    expect(screen.getByRole('img', { name: /kế hoạch giao việc mỗi ngày, 30 ngày, tổng 200/ })).toBeInTheDocument();
    expect(await screen.findByText(/tính từ 14\/11 08:00/)).toBeInTheDocument();
    expect(screen.getByText('2 lỗi trong 24 giờ')).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: /Thời gian phản hồi/ })).getByText('85 ms')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '7 ngày' }));
    await waitFor(() => expect(asked).toHaveLength(2));
    expect(new Date(asked[1]!).getTime() - new Date(asked[0]!).getTime()).toBe(23 * 86_400_000);
  });

  it('người không phải ADMIN mở /admin thì về trang của mình', async () => {
    server.use(...signedInHandlers());
    const { router } = renderApp('/admin/stores');
    await waitFor(() => expect(router.state.location.pathname).toBe('/plans'));
  });

  it('danh sách cửa hàng: tìm bằng Enter gửi q, sắp xếp, link sang chi tiết', async () => {
    const queries: URLSearchParams[] = [];
    server.use(
      http.get(`${API}/admin/stores`, ({ request }) => {
        queries.push(new URL(request.url).searchParams);
        return ok(page([summary, { ...summary, id: 7, name: 'Bánh mì Cô Ba', status: 'READ_ONLY' }]));
      }),
      ...adminHandlers(),
    );
    renderApp('/admin/stores');
    const user = userEvent.setup();

    expect(await screen.findByRole('link', { name: 'Tiệm Hoa Hồng' })).toHaveAttribute('href', '/admin/stores/3');
    expect(screen.getByText('TẠM KHOÁ')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Tìm theo tên cửa hàng hoặc email chủ'), 'hoa{Enter}');
    await waitFor(() => expect(queries.at(-1)?.get('q')).toBe('hoa'));
    await user.click(screen.getByRole('button', { name: /Sắp xếp theo tên/ }));
    await waitFor(() => expect(queries.at(-1)?.get('sort')).toBe('name,desc'));
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
    expect(await screen.findByText('admin@pathwise.vn: Tạm khoá cửa hàng: Chưa thanh toán')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Khoá (chỉ đọc)' }));
    const lockDialog = screen.getByRole('alertdialog', { name: 'Khoá cửa hàng Tiệm Hoa Hồng?' });
    expect(within(lockDialog).getByRole('button', { name: 'Khoá cửa hàng' })).toBeDisabled();
    await user.type(within(lockDialog).getByLabelText(/Lý do/), '  Chưa thanh toán ');
    await user.click(within(lockDialog).getByRole('button', { name: 'Khoá cửa hàng' }));

    expect(await screen.findByText('Đã khoá cửa hàng.')).toBeInTheDocument();
    expect(bodies[0]).toEqual({ reason: 'Chưa thanh toán' });
    expect(screen.getByText('Đang tạm khoá (chỉ đọc)')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Mở khoá' }));
    await user.click(
      within(screen.getByRole('dialog', { name: /Mở khoá cửa hàng/ })).getByRole('button', { name: 'Mở khoá' }),
    );
    expect(await screen.findByText('Đã mở khoá cửa hàng.')).toBeInTheDocument();
    // Lock and unlock update the page from their response: the store is not read again (no extra STORE_VIEWED).
    expect(views).toBe(1);

    await user.click(screen.getByRole('button', { name: 'Chuyển chủ cửa hàng' }));
    const transfer = screen.getByRole('alertdialog', { name: 'Chuyển chủ cửa hàng Tiệm Hoa Hồng?' });
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

  it('người dùng: mỗi trạng thái có đúng hành động; gửi link hỏi trước rồi gọi đúng API; không tự khoá mình', async () => {
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

    const table = await screen.findByRole('table', { name: 'Người dùng của nền tảng' });
    const ownerRow = (await within(table).findByText('chu@tiemhoahong.vn')).closest('tr')!;
    const adminRow = within(table).getByText('admin@pathwise.vn').closest('tr')!;
    const pendingRow = within(table).getByText('moi@gas.vn').closest('tr')!;
    expect(within(adminRow).queryByRole('button', { name: /^Khoá/ })).not.toBeInTheDocument();
    expect(within(pendingRow).getByRole('button', { name: 'Gửi lại email xác thực' })).toBeInTheDocument();
    expect(within(pendingRow).queryByRole('button', { name: 'Gửi link đặt lại mật khẩu' })).not.toBeInTheDocument();

    await user.click(within(ownerRow).getByRole('button', { name: 'Gửi link đặt lại mật khẩu' }));
    const dialog = screen.getByRole('dialog', { name: 'Gửi link đặt lại mật khẩu?' });
    expect(dialog).toHaveTextContent('mở được trong 24 giờ');
    await user.click(within(dialog).getByRole('button', { name: 'Gửi link' }));
    expect(await screen.findByText('Đã gửi link đặt lại mật khẩu cho chu@tiemhoahong.vn.')).toBeInTheDocument();
    expect(posted).toEqual(['449/password-reset-email']);
  });

  it('nhật ký: câu theo hành động, lọc hành động gửi action', async () => {
    const actions: (string | null)[] = [];
    server.use(
      http.get(`${API}/admin/audit`, ({ request }) => {
        actions.push(new URL(request.url).searchParams.get('action'));
        return ok(page([auditEvent]));
      }),
      ...adminHandlers(),
    );
    renderApp('/admin/audit?storeId=3');
    expect(await screen.findByText('Tạm khoá cửa hàng: Chưa thanh toán')).toBeInTheDocument();
    expect(screen.getByText('203.0.113.7')).toBeInTheDocument();
    expect(screen.getByLabelText('Mã cửa hàng')).toHaveValue(3);
    await userEvent.selectOptions(screen.getByLabelText('Hành động'), 'Khoá cửa hàng');
    await waitFor(() => expect(actions.at(-1)).toBe('STORE_LOCKED'));
  });
});
