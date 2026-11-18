import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { wideWindow } from '@/test/media';
import { API, ok, server, signedInHandlers } from '@/test/server';

type Page = {
  items: Array<Record<string, unknown>>;
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
};

afterEach(() => vi.restoreAllMocks());

describe('Danh sách kế hoạch', () => {
  it('màn rộng: bảng; lọc ngày và trạng thái nằm trên URL và gửi lên API', async () => {
    wideWindow();
    const queries: string[] = [];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        queries.push(new URL(request.url).search);
        return ok(sample<{ data: Page }>('plan-list').data);
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    await screen.findByRole('link', { name: 'Giao sáng 01/12' });
    expect(screen.getByRole('heading', { name: 'Kế hoạch', level: 1 })).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Kế hoạch giao hàng' });
    const row = within(table).getAllByRole('row')[1] as HTMLElement;
    expect(row).toHaveTextContent('Giao sáng 01/12');
    expect(row).toHaveTextContent('T3, 01/12/2026');
    // A long name is cut to two lines on screen; the whole name stays in the link and its tooltip.
    expect(within(row).getByRole('link', { name: 'Giao sáng 01/12' })).toHaveAttribute('title', 'Giao sáng 01/12');
    expect(row).toHaveTextContent('HOÀN THÀNH');
    expect(row).toHaveTextContent('15,4 km');
    expect(row).toHaveTextContent('5 giờ 06 phút');
    // What a narrow table's hidden columns hold is also under the name (shown by CSS only while those columns hide).
    expect(row.querySelector('.pw-plans-more')).toHaveTextContent('15,4 km');
    // One page: no pager, no count.
    expect(screen.queryByRole('navigation', { name: 'Trang danh sách kế hoạch' })).not.toBeInTheDocument();
    // "Xoá bộ lọc" only while filtering.
    expect(screen.queryByRole('button', { name: 'Xoá bộ lọc' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ngày giao: Tất cả' })).toBeInTheDocument();

    // Pop-up button: a menu with a checkmark on the current choice.
    await user.click(screen.getByRole('button', { name: 'Trạng thái: Tất cả' }));
    const menu = screen.getByRole('menu', { name: 'Trạng thái' });
    expect(within(menu).getByRole('menuitemradio', { name: 'Tất cả' })).toHaveAttribute('aria-checked', 'true');
    await user.click(within(menu).getByRole('menuitemradio', { name: 'Hoàn thành' }));
    await waitFor(() => expect(router.state.location.search).toBe('?status=COMPLETED'));
    await waitFor(() => expect(queries.at(-1)).toContain('status=COMPLETED'));
    await user.click(screen.getByRole('button', { name: 'Xoá bộ lọc' }));
    await waitFor(() => expect(router.state.location.search).toBe(''));

    // A new result is drawn again (it settles in): look the row up again.
    await user.click(await screen.findByRole('link', { name: 'Giao sáng 01/12' }));
    expect(router.state.location.pathname).toBe('/plans/83');
  });

  it('không có kế hoạch cho ngày đã chọn thì hiện trạng thái trống có nút tạo', async () => {
    server.use(
      http.get(`${API}/plans`, () => ok({ items: [], page: 0, size: 20, totalPages: 0, totalElements: 0 })),
      ...signedInHandlers(),
    );
    renderApp('/plans?date=2026-12-01');

    expect(
      await screen.findByRole('heading', { name: 'Chưa có kế hoạch nào cho ngày 01/12/2026' }),
    ).toBeInTheDocument();
    const create = screen.getAllByRole('link', { name: 'Tạo kế hoạch' });
    expect(create.every((link) => link.getAttribute('href') === '/plans/new')).toBe(true);
    expect(screen.getByRole('button', { name: 'Ngày giao: 01/12/2026' })).toBeInTheDocument();
  });

  it('bấm tiêu đề cột để sắp xếp; bấm lại thì đảo chiều; thứ tự nằm trên URL', async () => {
    wideWindow();
    const sorts: Array<string | null> = [];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        sorts.push(new URL(request.url).searchParams.get('sort'));
        return ok(sample<{ data: Page }>('plan-list').data);
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();
    await screen.findByRole('link', { name: 'Giao sáng 01/12' });
    expect(sorts.at(-1)).toBeNull();
    expect(screen.getByRole('columnheader', { name: /Ngày giao/ })).toHaveAttribute('aria-sort', 'descending');

    await user.click(screen.getByRole('button', { name: 'Tên kế hoạch' }));
    await waitFor(() => expect(sorts.at(-1)).toBe('name,asc'));
    expect(router.state.location.search).toBe('?order=name-asc');
    expect(screen.getByRole('columnheader', { name: /Tên kế hoạch/ })).toHaveAttribute('aria-sort', 'ascending');

    await user.click(screen.getByRole('button', { name: 'Tên kế hoạch' }));
    await waitFor(() => expect(sorts.at(-1)).toBe('name,desc'));
  });

  it('màn hẹp: danh sách dòng mở được kế hoạch; đang tải thì hiện dòng giữ chỗ', async () => {
    let release = () => {};
    const gate = new Promise<void>((resolve) => (release = resolve));
    server.use(
      http.get(`${API}/plans`, async () => {
        await gate;
        return ok(sample<{ data: Page }>('plan-list').data);
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    expect(await screen.findByRole('status', { name: 'Đang tải danh sách kế hoạch' })).toBeInTheDocument();
    release();
    const link = await screen.findByRole('link', { name: /Giao sáng 01\/12/ });
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(link.closest('li')).toHaveTextContent('T3, 01/12/2026 · ');
    expect(screen.getByRole('link', { name: 'Tạo kế hoạch' })).toHaveAttribute('href', '/plans/new');
    await user.click(link);
    expect(router.state.location.pathname).toBe('/plans/83');
  });

  it('chọn ngày bằng lịch: ngày áp dụng ngay, lịch đóng khi bấm Escape; "Mọi ngày" bỏ ngày', async () => {
    // A wide window: the calendar is a popover (compact date picker).
    wideWindow();
    server.use(
      http.get(`${API}/plans`, () => ok(sample<{ data: Page }>('plan-list').data)),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans?date=2026-12-01');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Ngày giao: 01/12/2026' }));
    const calendar = screen.getByRole('dialog', { name: 'Ngày giao' });
    expect(within(calendar).getByText('Tháng 12, 2026')).toBeInTheDocument();
    expect(within(calendar).getByRole('button', { name: 'Thứ Ba, 1 tháng 12, 2026' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(within(calendar).getByRole('button', { name: 'Thứ Tư, 2 tháng 12, 2026' })).toHaveFocus();
    await user.click(within(calendar).getByRole('button', { name: 'Tháng sau' }));
    expect(within(calendar).getByText('Tháng 1, 2027')).toBeInTheDocument();
    await user.click(within(calendar).getByRole('button', { name: 'Thứ Sáu, 15 tháng 1, 2027' }));
    await waitFor(() => expect(router.state.location.search).toBe('?date=2027-01-15'));
    expect(screen.getByRole('dialog', { name: 'Ngày giao' })).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.getByRole('button', { name: 'Ngày giao: 15/01/2027' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Ngày giao: 15/01/2027' }));
    await user.click(screen.getByRole('button', { name: 'Mọi ngày' }));
    await waitFor(() => expect(router.state.location.search).toBe(''));
  });

  it('điện thoại: lịch inline nằm trong trang dưới hàng lọc (không bật ra); chọn ngày vẫn mở; bấm lại nút thì đóng', async () => {
    server.use(
      http.get(`${API}/plans`, () => ok(sample<{ data: Page }>('plan-list').data)),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans?date=2026-12-01');
    const user = userEvent.setup();

    const button = await screen.findByRole('button', { name: 'Ngày giao: 01/12/2026' });
    expect(button).not.toHaveAttribute('aria-haspopup');
    await user.click(button);
    const calendar = screen.getByRole('group', { name: 'Ngày giao' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    // In the page, inside the filter row (not on document.body like a popover).
    expect(screen.getByRole('group', { name: 'Lọc kế hoạch' }).contains(calendar)).toBe(true);
    expect(button).toHaveAttribute('aria-expanded', 'true');

    await user.click(within(calendar).getByRole('button', { name: 'Thứ Sáu, 4 tháng 12, 2026' }));
    await waitFor(() => expect(router.state.location.search).toBe('?date=2026-12-04'));
    expect(screen.getByRole('group', { name: 'Ngày giao' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ngày giao: 04/12/2026' }));
    await waitFor(() => expect(screen.queryByRole('group', { name: 'Ngày giao' })).not.toBeInTheDocument());
  });

  it('thanh trang ‹ Trang 1/2 ›: trang trên URL, trang sau gửi page=1, hai đầu mờ nút', async () => {
    const queries: URLSearchParams[] = [];
    const item = sample<{ data: Page }>('plan-list').data.items[0];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        const query = new URL(request.url).searchParams;
        queries.push(query);
        const page = Number(query.get('page') ?? 0);
        return ok({ items: [{ ...item, id: 83 + page }], page, size: 20, totalPages: 2, totalElements: 21 });
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    const pager = await screen.findByRole('navigation', { name: 'Trang danh sách kế hoạch' });
    // A phone: 20 a page (a wide window fits the rows to its height).
    expect(queries.at(-1)?.get('size')).toBe('20');
    expect(within(pager).getByRole('button', { name: /^Trang 1\/2/ })).toBeInTheDocument();
    expect(within(pager).getByRole('button', { name: 'Trang trước' })).toHaveAttribute('aria-disabled', 'true');
    await user.click(within(pager).getByRole('button', { name: 'Trang sau' }));

    await waitFor(() => expect(router.state.location.search).toBe('?page=1'));
    expect(queries.at(-1)?.get('page')).toBe('1');
    expect(await within(pager).findByRole('button', { name: /^Trang 2\/2/ })).toBeInTheDocument();
    expect(within(pager).getByRole('button', { name: 'Trang sau' })).toHaveAttribute('aria-disabled', 'true');
    expect(within(pager).getByRole('button', { name: 'Trang sau' })).toHaveFocus();
  });

  it('bấm "Trang 1/12" mở khung: trang đầu, ô số trang, trang cuối; gõ số + Enter thì chuyển; số ngoài khoảng thì trả về', async () => {
    const item = sample<{ data: Page }>('plan-list').data.items[0];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        return ok({ items: [{ ...item, id: 83 + page }], page, size: 20, totalPages: 12, totalElements: 230 });
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /^Trang 1\/12/ }));
    const panel = screen.getByRole('dialog', { name: /^Trang 1\/12/ });
    const field = within(panel).getByRole('textbox', { name: 'Số trang' });
    expect(field).toHaveFocus();
    expect(field).toHaveValue('1');
    expect(within(panel).getByRole('button', { name: 'Trang đầu' })).toHaveAttribute('aria-disabled', 'true');

    await user.clear(field);
    await user.type(field, '99{Enter}');
    expect(field).toHaveValue('1');
    expect(router.state.location.search).toBe('');

    await user.clear(field);
    await user.type(field, '1x2{Enter}');
    await waitFor(() => expect(router.state.location.search).toBe('?page=11'));
    expect(screen.queryByRole('dialog', { name: /^Trang/ })).not.toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: /^Trang 12\/12/ }));
    await user.click(
      within(screen.getByRole('dialog', { name: /^Trang 12\/12/ })).getByRole('button', { name: 'Trang đầu' }),
    );
    await waitFor(() => expect(router.state.location.search).toBe(''));
  });

  it('trang quá cuối (link cũ) thì về trang cuối; chỉ một trang thì không có thanh trang', async () => {
    const item = sample<{ data: Page }>('plan-list').data.items[0];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        return ok({ items: page === 0 ? [item] : [], page, size: 20, totalPages: 1, totalElements: 1 });
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans?page=4');

    await waitFor(() => expect(router.state.location.search).toBe(''));
    expect(await screen.findByRole('link', { name: /Giao sáng 01\/12/ })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Trang danh sách kế hoạch' })).not.toBeInTheDocument();
  });

  it('menu ngữ cảnh: nhấp chuột phải hàng của bảng hoặc chạm giữ hàng của danh sách có "Sao chép…" mở hộp thoại sao chép', async () => {
    server.use(
      http.get(`${API}/plans`, () => ok(sample<{ data: Page }>('plan-list').data)),
      ...signedInHandlers(),
    );
    wideWindow();
    const { unmount } = renderApp('/plans');
    const user = userEvent.setup();

    const row = (await screen.findByRole('link', { name: 'Giao sáng 01/12' })).closest('tr') as HTMLElement;
    fireEvent.contextMenu(row, { clientX: 300, clientY: 200 });
    const menu = screen.getByRole('menu', { name: 'Giao sáng 01/12' });
    expect(within(menu).getByRole('menuitem', { name: 'Sao chép…' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    unmount();
    vi.restoreAllMocks();

    // Phone: the list of rows (a long press reaches the browser as a context menu event on Android).
    renderApp('/plans');
    const item = (await screen.findByRole('link', { name: /Giao sáng 01\/12/ })).closest('li') as HTMLElement;
    fireEvent.contextMenu(item);
    await user.click(within(screen.getByRole('menu')).getByRole('menuitem', { name: 'Sao chép…' }));
    expect(await screen.findByRole('dialog', { name: /Sao chép "Giao sáng 01\/12"/ })).toBeInTheDocument();
  });
});
