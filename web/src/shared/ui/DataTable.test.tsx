import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { DataTable } from './DataTable';

describe('DataTable', () => {
  it('kéo đổi độ rộng cột: tay nắm ở mọi cột trừ cột cuối, phím mũi tên đổi 16px, bấm đúp trả về tự động', async () => {
    render(
      <DataTable
        resizable
        caption="Xe"
        columns={[
          { key: 'name', header: 'Tên xe' },
          { key: 'plate', header: 'Biển số' },
          { key: 'actions', header: '' },
        ]}
        rows={[{ id: 1, name: 'Xe 1', plate: '43A' }]}
      />,
    );
    const user = userEvent.setup();
    const handles = screen.getAllByRole('separator');
    expect(handles).toHaveLength(2);
    expect(handles[0]).toHaveAccessibleName('Độ rộng cột Tên xe');

    handles[0]!.focus();
    await user.keyboard('{ArrowRight}');
    const table = screen.getByRole('table');
    expect(table).toHaveClass('is-resized');
    // jsdom measures every column as 0: 0 + 16 is held at the 64px minimum (a browser: the measured width + 16).
    expect(screen.getByRole('columnheader', { name: /Tên xe/ })).toHaveStyle({ width: '64px' });

    await user.dblClick(handles[0]!);
    expect(table).not.toHaveClass('is-resized');
  });
});
