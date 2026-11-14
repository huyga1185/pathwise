import { expect, request, test } from '@playwright/test';
import { API, blockThirdParties, otpSentTo, RUN } from './support';

const ADMIN = { email: 'admin@pathwise.test', password: 'matkhau-admin-e2e' };

/**
 * The ADMIN area on the real backend (W11 acceptance): the first ADMIN of the stack (BOOTSTRAP_ADMIN_*, T59) finds a
 * store, locks it read only, and unlocks it; the store's OWNER cannot write while it is locked (423 STORE_READ_ONLY).
 */
test('ADMIN khoá rồi mở khoá một cửa hàng', async ({ page }) => {
  const ownerEmail = `owner-admin-${RUN}@pathwise.test`;
  const storeName = `Tiệm Khoá ${RUN}`;
  const password = 'matkhau-e2e-1';
  await blockThirdParties(page);

  const owner = await test.step('một cửa hàng có chủ đã xác thực (qua API)', async () => {
    const api = await request.newContext({ baseURL: API });
    const register = await api.post('/api/v1/auth/register', { data: { storeName, email: ownerEmail, password } });
    expect(register.status(), await register.text()).toBe(201);
    const verify = await api.post('/api/v1/auth/email/verify', {
      data: { email: ownerEmail, otp: await otpSentTo(ownerEmail) },
    });
    expect(verify.ok(), await verify.text()).toBe(true);
    const logIn = await api.post('/api/v1/auth/log-in', {
      headers: { 'X-Client-Platform': 'mobile' },
      data: { email: ownerEmail, password },
    });
    const token = ((await logIn.json()) as { data: { accessToken: string } }).data.accessToken;
    await api.dispose();
    return request.newContext({ baseURL: API, extraHTTPHeaders: { Authorization: `Bearer ${token}` } });
  });
  const addVehicle = (name: string) =>
    owner.post('/api/v1/vehicles', { data: { name, capacity: 10, capacityUnit: 'BOX' } });

  await test.step('ADMIN đăng nhập thì vào khu quản trị', async () => {
    await page.goto('login');
    await page.getByLabel(/^Email/).fill(ADMIN.email);
    await page.getByLabel(/^Mật khẩu/).fill(ADMIN.password);
    await page.getByRole('button', { name: /Đăng nhập/ }).click();
    await expect(page.getByRole('heading', { name: 'Tổng quan nền tảng' })).toBeVisible();
  });

  await test.step('tìm cửa hàng và khoá kèm lý do', async () => {
    await page.getByRole('link', { name: 'Cửa hàng', exact: true }).click();
    await page.getByLabel('Tìm theo tên cửa hàng hoặc email chủ').fill(ownerEmail);
    await page.keyboard.press('Enter');
    await page.getByRole('link', { name: storeName }).click();
    await expect(page.getByRole('heading', { name: storeName })).toBeVisible();
    await page.getByRole('button', { name: 'Khoá (chỉ đọc)' }).click();
    const dialog = page.getByRole('alertdialog');
    await dialog.getByLabel(/Lý do/).fill('Kiểm thử E2E');
    await dialog.getByRole('button', { name: 'Khoá cửa hàng' }).click();
    await expect(page.getByText('Đã khoá cửa hàng.')).toBeVisible();
    await expect(page.getByText('Lý do: Kiểm thử E2E')).toBeVisible();
  });

  await test.step('chủ cửa hàng không ghi được khi đang khoá', async () => {
    const locked = await addVehicle('Xe khi khoá');
    expect(locked.status()).toBe(423);
    expect(((await locked.json()) as { code: string }).code).toBe('STORE_READ_ONLY');
  });

  await test.step('mở khoá thì ghi lại được', async () => {
    await page.getByRole('button', { name: 'Mở khoá' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Mở khoá' }).click();
    await expect(page.getByText('Đã mở khoá cửa hàng.')).toBeVisible();
    expect((await addVehicle('Xe sau khi mở')).status()).toBe(201);
    await owner.dispose();
  });
});
