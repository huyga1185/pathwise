import { expect, request, test } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { API, blockThirdParties, otpSentTo, RUN } from './support';

const FIXTURE = fileURLToPath(new URL('./fixtures/diem-giao.csv', import.meta.url));

/**
 * W12 acceptance on the real backend: a stop file (with coordinates, so no geocoding service is needed) goes into a
 * new plan through the import dialog, its bad row is left out, and the plan is saved with the two good stops.
 */
test('nhập file điểm giao vào kế hoạch mới rồi lưu', async ({ page }) => {
  const email = `owner-import-${RUN}@pathwise.test`;
  const password = 'matkhau-e2e-1';
  await blockThirdParties(page);

  await test.step('cửa hàng có kho (qua API)', async () => {
    const api = await request.newContext({ baseURL: API });
    const register = await api.post('/api/v1/auth/register', {
      data: { storeName: `Tiệm Nhập ${RUN}`, email, password },
    });
    expect(register.status(), await register.text()).toBe(201);
    const verify = await api.post('/api/v1/auth/email/verify', { data: { email, otp: await otpSentTo(email) } });
    expect(verify.ok(), await verify.text()).toBe(true);
    const logIn = await api.post('/api/v1/auth/log-in', {
      headers: { 'X-Client-Platform': 'mobile' },
      data: { email, password },
    });
    const token = ((await logIn.json()) as { data: { accessToken: string } }).data.accessToken;
    const store = await api.put('/api/v1/store', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: `Tiệm Nhập ${RUN}`,
        depot: { address: 'Kho 45 Lê Lợi, Hải Châu, Đà Nẵng', lat: 16.0544, lon: 108.2022 },
        timezone: 'Asia/Ho_Chi_Minh',
      },
    });
    expect(store.ok(), await store.text()).toBe(true);
    await api.dispose();
  });

  await test.step('đăng nhập web', async () => {
    await page.goto('login');
    await page.getByLabel(/^Email/).fill(email);
    await page.getByLabel(/^Mật khẩu/).fill(password);
    await page.getByRole('button', { name: /Đăng nhập/ }).click();
    await expect(page.getByRole('heading', { name: 'Kế hoạch giao hàng' })).toBeVisible();
  });

  await test.step('nhập file trong form tạo kế hoạch', async () => {
    await page.goto('plans/new');
    await page.getByRole('button', { name: 'Nhập từ file' }).click();
    const dialog = page.getByRole('dialog', { name: 'Nhập điểm giao từ file' });
    await dialog.getByLabel('File điểm giao (.xlsx hoặc .csv)').setInputFiles(FIXTURE);
    await expect(dialog.getByText('diem-giao.csv')).toBeVisible();
    await expect(dialog.getByText('2 thêm được')).toBeVisible();
    await expect(dialog.getByText('1 có lỗi')).toBeVisible();
    await dialog.getByRole('button', { name: 'Thêm 2 điểm giao' }).click();
    await expect(page.getByLabel('Mã đơn dòng 1')).toHaveValue('ĐH-E2E-1');
    await expect(page.getByLabel('Mã đơn dòng 2')).toHaveValue('ĐH-E2E-2');
  });

  await test.step('lưu kế hoạch', async () => {
    await page.getByLabel(/^Tên kế hoạch/).fill(`Từ file ${RUN}`);
    await page.getByRole('button', { name: 'Lưu và tìm toạ độ' }).first().click();
    await expect(page.getByRole('heading', { name: `Từ file ${RUN}` })).toBeVisible();
    await expect(page.getByRole('button', { name: 'ĐH-E2E-1 · Chị Lan' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'ĐH-E2E-2 · Anh Minh' })).toBeVisible();
  });
});
