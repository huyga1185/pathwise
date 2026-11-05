import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';
import { planRefetchInterval, POLL_INTERVAL_MS, TRACK_INTERVAL_MS, type PlanDetail } from './planApi';

type Plan = PlanDetail;
const completed = () => sample<{ data: Plan }>('plan-detail-completed').data;
const optimized = () => sample<{ data: Plan }>('plan-detail-optimized').data;
const geocoded = () => sample<{ data: Plan }>('plan-detail-geocoded').data;

function show(plan: Plan) {
  server.use(
    http.get(`${API}/plans/83`, () => ok(plan)),
    ...signedInHandlers(),
  );
  return renderApp('/plans/83');
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
    expect(screen.getByText(/Tự tải lại mỗi 30 giây · cập nhật lúc \d\d:\d\d/)).toBeInTheDocument();
  });

  it('plan đã giao việc huỷ được, nêu hậu quả với tài xế', async () => {
    const plan = optimized();
    show({ ...plan, status: 'PUBLISHED' });

    const cancel = await screen.findByRole('button', { name: 'Huỷ kế hoạch' });
    cancel.click();
    expect(await screen.findByRole('alertdialog')).toHaveTextContent('Tài xế sẽ không còn thấy tuyến của kế hoạch này');
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
