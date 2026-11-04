import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, owner, server, signedInHandlers } from '@/test/server';

type Vehicle = {
  id: number;
  name: string;
  plateNumber: string | null;
  capacity: number;
  capacityUnit: string;
  active: boolean;
};

/** The store's vehicles held like the server: the sample list plus one turned off. */
function vehicleHandlers(state: { vehicles: Vehicle[] }) {
  return [
    http.get(`${API}/vehicles`, ({ request }) => {
      const active = new URL(request.url).searchParams.get('active');
      return ok(state.vehicles.filter((vehicle) => active !== 'true' || vehicle.active));
    }),
    ...signedInHandlers(owner),
  ];
}

function initialVehicles(): Vehicle[] {
  return [
    ...sample<{ data: Vehicle[] }>('vehicle-list').data,
    { id: 76, name: 'Xe cũ', plateNumber: '43A-999.99', capacity: 200, capacityUnit: 'KG', active: false },
  ];
}

const rows = () =>
  within(screen.getByRole('table', { name: 'Xe của cửa hàng' }))
    .getAllByRole('row')
    .slice(1);

describe('Xe', () => {
  it('hiện danh sách theo prototype; bỏ "Hiện cả xe đã tắt" thì chỉ còn xe đang dùng', async () => {
    server.use(...vehicleHandlers({ vehicles: initialVehicles() }));
    const { router } = renderApp('/vehicles');
    const user = userEvent.setup();

    await waitFor(() => expect(rows()).toHaveLength(3));
    expect(rows()[0]).toHaveTextContent('Xe 1');
    expect(rows()[0]).toHaveTextContent('—');
    expect(rows()[0]).toHaveTextContent('30 thùng');
    expect(rows()[0]).toHaveTextContent('ĐANG DÙNG');
    expect(rows()[2]).toHaveTextContent('200 kg');
    expect(rows()[2]).toHaveTextContent('ĐÃ TẮT');

    await user.click(screen.getByLabelText('Hiện cả xe đã tắt'));
    await waitFor(() => expect(rows()).toHaveLength(2));
    expect(router.state.location.search).toBe('?active=true');
  });

  it('thêm xe: gửi đúng body, báo đã thêm và tải lại danh sách', async () => {
    const state = { vehicles: initialVehicles() };
    let body: unknown;
    server.use(
      http.post(`${API}/vehicles`, async ({ request }) => {
        body = await request.json();
        const vehicle = { id: 90, active: true, plateNumber: null, ...(body as object) } as Vehicle;
        state.vehicles = [...state.vehicles, vehicle];
        return ok(vehicle, { status: 201 });
      }),
      ...vehicleHandlers(state),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();
    const form = await screen.findByRole('form', { name: 'Thêm xe' });

    await user.type(within(form).getByLabelText(/^Tên xe/), ' Xe 3 ');
    await user.type(within(form).getByLabelText(/^Sức chứa/), '40');
    await user.selectOptions(within(form).getByLabelText(/^Đơn vị/), 'PACKAGE');
    await user.click(within(form).getByRole('button', { name: 'Thêm xe' }));

    expect(await screen.findByText('Đã thêm xe.')).toBeInTheDocument();
    expect(body).toEqual({ name: 'Xe 3', capacity: 40, capacityUnit: 'PACKAGE' });
    await waitFor(() => expect(rows()).toHaveLength(4));
    expect(within(form).getByLabelText(/^Tên xe/)).toHaveValue('');
  });

  it('lỗi validation của server (docs/ux/samples/error-validation.json) hiện đúng 3 ô', async () => {
    const error = sample<{ code: string; details: unknown[] }>('error-validation');
    server.use(
      http.post(`${API}/vehicles`, () => fail(400, error.code, error.details)),
      ...vehicleHandlers({ vehicles: initialVehicles() }),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();
    const form = await screen.findByRole('form', { name: 'Thêm xe' });

    await user.type(within(form).getByLabelText(/^Tên xe/), 'x');
    await user.type(within(form).getByLabelText(/^Sức chứa/), '5');
    await user.selectOptions(within(form).getByLabelText(/^Đơn vị/), 'KG');
    await user.click(within(form).getByRole('button', { name: 'Thêm xe' }));

    expect(await within(form).findByText('Vui lòng nhập tên xe.')).toBeInTheDocument();
    expect(within(form).getByText('Sức chứa phải lớn hơn 0.')).toBeInTheDocument();
    expect(within(form).getByText('Vui lòng chọn đơn vị.')).toBeInTheDocument();
    expect(within(form).getByLabelText(/^Tên xe/)).toHaveAttribute('aria-invalid', 'true');
    expect(within(form).getByLabelText(/^Sức chứa/)).toHaveAttribute('aria-invalid', 'true');
    expect(within(form).getByLabelText(/^Đơn vị/)).toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('bỏ trống thì báo ngay dưới ô và không gửi', async () => {
    let called = false;
    server.use(
      http.post(`${API}/vehicles`, () => {
        called = true;
        return ok({});
      }),
      ...vehicleHandlers({ vehicles: initialVehicles() }),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();
    const form = await screen.findByRole('form', { name: 'Thêm xe' });

    await user.type(within(form).getByLabelText(/^Sức chứa/), '0');
    await user.click(within(form).getByRole('button', { name: 'Thêm xe' }));

    expect(await within(form).findByText('Vui lòng nhập tên xe.')).toBeInTheDocument();
    expect(within(form).getByText('Sức chứa phải lớn hơn 0.')).toBeInTheDocument();
    expect(within(form).getByText('Vui lòng chọn đơn vị.')).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('sửa xe: điền sẵn form, tắt xe, gửi PUT thay toàn bộ; trùng tên thì báo dưới ô tên', async () => {
    const state = { vehicles: initialVehicles() };
    const bodies: unknown[] = [];
    server.use(
      http.put(`${API}/vehicles/:id`, async ({ request, params }) => {
        const body = (await request.json()) as Vehicle;
        bodies.push(body);
        if (body.name === 'Xe 2') return fail(409, 'VEHICLE_NAME_EXISTED');
        const vehicle = { ...body, plateNumber: body.plateNumber ?? null, id: Number(params.id) } as Vehicle;
        state.vehicles = state.vehicles.map((item) => (item.id === vehicle.id ? vehicle : item));
        return ok(vehicle);
      }),
      ...vehicleHandlers(state),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Sửa Xe 1' }));
    const form = screen.getByRole('form', { name: 'Sửa Xe 1' });
    expect(within(form).getByLabelText(/^Sức chứa/)).toHaveValue(30);
    expect(within(form).getByLabelText(/^Đơn vị/)).toHaveValue('BOX');
    expect(rows()[0]).toHaveClass('pw-row-selected');

    const name = within(form).getByLabelText(/^Tên xe/);
    await user.clear(name);
    await user.type(name, 'Xe 2');
    await user.click(within(form).getByRole('button', { name: 'Lưu' }));
    expect(await within(form).findByText('Tên xe đã tồn tại.')).toBeInTheDocument();

    await user.clear(name);
    await user.type(name, 'Xe 1');
    await user.click(within(form).getByLabelText('Đang dùng'));
    await user.click(within(form).getByRole('button', { name: 'Lưu' }));

    expect(await screen.findByText('Đã lưu Xe 1.')).toBeInTheDocument();
    expect(bodies[1]).toEqual({ name: 'Xe 1', capacity: 30, capacityUnit: 'BOX', active: false });
    await waitFor(() => expect(rows()[0]).toHaveTextContent('ĐÃ TẮT'));
  });

  it('điều phối viên cũng thêm và sửa được xe', async () => {
    const dispatcher = { ...owner, id: 450, role: 'DISPATCHER' };
    server.use(
      http.get(`${API}/vehicles`, () => ok(initialVehicles())),
      ...signedInHandlers(dispatcher as typeof owner),
    );
    renderApp('/vehicles');

    expect(await screen.findByRole('form', { name: 'Thêm xe' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Sửa Xe 1' })).toBeEnabled();
  });
});
