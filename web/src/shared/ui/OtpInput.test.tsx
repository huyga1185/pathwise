import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { OtpInput } from './OtpInput';

function Harness() {
  const [value, setValue] = useState('');
  return <OtpInput label="Mã xác thực" value={value} onChange={setValue} />;
}

const box = (n: number) => screen.getByLabelText(`Chữ số ${n} trên 6`);

describe('OtpInput', () => {
  it('gõ một số thì nhảy sang ô sau, Backspace ở ô trống thì lùi và xoá ô trước', async () => {
    render(<Harness />);
    const user = userEvent.setup();

    await user.click(box(1));
    await user.keyboard('48');
    expect(box(1)).toHaveValue('4');
    expect(box(2)).toHaveValue('8');
    expect(box(3)).toHaveFocus();

    await user.keyboard('{Backspace}');
    expect(box(2)).toHaveFocus();
    expect(box(2)).toHaveValue('');
  });

  it('dán mã vào ô bất kỳ thì điền đủ 6 ô, bỏ ký tự không phải số', async () => {
    render(<Harness />);
    const user = userEvent.setup();

    await user.click(box(4));
    await user.paste('482 913');

    expect([1, 2, 3, 4, 5, 6].map((n) => (box(n) as HTMLInputElement).value).join('')).toBe('482913');
    expect(box(6)).toHaveFocus();
  });

  it('chỉ ô đầu có autocomplete one-time-code để trình duyệt tự điền', () => {
    render(<Harness />);
    expect(box(1)).toHaveAttribute('autocomplete', 'one-time-code');
    expect(box(2)).toHaveAttribute('autocomplete', 'off');
  });
});
