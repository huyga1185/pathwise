import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { wideWindow } from '@/test/media';
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

/** Phone: the list of rows. */
const rows = () => within(screen.getByRole('list', { name: 'Xe của cửa hàng' })).getAllByRole('listitem');
/** Wide window: the table's rows. */
const tableRows = () =>
  within(screen.getByRole('table', { name: 'Xe của cửa hàng' }))
    .getAllByRole('row')
    .slice(1);

afterEach(() => vi.restoreAllMocks());

describe('Xe', () => {
  it('màn rộng: bảng; bộ lọc "Trạng thái: Đang dùng" chỉ còn xe đang dùng, nằm trên URL', async () => {
    wideWindow();
    server.use(...vehicleHandlers({ vehicles: initialVehicles() }));
    const { router } = renderApp('/vehicles');
    const user = userEvent.setup();

    await waitFor(() => expect(tableRows()).toHaveLength(3));
    expect(tableRows()[0]).toHaveTextContent('Xe 1');
    expect(tableRows()[0]).toHaveTextContent('—');
    expect(tableRows()[0]).toHaveTextContent('30 thùng');
    expect(tableRows()[0]).toHaveTextContent('ĐANG DÙNG');
    expect(tableRows()[2]).toHaveTextContent('200 kg');
    expect(tableRows()[2]).toHaveTextContent('ĐÃ TẮT');

    await user.click(screen.getByRole('button', { name: 'Trạng thái: Tất cả' }));
    await user.click(screen.getByRole('menuitemradio', { name: 'Đang dùng' }));
    await waitFor(() => expect(tableRows()).toHaveLength(2));
    expect(router.state.location.search).toBe('?active=true');
  });

  it('thêm xe trong sheet: gửi đúng body, đóng sheet, báo đã thêm và tải lại danh sách', async () => {
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

    await user.click(await screen.findByRole('button', { name: 'Thêm xe' }));
    const sheet = screen.getByRole('dialog', { name: 'Thêm xe' });
    await user.type(within(sheet).getByLabelText(/^Tên xe/), ' Xe 3 ');
    await user.type(within(sheet).getByLabelText(/^Sức chứa/), '40');
    await user.selectOptions(within(sheet).getByLabelText(/^Đơn vị/), 'PACKAGE');
    await user.click(within(sheet).getByRole('button', { name: 'Thêm' }));

    expect(await screen.findByText('Đã thêm xe.')).toBeInTheDocument();
    expect(body).toEqual({ name: 'Xe 3', capacity: 40, capacityUnit: 'PACKAGE' });
    await waitFor(() => expect(rows()).toHaveLength(4));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('lỗi validation của server (docs/ux/samples/error-validation.json) hiện đúng 3 ô trong sheet', async () => {
    const error = sample<{ code: string; details: unknown[] }>('error-validation');
    server.use(
      http.post(`${API}/vehicles`, () => fail(400, error.code, error.details)),
      ...vehicleHandlers({ vehicles: initialVehicles() }),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Thêm xe' }));
    const sheet = screen.getByRole('dialog', { name: 'Thêm xe' });

    await user.type(within(sheet).getByLabelText(/^Tên xe/), 'x');
    await user.type(within(sheet).getByLabelText(/^Sức chứa/), '5');
    await user.selectOptions(within(sheet).getByLabelText(/^Đơn vị/), 'KG');
    await user.click(within(sheet).getByRole('button', { name: 'Thêm' }));

    expect(await within(sheet).findByText('Vui lòng nhập tên xe.')).toBeInTheDocument();
    expect(within(sheet).getByText('Sức chứa phải lớn hơn 0.')).toBeInTheDocument();
    expect(within(sheet).getByText('Vui lòng chọn đơn vị.')).toBeInTheDocument();
    expect(within(sheet).getByLabelText(/^Tên xe/)).toHaveAttribute('aria-invalid', 'true');
    expect(within(sheet).getByLabelText(/^Sức chứa/)).toHaveAttribute('aria-invalid', 'true');
    expect(within(sheet).getByLabelText(/^Đơn vị/)).toHaveAttribute('aria-invalid', 'true');
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
    await user.click(await screen.findByRole('button', { name: 'Thêm xe' }));
    const sheet = screen.getByRole('dialog', { name: 'Thêm xe' });

    await user.type(within(sheet).getByLabelText(/^Sức chứa/), '0');
    await user.click(within(sheet).getByRole('button', { name: 'Thêm' }));

    expect(await within(sheet).findByText('Vui lòng nhập tên xe.')).toBeInTheDocument();
    expect(within(sheet).getByText('Sức chứa phải lớn hơn 0.')).toBeInTheDocument();
    expect(within(sheet).getByText('Vui lòng chọn đơn vị.')).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('bấm hàng mở sheet sửa: điền sẵn, tắt xe, gửi PUT thay toàn bộ; trùng tên thì báo dưới ô tên', async () => {
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
    const sheet = screen.getByRole('dialog', { name: 'Sửa Xe 1' });
    expect(within(sheet).getByLabelText(/^Sức chứa/)).toHaveValue(30);
    expect(within(sheet).getByLabelText(/^Đơn vị/)).toHaveValue('BOX');

    const name = within(sheet).getByLabelText(/^Tên xe/);
    await user.clear(name);
    await user.type(name, 'Xe 2');
    await user.click(within(sheet).getByRole('button', { name: 'Lưu' }));
    expect(await within(sheet).findByText('Tên xe đã tồn tại.')).toBeInTheDocument();

    await user.clear(name);
    await user.type(name, 'Xe 1');
    // A switch in the sheet (HIG Toggles: a switch in a list row).
    await user.click(within(sheet).getByRole('switch', { name: /^Đang dùng/ }));
    await user.click(within(sheet).getByRole('button', { name: 'Lưu' }));

    expect(await screen.findByText('Đã lưu Xe 1.')).toBeInTheDocument();
    expect(bodies[1]).toEqual({ name: 'Xe 1', capacity: 30, capacityUnit: 'BOX', active: false });
    await waitFor(() => expect(rows()[0]).toHaveTextContent('ĐÃ TẮT'));
  });

  it('menu ngữ cảnh của hàng: "Tắt xe" gửi PUT với active false; xe đã tắt thì có "Dùng lại"', async () => {
    const state = { vehicles: initialVehicles() };
    const bodies: Vehicle[] = [];
    server.use(
      http.put(`${API}/vehicles/:id`, async ({ request, params }) => {
        const body = (await request.json()) as Vehicle;
        bodies.push(body);
        const vehicle = { ...body, plateNumber: body.plateNumber ?? null, id: Number(params.id) } as Vehicle;
        state.vehicles = state.vehicles.map((item) => (item.id === vehicle.id ? vehicle : item));
        return ok(vehicle);
      }),
      ...vehicleHandlers(state),
    );
    renderApp('/vehicles');
    const user = userEvent.setup();

    await screen.findByRole('button', { name: 'Sửa Xe 1' });
    fireEvent.contextMenu(rows()[0]!);
    await user.click(within(screen.getByRole('menu', { name: 'Xe 1' })).getByRole('menuitem', { name: 'Tắt xe' }));
    expect(await screen.findByText('Đã tắt Xe 1.')).toBeInTheDocument();
    expect(bodies[0]).toEqual({ name: 'Xe 1', capacity: 30, capacityUnit: 'BOX', active: false });

    fireEvent.contextMenu(rows()[2]!);
    expect(
      within(screen.getByRole('menu', { name: 'Xe cũ' })).getByRole('menuitem', { name: 'Dùng lại' }),
    ).toBeInTheDocument();
  });

  it('điều phối viên cũng thêm và sửa được xe', async () => {
    const dispatcher = { ...owner, id: 450, role: 'DISPATCHER' };
    server.use(
      http.get(`${API}/vehicles`, () => ok(initialVehicles())),
      ...signedInHandlers(dispatcher as typeof owner),
    );
    renderApp('/vehicles');

    expect(await screen.findByRole('button', { name: 'Thêm xe' })).toBeEnabled();
    expect(await screen.findByRole('button', { name: 'Sửa Xe 1' })).toBeEnabled();
  });
});
