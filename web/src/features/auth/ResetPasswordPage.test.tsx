import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { anonymousHandlers, API, fail, server } from '@/test/server';

const TOKEN = 'x'.repeat(43);

describe('ResetPasswordPage (W11, T57)', () => {
  it('gửi mã theo token, rồi mã + mật khẩu mới thì xong; không hỏi email', async () => {
    const bodies: unknown[] = [];
    server.use(
      http.post(`${API}/auth/password/reset-link/otp`, async ({ request }) => {
        bodies.push(await request.json());
        return new HttpResponse(null, { status: 204 });
      }),
      http.post(`${API}/auth/password/reset-link`, async ({ request }) => {
        bodies.push(await request.json());
        return new HttpResponse(null, { status: 204 });
      }),
      ...anonymousHandlers(),
    );
    renderApp(`/reset-password?token=${TOKEN}`);
    const user = userEvent.setup();

    expect(await screen.findByRole('heading', { name: 'Đặt lại mật khẩu' })).toBeInTheDocument();
    expect(screen.queryByLabelText(/Email/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Gửi mã' }));
    expect(await screen.findByText('Đã gửi mã tới email của bạn.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Gửi lại mã \(/ })).toBeDisabled();

    await user.type(screen.getByLabelText('Mã xác nhận'), '123456');
    await user.type(screen.getByLabelText(/^Mật khẩu mới/), 'matkhau-moi-1');
    await user.type(screen.getByLabelText(/^Nhập lại mật khẩu mới/), 'matkhau-moi-1');
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));

    expect(await screen.findByText('Đã đặt mật khẩu mới.')).toBeInTheDocument();
    expect(bodies).toEqual([{ token: TOKEN }, { token: TOKEN, otp: '123456', newPassword: 'matkhau-moi-1' }]);
  });

  it('mã sai báo dưới ô và giữ trang; link hết hạn thay cả trang bằng đường sang Quên mật khẩu', async () => {
    server.use(
      http.post(`${API}/auth/password/reset-link/otp`, () => new HttpResponse(null, { status: 204 })),
      http.post(`${API}/auth/password/reset-link`, () => fail(400, 'INVALID_OTP')),
      ...anonymousHandlers(),
    );
    renderApp(`/reset-password?token=${TOKEN}`);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Gửi mã' }));
    await user.type(await screen.findByLabelText('Mã xác nhận'), '000000');
    await user.type(screen.getByLabelText(/^Mật khẩu mới/), 'matkhau-moi-1');
    await user.type(screen.getByLabelText(/^Nhập lại mật khẩu mới/), 'matkhau-moi-1');
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));
    expect(await screen.findByText('Mã OTP không đúng hoặc đã hết hạn.')).toBeInTheDocument();

    server.use(http.post(`${API}/auth/password/reset-link`, () => fail(400, 'INVALID_RESET_TOKEN')));
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));
    expect(await screen.findByText('Link đã hết hạn hoặc đã được dùng.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Quên mật khẩu' })).toHaveAttribute('href', '/forgot-password');
  });

  it('không có token thì báo link không dùng được ngay', async () => {
    server.use(...anonymousHandlers());
    renderApp('/reset-password');
    await waitFor(() => expect(screen.getByText('Link đã hết hạn hoặc đã được dùng.')).toBeInTheDocument());
  });
});
