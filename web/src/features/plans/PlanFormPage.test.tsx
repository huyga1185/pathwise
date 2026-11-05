import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, server, signedInHandlers } from '@/test/server';

type Detail = Record<string, unknown> & { id: number; version: number };
const planSample = () => sample<{ data: Detail }>('plan-detail-geocoded').data;
const storeSample = () => sample<{ data: Record<string, unknown> }>('store').data;
const drivers = () =>
  sample<{ data: Array<{ role: string }> }>('member-list').data.filter((member) => member.role === 'DRIVER');

/** A signed-in OWNER whose store has a depot, with the sample drivers and vehicles to assign. */
function formHandlers(store: Record<string, unknown> = storeSample()) {
  return [
    http.get(`${API}/store`, () => ok(store)),
    http.get(`${API}/store/members`, () => ok(drivers())),
    http.get(`${API}/vehicles`, () => ok(sample<{ data: unknown[] }>('vehicle-list').data)),
    ...signedInHandlers(),
  ];
}

async function fillStop(user: ReturnType<typeof userEvent.setup>, n: number, label: string, address: string) {
  await user.type(screen.getByLabelText(`Mã đơn dòng ${n}`), label);
  await user.type(screen.getByLabelText(`Địa chỉ dòng ${n}`), address);
}

