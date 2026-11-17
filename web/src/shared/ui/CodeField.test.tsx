import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { CodeField } from './CodeField';

function Field() {
  const [value, setValue] = useState('');
  return <CodeField label="Mã xác thực" value={value} onChange={setValue} />;
}

describe('CodeField (one input drawn as boxes)', () => {
  it('is one labelled one-time-code field with a numeric keyboard; the boxes are drawing only', () => {
    const { container } = render(<Field />);
    const input = screen.getByLabelText('Mã xác thực');

    expect(screen.getAllByRole('textbox')).toHaveLength(1);
    expect(input).toHaveAttribute('autocomplete', 'one-time-code');
    expect(input).toHaveAttribute('inputmode', 'numeric');
    expect(container.querySelector('.pw-code-cells')).toHaveAttribute('aria-hidden', 'true');
    expect(container.querySelectorAll('.pw-code-cell')).toHaveLength(6);
  });

  it('shows one digit per box, keeps digits only and at most six, also when pasted', async () => {
    const user = userEvent.setup();
    const { container } = render(<Field />);
    const input = screen.getByLabelText('Mã xác thực');

    await user.click(input);
    await user.paste('12 34-5678');

    expect(input).toHaveValue('123456');
    expect([...container.querySelectorAll('.pw-code-cell')].map((cell) => cell.textContent)).toEqual([
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
    ]);

    await user.keyboard('{Backspace}');

    expect(input).toHaveValue('12345');
  });

  it('lights the next box to fill while focused', async () => {
    const user = userEvent.setup();
    const { container } = render(<Field />);

    await user.type(screen.getByLabelText('Mã xác thực'), '12');

    const cells = container.querySelectorAll('.pw-code-cell');
    expect(cells[2]).toHaveClass('is-active');
    expect(container.querySelectorAll('.pw-code-cell.is-active')).toHaveLength(1);
  });
});
