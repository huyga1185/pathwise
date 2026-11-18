import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AppShell, initials, pageMotion } from './AppShell';
import { ChartIcon, RouteIcon } from './icons';

const ITEMS = [
  { to: '/plans', label: 'Kế hoạch', icon: <RouteIcon /> },
  { to: '/reports', label: 'Báo cáo', icon: <ChartIcon /> },
];

function renderShell(wide: boolean, onLogOut = () => {}) {
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query: string) =>
      ({
        matches: query.includes('min-width: 960px') ? wide : false,
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
      }) as unknown as MediaQueryList,
  );
  return render(
    <MemoryRouter initialEntries={['/plans']}>
      <AppShell
        items={ITEMS}
        navLabel="Điều hướng chính"
        account={{
          name: 'Chị Ngọc',
          email: 'owner@example.com',
          context: 'Tiệm Hoa Hồng',
          profileTo: '/profile',
          onLogOut,
        }}
      >
        <h1>Kế hoạch</h1>
      </AppShell>
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  window.localStorage.clear();
});

describe('AppShell (P-74)', () => {
  it('wide: one sidebar navigation with the current area marked, which can be hidden and stays hidden', async () => {
    const user = userEvent.setup();
    const { unmount } = renderShell(true);

    expect(screen.getAllByRole('navigation', { name: 'Điều hướng chính' })).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Kế hoạch' })).toHaveAttribute('aria-current', 'page');

    await user.click(screen.getByRole('button', { name: 'Ẩn thanh bên' }));

    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    unmount();
    renderShell(true);
    expect(screen.getByRole('button', { name: 'Hiện thanh bên' })).toHaveAttribute('aria-expanded', 'false');
  });

  it('phone: one tab bar navigation, no sidebar button', () => {
    renderShell(false);

    expect(screen.getAllByRole('navigation', { name: 'Điều hướng chính' })).toHaveLength(1);
    expect(screen.queryByRole('button', { name: /thanh bên/ })).not.toBeInTheDocument();
  });

  it('account menu: who and which store, keyboard to the items, Escape back to the button, log out', async () => {
    const user = userEvent.setup();
    const onLogOut = vi.fn();
    renderShell(true, onLogOut);
    const button = screen.getByRole('button', { name: 'Tài khoản: Chị Ngọc' });

    expect(button).toHaveTextContent('CN');
    await user.click(button);

    const menu = screen.getByRole('menu');
    expect(menu).toHaveTextContent('Tiệm Hoa Hồng');
    expect(screen.getByRole('menuitem', { name: 'Tài khoản' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Đăng xuất' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(button).toHaveFocus();

    await user.click(button);
    await user.click(screen.getByRole('menuitem', { name: 'Đăng xuất' }));
    expect(onLogOut).toHaveBeenCalledOnce();
  });

  it('initials take the first and last words', () => {
    expect(initials('Chị Ngọc')).toBe('CN');
    expect(initials('Nguyễn Thị Lan')).toBe('NL');
    expect(initials('lan@example.com')).toBe('L');
    expect(initials('')).toBe('');
  });

  it('page motion: a rise between areas, a slide in going deeper, a slide back coming up, nothing on the same page', () => {
    const roots = ['/plans', '/reports', '/store'];
    const kind = (from: string, to: string) => {
      const frames = pageMotion(from, to, roots);
      if (!frames) return 'none';
      return String(frames[0]?.transform ?? 'fade');
    };

    expect(kind('/plans', '/reports')).toBe('translateY(12px)');
    expect(kind('/plans', '/plans/new')).toBe('translateX(32px)');
    expect(kind('/plans/12', '/plans')).toBe('translateX(-32px)');
    expect(kind('/store', '/store/audit')).toBe('translateX(32px)');
    expect(kind('/plans/12', '/plans/13')).toBe('translateY(12px)');
    expect(kind('/plans', '/profile')).toBe('translateY(12px)');
    expect(kind('/plans', '/plans')).toBe('none');
  });
});
