import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
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
    await user.click(within(panel).getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
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
    expect(screen.getByText('ĐANG TÌM TOẠ ĐỘ', { selector: '.pw-plan-title *' })).toBeInTheDocument();

    await waitFor(() => expect(bar).toHaveAttribute('aria-valuenow', '22'), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByRole('progressbar')).not.toBeInTheDocument(), { timeout: 5000 });
    expect(screen.getByRole('button', { name: 'Sửa điểm giao' })).toBeEnabled();
    const readsWhenIdle = reads;
    await new Promise((resolve) => setTimeout(resolve, 2500));
    expect(reads).toBe(readsWhenIdle);

    const panel = screen.getByRole('region', { name: 'Sửa ĐH-1008 · Chị Thảo' });
    await user.click(within(panel).getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
    await user.click(within(panel).getByRole('button', { name: 'Lưu toạ độ' }));
    await waitFor(() => expect(patches).toHaveLength(1));
    expect(patches[0]).toMatchObject({ version: 1 });
  }, 20000);
});
