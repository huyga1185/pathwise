import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { fromMore } from '@/test/moreMenu';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';
import {
  FIRST_POLL_MS,
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

    // The whole plan: a bar and the same in words (G5-Q13); the stop no route took in its own group (G5.10).
    expect(await screen.findByText('11/23 đã giao · 1 không giao được · 11 bỏ qua')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: 'Tiến độ cả kế hoạch: 11/23 đã giao, 1 không giao được, 11 bỏ qua' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Chưa xếp tuyến' })).toHaveTextContent('ĐH-1024');
    expect(screen.getByText(/Kế hoạch đã kết thúc: mọi điểm giao đều đã có kết quả\./)).toBeInTheDocument();

    const routeB = screen.getByRole('region', { name: 'Tài xế B' });
    expect(within(routeB).getByText('HOÀN THÀNH')).toBeInTheDocument();
    expect(within(routeB).getByRole('img', { name: '0/12 đã giao, 1 không giao được, 11 bỏ qua' })).toBeInTheDocument();
    expect(routeB).toHaveTextContent('Lý do: Vắng nhà');
    // Ended: the More menu has only "Sao chép" (G5-Q12).
    await userEvent.setup().click(screen.getByRole('button', { name: 'Thao tác khác' }));
    expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual(['Sao chép']);
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
    expect(within(routeA).getByText('ĐANG GIAO')).toBeInTheDocument();
    expect(
      screen.getByText(/Tự tải lại mỗi 30 giây \(kết quả và vị trí tài xế\) · cập nhật lúc \d\d:\d\d/),
    ).toBeInTheDocument();
  });

  it('plan đã giao việc huỷ được, nêu hậu quả với tài xế', async () => {
    const plan = optimized();
    show({ ...plan, status: 'PUBLISHED' });

    await fromMore(userEvent.setup(), 'Huỷ kế hoạch');
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

    // The driver's card (G5-Q14).
    arrow.click();
    const title = await screen.findByRole('heading', { name: 'Tài xế A', level: 2 });
    expect(title).toHaveFocus();
    const card = title.closest('section') as HTMLElement;
    expect(card).toHaveTextContent('Vị trí cập nhật vừa xong (11:01)');
    expect(card).toHaveTextContent('Sai số12 m');
    expect(card).toHaveTextContent('Tốc độ28 km/h');
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

  it('tải lại: 0,5 s rồi 2 s khi đang xử lý, 30 s khi đang giao (chỉ khi tab hiện), dừng khi kết thúc', () => {
    const plan = optimized();
    // A task just started: the first read comes after 0.5 s, then every 2 s.
    const busy = { ...plan, processingStatus: 'OPTIMIZING' as const };
    expect(planRefetchInterval(busy, 1000)).toBe(FIRST_POLL_MS);
    expect(planRefetchInterval(busy, 1600)).toBe(POLL_INTERVAL_MS);
    expect(planRefetchInterval(plan, 3700)).toBe(false);
    expect(planRefetchInterval({ ...plan, status: 'PUBLISHED' })).toBe(TRACK_INTERVAL_MS);
    expect(planRefetchInterval({ ...plan, status: 'IN_PROGRESS' })).toBe(TRACK_INTERVAL_MS);
    expect(planRefetchInterval(plan)).toBe(false);
    expect(planRefetchInterval({ ...plan, status: 'COMPLETED' })).toBe(false);
    expect(planRefetchInterval({ ...plan, status: 'CANCELLED' })).toBe(false);
  });
});
