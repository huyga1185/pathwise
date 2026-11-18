import { screen } from '@testing-library/react';
import type userEvent from '@testing-library/user-event';

/** A command in a plan's More menu (OWNER_UI_REDESIGN G5-Q5): the ellipsis button, then the item. */
export async function fromMore(user: ReturnType<typeof userEvent.setup>, item: string) {
  await user.click(await screen.findByRole('button', { name: 'Thao tác khác' }));
  await user.click(screen.getByRole('menuitem', { name: item }));
}
