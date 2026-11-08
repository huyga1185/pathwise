import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, fail, ok, owner, server, signedInHandlers } from '@/test/server';

type Store = {
  id: number;
  name: string;
  depot: { address: string | null; lat: number; lon: number } | null;
  timezone: string;
};

const withDepot = () => sample<{ data: Store }>('store').data;
const withoutDepot = (): Store => ({ ...withDepot(), depot: null });

/** A signed-in user on /store whose GET /store returns `store` (the sample store unless given). */
function storeHandlers(store: Store, user: typeof owner = owner) {
  return [http.get(`${API}/store`, () => ok(store)), ...signedInHandlers(user)];
}

describe('Cửa hàng', () => {
  it('hiện thông tin, toạ độ kho và bản đồ có attribution của basemap', async () => {
    const store = withDepot();
    server.use(...storeHandlers(store));
    renderApp('/store');

    expect(await screen.findByLabelText(/^Tên cửa hàng/)).toHaveValue(store.name);
    expect(screen.getByLabelText(/^Múi giờ/)).toHaveValue('Asia/Ho_Chi_Minh');
    expect(screen.getByText(/^\d+,\d{5} \/ \d+,\d{5}$/)).toBeInTheDocument();
    expect(screen.queryByText(/Chưa có kho xuất phát/)).not.toBeInTheDocument();
    // The screen draws whatever basemap the composition root gives (P-45): here the test one.
    expect(screen.getByText('Bản đồ thử')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Phóng to bản đồ' })).toBeInTheDocument();
  });

  it('chọn kết quả tìm địa chỉ thì đặt ghim, điền địa chỉ trống; lưu gửi đúng depot', async () => {
    const store = { ...withDepot(), depot: { address: null, lat: 16.05, lon: 108.2 } };
    let body: unknown;
    let query: string | null = null;
    server.use(
      http.get(`${API}/geocoding/search`, ({ request }) => {
        query = new URL(request.url).searchParams.get('q');
        return ok([{ displayName: '45 Lê Lợi, Hải Châu, Đà Nẵng', lat: 16.0731, lon: 108.2219 }]);
      }),
      http.put(`${API}/store`, async ({ request }) => {
        body = await request.json();
        return ok({ ...store, ...(body as object) });
      }),
      ...storeHandlers(store),
    );
    renderApp('/store');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText('Tìm địa chỉ kho'), '45 Lê Lợi');
    const results = await screen.findByRole('list', { name: 'Kết quả tìm' });
    expect(query).toBe('45 Lê Lợi');
    await user.click(within(results).getByRole('button', { name: /45 Lê Lợi/ }));

    expect(screen.getByText('16,07310 / 108,22190')).toBeInTheDocument();
    expect(screen.getByLabelText(/^Địa chỉ kho/)).toHaveValue('45 Lê Lợi, Hải Châu, Đà Nẵng');
    await user.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    expect(await screen.findByText(/Đã lưu thông tin cửa hàng/)).toBeInTheDocument();
    expect(body).toEqual({
      name: store.name,
      timezone: 'Asia/Ho_Chi_Minh',
      depot: { lat: 16.0731, lon: 108.2219, address: '45 Lê Lợi, Hải Châu, Đà Nẵng' },
    });
  });

  it('chưa có kho: cảnh báo, chưa lưu được; đặt ghim bằng bàn phím rồi lưu toạ độ không kèm địa chỉ', async () => {
    let body: { depot?: { lat: number; lon: number; address?: string } } | undefined;
    server.use(
      http.put(`${API}/store`, async ({ request }) => {
        body = (await request.json()) as typeof body;
        return ok({ ...withoutDepot(), depot: { address: null, ...body?.depot } });
      }),
      ...storeHandlers(withoutDepot()),
    );
    renderApp('/store');
    const user = userEvent.setup();

    expect(await screen.findByRole('alert')).toHaveTextContent('Chưa có kho xuất phát nên chưa tạo được kế hoạch.');
    expect(screen.getByText('Chưa đặt')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
    expect(screen.queryByText(/Chưa có kho xuất phát/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    await waitFor(() => expect(body).toBeDefined());
    expect(body?.depot?.lat).toBeCloseTo(16.0544, 4);
    expect(body?.depot?.lon).toBeCloseTo(108.2022, 4);
    expect(body?.depot).not.toHaveProperty('address');
  });

  it('lỗi của server hiện dưới đúng ô; tìm địa chỉ bận thì báo dưới ô tìm', async () => {
    server.use(
      http.get(`${API}/geocoding/search`, () => fail(503, 'GEOCODING_BUSY')),
      http.put(`${API}/store`, () =>
        fail(400, 'VALIDATION_ERROR', [
          { field: 'name', code: 'INVALID_STORE_NAME_LENGTH' },
          { field: 'depot.address', code: 'INVALID_ADDRESS_LENGTH' },
        ]),
      ),
      ...storeHandlers(withDepot()),
    );
    renderApp('/store');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Lưu thay đổi' }));
    expect(await screen.findByText('Tên cửa hàng quá dài hoặc quá ngắn.')).toBeInTheDocument();
    expect(screen.getByText('Địa chỉ kho quá dài hoặc quá ngắn.')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Tìm địa chỉ kho'), 'Lê Lợi');
    expect(await screen.findByText('Dịch vụ tìm địa chỉ đang bận, thử lại sau ít phút.')).toBeInTheDocument();
  });

  it('múi giờ không hợp lệ thì báo dưới ô múi giờ', async () => {
    server.use(
      http.put(`${API}/store`, () => fail(400, 'INVALID_TIMEZONE')),
      ...storeHandlers(withDepot()),
    );
    renderApp('/store');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Lưu thay đổi' }));

    expect(await screen.findByText('Múi giờ không hợp lệ.')).toBeInTheDocument();
    expect(screen.getByLabelText(/^Múi giờ/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('điều phối viên chỉ xem: ô bị khoá, không có nút lưu, ô tìm và nút đặt ghim', async () => {
    const dispatcher = { ...owner, id: 450, role: 'DISPATCHER' } as typeof owner;
    server.use(...storeHandlers(withoutDepot(), dispatcher));
    renderApp('/store');

    expect(await screen.findByLabelText(/^Tên cửa hàng/)).toBeDisabled();
    expect(screen.getByLabelText(/^Múi giờ/)).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('Nhờ chủ cửa hàng đặt kho.');
    expect(screen.queryByRole('button', { name: 'Lưu thay đổi' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Tìm địa chỉ kho')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Đặt ghim ở giữa bản đồ' })).not.toBeInTheDocument();
  });
});
