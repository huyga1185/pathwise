import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { Dialog } from './Dialog';

function Page() {
  const [open, setOpen] = useState(false);
  return (
    <main>
      <button type="button" onClick={() => setOpen(true)}>
        Mở hộp
      </button>
      <button type="button">Nút phía sau</button>
      <Dialog open={open} title="Huỷ kế hoạch?" confirmLabel="Huỷ kế hoạch" onClose={() => setOpen(false)}>
        <p>Không hoàn tác được.</p>
      </Dialog>
    </main>
  );
}

describe('Dialog', () => {
  it('bàn phím: mở thì focus nút đầu, Tab và Shift+Tab không ra khỏi hộp, Esc đóng và trả focus', async () => {
    render(<Page />);
    const user = userEvent.setup();
    const opener = screen.getByRole('button', { name: 'Mở hộp' });

    await user.click(opener);
    const back = screen.getByRole('button', { name: 'Quay lại' });
    const confirm = screen.getByRole('button', { name: 'Huỷ kế hoạch' });
    expect(back).toHaveFocus();
    await user.tab();
    expect(confirm).toHaveFocus();
    await user.tab();
    expect(back).toHaveFocus();
    await user.tab({ shift: true });
    expect(confirm).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });
});
