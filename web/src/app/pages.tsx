import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, EmptyState, LogoMark } from '@/shared/ui';

function StandalonePage({ children }: { children: ReactNode }) {
  return (
    <main className="pw-standalone">
      <div className="pw-standalone-logo">
        <LogoMark size={40} />
        <span>{vi.appName}</span>
      </div>
      {children}
    </main>
  );
}

export function NotFoundPage() {
  return (
    <StandalonePage>
      <EmptyState
        title={vi.pages.notFoundTitle}
        action={
          <Link className={buttonClass('primary')} to="/plans">
            {vi.pages.backHome}
          </Link>
        }
      >
        {vi.pages.notFoundLead}
      </EmptyState>
    </StandalonePage>
  );
}

export function ForbiddenPage() {
  return (
    <StandalonePage>
      <EmptyState
        title={vi.pages.forbiddenTitle}
        action={
          <Link className={buttonClass('primary')} to="/plans">
            {vi.pages.backHome}
          </Link>
        }
      >
        {vi.pages.forbiddenLead}
      </EmptyState>
    </StandalonePage>
  );
}

/** A DRIVER signed in on the web (P-46): the dispatcher pages are not for them. */
export function DriverUseAppPage() {
  const { logOut } = useSession();
  return (
    <StandalonePage>
      <EmptyState
        title={vi.pages.driverTitle}
        action={
          <Button variant="primary" onClick={() => void logOut()}>
            {vi.auth.logout}
          </Button>
        }
      >
        {vi.pages.driverLead}
      </EmptyState>
    </StandalonePage>
  );
}

/** Placeholder of a screen built by a later task (docs/client/PLAN.md), so the menu works from W01. */
export function ComingSoonPage({ title, task }: { title: string; task: string }) {
  return (
    <section className="pw-page-section">
      <h1 className="pw-page-title">{title}</h1>
      <EmptyState title={vi.pages.soonTitle}>{`${vi.pages.soonLead} (${task})`}</EmptyState>
    </section>
  );
}
