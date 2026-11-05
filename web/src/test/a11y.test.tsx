import { render, screen } from '@testing-library/react';
import axe from 'axe-core';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { http } from 'msw';
import { anonymousHandlers, API, ok, server, signedInHandlers } from '@/test/server';
import {
  Button,
  DataTable,
  Dialog,
  ErrorBanner,
  MapMarker,
  OtpInput,
  ProgressBar,
  StatusPill,
  StopCard,
  TextField,
  Toast,
} from '@/shared/ui';

/** axe without color-contrast: jsdom cannot paint (see shared/theme/contrast.test.ts for colours). */
async function violations(container: Element) {
  const result = await axe.run(container, { rules: { 'color-contrast': { enabled: false } } });
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`);
}

function ComponentGallery() {
  return (
    <main>
      <h1>Component</h1>
      <Button variant="primary">Giao việc</Button>
      <Button busy>Đang lưu…</Button>
      <TextField label="Tên xe" required error="Vui lòng nhập tên xe." />
      <TextField label="Biển số" hint="Tuỳ chọn." />
      <StatusPill kind="plan" status="PUBLISHED" />
      <ProgressBar label="Đang tìm toạ độ" value={7} max={12} />
      <ProgressBar label="Đang tối ưu" />
      <DataTable
        caption="Xe"
        columns={[
          { key: 'name', header: 'Tên' },
          { key: 'capacity', header: 'Sức chứa', numeric: true },
        ]}
        rows={[{ id: 1, name: 'Xe 1', capacity: 30 }]}
        onRowClick={() => {}}
      />
      {/* StopCard titles are h3: it always sits in a section with an h2, like a stop list. */}
      <h2>Điểm tiếp theo</h2>
      <StopCard
        sequence={1}
        title="ĐH-1009 · Chị Lan"
        address="887 Nguyễn Tri Phương"
        phone="+84905123008"
        status="PENDING"
        current
      />
      <MapMarker sequence={3} routeIndex={1} />
      <ErrorBanner title="Email hoặc mật khẩu không đúng." />
      <OtpInput label="Mã xác thực" value="48" onChange={() => {}} />
      <Toast tone="success" onClose={() => {}}>
        Đã giao việc cho 2 tài xế.
      </Toast>
      <Dialog open title="Huỷ kế hoạch?" confirmLabel="Huỷ kế hoạch" tone="danger" onClose={() => {}}>
        <p>Không hoàn tác được.</p>
      </Dialog>
    </main>
  );
}

describe('accessibility', () => {
  it('trang component ở theme tối không có lỗi axe', async () => {
    document.documentElement.dataset.theme = 'dark';
    const { container } = render(<ComponentGallery />);
    expect(await violations(container)).toEqual([]);
  });

  it('màn đăng nhập không có lỗi axe', async () => {
    server.use(...anonymousHandlers());
    const { container } = renderApp('/login');
    await screen.findByRole('heading', { name: 'Đăng nhập' });
    expect(await violations(container)).toEqual([]);
  });

  it.each([
    ['/register', 'Đăng ký cửa hàng'],
    ['/verify-email?email=lan%40example.com', 'Xác thực email'],
    ['/forgot-password', 'Quên mật khẩu'],
    ['/invite', 'Nhận lời mời'],
  ])('màn %s không có lỗi axe', async (path, heading) => {
    server.use(...anonymousHandlers());
    const { container } = renderApp(path);
    await screen.findByRole('heading', { name: heading });
    expect(await violations(container)).toEqual([]);
  });

  it('màn hồ sơ không có lỗi axe', async () => {
    server.use(
      http.get(`${API}/auth/sessions`, () => ok([])),
      ...signedInHandlers(),
    );
    const { container } = renderApp('/profile');
    await screen.findByLabelText('Họ tên');
    expect(await violations(container)).toEqual([]);
  });

  it.each([
    ['/vehicles', 'Xe'],
    ['/members', 'Thành viên'],
    ['/members?tab=invitations', 'Thành viên'],
  ])('màn %s không có lỗi axe', async (path, heading) => {
    server.use(
      http.get(`${API}/vehicles`, () =>
        ok([{ id: 1, name: 'Xe 1', plateNumber: null, capacity: 30, capacityUnit: 'BOX', active: true }]),
      ),
      http.get(`${API}/store/members`, () =>
        ok([
          {
            id: 449,
            email: 'chu.cuahang@example.com',
            fullName: null,
            phoneNumber: null,
            role: 'OWNER',
            status: 'ACTIVE',
            canDrive: false,
          },
          {
            id: 451,
            email: 'taixe@example.com',
            fullName: 'Tài xế A',
            phoneNumber: null,
            role: 'DRIVER',
            status: 'ACTIVE',
            canDrive: true,
          },
        ]),
      ),
      http.get(`${API}/store/invitations`, () =>
        ok([{ id: 1, email: 'moi@example.com', role: 'DRIVER', status: 'PENDING', expiresAt: '2026-11-10T03:00:00Z' }]),
      ),
      ...signedInHandlers(),
    );
    const { container } = renderApp(path);
    await screen.findByRole('heading', { name: heading, level: 1 });
    await screen.findAllByRole('row');
    expect(await violations(container)).toEqual([]);
  });

  it('màn cửa hàng (bản đồ, ô tìm, ghim) không có lỗi axe', async () => {
    server.use(...signedInHandlers());
    const { container } = renderApp('/store');
    await screen.findByLabelText(/^Tên cửa hàng/);
    expect(await violations(container)).toEqual([]);
  });
});
