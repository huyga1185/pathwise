import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { TextField } from './TextField';

function Controlled() {
  const [value, setValue] = useState('');
  return <TextField label="Email" clearable value={value} onChange={(event) => setValue(event.target.value)} />;
}

function WithForm({ onValue }: { onValue: (value: string) => void }) {
  const { register, watch } = useForm({ defaultValues: { name: '' } });
  onValue(watch('name'));
  return <TextField label="Họ tên" clearable {...register('name')} />;
}

describe('TextField clearable (HIG Text fields: Clear button)', () => {
  it('shows the clear button only while there is text, clears and keeps focus in the field', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const field = screen.getByLabelText('Email');

    expect(screen.queryByRole('button', { name: 'Xoá nội dung' })).not.toBeInTheDocument();

    await user.type(field, 'lan@example.com');
    // Pointer and touch only: keyboard users select and delete.
    expect(screen.getByRole('button', { name: 'Xoá nội dung' })).toHaveAttribute('tabindex', '-1');
    await user.click(screen.getByRole('button', { name: 'Xoá nội dung' }));

    expect(field).toHaveValue('');
    expect(field).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Xoá nội dung' })).not.toBeInTheDocument();
  });

  it('clears a react-hook-form field so that the form sees the empty value', async () => {
    const user = userEvent.setup();
    let seen = '';
    render(<WithForm onValue={(value) => (seen = value)} />);

    await user.type(screen.getByLabelText('Họ tên'), 'Tài xế B');
    expect(seen).toBe('Tài xế B');

    await user.click(screen.getByRole('button', { name: 'Xoá nội dung' }));

    expect(screen.getByLabelText('Họ tên')).toHaveValue('');
    expect(seen).toBe('');
  });

  it('has no clear button when the field has its own end action (password eye)', async () => {
    const user = userEvent.setup();
    render(<TextField label="Mật khẩu" type="password" clearable endAction={<button type="button">Hiện</button>} />);

    await user.type(screen.getByLabelText('Mật khẩu'), 'matkhau123');

    expect(screen.queryByRole('button', { name: 'Xoá nội dung' })).not.toBeInTheDocument();
  });
});
