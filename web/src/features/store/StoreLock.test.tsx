import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Schemas } from '@/data/types';
import { renderApp } from '@/test/renderApp';
import { API, ok, owner, server, signedInHandlers } from '@/test/server';

const lockedOwner = { ...owner, storeStatus: 'READ_ONLY', storeLockReason: 'Chưa thanh toán phí tháng 11' };
const event = (patch: Partial<Schemas['StoreAuditEventResponse']>): Schemas['StoreAuditEventResponse'] => ({
  id: 1,
  occurredAt: '2026-11-14T02:30:00Z',
  actorId: 1,
  actorEmail: 'admin@pathwise.vn',
  actorRole: 'ADMIN',
  action: 'STORE_LOCKED',
  targetType: 'STORE',
  targetId: 182,
  details: { reason: 'Chưa thanh toán phí tháng 11' },
  requestId: null,
  ip: null,
  ...patch,
});

describe('Cửa hàng bị khoá (W11, T56)', () => {
  it('băng khoá hiện lý do trên mọi màn; không có nút tạo kế hoạch; form xe không lưu được', async () => {
    server.use(
      http.get(`${API}/vehicles`, () => ok([])),
      ...signedInHandlers(lockedOwner as never),
    );
    const { router } = renderApp('/plans');

    const banner = await screen.findByText('Cửa hàng đang tạm khoá:');
    expect(banner.closest('[role="status"]')).toHaveTextContent('Chưa thanh toán phí tháng 11');
    expect(screen.queryByRole('link', { name: 'Tạo kế hoạch' })).not.toBeInTheDocument();

    await router.navigate('/vehicles');
    const form = await screen.findByRole('form', { name: 'Thêm xe' });
    expect(within(form).getByRole('button', { name: 'Thêm xe' })).toBeDisabled();
    expect(screen.getByText('Cửa hàng đang tạm khoá:')).toBeInTheDocument();
  });

  it('nhật ký của chủ: tab trong Cửa hàng, người làm là quản trị viên thì không có email hay IP', async () => {
    server.use(
      http.get(`${API}/store/audit`, () =>
        ok({
          items: [
            event({}),
            event({
              id: 2,
              actorEmail: 'chu.cuahang@example.com',
              actorRole: 'OWNER',
              action: 'INVITATION_REVOKED',
              details: { email: 'moi@x.vn', role: 'DRIVER' },
              ip: '113.161.4.20',
            }),
          ],
          page: 0,
          size: 20,
          totalPages: 1,
          totalElements: 2,
        }),
      ),
      ...signedInHandlers(),
    );
    renderApp('/store/audit');

    expect(await screen.findByText('Tạm khoá cửa hàng: Chưa thanh toán phí tháng 11')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nhật ký' })).toHaveAttribute('aria-current', 'page');
    const adminRow = screen.getByText('Quản trị viên PathWise').closest('tr')!;
    expect(adminRow).not.toHaveTextContent('admin@pathwise.vn');
    expect(screen.getByText('Huỷ lời mời moi@x.vn (Tài xế)')).toBeInTheDocument();
    expect(screen.getByText('113.161.4.20')).toBeInTheDocument();
  });
});
