import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';
import {
  planRefetchInterval,
  POLL_INTERVAL_MS,
  TRACK_INTERVAL_MS,
  type DriverLocation,
  type PlanDetail,
} from './planApi';

type Plan = PlanDetail;
const completed = () => sample<{ data: Plan }>('plan-detail-completed').data;
const optimized = () => sample<{ data: Plan }>('plan-detail-optimized').data;
const geocoded = () => sample<{ data: Plan }>('plan-detail-geocoded').data;

const driverLocations = () => sample<{ data: DriverLocation[] }>('plan-driver-locations').data;

function show(plan: Plan, locations: DriverLocation[] = [], onLocations?: () => void) {
  server.use(
    http.get(`${API}/plans/83`, () => ok(plan)),
    http.get(`${API}/plans/83/driver-locations`, () => {
      onLocations?.();
      return ok(locations);
    }),
    ...signedInHandlers(),
  );
  return renderApp('/plans/83');
}

/** The completed sample still being delivered: both routes running. */
function delivering(): Plan {
  const plan = completed();
  return { ...plan, status: 'IN_PROGRESS', routes: plan.routes.map((route) => ({ ...route, status: 'IN_PROGRESS' })) };
}

describe('Theo dõi tiến độ', () => {
  it('đếm theo kết quả, trạng thái tuyến của server, thanh tiến độ, lý do không giao được', async () => {
    show(completed());

    const results = await screen.findByRole('region', { name: 'Kết quả' });
    expect(results).toHaveTextContent('11 / 23 điểm');
    expect(results).toHaveTextContent('KHÔNG GIAO ĐƯỢC1 điểm');
    expect(results).toHaveTextContent('BỎ QUA11 điểm');
    expect(results).toHaveTextContent('1 điểm · ĐH-1024');
    expect(screen.getByText(/Kế hoạch đã kết thúc: mọi điểm giao đều đã có kết quả\./)).toBeInTheDocument();

    const routeB = screen.getByRole('region', { name: 'Tài xế B' });
    expect(within(routeB).getByText('HOÀN THÀNH')).toBeInTheDocument();
    expect(within(routeB).getByRole('img', { name: '0/12 đã giao, 1 không giao được, 11 bỏ qua' })).toBeInTheDocument();
    expect(routeB).toHaveTextContent('Lý do: Vắng nhà');
    expect(screen.queryByRole('button', { name: 'Huỷ kế hoạch' })).not.toBeInTheDocument();
  });

  it('trạng thái tuyến lấy nguyên từ server (T45), không tự tính từ các điểm', async () => {
    const plan = completed();
    // Every stop of route A has a result, but the server says IN_PROGRESS: the screen shows what the server says.
    show({
      ...plan,
      status: 'IN_PROGRESS',
      routes: plan.routes.map((route, index) => (index === 0 ? { ...route, status: 'IN_PROGRESS' } : route)),
    });

    const routeA = await screen.findByRole('region', { name: 'Tài xế A' });
    expect(within(routeA).getByText('ĐANG GIAO', { selector: '.pw-track-title *' })).toBeInTheDocument();
    expect(
      screen.getByText(/Tự tải lại mỗi 30 giây \(kết quả và vị trí tài xế\) · cập nhật lúc \d\d:\d\d/),
    ).toBeInTheDocument();
  });

  it('plan đã giao việc huỷ được, nêu hậu quả với tài xế', async () => {
    const plan = optimized();
    show({ ...plan, status: 'PUBLISHED' });

    const cancel = await screen.findByRole('button', { name: 'Huỷ kế hoạch' });
    cancel.click();
    expect(await screen.findByRole('alertdialog')).toHaveTextContent('Tài xế sẽ không còn thấy tuyến của kế hoạch này');
  });
});

