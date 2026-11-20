import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MoreMenu } from './MoreMenu';

const nextFrame = () => act(() => new Promise<void>((resolve) => requestAnimationFrame(() => resolve())));

describe('MoreMenu', () => {
  it('a scroll made before the menu opened, reported at the next frame, does not close it; a later scroll does', async () => {
    const onSelect = vi.fn();
    render(<MoreMenu label="Thao tác với cửa hàng" items={[{ label: 'Mở khoá', onSelect }]} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Thao tác với cửa hàng' }));
    // The browser reports the scroll that brought the button into view only now, with the menu already open.
    fireEvent.scroll(window);
    expect(screen.getByRole('menu', { name: 'Thao tác với cửa hàng' })).toBeInTheDocument();

    await nextFrame();
    fireEvent.scroll(window);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });
});
