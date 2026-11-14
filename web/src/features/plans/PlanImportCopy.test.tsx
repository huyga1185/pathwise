import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Schemas } from '@/data/types';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, server, signedInHandlers } from '@/test/server';

const storeSample = () => sample<{ data: Record<string, unknown> }>('store').data;
const completed = () => sample<{ data: Schemas['PlanDetailResponse'] }>('plan-detail-completed').data;
const geocoded = () => sample<{ data: Schemas['PlanDetailResponse'] }>('plan-detail-geocoded').data;

function formHandlers() {
  return [
    http.get(`${API}/store`, () => ok(storeSample())),
    http.get(`${API}/store/members`, () => ok([])),
    http.get(`${API}/vehicles`, () => ok([])),
    ...signedInHandlers(),
  ];
}

const stop = (over: Partial<Schemas['ImportedStopResponse']> = {}): Schemas['ImportedStopResponse'] => ({
  label: 'ĐH-1',
  recipientName: 'Chị Lan',
  recipientPhone: '+84905123456',
  address: '12 Lê Duẩn, Đà Nẵng',
  lat: null,
  lon: null,
  demands: { BOX: 2 },
  serviceTimeSeconds: 300,
  timeWindow: { start: '09:00', end: '11:00' },
  note: null,
  ...over,
});

const imported: Schemas['PlanImportResponse'] = {
  rows: [
    { row: 2, stop: stop(), errors: [] },
    { row: 3, stop: stop({ label: null }), errors: [{ field: 'label', code: 'STOP_LABEL_REQUIRED' }] },
    { row: 4, stop: stop({ label: 'ĐH-3', address: null, lat: 16.06, lon: 108.2, recipientPhone: null }), errors: [] },
  ],
  validCount: 2,
  errorCount: 1,
  ignoredColumns: ['Mã khách'],
};

function file(name = 'diem-giao.xlsx') {
  return new File(['x'], name, { type: 'application/octet-stream' });
}

describe('Nhập điểm giao từ file', () => {
  it('gửi file multipart, xem trước dòng lỗi, chỉ thêm dòng hợp lệ thay cho dòng trống rồi lưu đúng body', async () => {
    // jsdom's File does not reach Node's FormData (its part arrives empty, and reading the body can abort): the test
    // checks the request is multipart, not the file's bytes. Browsers send the file itself.
    let contentType = '';
    let body: { stops?: unknown[] } | null = null;
    server.use(
      http.post(`${API}/plans/import`, async ({ request }) => {
        contentType = request.headers.get('content-type') ?? '';
        return ok(imported);
      }),
      http.post(`${API}/plans`, async ({ request }) => {
        body = (await request.json()) as { stops?: unknown[] };
        return ok({ ...geocoded(), id: 90 }, { status: 201 });
      }),
      http.get(`${API}/plans/90`, () => ok({ ...geocoded(), id: 90 })),
      ...formHandlers(),
    );
    renderApp('/plans/new');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Nhập từ file' }));
    const dialog = await screen.findByRole('dialog', { name: 'Nhập điểm giao từ file' });
    await user.upload(within(dialog).getByLabelText('File điểm giao (.xlsx hoặc .csv)'), file());

    expect(await within(dialog).findByText('diem-giao.xlsx')).toBeInTheDocument();
    expect(contentType).toMatch(/^multipart\/form-data; boundary=/);
    expect(within(dialog).getByText('Bỏ qua cột không có trong file mẫu: Mã khách.')).toBeInTheDocument();
    expect(within(dialog).getByText('Nhãn / mã đơn: Vui lòng nhập nhãn / mã đơn.')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Thêm 2 điểm giao' }));

    expect(screen.getByText('Đã thêm 2 điểm giao từ file. Kiểm tra rồi bấm Lưu.')).toBeInTheDocument();
    expect(screen.getByLabelText('Mã đơn dòng 1')).toHaveValue('ĐH-1');
    expect(screen.getByLabelText('Số điện thoại dòng 1')).toHaveValue('0905 123 456');
    expect(screen.getByLabelText('Mã đơn dòng 2')).toHaveValue('ĐH-3');
    expect(screen.queryByLabelText('Mã đơn dòng 3')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText(/^Tên kế hoạch/), 'Từ file');
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);
    await waitFor(() => expect(body).not.toBeNull());
    expect((body as unknown as { stops: unknown[] }).stops).toEqual([
      {
        label: 'ĐH-1',
        recipientName: 'Chị Lan',
        recipientPhone: '0905 123 456',
        address: '12 Lê Duẩn, Đà Nẵng',
        demands: { BOX: 2 },
        serviceTimeSeconds: 300,
        timeWindow: { start: '09:00', end: '11:00' },
      },
      {
        label: 'ĐH-3',
        recipientName: 'Chị Lan',
        lat: 16.06,
        lon: 108.2,
        demands: { BOX: 2 },
        serviceTimeSeconds: 300,
        timeWindow: { start: '09:00', end: '11:00' },
      },
    ]);
  });

  it('file bị từ chối thì báo lý do của details; vượt 100 điểm thì không cho thêm', async () => {
    let calls = 0;
    server.use(
      http.post(`${API}/plans/import`, () => {
        calls += 1;
        if (calls === 1) return fail(422, 'INVALID_IMPORT_FILE', [{ field: 'file', code: 'LABEL_COLUMN_REQUIRED' }]);
        return ok({
          rows: Array.from({ length: 100 }, (_, i) => ({ row: i + 2, stop: stop({ label: `ĐH-${i}` }), errors: [] })),
          validCount: 100,
          errorCount: 0,
          ignoredColumns: [],
        });
      }),
      ...formHandlers(),
    );
    renderApp('/plans/new');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Mã đơn dòng 1'), 'Đã có');
    await user.click(screen.getByRole('button', { name: 'Nhập từ file' }));
    const dialog = await screen.findByRole('dialog', { name: 'Nhập điểm giao từ file' });

    await user.upload(within(dialog).getByLabelText('File điểm giao (.xlsx hoặc .csv)'), file('thieu-cot.csv'));
    expect(
      await within(dialog).findByText('File thiếu cột "Nhãn / mã đơn". Tải file mẫu để xem các cột.'),
    ).toBeInTheDocument();

    await user.upload(within(dialog).getByLabelText('File điểm giao (.xlsx hoặc .csv)'), file('du.csv'));
    expect(
      await within(dialog).findByText(/Kế hoạch đang có 1 điểm giao; thêm 100 điểm sẽ vượt 100/),
    ).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Thêm 100 điểm giao' })).toBeDisabled();
  });
});