describe('Tạo kế hoạch', () => {
  it('tạo: phân công, điểm giao, lưu gửi đúng body rồi sang /plans/:id', async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/plans`, async ({ request }) => {
        body = await request.json();
        return ok({ ...planSample(), id: 90, version: 0 }, { status: 201 });
      }),
      http.get(`${API}/plans/90`, () => ok({ ...planSample(), id: 90 })),
      ...formHandlers(),
    );
    const { router } = renderApp('/plans/new');
    const user = userEvent.setup();

    expect(await screen.findByText('Kho 45 Lê Lợi, Hải Châu, Đà Nẵng')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^Tên kế hoạch/), 'Giao sáng 01/12');
    await user.clear(screen.getByLabelText(/^Ngày giao/));
    await user.type(screen.getByLabelText(/^Ngày giao/), '2026-12-01');
    await user.click(screen.getByRole('button', { name: '+ Thêm phân công' }));
    await user.selectOptions(await screen.findByLabelText('Tài xế phân công 1'), '451');
    await user.selectOptions(screen.getByLabelText('Xe phân công 1'), '74');
    await user.type(screen.getByLabelText('Quãng đường tối đa (km) phân công 1'), '80');
    await fillStop(user, 1, 'ĐH-1001', '879 Hùng Vương, Hải Châu, Đà Nẵng');
    // The vehicle counts boxes: the stops table gets a "Thùng" column.
    await user.type(screen.getByLabelText('Số thùng dòng 1'), '2');
    await user.type(screen.getByLabelText('Giờ nhận từ dòng 1'), '09:00');
    await user.type(screen.getByLabelText('Giờ nhận đến dòng 1'), '11:00');
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);

    await waitFor(() => expect(router.state.location.pathname).toBe('/plans/90'));
    expect(body).toEqual({
      name: 'Giao sáng 01/12',
      planDate: '2026-12-01',
      assignments: [{ driverId: 451, vehicleId: 74, shiftStart: '08:00', shiftEnd: '17:00', maxDistanceMeters: 80000 }],
      stops: [
        {
          label: 'ĐH-1001',
          address: '879 Hùng Vương, Hải Châu, Đà Nẵng',
          demands: { BOX: 2 },
          timeWindow: { start: '09:00', end: '11:00' },
        },
      ],
    });
  });

  it('lỗi stops[3].timeWindow.end của server hiện ở ô giờ của dòng 4', async () => {
    server.use(
      http.post(`${API}/plans`, () =>
        fail(400, 'VALIDATION_ERROR', [{ field: 'stops[3].timeWindow.end', code: 'INVALID_TIME_WINDOW' }]),
      ),
      ...formHandlers(),
    );
    renderApp('/plans/new');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên kế hoạch/), 'Giao sáng');
    for (let n = 1; n <= 4; n += 1) {
      if (n > 1) await user.click(screen.getByRole('button', { name: '+ Thêm điểm giao' }));
      await fillStop(user, n, `ĐH-${n}`, `${n} Lê Duẩn`);
    }
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);

    const end = await screen.findByLabelText('Giờ nhận đến dòng 4');
    await waitFor(() => expect(end).toHaveAttribute('aria-invalid', 'true'));
    expect(end.closest('td')).toHaveTextContent('Giờ kết thúc phải sau giờ bắt đầu.');
    expect(screen.getByLabelText('Giờ nhận đến dòng 3')).not.toHaveAttribute('aria-invalid');
    expect(screen.getByText('Còn 1 ô cần sửa trong biểu mẫu.')).toBeInTheDocument();
  });

  it('kiểm ngay trước khi gửi: thiếu mã đơn báo ở đúng ô, không gọi API', async () => {
    let called = false;
    server.use(
      http.post(`${API}/plans`, () => {
        called = true;
        return ok({});
      }),
      ...formHandlers(),
    );
    renderApp('/plans/new');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên kế hoạch/), 'Giao sáng');
    await user.type(screen.getByLabelText('Địa chỉ dòng 1'), '12 Lê Duẩn');
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);

    expect(await screen.findByText('Vui lòng nhập mã đơn.')).toBeInTheDocument();
    expect(screen.getByLabelText('Mã đơn dòng 1')).toHaveAttribute('aria-invalid', 'true');
    expect(called).toBe(false);
  });

  it('cửa hàng chưa có kho: báo và dẫn sang /store, nút lưu bị khoá; server trả STORE_DEPOT_REQUIRED cũng vậy', async () => {
    server.use(...formHandlers({ ...storeSample(), depot: null }));
    renderApp('/plans/new');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Cửa hàng chưa có kho xuất phát.');
    expect(within(alert).getByRole('link', { name: 'Chọn kho xuất phát' })).toHaveAttribute('href', '/store');
    screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' }).forEach((button) => expect(button).toBeDisabled());
  });

  it('server trả STORE_DEPOT_REQUIRED thì hiện link sang /store', async () => {
    server.use(
      http.post(`${API}/plans`, () => fail(422, 'STORE_DEPOT_REQUIRED')),
      ...formHandlers(),
    );
    renderApp('/plans/new');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên kế hoạch/), 'Giao sáng');
    await fillStop(user, 1, 'ĐH-1', '12 Lê Duẩn');
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);

    expect(await screen.findByRole('link', { name: 'Chọn kho xuất phát' })).toHaveAttribute('href', '/store');
  });
});

describe('Sửa kế hoạch', () => {
  it('nạp plan, PUT gửi version và id điểm giao; PLAN_MODIFIED hỏi tải lại', async () => {
    const plan = planSample();
    const bodies: Array<{ version: number; stops: Array<{ id?: number }> }> = [];
    // Someone else saves the plan (version 2) just before our first PUT.
    let current = plan;
    server.use(
      http.get(`${API}/plans/83`, () => ok(current)),
      http.put(`${API}/plans/83`, async ({ request }) => {
        bodies.push((await request.json()) as (typeof bodies)[number]);
        if (bodies.length === 1) {
          current = { ...plan, version: 2 };
          return fail(409, 'PLAN_MODIFIED');
        }
        return ok({ ...plan, version: 3 });
      }),
      ...formHandlers(),
    );
    const { router } = renderApp('/plans/83/edit');
    const user = userEvent.setup();

    expect(await screen.findByRole('heading', { name: 'Sửa Giao sáng 01/12' })).toBeInTheDocument();
    expect(screen.getByLabelText('Mã đơn dòng 1')).toHaveValue('ĐH-1001');
    expect(screen.getByLabelText('Tài xế phân công 2')).toHaveValue('452');
    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);

    const dialog = await screen.findByRole('dialog', { name: 'Kế hoạch vừa được người khác thay đổi' });
    expect(bodies[0]?.version).toBe(1);
    expect(bodies[0]?.stops[0]?.id).toBe(108);
    await user.click(within(dialog).getByRole('button', { name: 'Tải lại' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(screen.getAllByRole('button', { name: 'Lưu và tìm toạ độ' })[0] as HTMLElement);
    await waitFor(() => expect(router.state.location.pathname).toBe('/plans/83'));
    expect(bodies[1]?.version).toBe(2);
  });
});
