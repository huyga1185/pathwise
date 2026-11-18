import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Schemas } from '@/data/types';
import { wideWindow } from '@/test/media';
import { renderApp } from '@/test/renderApp';
import { API, ok, server, signedInHandlers } from '@/test/server';

const LONG_EMAIL = 'nguyen.thi.phuong.thao.hoang.bao.ngoc@chi-nhanh-hai-chau-da-nang.pathwise.test';
const row: Schemas['StoreAuditEventResponse'] = {
  id: 7,
  occurredAt: '2026-11-14T02:30:00Z',
  actorId: 3,
  actorEmail: LONG_EMAIL,
  actorRole: 'OWNER',
  action: 'INVITATION_REVOKED',
  targetType: 'INVITATION',
  targetId: 9,
  details: { email: 'moi@x.vn', role: 'DRIVER' },
  requestId: null,
  ip: '113.161.4.20',
};

describe('Nhật ký cửa hàng (G2.9, §8.10)', () => {
  it('bấm một dòng mở sheet "Chi tiết" với đủ chữ: hành động, thời điểm có năm, người làm, IP', async () => {
    server.use(
      http.get(`${API}/store/audit`, () => ok({ items: [row], page: 0, size: 20, totalPages: 1, totalElements: 1 })),
      ...signedInHandlers(),
    );
    wideWindow();
    renderApp('/store/audit');
    const user = userEvent.setup();

    // In the table the address is cut to one line, the whole of it in its title.
    const cell = await screen.findByTitle(LONG_EMAIL);
    await user.click(cell.closest('tr')!);
    const sheet = screen.getByRole('dialog', { name: 'Chi tiết' });
    expect(within(sheet).getByText('Huỷ lời mời moi@x.vn (Tài xế)')).toBeInTheDocument();
    expect(within(sheet).getByText('14/11/2026 09:30')).toBeInTheDocument();
    expect(within(sheet).getByText(LONG_EMAIL)).toBeInTheDocument();
    expect(within(sheet).getByText('113.161.4.20')).toBeInTheDocument();

    await user.click(within(sheet).getByRole('button', { name: 'Đóng' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('điện thoại: dòng là nút mở sheet; trang quá cuối (link cũ) thì về trang cuối', async () => {
    const pages: string[] = [];
    server.use(
      http.get(`${API}/store/audit`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        pages.push(String(page));
        return ok({ items: page >= 3 ? [] : [row], page, size: 20, totalPages: 3, totalElements: 41 });
      }),
      ...signedInHandlers(),
    );
    const { router } = renderApp('/store/audit?page=7');

    await waitFor(() => expect(router.state.location.search).toBe('?page=2'));
    await waitFor(() => expect(pages).toContain('2'));
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Huỷ lời mời moi@x\.vn/ }));
    expect(screen.getByRole('dialog', { name: 'Chi tiết' })).toBeInTheDocument();
  });
});
