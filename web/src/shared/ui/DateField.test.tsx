import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { wideWindow } from '@/test/media';
import { DateField } from './DateField';

function Harness() {
  const [value, setValue] = useState('2026-11-18');
  return <DateField label="Ngày giao" required value={value} onChange={setValue} today="2026-11-18" />;
}

describe('DateField', () => {
  it('nút dạng ô hiện ngày "T4, 18/11/2026", có nhãn; chọn ngày trong lịch; ngày bắt buộc không có "Bỏ chọn ngày"', async () => {
    wideWindow();
    render(<Harness />);
    const user = userEvent.setup();
    const button = screen.getByLabelText(/^Ngày giao/);
    expect(button).toHaveTextContent('T4, 18/11/2026');

    await user.click(button);
    const calendar = screen.getByRole('dialog', { name: 'Ngày giao' });
    expect(within(calendar).queryByRole('button', { name: 'Bỏ chọn ngày' })).not.toBeInTheDocument();
    await user.click(within(calendar).getByRole('button', { name: 'Thứ Sáu, 20 tháng 11, 2026' }));
    expect(button).toHaveTextContent('T6, 20/11/2026');
  });
});
