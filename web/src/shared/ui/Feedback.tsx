import { Children, type ReactNode } from 'react';

/** Error box at the top of a form or section (role="alert"). Field errors go under the field instead. */
export function ErrorBanner({
  title,
  children,
  tone = 'danger',
  repeat = false,
}: {
  title: ReactNode;
  children?: ReactNode;
  tone?: 'danger' | 'info';
  /** The same place said again after another try: it does not move in again (that read as a flash). */
  repeat?: boolean;
}) {
  return (
    <div
      className={['pw-banner', tone === 'info' && 'pw-banner-info', repeat && 'is-repeat'].filter(Boolean).join(' ')}
      role={tone === 'danger' ? 'alert' : 'status'}
    >
      <span className="pw-banner-title">{title}</span>
      {/* Children.toArray drops null and false: a page passing only empty parts gets no empty second row. */}
      {Children.toArray(children).length > 0 ? (
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
