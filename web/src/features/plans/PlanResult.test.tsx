import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, server, signedInHandlers } from '@/test/server';

type Plan = Record<string, unknown> & { id: number; version: number; status: string; processingStatus: string };
const optimized = () => sample<{ data: Plan }>('plan-detail-optimized').data;
const job = (status: string, errorCode: string | null = null) => ({
  id: 12,
  type: 'OPTIMIZE_PLAN',
  status,
  planId: 83,
  errorCode,
  createdAt: '2026-11-03T09:20:00Z',
  startedAt: null,
  finishedAt: null,
});

/** The plan the server holds; actions change it like the server would. */
function planHandlers(state: { plan: Plan }) {
  return [http.get(`${API}/plans/83`, () => ok(state.plan)), ...signedInHandlers()];
}

describe('Soạn plan — sau khi tối ưu', () => {
  it('tổng quan, cảnh báo điểm không xếp được, khối tuyến thu gọn được, chọn dòng thì chọn marker', async () => {
    server.use(...planHandlers({ plan: optimized() }));
    const { container } = renderApp('/plans/83');
    const user = userEvent.setup();

    const overview = await screen.findByRole('region', { name: 'Tổng quan' });
    expect(overview).toHaveTextContent('23/24 đã xếp tuyến');
    expect(overview).toHaveTextContent('15,4 km');
    expect(overview).toHaveTextContent('5 giờ 06 phút');
    expect(overview).toHaveTextContent('2 tài xế · 2 xe');
    expect(screen.getByRole('heading', { name: '1 điểm giao chưa có tài xế: ĐH-1024 · Chị Thảo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Giao việc cho 2 tài xế' })).toBeEnabled();

    const routeA = screen.getByRole('region', { name: 'Tài xế A' });
    expect(routeA).toHaveTextContent('Xe 1 · 30 thùng');
    expect(routeA).toHaveTextContent('11 điểm · 8,3 km');
    expect(routeA).toHaveTextContent('10:50–13:23 · tải 22/30 thùng');
    const table = within(routeA).getByRole('table', { name: 'Điểm giao của Tài xế A' });
    const firstRow = within(table).getAllByRole('row')[1] as HTMLElement;
    await user.click(within(firstRow).getByRole('button'));
    expect(firstRow).toHaveClass('pw-row-selected');
    expect(container.querySelector('.pw-stop-marker .pw-marker-selected')).toHaveTextContent('1');
    expect(screen.getByRole('region', { name: 'Bản đồ tuyến' })).toHaveTextContent(/Tài xế A · điểm 1:/);

    await user.click(within(routeA).getByRole('button', { name: /Thu gọn/ }));
    expect(within(routeA).queryByRole('table')).not.toBeInTheDocument();
    expect(within(routeA).getByRole('button', { name: /Mở danh sách/ })).toHaveAttribute('aria-expanded', 'false');
  });

  it('giao việc: hộp xác nhận nêu từng tài xế và điểm bỏ lại; xong thì toast và sang theo dõi', async () => {
    const state = { plan: optimized() };
    let body: unknown;
    server.use(
      http.post(`${API}/plans/83/publish`, async ({ request }) => {
        body = await request.json();
        state.plan = { ...state.plan, status: 'PUBLISHED', version: 5 };
        return ok(state.plan);
      }),
      ...planHandlers(state),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Giao việc cho 2 tài xế' }));
    const dialog = screen.getByRole('dialog', { name: 'Giao việc cho 2 tài xế?' });
    expect(dialog).toHaveTextContent('Tài xế A · Xe 1 · 11 điểm · 10:50–13:23');
    expect(dialog).toHaveTextContent('ĐH-1024 · Chị Thảo chưa có tài xế nên sẽ không được giao.');
    await user.click(within(dialog).getByRole('button', { name: 'Giao việc' }));

    expect(await screen.findByText('Đã giao việc cho 2 tài xế.')).toBeInTheDocument();
    expect(body).toEqual({ version: 4 });
    expect(screen.getByText(/Tự tải lại mỗi 30 giây/)).toBeInTheDocument();
    expect(screen.getByText('ĐÃ GIAO VIỆC')).toBeInTheDocument();
  });

  it('trùng ca: liệt kê phân công trùng kèm link plan kia; giao diện vẫn là plan nháp', async () => {
    server.use(
      http.post(`${API}/plans/83/publish`, () =>
        fail(422, 'SHIFT_OVERLAP', [{ field: 'assignments[1].driverId', code: 'SHIFT_OVERLAP', conflictPlanId: 84 }]),
      ),
      ...planHandlers({ plan: optimized() }),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Giao việc cho 2 tài xế' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Giao việc' }));

    const overlap = await screen.findByRole('dialog', { name: 'Chưa giao việc được: trùng ca làm' });
    expect(overlap).toHaveTextContent('Ca làm của Tài xế B (08:00–17:00, ngày 01/12) trùng với một kế hoạch khác');
    expect(within(overlap).getByRole('link', { name: 'Mở kế hoạch #84' })).toHaveAttribute('href', '/plans/84');
    await user.click(within(overlap).getByRole('button', { name: 'Đóng' }));

    expect(screen.getByText('NHÁP')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Giao việc cho 2 tài xế' })).toBeEnabled();
    expect(screen.queryByText(/Đã giao việc/)).not.toBeInTheDocument();
  });

  it('tối ưu lại mà job thất bại (NO_SOLUTION): báo câu theo UX_BRIEF, giữ kết quả cũ', async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/plans/83/optimize`, async ({ request }) => {
        body = await request.json();
        return ok(job('FAILED', 'NO_SOLUTION'), { status: 202 });
      }),
      ...planHandlers({ plan: optimized() }),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Tối ưu lại' }));

    expect(
      await screen.findByText(/Không tìm được lộ trình, ca làm hoặc quãng đường tối đa có thể quá chặt/),
    ).toBeInTheDocument();
    expect(body).toEqual({ version: 4 });
    expect(screen.getByRole('region', { name: 'Tài xế A' })).toBeInTheDocument();
  });

  it('tối ưu lại chạy nền: theo dõi job tới khi xong, OPTIMIZING thì khoá các nút', async () => {
    const state = { plan: optimized() };
    let jobReads = 0;
    server.use(
      http.post(`${API}/plans/83/optimize`, () => {
        state.plan = { ...state.plan, processingStatus: 'OPTIMIZING', version: 5 };
        return ok(job('QUEUED'), { status: 202 });
      }),
      http.get(`${API}/jobs/12`, () => {
        jobReads += 1;
        state.plan = { ...state.plan, processingStatus: 'IDLE', version: 6 };
        return ok(job('FAILED', 'ROUTING_PROVIDER_ERROR'));
      }),
      ...planHandlers(state),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Tối ưu lại' }));
    expect(await screen.findByRole('progressbar', { name: 'Đang tối ưu lộ trình' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Giao việc cho 2 tài xế' })).toBeDisabled();

    expect(await screen.findByText(/Dịch vụ bản đồ đang gặp sự cố/, {}, { timeout: 6000 })).toBeInTheDocument();
    expect(jobReads).toBeGreaterThan(0);
    await waitFor(() => expect(screen.queryByRole('progressbar')).not.toBeInTheDocument(), { timeout: 6000 });
  }, 15000);

  it('huỷ plan nháp: hộp xác nhận danger, xong thì báo đã huỷ', async () => {
    const state = { plan: optimized() };
    let body: unknown;
    server.use(
      http.post(`${API}/plans/83/cancel`, async ({ request }) => {
        body = await request.json();
        state.plan = { ...state.plan, status: 'CANCELLED', version: 5 };
        return ok(state.plan);
      }),
      ...planHandlers(state),
    );
    renderApp('/plans/83');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Huỷ kế hoạch' }));
    const dialog = screen.getByRole('alertdialog', { name: 'Huỷ kế hoạch "Giao sáng 01/12"?' });
    expect(dialog).toHaveTextContent('Kế hoạch chưa giao việc nên tài xế chưa thấy tuyến nào.');
    await user.click(within(dialog).getByRole('button', { name: 'Huỷ kế hoạch' }));

    expect(await screen.findByText('Kế hoạch đã huỷ.')).toBeInTheDocument();
    expect(body).toEqual({ version: 4 });
    expect(screen.getByRole('link', { name: 'Về danh sách kế hoạch' })).toHaveAttribute('href', '/plans');
  });
});
