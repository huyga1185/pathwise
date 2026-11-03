import type { ReactNode } from 'react';

/** Error box at the top of a form or section (role="alert"). Field errors go under the field instead. */
export function ErrorBanner({
  title,
  children,
  tone = 'danger',
}: {
  title: ReactNode;
  children?: ReactNode;
  tone?: 'danger' | 'info';
}) {
  return (
    <div
      className={tone === 'info' ? 'pw-banner pw-banner-info' : 'pw-banner'}
      role={tone === 'danger' ? 'alert' : 'status'}
    >
      <span className="pw-banner-title">{title}</span>
      {children ? (
        <>
          {' '}
          <span>{children}</span>
        </>
      ) : null}
    </div>
  );
}

/** A field error written by hand next to a custom control (TextField shows its own). */
export function FieldError({ id, children }: { id: string; children?: ReactNode }) {
  if (!children) return null;
  return (
    <p id={id} className="pw-field-msg pw-field-msg-error">
      {children}
    </p>
  );
}

/** Empty or placeholder state of a page or list, with the next step as the action. */
export function EmptyState({
  title,
  children,
  action,
  illustration,
}: {
  title: ReactNode;
  children?: ReactNode;
  action?: ReactNode;
  illustration?: ReactNode;
}) {
  return (
    <section className="pw-empty">
      {illustration}
      <h2>{title}</h2>
      {children ? <p>{children}</p> : null}
      {action}
    </section>
  );
}