describe('Vị trí tài xế (T32)', () => {
  it('vẽ mũi tên theo hướng đi, ghi giờ cập nhật, nêu lý do không có vị trí kèm số để gọi', async () => {
    const { container } = show(delivering(), driverLocations());

    const routeA = await screen.findByRole('region', { name: 'Tài xế A' });
    expect(await within(routeA).findByText('Vị trí cập nhật vừa xong (11:01)')).toBeInTheDocument();
    const routeB = screen.getByRole('region', { name: 'Tài xế B' });
    expect(routeB).toHaveTextContent('Tài xế B tắt chia sẻ vị trí cho tuyến này · gọi 0905 123 019 để nhắc');

    const arrows = container.querySelectorAll('.pw-driver-marker');
    expect(arrows).toHaveLength(1);
    const arrow = arrows[0] as HTMLElement;
    expect(arrow.getAttribute('title')).toMatch(/^Tài xế A · .+, vị trí cập nhật vừa xong$/);
    expect(arrow.querySelector('g')).toHaveAttribute('transform', 'rotate(90 20 20)');
    expect(arrow.querySelector('.pw-driver-r1')).toBeInTheDocument();
    expect(screen.getByText('Vị trí tài xế (mũi tên chỉ hướng đi)')).toBeInTheDocument();

    arrow.click();
    const status = await screen.findByText(/^Tài xế A · .+ · vị trí lúc 11:01 \(vừa xong\)$/);
    expect(status.parentElement).toHaveTextContent('Sai số khoảng 12 m · 28 km/h');
  });

  it.each([
    ['NO_PERMISSION', 'Máy của Tài xế B chưa cho PathWise dùng vị trí'],
    ['GPS_OFF', 'GPS trên máy của Tài xế B đang tắt'],
    ['SHARING', 'Chưa có vị trí trong 10 phút qua (có thể mất sóng)'],
    [null, 'Chưa có vị trí trong 10 phút qua (có thể mất sóng)'],
  ] as const)('trạng thái %s → "%s"', async (state, text) => {
    const [a, b] = driverLocations();
    show(delivering(), [
      a,
      { ...b, sharing: state ? { state, updatedAt: '2026-12-01T03:40:05Z' } : null },
    ] as DriverLocation[]);

    const routeB = await screen.findByRole('region', { name: 'Tài xế B' });
    expect(await within(routeB).findByText(`${text} · gọi 0905 123 019 để nhắc`)).toBeInTheDocument();
  });

  it('tuyến đã xong không hiện vị trí hay lý do; tài xế không có số thì không ghi "gọi"', async () => {
    const plan = delivering();
    const [a, b] = driverLocations();
    const routes = plan.routes.map((route, index) =>
      index === 0
        ? { ...route, status: 'COMPLETED' as const }
        : { ...route, driver: { ...route.driver, phoneNumber: null } },
    );
    const { container } = show(
      { ...plan, routes },
      [a, b].map((found) => ({ ...found, sharing: null })) as DriverLocation[],
    );
    const routeB = await screen.findByRole('region', { name: 'Tài xế B' });
    expect(await within(routeB).findByText('Chưa có vị trí trong 10 phút qua (có thể mất sóng)')).toBeInTheDocument();

    expect(
      within(screen.getByRole('region', { name: 'Tài xế A' })).queryByText(/Vị trí cập nhật/),
    ).not.toBeInTheDocument();
    expect(container.querySelectorAll('.pw-driver-marker')).toHaveLength(0);
  });

  it('plan đã kết thúc không đọc vị trí', async () => {
    let calls = 0;
    show(completed(), driverLocations(), () => (calls += 1));

    expect(await screen.findByText(/Kế hoạch đã kết thúc/)).toBeInTheDocument();
    expect(screen.queryByText('Vị trí tài xế (mũi tên chỉ hướng đi)')).not.toBeInTheDocument();
    expect(calls).toBe(0);
  });
});

describe('Một route cho mọi trạng thái', () => {
  it.each([
    ['DRAFT chưa tối ưu → tìm toạ độ', geocoded, /chưa tối ưu$/],
    ['DRAFT đã tối ưu → kết quả', optimized, /Tối ưu lúc/],
    ['COMPLETED → theo dõi', completed, /Kế hoạch đã kết thúc/],
    [
      'CANCELLED → theo dõi kèm thông báo',
      () => ({ ...optimized(), status: 'CANCELLED' as const }),
      /^Kế hoạch đã huỷ\.$/,
    ],
  ])('%s', async (_name, plan, subtitle) => {
    show(plan());
    expect(await screen.findByText(subtitle)).toBeInTheDocument();
  });

  it('tải lại: 2 s khi đang xử lý, 30 s khi đang giao (chỉ khi tab hiện), dừng khi kết thúc', () => {
    const plan = optimized();
    expect(planRefetchInterval({ ...plan, processingStatus: 'OPTIMIZING' })).toBe(POLL_INTERVAL_MS);
    expect(planRefetchInterval({ ...plan, status: 'PUBLISHED' })).toBe(TRACK_INTERVAL_MS);
    expect(planRefetchInterval({ ...plan, status: 'IN_PROGRESS' })).toBe(TRACK_INTERVAL_MS);
    expect(planRefetchInterval(plan)).toBe(false);
    expect(planRefetchInterval({ ...plan, status: 'COMPLETED' })).toBe(false);
    expect(planRefetchInterval({ ...plan, status: 'CANCELLED' })).toBe(false);
  });
});
