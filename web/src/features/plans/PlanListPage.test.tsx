import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';

type Page = {
  items: Array<Record<string, unknown>>;
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
};

describe('Danh sách kế hoạch', () => {
  it('hiện kế hoạch theo prototype; lọc ngày và trạng thái nằm trên URL và gửi lên API', async () => {
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
    const table = screen.getByRole('table', { name: 'Kế hoạch giao hàng' });
    const row = within(table).getAllByRole('row')[1] as HTMLElement;
    expect(row).toHaveTextContent('Giao sáng 01/12');
    expect(row).toHaveTextContent('Thứ Ba, 01/12/2026');
    expect(row).toHaveTextContent('HOÀN THÀNH');
    expect(row).toHaveTextContent('15,4 km');
    expect(row).toHaveTextContent('5 giờ 06 phút');
    expect(screen.getByText('1–1 trong 1 kế hoạch')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Trạng thái'), 'COMPLETED');
    await waitFor(() => expect(router.state.location.search).toBe('?status=COMPLETED'));
    await waitFor(() => expect(queries.at(-1)).toContain('status=COMPLETED'));

    await user.click(within(row).getByRole('link', { name: 'Giao sáng 01/12' }));
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
    expect(screen.getByRole('link', { name: 'Tạo kế hoạch' })).toHaveAttribute('href', '/plans/new');
    expect(screen.getByLabelText('Ngày giao')).toHaveValue('2026-12-01');
  });

  it('phân trang: trang sau gửi page=1', async () => {
    const pages: string[] = [];
    const item = sample<{ data: Page }>('plan-list').data.items[0];
    server.use(
      http.get(`${API}/plans`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        pages.push(String(page));
        return ok({ items: [{ ...item, id: 83 + page }], page, size: 20, totalPages: 3, totalElements: 41 });
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/plans');
    const user = userEvent.setup();

    expect(await screen.findByText('1–1 trong 41 kế hoạch')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Trang trước' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Trang sau' }));

    expect(await screen.findByText('21–21 trong 41 kế hoạch')).toBeInTheDocument();
    expect(router.state.location.search).toBe('?page=1');
    expect(pages).toContain('1');
  });
});