describe('Sao chép kế hoạch', () => {
  it('từ danh sách: chọn chỉ điểm chưa giao được, gửi đúng body, mở kế hoạch mới và báo phân công bị bỏ', async () => {
    let body: unknown;
    const copy = { ...geocoded(), id: 91, name: 'Giao lại', planDate: '2026-12-02' };
    server.use(
      http.get(`${API}/plans`, () => ok(sample<{ data: unknown }>('plan-list').data)),
      http.get(`${API}/plans/83`, () => ok(completed())),
      http.post(`${API}/plans/83/copy`, async ({ request }) => {
        body = await request.json();
        return ok(
          { plan: copy, skippedAssignments: [{ driverId: 452, vehicleId: 75, reason: 'DRIVER_UNAVAILABLE' }] },
          { status: 201 },
        );
      }),
      http.get(`${API}/plans/91`, () => ok(copy)),
      http.get(`${API}/store`, () => ok(storeSample())),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Sao chép Giao sáng 01/12' }));
    const dialog = await screen.findByRole('dialog', { name: 'Sao chép "Giao sáng 01/12"' });
    expect(router.state.location.pathname).toBe('/plans');
    await user.click(await within(dialog).findByRole('radio', { name: /^Chỉ \d+ điểm chưa giao được/ }));
    await user.clear(within(dialog).getByLabelText(/^Ngày giao/));
    await user.type(within(dialog).getByLabelText(/^Ngày giao/), '2026-12-02');
    await user.clear(within(dialog).getByLabelText('Tên kế hoạch'));
    await user.type(within(dialog).getByLabelText('Tên kế hoạch'), 'Giao lại');
    await user.click(within(dialog).getByRole('button', { name: 'Sao chép' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/plans/91'));
    expect(body).toEqual({ planDate: '2026-12-02', name: 'Giao lại', stops: 'NOT_DELIVERED' });
    expect(await screen.findByText(/^Đã sao chép từ "Giao sáng 01\/12"/)).toBeInTheDocument();
    expect(
      screen.getByText(
        'Không chép phân công của tài xế Tài xế B (không còn nhận tuyến được). Thêm phân công rồi tối ưu lộ trình.',
      ),
    ).toBeInTheDocument();
  });

  it('kế hoạch nháp chỉ có lựa chọn tất cả; cửa hàng khoá thì không có nút sao chép', async () => {
    server.use(
      http.get(`${API}/plans`, () =>
        ok({ ...sample<{ data: { items: Array<Record<string, unknown>> } }>('plan-list').data }),
      ),
      http.get(`${API}/plans/83`, () => ok({ ...geocoded(), id: 83, name: 'Giao sáng 01/12' })),
      http.get(`${API}/store`, () => ok(storeSample())),
      ...signedInHandlers(),
    );
    renderApp('/plans');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Sao chép Giao sáng 01/12' }));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByRole('radio', { name: /^Tất cả/ })).toBeChecked();
    expect(within(dialog).queryByRole('radio', { name: /chưa giao được/ })).not.toBeInTheDocument();
  });
});
