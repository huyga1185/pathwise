import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SelectField } from './SelectField';

const UNITS = [
  { value: 'BOX', label: 'Thùng' },
  { value: 'KG', label: 'Kg' },
];

describe('SelectField', () => {
  it('mở menu của app (không phải danh sách của trình duyệt), lựa chọn hiện tại có dấu tích, chọn thì đổi giá trị', async () => {
    const onChange = vi.fn();
    render(
      <SelectField label="Đơn vị" options={UNITS} placeholder="Chọn đơn vị" defaultValue="BOX" onChange={onChange} />,
    );
    const user = userEvent.setup();
    const select = screen.getByLabelText('Đơn vị');

    await user.click(select);
    const menu = screen.getByRole('menu', { name: 'Đơn vị' });
    expect(select).toHaveAttribute('aria-expanded', 'true');
    // The placeholder is not a choice.
    expect(screen.getAllByRole('menuitemradio').map((item) => item.textContent)).toEqual(['Thùng', 'Kg']);
    expect(screen.getByRole('menuitemradio', { name: 'Thùng' })).toHaveAttribute('aria-checked', 'true');
    expect(menu.contains(document.activeElement)).toBe(true);

    await user.click(screen.getByRole('menuitemradio', { name: 'Kg' }));
    expect(select).toHaveValue('KG');
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(select).toHaveFocus();
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('bàn phím: Space mở, mũi tên đi, Escape đóng và trả focus về ô', async () => {
    render(<SelectField label="Đơn vị" options={UNITS} defaultValue="BOX" />);
    const user = userEvent.setup();
    const select = screen.getByLabelText('Đơn vị');
    select.focus();

    await user.keyboard(' ');
    expect(screen.getByRole('menuitemradio', { name: 'Thùng' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitemradio', { name: 'Kg' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(select).toHaveFocus();
    expect(select).toHaveValue('BOX');
  });
});
