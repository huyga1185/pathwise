import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { anonymousHandlers, driver, owner, server, signedInHandlers } from '@/test/server';

describe('Trang không tìm thấy', () => {
  it('chưa đăng nhập: thẻ trên ảnh màn tài khoản và nút đăng nhập', async () => {
    server.use(...anonymousHandlers());
    renderApp('/khong-co-trang-nay');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang', level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('navigation', { name: 'Điều hướng chính' })).not.toBeInTheDocument();
  });

  it('đã đăng nhập: vẫn trong khung app (điều hướng, tài khoản), về trang chính của người dùng', async () => {
    server.use(...signedInHandlers());
    renderApp('/khong-co-trang-nay');

    const section = await screen.findByRole('region', { name: 'Không tìm thấy trang' });
    expect(within(section).getByRole('link', { name: 'Về trang chính' })).toHaveAttribute('href', '/plans');
    expect(screen.getByRole('navigation', { name: 'Điều hướng chính' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Tài khoản:/ })).toBeInTheDocument();
  });
});

describe('Trang không có quyền', () => {
  it('đã đăng nhập: ổ khoá trong khung app, câu chung, về trang chính', async () => {
    server.use(...signedInHandlers());
    renderApp('/forbidden');

    const section = await screen.findByRole('region', { name: 'Bạn không có quyền xem trang này' });
    expect(within(section).getByText('Về trang chính hoặc quay lại trang trước để tiếp tục.')).toBeInTheDocument();
    expect(within(section).getByRole('link', { name: 'Về trang chính' })).toHaveAttribute('href', '/plans');
    expect(screen.getByRole('navigation', { name: 'Điều hướng chính' })).toBeInTheDocument();
  });

  it('chưa đăng nhập: bảo đăng nhập bằng tài khoản có quyền, nút đăng nhập', async () => {
    server.use(...anonymousHandlers());
    renderApp('/forbidden');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
    expect(screen.getByText('Đăng nhập bằng tài khoản có quyền để xem trang này.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
  });
});

describe('Phân quyền theo vai trò (P-75)', () => {
  it('tài xế cố mở trang của cửa hàng thì thấy 403, nút về trang dùng app', async () => {
    server.use(...signedInHandlers(driver));
    renderApp('/plans');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
    expect(screen.getByText('Về trang chính hoặc quay lại trang trước để tiếp tục.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Về trang chính' })).toHaveAttribute('href', '/driver');
  });

  it('tài xế vẫn vào được trang dùng app', async () => {
    server.use(...signedInHandlers(driver));
    renderApp('/driver');

    expect(await screen.findByRole('heading', { name: 'Dùng app tài xế' })).toBeInTheDocument();
  });

  it('chủ cửa hàng mở trang dùng app của tài xế thì thấy 403', async () => {
    server.use(...signedInHandlers());
    renderApp('/driver');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
  });

  it('điều phối viên mở nhật ký cửa hàng (chỉ chủ) thì thấy 403', async () => {
    server.use(...signedInHandlers({ ...owner, role: 'DISPATCHER' }));
    renderApp('/store/audit');

    expect(await screen.findByRole('heading', { name: 'Bạn không có quyền xem trang này' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Điều hướng chính' })).toBeInTheDocument();
  });

  it('tài xế mở / thì về trang của mình, không 403', async () => {
    server.use(...signedInHandlers(driver));
    const { router } = renderApp('/');

    expect(await screen.findByRole('heading', { name: 'Dùng app tài xế' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/driver');
  });
});
