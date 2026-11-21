import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { PasswordField } from './PasswordField';

describe('PasswordField', () => {
  it('ẩn chữ; nút con mắt cuối ô hiện/ẩn mật khẩu, có tên (cả khi rê chuột) và trạng thái bấm', async () => {
    render(<PasswordField label="Mật khẩu mới" defaultValue="matkhau123" />);
    const user = userEvent.setup();
    const input = screen.getByLabelText('Mật khẩu mới');
    expect(input).toHaveAttribute('type', 'password');

    const show = screen.getByRole('button', { name: 'Hiện mật khẩu' });
    expect(show).toHaveAttribute('title', 'Hiện mật khẩu');
    expect(show).toHaveAttribute('aria-pressed', 'false');
    await user.click(show);
    expect(input).toHaveAttribute('type', 'text');
    const hide = screen.getByRole('button', { name: 'Ẩn mật khẩu' });
    expect(hide).toHaveAttribute('aria-pressed', 'true');
    await user.click(hide);
    expect(input).toHaveAttribute('type', 'password');
  });
});
