import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Alert, Sheet } from './Sheet';

describe('Sheet', () => {
  it('Huỷ ở đầu thanh trên, nút xong ở cuối gửi form; Escape đóng; focus vào ô đầu', async () => {
    const onSubmit = vi.fn((event: { preventDefault: () => void }) => event.preventDefault());
    const onClose = vi.fn();
    render(
      <Sheet open title="Thêm xe" doneLabel="Thêm" onSubmit={onSubmit} onClose={onClose}>
        <label>
          Tên xe
          <input name="name" />
        </label>
      </Sheet>,
    );
    const user = userEvent.setup();
    const sheet = screen.getByRole('dialog', { name: 'Thêm xe' });
    expect(sheet).toHaveAttribute('aria-modal', 'true');
    const buttons = sheet.querySelectorAll('.pw-sheet-bar button');
    expect(buttons[0]).toHaveTextContent('Huỷ');
    expect(buttons[1]).toHaveTextContent('Thêm');
    expect(screen.getByLabelText('Tên xe')).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'Thêm' }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Huỷ' }));
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});

describe('Alert', () => {
  it('Huỷ bên trái là nút được focus; hành động bên phải; Escape huỷ; chỉ tô đỏ khi destructive', async () => {
    const onAction = vi.fn();
    const onCancel = vi.fn();
    const { rerender } = render(
      <Alert
        open
        title="Vô hiệu hoá Anh Hải?"
        message="Người này bị đăng xuất ngay."
        actionLabel="Vô hiệu hoá"
        onAction={onAction}
        onCancel={onCancel}
      />,
    );
    const user = userEvent.setup();
    const alert = screen.getByRole('alertdialog', { name: 'Vô hiệu hoá Anh Hải?' });
    expect(alert).toHaveAccessibleDescription('Người này bị đăng xuất ngay.');
    expect(screen.getByRole('button', { name: 'Huỷ' })).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Vô hiệu hoá' })).not.toHaveClass('is-destructive');
    await user.click(screen.getByRole('button', { name: 'Vô hiệu hoá' }));
    expect(onAction).toHaveBeenCalledTimes(1);
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(1);

    rerender(<Alert open title="Xoá?" actionLabel="Xoá" destructive onAction={onAction} onCancel={onCancel} />);
    expect(screen.getByRole('button', { name: 'Xoá' })).toHaveClass('is-destructive');
  });
});
