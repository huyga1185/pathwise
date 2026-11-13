import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, server, signedInHandlers } from '@/test/server';

type Stop = Record<string, unknown> & { id: number };
type Route = Record<string, unknown> & { id: number; stops: Stop[] };
type Plan = Record<string, unknown> & {
  id: number;
  version: number;
  routes: Route[];
  unassignedStops: Stop[];
  violations: unknown[];
};
const optimized = () => sample<{ data: Plan }>('plan-detail-optimized').data;

function planHandlers(state: { plan: Plan }) {
  return [http.get(`${API}/plans/83`, () => ok(state.plan)), ...signedInHandlers()];
}

/** Opens the plan and turns on manual editing. */
async function startEditing() {
  renderApp('/plans/83');
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'Chỉnh tay' }));
  return user;
}

describe('Chỉnh tay tuyến (W10)', () => {
  it('chuyển điểm sang tuyến khác bằng menu: cột cập nhật ngay, "Lưu" gửi mọi tuyến và hiện vi phạm', async () => {
    const state = { plan: optimized() };
    let body: unknown;
    server.use(
      http.put(`${API}/plans/83/routes`, async ({ request }) => {
        body = await request.json();
        state.plan = {
          ...state.plan,
          version: 5,
          violations: [
            { code: 'CAPACITY_EXCEEDED', routeId: 63, stopId: null, excess: 3 },
            { code: 'TIME_WINDOW_MISSED', routeId: 64, stopId: 122, excess: 700 },
          ],
        };
        return ok(state.plan);
      }),
      ...planHandlers(state),
    );
    const user = await startEditing();

    expect(screen.getByRole('region', { name: 'Chế độ chỉnh tay' })).toBeInTheDocument();
    const save = screen.getByRole('button', { name: 'Lưu thay đổi' });
    expect(save).toBeDisabled();
    const columnA = screen.getByRole('region', { name: 'Tài xế A' });
    expect(columnA).toHaveTextContent('11 điểm · tải 22/30 thùng');

    await user.selectOptions(screen.getByLabelText('Chuyển ĐH-1009 · Chị Lan sang'), 'Tài xế A');
    expect(screen.getByRole('region', { name: 'Tài xế A' })).toHaveTextContent('12 điểm · tải 24/30 thùng');
    expect(screen.getByRole('region', { name: 'Tài xế B' })).toHaveTextContent('11 điểm');
    expect(save).toBeEnabled();

    await user.click(save);

    expect(await screen.findByText('Đã lưu tuyến chỉnh tay.')).toBeInTheDocument();
    const sent = body as {
      version: number;
      routes: { routeId: number; stopIds: number[] }[];
      unassignedStopIds: number[];
    };
    expect(sent.version).toBe(4);
    expect(sent.routes.map((route) => route.routeId)).toEqual([63, 64]);
    expect(sent.routes[0]?.stopIds.at(-1)).toBe(116);
    expect(sent.routes[1]?.stopIds).not.toContain(116);
    expect(sent.unassignedStopIds).toEqual([131]);

    const violations = screen.getByRole('region', { name: '2 vi phạm sau khi chỉnh tay' });
    expect(violations).toHaveTextContent('Tài xế A · Xe 1: vượt tải 3 thùng');
    expect(violations).toHaveTextContent('ĐH-1015 · Anh Khoa: tới muộn 12 phút so với khung giờ');
    expect(within(screen.getByRole('region', { name: 'Tài xế B' })).getByText('Trễ 12 phút')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Chế độ chỉnh tay' })).not.toBeInTheDocument();
  });

  it('"Bỏ ra khỏi tuyến" đưa điểm vào cột chưa xếp; "Huỷ" khi đã đổi thì hỏi rồi trả về như cũ', async () => {
    const state = { plan: optimized() };
    server.use(...planHandlers(state));
    const user = await startEditing();

    await user.selectOptions(screen.getByLabelText('Chuyển ĐH-1016 · Chị Thảo sang'), 'Bỏ ra khỏi tuyến');
    expect(screen.getByRole('region', { name: 'Chưa xếp tuyến' })).toHaveTextContent('2 điểm');

    await user.click(screen.getByRole('button', { name: 'Huỷ' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Bỏ các thay đổi chưa lưu?' });
    await user.click(within(dialog).getByRole('button', { name: 'Bỏ thay đổi' }));

    expect(screen.queryByRole('region', { name: 'Chế độ chỉnh tay' })).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Tài xế A' })).toHaveTextContent('11 điểm · 8,3 km');
  });

  it('máy chủ không tính lại được (502): báo, giữ nguyên bản nháp để lưu lại', async () => {
    const state = { plan: optimized() };
    server.use(
      http.put(`${API}/plans/83/routes`, () => fail(502, 'ROUTING_PROVIDER_ERROR')),
      ...planHandlers(state),
    );
    const user = await startEditing();

    await user.selectOptions(screen.getByLabelText('Chuyển ĐH-1009 · Chị Lan sang'), 'Tài xế A');
    await user.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    expect(await screen.findByText('Không tính lại được giờ và quãng đường, thử lại.')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Tài xế A' })).toHaveTextContent('12 điểm');
    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeEnabled();
  });

  it('điểm bỏ ra hiện "Đã bỏ ra"; giao việc khi còn vi phạm liệt kê và hỏi "Vẫn giao việc"', async () => {
    const plan = optimized();
    const state = {
      plan: {
        ...plan,
        unassignedStops: plan.unassignedStops.map((stop) => ({ ...stop, dropReason: 'MANUAL' })),
        violations: [{ code: 'SHIFT_EXCEEDED', routeId: 64, stopId: null, excess: 1500 }],
      },
    };
    server.use(...planHandlers(state));
    renderApp('/plans/83');
    const user = userEvent.setup();

    expect(
      await screen.findByRole('heading', { name: '1 điểm đã bỏ ra khỏi tuyến: ĐH-1024 · Chị Thảo' }),
    ).toBeInTheDocument();
    expect(screen.getByText('ĐÃ BỎ RA')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Tài xế B' })).getByText('Vi phạm')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Giao việc cho 2 tài xế' }));
    const dialog = screen.getByRole('dialog', { name: 'Giao việc cho 2 tài xế?' });
    expect(dialog).toHaveTextContent('Tài xế B · Xe 2: về muộn 25 phút so với ca');
    expect(within(dialog).getByRole('button', { name: 'Vẫn giao việc' })).toBeInTheDocument();
  });

  it('"Tối ưu lại" khi đã có chỉnh tay thì hỏi trước', async () => {
    const plan = optimized();
    const state = {
      plan: { ...plan, violations: [{ code: 'MAX_DISTANCE_EXCEEDED', routeId: 63, stopId: null, excess: 4200 }] },
    };
    server.use(...planHandlers(state));
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Tối ưu lại' }));

    const dialog = screen.getByRole('dialog', { name: 'Tối ưu lại kế hoạch?' });
    expect(dialog).toHaveTextContent('Kết quả tối ưu mới sẽ thay các chỉnh tay.');
    expect(screen.getByText('Tài xế A · Xe 1: vượt 4,2 km quãng đường tối đa')).toBeInTheDocument();
  });
});
