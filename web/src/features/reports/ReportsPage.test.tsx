import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi as vitest } from 'vitest';
import type { Schemas } from '@/data/types';
import { wideWindow } from '@/test/media';
import { renderApp } from '@/test/renderApp';
import { sample } from '@/test/samples';
import { API, ok, server, signedInHandlers } from '@/test/server';

const report = (from: string, to: string): Schemas['StoreReportResponse'] => ({
  from,
  to,
  plans: { total: 9, draft: 1, published: 1, inProgress: 0, completed: 6, cancelled: 1 },
  stops: { total: 210, delivered: 190, failed: 12, skipped: 3, pending: 5, deliveredRate: 190 / 205 },
  plannedDistanceMeters: 412300,
  days: [
    { date: from, plans: 1, delivered: 30, failed: 2, skipped: 0, pending: 0 },
    { date: to, plans: 1, delivered: 31, failed: 3, skipped: 1, pending: 0 },
  ],
  drivers: [
    {
      driverId: 451,
      fullName: 'Tùng',
      routes: 6,
      delivered: 95,
      failed: 5,
      skipped: 1,
      pending: 0,
      plannedDistanceMeters: 201000,
      firstResultAt: '2026-12-01T01:12:00Z',
      lastResultAt: '2026-12-07T09:40:00Z',
    },
  ],
  failureReasons: [{ reason: 'Vắng nhà', count: 7 }],
});

const storeHandler = () => http.get(`${API}/store`, () => ok(sample<{ data: unknown }>('store').data));

afterEach(() => {
  vitest.restoreAllMocks();
});

