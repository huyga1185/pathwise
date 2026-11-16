import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { MapArt } from './AuthLayout';

describe('MapArt', () => {
  it('pauses the animations with the still picture and plays them again (WCAG 2.2.2)', () => {
    const { container } = render(<MapArt />);
    const picture = () => container.querySelector('img.pw-auth-map')?.getAttribute('src') ?? '';
    const animated = picture();

    fireEvent.click(screen.getByRole('button', { name: 'Tạm dừng hoạt ảnh' }));

    expect(picture()).toMatch(/map-route-static/);
    expect(window.localStorage.getItem('pw.auth.mapPaused')).toBe('1');

    fireEvent.click(screen.getByRole('button', { name: 'Chạy hoạt ảnh' }));

    expect(picture()).toBe(animated);
    expect(window.localStorage.getItem('pw.auth.mapPaused')).toBe('0');
  });
});
