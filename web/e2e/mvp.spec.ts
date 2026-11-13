import { expect, test } from '@playwright/test';
import { blockThirdParties, driverApi, invitationCodeSentTo, otpSentTo, RUN, todayInVietnam } from './support';

/**
 * The MVP path of docs/client/PLAN.md W07 on the real backend: register → verify (Mailpit) → depot → vehicle →
 * invite a driver → plan → coordinates → optimize → publish → the driver reports results (API) → COMPLETED.
 */
test('chủ cửa hàng đi hết luồng MVP tới khi kế hoạch hoàn thành', async ({ page, browser }) => {
  const owner = `owner-${RUN}@pathwise.test`;
  const driverEmail = `driver-${RUN}@pathwise.test`;
  const password = 'matkhau-e2e-1';
  await blockThirdParties(page);

  await test.step('đăng ký cửa hàng và xác thực email bằng mã trong Mailpit', async () => {
    await page.goto('register');
    await page.getByLabel(/^Tên cửa hàng/).fill(`Tiệm E2E ${RUN}`);
    await page.getByLabel(/^Email/).fill(owner);
    await page.getByLabel(/^Mật khẩu/).fill(password);
    await page.getByRole('button', { name: /Tạo cửa hàng/ }).click();
    await expect(page.getByRole('heading', { name: 'Xác thực email' })).toBeVisible();
    await page.getByLabel('Chữ số 1 trên 6').fill(await otpSentTo(owner));
    await page.getByRole('button', { name: 'Xác thực' }).click();
    await expect(page.getByText('Email đã được xác thực.')).toBeVisible();
  });

  await test.step('đăng nhập', async () => {
    await page.getByRole('link', { name: 'Đăng nhập', exact: true }).click();
    await page.getByLabel(/^Email/).fill(owner);
    await page.getByLabel(/^Mật khẩu/).fill(password);
    await page.getByRole('button', { name: /Đăng nhập/ }).click();
    await expect(page.getByRole('heading', { name: 'Kế hoạch giao hàng' })).toBeVisible();
  });

  await test.step('đặt kho xuất phát bằng ghim', async () => {
    await page.getByRole('link', { name: 'Cửa hàng', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('Chưa có kho xuất phát');
    await page.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }).click();
    await page.getByLabel(/^Địa chỉ kho/).fill('Kho 45 Lê Lợi, Hải Châu, Đà Nẵng');
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
    await expect(page.getByText(/Đã lưu thông tin cửa hàng/)).toBeVisible();
  });

  await test.step('thêm xe', async () => {
    await page.getByRole('link', { name: 'Xe', exact: true }).click();
    const form = page.getByRole('form', { name: 'Thêm xe' });
    await form.getByLabel(/^Tên xe/).fill('Xe 1');
    await form.getByLabel(/^Sức chứa/).fill('30');
    await form.getByLabel(/^Đơn vị/).selectOption('BOX');
    await form.getByRole('button', { name: 'Thêm xe' }).click();
    await expect(page.getByText('Đã thêm xe.')).toBeVisible();
    await expect(page.getByRole('table', { name: 'Xe của cửa hàng' })).toContainText('30 thùng');
  });

  await test.step('mời tài xế; tài xế nhận lời mời bằng mã trong email', async () => {
    await page.getByRole('link', { name: 'Thành viên', exact: true }).click();
    await page.getByRole('tab', { name: /^Lời mời/ }).click();
    const form = page.getByRole('form', { name: 'Mời thành viên' });
    await form.getByLabel(/^Email/).fill(driverEmail);
    await form.getByRole('button', { name: 'Gửi lời mời' }).click();
    await expect(page.getByText(`Đã gửi lời mời tới ${driverEmail}.`)).toBeVisible();

    const code = await invitationCodeSentTo(driverEmail);
    const driverContext = await browser.newContext({ locale: 'vi-VN', timezoneId: 'Asia/Ho_Chi_Minh' });
    const invite = await driverContext.newPage();
    await blockThirdParties(invite);
    await invite.goto('invite');
    await invite.getByLabel(/^Mã mời/).fill(code);
    await invite.getByRole('button', { name: 'Kiểm tra mã' }).click();
    await invite.getByLabel(/^Họ tên/).fill('Tài xế E2E');
    await invite.getByLabel(/^Mật khẩu/).fill(password);
    await invite.getByRole('button', { name: 'Tham gia cửa hàng' }).click();
    await expect(invite.getByText(/^Bạn đã tham gia/)).toBeVisible();
    await driverContext.close();
  });

  const addresses = ['879 Hùng Vương, Hải Châu', '12 Lê Duẩn, Hải Châu', '45 Nguyễn Văn Linh, Thanh Khê'];
  await test.step('tạo kế hoạch: một phân công, ba điểm giao; lưu thì tìm toạ độ', async () => {
    await page.getByRole('link', { name: 'Kế hoạch', exact: true }).click();
    await page.getByRole('link', { name: 'Tạo kế hoạch' }).first().click();
    await page.getByLabel(/^Tên kế hoạch/).fill(`Giao E2E ${RUN}`);
    await page.getByRole('button', { name: '+ Thêm phân công' }).click();
    await page.getByLabel('Tài xế phân công 1').selectOption({ label: 'Tài xế E2E' });
    await page.getByLabel('Xe phân công 1').selectOption({ label: 'Xe 1 · 30 thùng' });
    for (const [index, address] of addresses.entries()) {
      const n = index + 1;
      if (n > 1) await page.getByRole('button', { name: '+ Thêm điểm giao' }).click();
      await page.getByLabel(`Mã đơn dòng ${n}`).fill(`ĐH-${n}`);
      await page.getByLabel(`Địa chỉ dòng ${n}`).fill(address);
      await page.getByLabel(`Số thùng dòng ${n}`).fill('2');
    }
    await page.getByRole('button', { name: 'Lưu và tìm toạ độ' }).first().click();
    await expect(page).toHaveURL(/\/plans\/\d+$/);
    // The geocode job runs in the background; the page polls until every stop has coordinates.
    await expect(page.getByText('Mọi điểm giao đã có toạ độ.')).toBeVisible({ timeout: 60_000 });
  });

  await test.step('tối ưu lộ trình rồi giao việc', async () => {
    await page.getByRole('button', { name: 'Tối ưu lộ trình' }).click();
    await expect(page.getByRole('region', { name: 'Tổng quan' })).toContainText('3/3 đã xếp tuyến', {
      timeout: 60_000,
    });
  });

  await test.step('chỉnh tay: kéo điểm đầu xuống cuối bằng bàn phím rồi lưu (W10)', async () => {
    await page.getByRole('button', { name: 'Chỉnh tay' }).click();
    const save = page.getByRole('button', { name: 'Lưu thay đổi' });
    await expect(save).toBeDisabled();
    const firstHandle = page.getByRole('button', { name: /^Kéo .*vị trí 1$/ }).first();
    const moved = ((await firstHandle.getAttribute('aria-label')) ?? '').replace(/^Kéo (.*), .* vị trí 1$/, '$1');
    await firstHandle.focus();
    // The sensor measures the items after the lift: give each key a moment, as a person would.
    for (const key of ['Space', 'ArrowDown', 'ArrowDown', 'Space']) {
      await page.keyboard.press(key);
      await page.waitForTimeout(200);
    }
    await expect(page.getByRole('button', { name: `Kéo ${moved}, Tài xế E2E vị trí 3` })).toBeVisible();
    await save.click();
    await expect(page.getByText('Đã lưu tuyến chỉnh tay.')).toBeVisible();
  });

  await test.step('giao việc', async () => {
    await page.getByRole('button', { name: 'Giao việc cho 1 tài xế' }).click();
    // A manual edit may break a time window: the button then reads "Vẫn giao việc" (P-58).
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /^(Giao việc|Vẫn giao việc)$/ })
      .click();
    await expect(page.getByText('Đã giao việc cho 1 tài xế.')).toBeVisible();
    await expect(page.getByText(/Tự tải lại mỗi 30 giây/)).toBeVisible();
  });

  await test.step('tài xế báo kết quả qua API của app', async () => {
    const driver = await driverApi(driverEmail, password);
    const routes = await driver.get(`/api/v1/driver/routes?date=${todayInVietnam()}`);
    expect(routes.status()).toBe(200);
    const routeId = ((await routes.json()) as { data: Array<{ routeId: number }> }).data[0]?.routeId;
    const detail = await driver.get(`/api/v1/driver/routes/${routeId}`);
    const stops = ((await detail.json()) as { data: { stops: Array<{ id: number }> } }).data.stops;
    expect(stops).toHaveLength(3);
    const results = [{ status: 'DELIVERED' }, { status: 'FAILED', failureReason: 'Vắng nhà' }, { status: 'DELIVERED' }];
    for (const [index, stop] of stops.entries()) {
      const update = await driver.patch(`/api/v1/driver/routes/${routeId}/stops/${stop.id}/status`, {
        data: results[index],
      });
      expect(update.status(), await update.text()).toBe(200);
    }
    await driver.dispose();
  });

  await test.step('theo dõi: kế hoạch hoàn thành, có lý do không giao được', async () => {
    await page.reload();
    await expect(page.getByText(/Kế hoạch đã kết thúc/)).toBeVisible();
    await expect(page.getByRole('region', { name: 'Kết quả', exact: true })).toContainText('2 / 3 điểm');
    await expect(page.getByText('Lý do: Vắng nhà')).toBeVisible();
    await expect(page.getByText('HOÀN THÀNH').first()).toBeVisible();
  });
});