describe('Báo cáo', () => {
  it('đọc khoảng trên URL, hiện thẻ số, tài xế, lý do; nút 7 ngày đổi URL và gọi lại API', async () => {
    const queries: string[] = [];
    server.use(
      http.get(`${API}/reports/summary`, ({ request }) => {
        const url = new URL(request.url);
        queries.push(url.search);
        return ok(report(url.searchParams.get('from') ?? '', url.searchParams.get('to') ?? ''));
      }),
      storeHandler(),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/reports?from=2026-12-01&to=2026-12-07');
    const user = userEvent.setup();

    expect(await screen.findByRole('heading', { name: 'Báo cáo' })).toBeInTheDocument();
    expect(await screen.findByText('412,3 km')).toBeInTheDocument();
    expect(queries[0]).toBe('?from=2026-12-01&to=2026-12-07');
    // Given to drivers: 1 published + 6 completed.
    expect(screen.getByText('Kế hoạch giao việc').parentElement).toHaveTextContent('Kế hoạch giao việc7');
    expect(screen.getByText('6 hoàn thành · 1 nháp · 1 đã huỷ')).toBeInTheDocument();
    expect(screen.getByText('92,7%')).toBeInTheDocument();
    expect(screen.getByRole('slider', { name: /Biểu đồ cột chồng/ })).toBeInTheDocument();
    // Phone (tests run at phone width): drivers and reasons are rows; a driver opens a sheet with all numbers (G6.5).
    const drivers = screen.getByRole('list', { name: 'Theo tài xế' });
    expect(within(drivers).getByText('Tùng')).toBeInTheDocument();
    await user.click(within(drivers).getByRole('button', { name: 'Tùng: xem chi tiết' }));
    const sheet = screen.getByRole('dialog', { name: 'Tài xế' });
    expect(sheet).toHaveTextContent('201,0 km');
    await user.click(within(sheet).getByRole('button', { name: 'Đóng' }));
    expect(
      within(screen.getByRole('list', { name: 'Lý do không giao được hay gặp' })).getByText('Vắng nhà'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Báo cáo' })).toHaveAttribute('aria-current', 'page');

    // The period is a segmented control (G6-Q1): a range not matching a preset shows "Tùy chọn" with the two dates.
    expect(screen.getByRole('tab', { name: 'Tùy chọn' })).toHaveAttribute('aria-selected', 'true');
    await user.click(screen.getByRole('tab', { name: '7 ngày' }));
    await waitFor(() =>
      expect(router.state.location.search).toMatch(/^\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}$/),
    );
    await waitFor(() => expect(queries.length).toBe(2));
    expect(screen.getByRole('tab', { name: '7 ngày' })).toHaveAttribute('aria-selected', 'true');
  });

  it('khoảng quá 31 ngày thì báo lỗi, không gọi API; một ngày thì không có biểu đồ', async () => {
    let calls = 0;
    server.use(
      http.get(`${API}/reports/summary`, ({ request }) => {
        calls += 1;
        const url = new URL(request.url);
        return ok(report(url.searchParams.get('from') ?? '', url.searchParams.get('to') ?? ''));
      }),
      storeHandler(),
      ...signedInHandlers(),
    );
    renderApp('/reports?from=2026-01-01&to=2026-02-01');
    expect(await screen.findByText('Chọn tối đa 31 ngày.')).toBeInTheDocument();
    expect(calls).toBe(0);
    expect(screen.getByRole('button', { name: 'Xuất CSV' })).toBeDisabled();
  });

  it('một ngày: không biểu đồ, giờ báo theo giờ cửa hàng; không có kế hoạch thì hiện trạng thái trống', async () => {
    server.use(
      http.get(`${API}/reports/summary`, ({ request }) => {
        const url = new URL(request.url);
        const from = url.searchParams.get('from') ?? '';
        if (from === '2026-12-09')
          return ok({ ...report(from, from), plans: { ...report(from, from).plans, total: 0 } });
        return ok(report(from, url.searchParams.get('to') ?? ''));
      }),
      storeHandler(),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/reports?from=2026-12-01&to=2026-12-01');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Tùng: xem chi tiết' }));
    expect(screen.getByRole('dialog', { name: 'Tài xế' })).toHaveTextContent('08:12');
    await user.click(screen.getByRole('button', { name: 'Đóng' }));
    expect(screen.queryByRole('slider', { name: /Biểu đồ/ })).not.toBeInTheDocument();

    await router.navigate('/reports?from=2026-12-09&to=2026-12-09');
    expect(await screen.findByText('Chưa có kế hoạch nào trong khoảng này')).toBeInTheDocument();
  });

  it('màn rộng: bảng tài xế trong nhóm, tuyến và km dưới tên, báo kết quả đầu – cuối một cột', async () => {
    wideWindow();
    server.use(
      http.get(`${API}/reports/summary`, () => ok(report('2026-12-01', '2026-12-07'))),
      storeHandler(),
      ...signedInHandlers(),
    );
    renderApp('/reports?from=2026-12-01&to=2026-12-07');
    const drivers = await screen.findByRole('table', { name: 'Theo tài xế' });
    expect(within(drivers).getAllByRole('columnheader')).toHaveLength(6);
    expect(within(drivers).getByText('Tùng').closest('td')).toHaveTextContent('6 tuyến · 201,0 km');
    expect(within(drivers).getByRole('columnheader', { name: 'Báo kết quả' })).toBeInTheDocument();
  });

  it('tài xế 10 người một trang: trang trên URL, đổi khoảng thì về trang 1, trang quá cuối thì về trang cuối', async () => {
    const many = (from: string, to: string) => {
      const base = report(from, to);
      const one = base.drivers[0] as Record<string, unknown>;
      return {
        ...base,
        drivers: Array.from({ length: 12 }, (_, i) => ({ ...one, driverId: 100 + i, fullName: `Tài xế ${i + 1}` })),
      };
    };
    server.use(
      http.get(`${API}/reports/summary`, ({ request }) => {
        const url = new URL(request.url);
        return ok(many(url.searchParams.get('from') ?? '', url.searchParams.get('to') ?? ''));
      }),
      storeHandler(),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/reports?from=2026-12-01&to=2026-12-07&page=9');
    const user = userEvent.setup();

    // Past the end: the last page.
    const list = await screen.findByRole('list', { name: 'Theo tài xế' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByRole('navigation', { name: 'Trang danh sách tài xế' })).toHaveTextContent('Trang 2/2');

    await user.click(screen.getByRole('button', { name: /Trang trước/ }));
    await waitFor(() => expect(router.state.location.search).not.toMatch(/page=/));
    expect(within(screen.getByRole('list', { name: 'Theo tài xế' })).getAllByRole('listitem')).toHaveLength(10);

    await user.click(screen.getByRole('button', { name: /Trang sau/ }));
    await waitFor(() => expect(router.state.location.search).toMatch(/page=2/));
    await user.click(screen.getByRole('tab', { name: '7 ngày' }));
    await waitFor(() => expect(router.state.location.search).not.toMatch(/page=/));
  });

  it('Xuất CSV tải file của API với tên trong Content-Disposition', async () => {
    let query = '';
    server.use(
      http.get(`${API}/reports/summary`, () => ok(report('2026-12-01', '2026-12-07'))),
      http.get(`${API}/reports/stops.csv`, ({ request }) => {
        query = new URL(request.url).search;
        return new HttpResponse('﻿Ngày\r\n', {
          headers: {
            'Content-Type': 'text/csv;charset=UTF-8',
            'Content-Disposition': 'attachment; filename="pathwise-20261201-20261207.csv"',
          },
        });
      }),
      storeHandler(),
      ...signedInHandlers(),
    );
    const createUrl = vitest.fn(() => 'blob:report');
    Object.assign(URL, { createObjectURL: createUrl, revokeObjectURL: vitest.fn() });
    const clicks: string[] = [];
    vitest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicks.push(this.download);
    });
    renderApp('/reports?from=2026-12-01&to=2026-12-07');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Xuất CSV' }));
    await waitFor(() => expect(clicks).toEqual(['pathwise-20261201-20261207.csv']));
    expect(query).toBe('?from=2026-12-01&to=2026-12-07');
    expect(createUrl).toHaveBeenCalledTimes(1);
  });
});
