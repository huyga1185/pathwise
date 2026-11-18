import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { wideWindow } from '@/test/media';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';

type Stop = Record<string, unknown> & { id: number; geocodeStatus: string; lat: number | null; lon: number | null };
type Plan = Record<string, unknown> & {
  id: number;
  version: number;
  processingStatus: string;
  geocodingSummary: { total: number; resolved: number; provided: number; pending: number; failed: number };
  unassignedStops: Stop[];
};

const geocoded = () => sample<{ data: Plan }>('plan-detail-geocoded').data;
const job = {
  id: 9,
  type: 'OPTIMIZE_PLAN',
  status: 'SUCCEEDED',
  planId: 83,
  errorCode: null,
  createdAt: '2026-11-03T09:17:26Z',
  startedAt: null,
  finishedAt: null,
};

/** The plan with stop ĐH-1008 (id 115) placed by a pin: every stop has coordinates. */
function fixed(plan: Plan, version: number): Plan {
  return {
    ...plan,
    version,
    geocodingSummary: { ...plan.geocodingSummary, provided: 5, failed: 0 },
    unassignedStops: plan.unassignedStops.map((stop) =>
      stop.id === 115 ? { ...stop, geocodeStatus: 'PROVIDED', geocodeError: null, lat: 16.0544, lon: 108.2022 } : stop,
    ),
  };
}

