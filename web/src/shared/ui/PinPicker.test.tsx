import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { blankBasemap, BasemapProvider } from '@/platform/map';
import { PinPickerDialog } from './PinPicker';

describe('PinPickerDialog', () => {
  it('chỉ xác nhận được khi đã có ghim; xác nhận trả về vị trí đã chọn', async () => {
    const onConfirm = vi.fn();
    render(
      <BasemapProvider basemap={blankBasemap()}>
        <PinPickerDialog open initial={null} onConfirm={onConfirm} onClose={() => {}} pinLabel="Điểm giao" />
      </BasemapProvider>,
    );
    const user = userEvent.setup();

    expect(screen.getByRole('dialog', { name: 'Chọn vị trí' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dùng vị trí này' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Đặt ghim ở giữa bản đồ' }));
    await user.click(screen.getByRole('button', { name: 'Dùng vị trí này' }));

    expect(onConfirm).toHaveBeenCalledWith({ lat: expect.closeTo(16.0544, 4), lon: expect.closeTo(108.2022, 4) }, null);
  });
});
