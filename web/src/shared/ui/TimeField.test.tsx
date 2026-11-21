import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { normalizeTime, TimeField } from './TimeField';

function Harness({ start = '' }: { start?: string }) {
  const [value, setValue] = useState(start);
  return (
    <>
      <TimeField aria-label="Giờ bắt đầu" value={value} onChange={setValue} />
      <output>{value}</output>
    </>
  );
}

/** A touch screen: matchMedia says the pointer is coarse. */
function touchScreen() {
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query: string) =>
      ({
        matches: query.includes('pointer: coarse'),
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
      }) as unknown as MediaQueryList,
  );
}

afterEach(() => vi.restoreAllMocks());

describe('TimeField', () => {
  it('chuẩn hoá giờ gõ tay về HH:mm 24 giờ; giá trị sai giữ nguyên để form báo', () => {
    expect(normalizeTime('830')).toBe('08:30');
    expect(normalizeTime('8:30')).toBe('08:30');
    expect(normalizeTime('8h30')).toBe('08:30');
    expect(normalizeTime('0830')).toBe('08:30');
    expect(normalizeTime('8')).toBe('08:00');
    expect(normalizeTime('17.05')).toBe('17:05');
    expect(normalizeTime('')).toBe('');
    expect(normalizeTime('25:00')).toBe('25:00');
    expect(normalizeTime('abc')).toBe('abc');
  });

  it('chuột: ô gõ; rời ô thì "930" thành "09:30"', async () => {
    render(<Harness />);
    const user = userEvent.setup();
    const field = screen.getByRole('textbox', { name: 'Giờ bắt đầu' });
    await user.type(field, '930');
    await user.tab();
    expect(field).toHaveValue('09:30');
  });

  it('cảm ứng: nút mở hai bánh xe giờ và phút (bước 5 phút); "Bỏ giờ" xoá giờ', async () => {
    touchScreen();
    render(<Harness start="08:30" />);
    const user = userEvent.setup();
    const button = screen.getByRole('button', { name: 'Giờ bắt đầu: 08:30' });
    await user.click(button);
    const hours = screen.getByRole('spinbutton', { name: 'Giờ' });
    const minutes = screen.getByRole('spinbutton', { name: 'Phút' });
    expect(hours).toHaveAttribute('aria-valuetext', '08');
    expect(minutes).toHaveAttribute('aria-valuemax', '11');
    minutes.focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByText('08:35', { selector: 'output' })).toBeInTheDocument();
    hours.focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByText('09:35', { selector: 'output' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Bỏ giờ' }));
    expect(screen.getByRole('button', { name: 'Giờ bắt đầu: chưa có giờ' })).toHaveTextContent('--:--');
  });
});
