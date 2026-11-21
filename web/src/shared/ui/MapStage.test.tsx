import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { wideWindow } from '@/test/media';
import { MapStage, type MapInset } from './MapStage';

afterEach(() => vi.restoreAllMocks());

function stage(onInset?: (inset: MapInset) => void) {
  return render(
    <MapStage
      label="Tuyến của kế hoạch"
      map={(inset) => {
        onInset?.(inset);
        return <div>bản đồ</div>;
      }}
    >
      <h1 data-peek>Giao sáng 01/12</h1>
      <p>Danh sách tuyến</p>
    </MapStage>,
  );
}

describe('MapStage (G5.1–G5.3)', () => {
  it('màn rộng: nội dung trong panel bên trái; bản đồ chừa phần panel che', () => {
    wideWindow();
    const insets: MapInset[] = [];
    stage((inset) => insets.push(inset));
    const panel = screen.getByRole('region', { name: 'Tuyến của kế hoạch' });
    expect(panel).toHaveClass('pw-map-panel');
    expect(panel).toHaveTextContent('Danh sách tuyến');
    expect(screen.queryByRole('button', { name: /Đổi chiều cao/ })).not.toBeInTheDocument();
    expect(insets.at(-1)?.left).toBe(484);
  });

  it('điện thoại: sheet có grabber; bấm grabber chuyển nấc thấp → giữa → cao → thấp, mũi tên lên/xuống đổi nấc', async () => {
    stage();
    const user = userEvent.setup();
    const sheet = screen.getByRole('region', { name: 'Tuyến của kế hoạch' });
    expect(sheet).toHaveAttribute('data-detent', 'mid');
    const grabber = screen.getByRole('button', { name: 'Đổi chiều cao danh sách (đang nửa màn hình)' });

    await user.click(grabber);
    expect(sheet).toHaveAttribute('data-detent', 'high');
    await user.click(screen.getByRole('button', { name: /Đổi chiều cao/ }));
    expect(sheet).toHaveAttribute('data-detent', 'low');

    screen.getByRole('button', { name: /Đổi chiều cao/ }).focus();
    await user.keyboard('{ArrowUp}');
    expect(sheet).toHaveAttribute('data-detent', 'mid');
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(sheet).toHaveAttribute('data-detent', 'low');
    // Folded: only the handle (fb_14, §10.11) — the content is inert.
    expect(screen.getByText('Danh sách tuyến').closest('.pw-map-sheet-body')).toHaveAttribute('inert');
    expect(screen.getByRole('button', { name: 'Đổi chiều cao danh sách (đang thu nhỏ)' })).toHaveFocus();
  });
});
