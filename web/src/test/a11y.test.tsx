import { render, screen } from '@testing-library/react';
import axe from 'axe-core';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { anonymousHandlers, server } from '@/test/server';
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
});
