import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ErrorBanner } from './Feedback';

describe('ErrorBanner', () => {
  it('has no second row when every detail part is empty', () => {
    // The login page passes its optional parts even when both are empty: [null, null] must not add an empty row,
    // which pushed the title above the banner's centre.
    render(
      <ErrorBanner title="Email hoặc mật khẩu không đúng.">
        {null}
        {false}
      </ErrorBanner>,
    );

    const banner = screen.getByRole('alert');
    expect(banner.children).toHaveLength(1);
    expect(banner).toHaveTextContent(/^Email hoặc mật khẩu không đúng\.$/);
  });

  it('shows the detail after the title when there is one', () => {
    render(<ErrorBanner title="Tài khoản đã bị vô hiệu hoá.">Liên hệ chủ cửa hàng.</ErrorBanner>);

    const banner = screen.getByRole('alert');
    expect(banner.children).toHaveLength(2);
    expect(banner).toHaveTextContent('Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng.');
  });
});