describe('Soạn plan — tìm toạ độ', () => {
  it('kế hoạch nháp cũng có nút "Sao chép" (sao chép được ở mọi trạng thái)', async () => {
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    expect(await screen.findByRole('button', { name: 'Sao chép' })).toBeInTheDocument();
  });

  it('còn điểm không tìm thấy: cảnh báo, khoá "Tối ưu lộ trình", mở sẵn điểm lỗi kèm lý do', async () => {
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');

    expect(await screen.findByRole('heading', { name: 'Giao sáng 01/12', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('Thứ Ba, 01/12/2026 · 24 điểm giao · 2 tài xế · chưa tối ưu')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Còn 1 điểm giao chưa có toạ độ. Sửa địa chỉ hoặc đặt ghim trên bản đồ.' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Toạ độ: 19 tìm thấy · 4 nhập sẵn · 1 không tìm thấy · tổng 24')).toBeInTheDocument();
    const optimize = screen.getByRole('button', { name: 'Tối ưu lộ trình' });
    expect(optimize).toBeDisabled();
    expect(optimize).toHaveAccessibleDescription('Sửa 1 điểm chưa có toạ độ để tối ưu.');

    const panel = screen.getByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    expect(
      within(panel).getByText('Không tìm thấy địa chỉ này. Sửa địa chỉ hoặc đặt ghim trên bản đồ.'),
    ).toBeInTheDocument();
    expect(within(panel).getByText('KHÔNG TÌM THẤY')).toBeInTheDocument();
  });

  it('đặt ghim cho điểm lỗi rồi lưu: PATCH gửi version và toạ độ; xong thì tối ưu được', async () => {
    let current = geocoded();
    const patches: unknown[] = [];
    const optimizes: unknown[] = [];
    server.use(
      http.get(`${API}/plans/83`, () => ok(current)),
      http.patch(`${API}/plans/83/stops/115`, async ({ request }) => {
        patches.push(await request.json());
        current = fixed(current, 2);
        return ok(current);
      }),
      http.post(`${API}/plans/83/optimize`, async ({ request }) => {
        optimizes.push(await request.json());
        return ok(job, { status: 202 });
      }),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    const panel = await screen.findByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    await user.click(screen.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
    expect(within(panel).getByRole('status')).toHaveTextContent(
      /^Ghim tại 16,\d{5} \/ 108,\d{5}\. Bấm chỗ khác để đổi\.$/,
    );
    await user.click(within(panel).getByRole('button', { name: 'Lưu toạ độ' }));

    await waitFor(() => expect(patches).toHaveLength(1));
    expect(patches[0]).toMatchObject({ version: 1, lat: expect.any(Number), lon: expect.any(Number) });
    const optimize = screen.getByRole('button', { name: 'Tối ưu lộ trình' });
    await waitFor(() => expect(optimize).toBeEnabled());
    expect(optimize).toHaveAccessibleDescription('Mọi điểm giao đã có toạ độ.');

    await user.click(optimize);
    await waitFor(() => expect(optimizes).toEqual([{ version: 2 }]));
  });

  it('sửa địa chỉ rồi "Tìm lại địa chỉ này" gửi PATCH địa chỉ; giữ nguyên địa chỉ thì gọi geocode lại', async () => {
    const calls: Array<[string, unknown]> = [];
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      http.patch(`${API}/plans/83/stops/115`, async ({ request }) => {
        calls.push(['patch', await request.json()]);
        return ok(geocoded());
      }),
      http.post(`${API}/plans/83/geocode`, async ({ request }) => {
        calls.push(['geocode', await request.json()]);
        return ok({ ...job, type: 'GEOCODE_PLAN' }, { status: 202 });
      }),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    const panel = await screen.findByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    await user.click(within(panel).getByRole('button', { name: 'Tìm lại địa chỉ này' }));
    await waitFor(() => expect(calls).toHaveLength(1));
    const address = within(panel).getByLabelText('Địa chỉ');
    await user.clear(address);
    await user.type(address, '886 Điện Biên Phủ, Đà Nẵng');
    await user.click(within(panel).getByRole('button', { name: 'Tìm lại địa chỉ này' }));

    await waitFor(() => expect(calls).toHaveLength(2));
    expect(calls).toEqual([
      ['geocode', { version: 1 }],
      ['patch', { version: 1, address: '886 Điện Biên Phủ, Đà Nẵng' }],
    ]);
  });

  it('polling khi GEOCODING: thanh tiến độ, khoá nút sửa; dừng khi IDLE và dùng version mới', async () => {
    const base = geocoded();
    // GEOCODING with 6, then 2 addresses left, then IDLE: the geocode job ended and version went 0 → 1.
    const states: Plan[] = [
      {
        ...base,
        version: 0,
        processingStatus: 'GEOCODING',
        geocodingSummary: { ...base.geocodingSummary, resolved: 13, failed: 0, pending: 7 },
      },
      {
        ...base,
        version: 0,
        processingStatus: 'GEOCODING',
        geocodingSummary: { ...base.geocodingSummary, resolved: 18, failed: 0, pending: 2 },
      },
      base,
    ];
    let reads = 0;
    const patches: unknown[] = [];
    server.use(
      http.get(`${API}/plans/83`, () => {
        const state = states[Math.min(reads, states.length - 1)] as Plan;
        reads += 1;
        return ok(state);
      }),
      http.patch(`${API}/plans/83/stops/115`, async ({ request }) => {
        patches.push(await request.json());
        return ok(base);
      }),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    const bar = await screen.findByRole('progressbar', { name: 'Đang tìm toạ độ cho các địa chỉ' });
    expect(bar).toHaveAttribute('aria-valuenow', '17');
    expect(screen.getByRole('button', { name: 'Sửa điểm giao' })).toBeDisabled();
    // The progress bar says it; the title has no static label for a running task (HIG Progress indicators).
    expect(screen.queryByText('ĐANG TÌM TOẠ ĐỘ', { selector: '.pw-plan-title *' })).not.toBeInTheDocument();

    await waitFor(() => expect(bar).toHaveAttribute('aria-valuenow', '22'), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByRole('progressbar')).not.toBeInTheDocument(), { timeout: 5000 });
    expect(screen.getByRole('button', { name: 'Sửa điểm giao' })).toBeEnabled();
    const readsWhenIdle = reads;
    await new Promise((resolve) => setTimeout(resolve, 2500));
    expect(reads).toBe(readsWhenIdle);

    const panel = screen.getByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    await user.click(screen.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
    await user.click(within(panel).getByRole('button', { name: 'Lưu toạ độ' }));
    await waitFor(() => expect(patches).toHaveLength(1));
    expect(patches[0]).toMatchObject({ version: 1 });
  }, 20000);
});

describe('Chưa có phân công (§9.7)', () => {
  it('hiện khối "Chưa có tài xế nào được phân công." với nút sang form sửa; lý do khoá chỉ cho trình đọc màn hình', async () => {
    server.use(
      http.get(`${API}/plans/83`, () => ok({ ...geocoded(), routes: [] })),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans/83');
    const user = userEvent.setup();

    const block = await screen.findByRole('region', { name: 'Chưa có tài xế nào được phân công.' });
    expect(screen.getByRole('button', { name: 'Tối ưu lộ trình' })).toHaveAccessibleDescription(
      'Thêm phân công tài xế để tối ưu.',
    );
    await user.click(within(block).getByRole('button', { name: 'Thêm phân công' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/plans/83/edit'));
  });
});

describe('Nút quay lại (§9.11)', () => {
  it('điện thoại: ở đầu thanh trên, ngang hàng nút tài khoản; màn rộng: trong trang', async () => {
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      ...signedInHandlers(),
    );
    // (The tab bar also has a "Kế hoạch" link: the back button is the one with the chevron, .pw-back-link.)
    const phone = renderApp('/plans/83');
    await screen.findByRole('heading', { name: 'Giao sáng 01/12', level: 1 });
    const backs = document.querySelectorAll('a.pw-back-link');
    expect(backs).toHaveLength(1);
    expect(backs[0]?.closest('.pw-topbar')).not.toBeNull();
    expect(backs[0]).toHaveAccessibleName('Kế hoạch');
    phone.unmount();

    wideWindow();
    renderApp('/plans/83');
    await screen.findByRole('heading', { name: 'Giao sáng 01/12', level: 1 });
    expect(document.querySelector('a.pw-back-link')?.closest('main')).not.toBeNull();
  });
});

describe('Ba nút đầu trang trên màn hẹp (§9.12)', () => {
  it('dưới 440px: xếp dọc, nút chính đầu tiên (cả thứ tự trong trang, để Tab theo đúng thứ tự nhìn thấy)', async () => {
    vi.spyOn(window, 'matchMedia').mockImplementation(
      (query: string) =>
        ({
          matches: query.includes('max-width: 439px'),
          media: query,
          addEventListener: () => {},
          removeEventListener: () => {},
        }) as unknown as MediaQueryList,
    );
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    await screen.findByRole('heading', { name: 'Giao sáng 01/12', level: 1 });
    const actions = document.querySelector('.pw-plan-actions') as HTMLElement;
    expect(actions).toHaveClass('is-stacked');
    expect(
      within(actions)
        .getAllByRole('button')
        .map((button) => button.textContent),
    ).toEqual(['Tối ưu lộ trình', 'Sửa điểm giao', 'Sao chép']);
  });
});

describe('Tìm toạ độ trên bản đồ (G5.15)', () => {
  it('mở sẵn thẻ điểm lỗi (focus không bị kéo vào); quay lại thì là danh sách, không tự mở lại; bấm hàng thì vào thẻ', async () => {
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    const title = await screen.findByRole('heading', { name: 'Sửa ĐH-1008 · Chị Thảo', level: 2 });
    expect(title).not.toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Điểm giao' }));
    const row = await screen.findByRole('button', { name: 'ĐH-1008 · Chị Thảo: xem chi tiết' });
    expect(screen.queryByRole('heading', { name: 'Sửa ĐH-1008 · Chị Thảo' })).not.toBeInTheDocument();

    await user.click(row);
    expect(await screen.findByRole('heading', { name: 'Sửa ĐH-1008 · Chị Thảo', level: 2 })).toHaveFocus();
  });
});

describe('Tìm lại một địa chỉ (G4 §9.5)', () => {
  it('trong lúc chờ: "Đang tìm toạ độ…" cạnh nút; bản đồ và các nút giữ nguyên (không tắt, không mất)', async () => {
    let answer: () => void = () => {};
    server.use(
      http.get(`${API}/plans/83`, () => ok(geocoded())),
      http.post(`${API}/plans/83/geocode`, () => new Promise((resolve) => (answer = () => resolve(ok(geocoded()))))),
      ...signedInHandlers(),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    const panel = await screen.findByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    const retry = within(panel).getByRole('button', { name: 'Tìm lại địa chỉ này' });
    await user.click(retry);
    expect(within(panel).getByText('Đang tìm toạ độ…')).toBeInTheDocument();
    expect(retry).toBeEnabled();
    expect(retry).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' })).toBeEnabled();
    answer();
    await waitFor(() => expect(within(panel).queryByText('Đang tìm toạ độ…')).not.toBeInTheDocument());
  });
});
