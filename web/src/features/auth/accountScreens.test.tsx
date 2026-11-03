import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { anonymousHandlers, API, fail, ok, server } from '@/test/server';

const noContent = () => new HttpResponse(null, { status: 204 });

/** Fills the six OTP boxes by pasting into the first one, as a code copied from the email. */
async function pasteCode(user: ReturnType<typeof userEvent.setup>, code: string) {
  await user.click(screen.getByLabelText('Chữ số 1 trên 6'));
  await user.paste(code);
}

describe('Đăng ký cửa hàng', () => {
  it('tạo cửa hàng rồi sang xác thực email với nút gửi lại bị khoá 60 giây', async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/auth/register`, async ({ request }) => {
        body = await request.json();
        return ok({ email: 'lan@example.com', status: 'PENDING_VERIFICATION' }, { status: 201 });
      }),
      ...anonymousHandlers(),
    );
    const { router } = renderApp('/register');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên cửa hàng/), '  Tiệm bánh Cô Lan ');
    await user.type(screen.getByLabelText(/^Email/), 'lan@example.com');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: /Tạo cửa hàng/ }));

    expect(await screen.findByRole('heading', { name: 'Xác thực email' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?email=lan%40example.com');
    expect(body).toEqual({ storeName: 'Tiệm bánh Cô Lan', email: 'lan@example.com', password: 'matkhau123' });
    expect(screen.getByText('lan@example.com')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /Gửi lại mã sau (1:00|0:5\d)/ })).toBeDisabled();
  });

  it('báo lỗi ngay dưới ô khi bỏ trống và không gửi request', async () => {
    let called = false;
    server.use(
      http.post(`${API}/auth/register`, () => {
        called = true;
        return ok({});
      }),
      ...anonymousHandlers(),
    );
    renderApp('/register');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Tạo cửa hàng/ }));

    expect(await screen.findByText('Vui lòng nhập tên cửa hàng.')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng nhập email.')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng nhập mật khẩu.')).toBeInTheDocument();
    expect(screen.getByLabelText(/^Tên cửa hàng/)).toHaveAttribute('aria-invalid', 'true');
    expect(called).toBe(false);
  });

  it('email đã dùng thì báo dưới ô email kèm link đăng nhập', async () => {
    server.use(
      http.post(`${API}/auth/register`, () => fail(409, 'EMAIL_EXISTED')),
      ...anonymousHandlers(),
    );
    renderApp('/register');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên cửa hàng/), 'Tiệm A');
    await user.type(screen.getByLabelText(/^Email/), 'lan@example.com');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: /Tạo cửa hàng/ }));

    const message = await screen.findByText(/Email này đã được dùng\./);
    expect(screen.getByLabelText(/^Email/)).toHaveAttribute('aria-invalid', 'true');
    expect(within(message).getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
  });

  it('lỗi theo field của server hiện dưới đúng ô', async () => {
    server.use(
      http.post(`${API}/auth/register`, () =>
        fail(400, 'VALIDATION_ERROR', [{ field: 'storeName', code: 'INVALID_STORE_NAME_LENGTH' }]),
      ),
      ...anonymousHandlers(),
    );
    renderApp('/register');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên cửa hàng/), 'Tiệm A');
    await user.type(screen.getByLabelText(/^Email/), 'lan@example.com');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: /Tạo cửa hàng/ }));

    expect(await screen.findByText('Tên cửa hàng quá dài hoặc quá ngắn.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('bị giới hạn thì hiện hộp lỗi và khoá nút theo Retry-After', async () => {
    server.use(
      http.post(`${API}/auth/register`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '3480' })),
      ...anonymousHandlers(),
    );
    renderApp('/register');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Tên cửa hàng/), 'Tiệm A');
    await user.type(screen.getByLabelText(/^Email/), 'lan@example.com');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: /Tạo cửa hàng/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Bạn thao tác quá nhiều lần. Thử lại sau 58 phút.');
    expect(screen.getByRole('button', { name: /Thử lại sau/ })).toBeDisabled();
  });
});

describe('Xác thực email', () => {
  it('dán mã 6 số vào ô đầu rồi xác thực thành công', async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/auth/email/verify`, async ({ request }) => {
        body = await request.json();
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/verify-email?email=lan%40example.com');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Xác thực email' });
    await pasteCode(user, '482913');
    expect(screen.getByLabelText('Chữ số 6 trên 6')).toHaveValue('3');
    await user.click(screen.getByRole('button', { name: 'Xác thực' }));

    expect(await screen.findByText('Email đã được xác thực.')).toBeInTheDocument();
    expect(body).toEqual({ email: 'lan@example.com', otp: '482913' });
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
  });

  it('sai 3 lần thì khoá nút xác thực tới khi gửi lại mã mới', async () => {
    let resent = 0;
    server.use(
      http.post(`${API}/auth/email/verify`, () => fail(400, 'INVALID_OTP')),
      http.post(`${API}/auth/email/verify/resend`, () => {
        resent += 1;
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/verify-email?email=lan%40example.com');
    const user = userEvent.setup();
    await screen.findByRole('heading', { name: 'Xác thực email' });
    const verify = screen.getByRole('button', { name: 'Xác thực' });

    await pasteCode(user, '111111');
    await user.click(verify);
    expect(await screen.findByText('Mã OTP không đúng hoặc đã hết hạn.')).toBeInTheDocument();
    expect(screen.getByLabelText('Chữ số 1 trên 6')).toHaveAttribute('aria-invalid', 'true');
    await user.click(verify);
    await waitFor(() => expect(verify).toBeEnabled());
    await user.click(verify);

    expect(await screen.findByText('Bạn đã nhập sai 3 lần, mã này đã bị huỷ. Gửi lại mã mới.')).toBeInTheDocument();
    expect(verify).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Gửi lại mã' }));
    expect(await screen.findByText('Đã gửi mã mới.')).toBeInTheDocument();
    expect(resent).toBe(1);
    expect(screen.getByLabelText('Chữ số 1 trên 6')).toHaveValue('');
    expect(screen.getByRole('button', { name: /Gửi lại mã sau/ })).toBeDisabled();
    await pasteCode(user, '222222');
    expect(verify).toBeEnabled();
  });

  it('email đã xác thực thì báo có thể đăng nhập', async () => {
    server.use(
      http.post(`${API}/auth/email/verify`, () => fail(409, 'EMAIL_ALREADY_VERIFIED')),
      ...anonymousHandlers(),
    );
    renderApp('/verify-email?email=lan%40example.com');
    const user = userEvent.setup();
    await screen.findByRole('heading', { name: 'Xác thực email' });

    await pasteCode(user, '482913');
    await user.click(screen.getByRole('button', { name: 'Xác thực' }));

    expect(await screen.findByText('Email đã được xác thực, bạn có thể đăng nhập.')).toBeInTheDocument();
  });

  it('gửi lại mã bị giới hạn thì báo lỗi và khoá nút gửi lại', async () => {
    server.use(
      http.post(`${API}/auth/email/verify/resend`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '45' })),
      ...anonymousHandlers(),
    );
    renderApp('/verify-email?email=lan%40example.com');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Gửi lại mã' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Thử lại sau 45 giây.');
    expect(screen.getByRole('button', { name: /Gửi lại mã sau 0:4\d/ })).toBeDisabled();
  });

  it('mở từ đăng nhập mà không có email trên URL thì hỏi email', async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/auth/email/verify`, async ({ request }) => {
        body = await request.json();
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/verify-email');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Email/), 'lan@example.com');
    await pasteCode(user, '482913');
    await user.click(screen.getByRole('button', { name: 'Xác thực' }));

    expect(await screen.findByText('Email đã được xác thực.')).toBeInTheDocument();
    expect(body).toEqual({ email: 'lan@example.com', otp: '482913' });
  });
});

describe('Quên mật khẩu', () => {
  it('đi đủ 3 bước: email, mã, mật khẩu mới', async () => {
    const calls: Array<[string, unknown]> = [];
    const record =
      (name: string) =>
      async ({ request }: { request: Request }) => {
        calls.push([name, await request.json()]);
      };
    server.use(
      http.post(`${API}/auth/password/forgot`, async (info) => {
        await record('forgot')(info);
        return noContent();
      }),
      http.post(`${API}/auth/password/reset/verify`, async (info) => {
        await record('verify')(info);
        return ok({ resetToken: 'reset-1', expiresAt: '2026-11-04T10:10:00Z' });
      }),
      http.post(`${API}/auth/password/reset`, async (info) => {
        await record('reset')(info);
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/forgot-password');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Email/), 'lan@example.com');
    await user.click(screen.getByRole('button', { name: 'Gửi mã' }));
    expect(
      await screen.findByText('Nếu email này có tài khoản, mã 6 số đã được gửi tới hộp thư. Mã có hiệu lực 5 phút.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nhập mã xác thực' })).toBeInTheDocument();

    await user.type(screen.getByLabelText('Mã xác thực'), '48 29 13');
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));
    await user.type(await screen.findByLabelText(/^Mật khẩu mới/), 'matkhaumoi1');
    await user.type(screen.getByLabelText(/^Nhập lại mật khẩu mới/), 'matkhaumoi1');
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));

    expect(
      await screen.findByText('Mọi thiết bị đã bị đăng xuất. Đăng nhập lại bằng mật khẩu mới.'),
    ).toBeInTheDocument();
    expect(calls).toEqual([
      ['forgot', { email: 'lan@example.com' }],
      ['verify', { email: 'lan@example.com', otp: '482913' }],
      ['reset', { resetToken: 'reset-1', newPassword: 'matkhaumoi1' }],
    ]);
  });

  it('mật khẩu nhập lại không khớp thì báo dưới ô và không gửi', async () => {
    let reset = false;
    server.use(
      http.post(`${API}/auth/password/forgot`, noContent),
      http.post(`${API}/auth/password/reset/verify`, () => ok({ resetToken: 'reset-1' })),
      http.post(`${API}/auth/password/reset`, () => {
        reset = true;
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/forgot-password');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Email/), 'lan@example.com');
    await user.click(screen.getByRole('button', { name: 'Gửi mã' }));
    await user.type(await screen.findByLabelText('Mã xác thực'), '482913');
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));

    await user.type(await screen.findByLabelText(/^Mật khẩu mới/), 'matkhaumoi1');
    await user.type(screen.getByLabelText(/^Nhập lại mật khẩu mới/), 'matkhaumoi2');
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));

    expect(await screen.findByText('Mật khẩu nhập lại không khớp.')).toBeInTheDocument();
    expect(reset).toBe(false);
  });

  it('mã sai thì báo dưới ô; quá 10 phút thì báo làm lại từ bước 1', async () => {
    let tries = 0;
    server.use(
      http.post(`${API}/auth/password/forgot`, noContent),
      http.post(`${API}/auth/password/reset/verify`, () =>
        ++tries === 1 ? fail(400, 'INVALID_OTP') : ok({ resetToken: 'reset-1' }),
      ),
      http.post(`${API}/auth/password/reset`, () => fail(400, 'INVALID_RESET_TOKEN')),
      ...anonymousHandlers(),
    );
    renderApp('/forgot-password');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Email/), 'lan@example.com');
    await user.click(screen.getByRole('button', { name: 'Gửi mã' }));
    await user.type(await screen.findByLabelText('Mã xác thực'), '111111');
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));

    expect(await screen.findByText('Mã OTP không đúng hoặc đã hết hạn.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tiếp tục' }));
    await user.type(await screen.findByLabelText(/^Mật khẩu mới/), 'matkhaumoi1');
    await user.type(screen.getByLabelText(/^Nhập lại mật khẩu mới/), 'matkhaumoi1');
    await user.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Phiên đặt lại mật khẩu đã hết hạn, vui lòng làm lại.');
    await user.click(screen.getByRole('button', { name: 'Làm lại từ bước 1' }));
    expect(screen.getByRole('heading', { name: 'Quên mật khẩu' })).toBeInTheDocument();
  });

  it('bị giới hạn ở bước 1 thì khoá nút theo Retry-After', async () => {
    server.use(
      http.post(`${API}/auth/password/forgot`, () => fail(429, 'RATE_LIMITED', undefined, { 'Retry-After': '120' })),
      ...anonymousHandlers(),
    );
    renderApp('/forgot-password');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Email/), 'lan@example.com');
    await user.click(screen.getByRole('button', { name: 'Gửi mã' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Thử lại sau 2 phút.');
    expect(screen.getByRole('button', { name: /Thử lại sau/ })).toBeDisabled();
  });
});

describe('Nhận lời mời', () => {
  const preview = {
    storeName: 'Tiệm bánh Cô Lan',
    email: 'taixe.b@example.com',
    role: 'DRIVER',
    expiresAt: '2026-11-10T03:00:00Z',
  };
  const CODE = 'a'.repeat(43);

  it('kiểm tra mã, tạo tài khoản, tài xế được hướng dẫn mở app', async () => {
    let body: unknown;
    server.use(
      http.get(`${API}/invitations/:code`, ({ params }) =>
        params.code === CODE ? ok(preview) : fail(404, 'NOT_FOUND'),
      ),
      http.post(`${API}/invitations/accept`, async ({ request }) => {
        body = await request.json();
        return noContent();
      }),
      ...anonymousHandlers(),
    );
    renderApp('/invite');
    const user = userEvent.setup();

    await user.click(await screen.findByLabelText(/^Mã mời/));
    await user.paste(`  ${CODE}\n`);
    await user.click(screen.getByRole('button', { name: 'Kiểm tra mã' }));

    expect(await screen.findByRole('heading', { name: 'Tạo tài khoản' })).toBeInTheDocument();
    expect(screen.getByText('Tiệm bánh Cô Lan')).toBeInTheDocument();
    expect(screen.getByText('Tài xế')).toBeInTheDocument();
    expect(screen.getByText('taixe.b@example.com')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/^Họ tên/), 'Tài xế B');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Tham gia cửa hàng' }));

    expect(await screen.findByText('Bạn đã tham gia Tiệm bánh Cô Lan.')).toBeInTheDocument();
    expect(
      screen.getByText('Mở app PathWise trên điện thoại và đăng nhập bằng email này để nhận tuyến giao.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Đăng nhập trên web' })).not.toBeInTheDocument();
    expect(body).toEqual({ code: CODE, fullName: 'Tài xế B', password: 'matkhau123' });
  });

  it('điều phối viên nhận lời mời xong thì được mời đăng nhập web', async () => {
    server.use(
      http.get(`${API}/invitations/:code`, () => ok({ ...preview, role: 'DISPATCHER' })),
      http.post(`${API}/invitations/accept`, noContent),
      ...anonymousHandlers(),
    );
    renderApp('/invite');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Mã mời/), CODE);
    await user.click(screen.getByRole('button', { name: 'Kiểm tra mã' }));
    await user.type(await screen.findByLabelText(/^Họ tên/), 'Điều phối C');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Tham gia cửa hàng' }));

    expect(await screen.findByRole('link', { name: 'Đăng nhập trên web' })).toHaveAttribute('href', '/login');
  });

  it('mã không dùng được thì báo dưới ô mã', async () => {
    server.use(
      http.get(`${API}/invitations/:code`, () => fail(404, 'INVITATION_INVALID')),
      ...anonymousHandlers(),
    );
    renderApp('/invite');
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText(/^Mã mời/), 'sai-ma');
    await user.click(screen.getByRole('button', { name: 'Kiểm tra mã' }));

    expect(await screen.findByText(/^Lời mời không còn hiệu lực/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Mã mời/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('số điện thoại sai thì báo dưới ô số điện thoại', async () => {
    server.use(
      http.get(`${API}/invitations/:code`, () => ok(preview)),
      http.post(`${API}/invitations/accept`, () => fail(400, 'INVALID_PHONE_NUMBER')),
      ...anonymousHandlers(),
    );
    renderApp('/invite');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/^Mã mời/), CODE);
    await user.click(screen.getByRole('button', { name: 'Kiểm tra mã' }));
    await user.type(await screen.findByLabelText(/^Họ tên/), 'Tài xế B');
    await user.type(screen.getByLabelText(/^Số điện thoại/), '12345');
    await user.type(screen.getByLabelText(/^Mật khẩu/), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Tham gia cửa hàng' }));

    expect(await screen.findByText('Số điện thoại không hợp lệ.')).toBeInTheDocument();
    expect(screen.getByLabelText(/^Số điện thoại/)).toHaveAttribute('aria-invalid', 'true');
  });
});
