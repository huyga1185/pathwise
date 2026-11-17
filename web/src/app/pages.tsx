import { lazy, Suspense, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router';
import { AuthLayout, AuthStage, canUseWeb, homeOf, MapArt, useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, EmptyState, LogoMark, useInAppShell, Wordmark, ActivityIndicator } from '@/shared/ui';
import { DispatcherShell } from './AppLayout';

function StandalonePage({ children }: { children: ReactNode }) {
  return (
    <main className="pw-standalone">
      <div className="pw-standalone-logo">
        <LogoMark size={40} />
        <Wordmark />
      </div>
      {children}
    </main>
  );
}

/**
 * The symbol of a content-unavailable view (Apple: `ContentUnavailableView` = a symbol, a title, a description and
 * actions): one large monochrome symbol in the secondary colour, like an SF Symbol — a pin with a slash for a page
 * that is not there, a lock for one this user may not open.
 */
function MissingSymbol({ kind }: { kind: Missing }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="56"
      height="56"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="pw-missing-symbol"
    >
      {kind === 'notFound' ? (
        <>
          <path d="M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 11.4-4.3M18.4 8.4c.07.5.1 1 .1 1.6 0 5.4-6.5 11-6.5 11" />
          <circle cx="12" cy="10" r="2.2" />
          <path d="M3.5 3.5l17 17" />
        </>
      ) : (
        <>
          <rect x="5" y="10.5" width="14" height="10" rx="2.5" />
          <path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" />
          <path d="M12 14.5v2.5" />
        </>
      )}
    </svg>
  );
}

type Missing = 'notFound' | 'forbidden';

type Viewer = 'signedOut' | 'signedIn';

const TITLE: Record<Missing, string> = { notFound: vi.pages.notFoundTitle, forbidden: vi.pages.forbiddenTitle };

/** What to tell this viewer: signed out, the way in is to sign in; signed in, a general next step that does not say
 * who the page is for (owner's choice, P-75: the page should not reveal which roles may open it). */
function leadOf(kind: Missing, viewer: Viewer): string {
  if (kind === 'notFound') return vi.pages.notFoundLead;
  return viewer === 'signedOut' ? vi.pages.forbiddenLeadSignedOut : vi.pages.forbiddenLead;
}

/** "Quay lại" only when there is a page of this app to go back to (React Router numbers its history entries). */
function canGoBack(): boolean {
  const state = window.history.state as { idx?: number } | null;
  return typeof state?.idx === 'number' && state.idx > 0;
}

/** Symbol, title, explanation and the ways out (HIG Writing: "Provide clear next steps on any blank screens"): home of
 * this user, and back when possible. */
function MissingContent({
  kind,
  viewer,
  home,
  homeLabel,
}: {
  kind: Missing;
  viewer: Viewer;
  home: string;
  homeLabel: string;
}) {
  const navigate = useNavigate();
  return (
    <>
      <MissingSymbol kind={kind} />
      <h1 id="missing-title">{TITLE[kind]}</h1>
      <p>{leadOf(kind, viewer)}</p>
      <div className="pw-missing-actions">
        <Link className={buttonClass('primary', 'lg')} to={home} viewTransition>
          {homeLabel}
        </Link>
        {canGoBack() ? (
          <Button variant="quiet" size="lg" onClick={() => void navigate(-1)}>
            {vi.pages.goBack}
          </Button>
        ) : null}
      </div>
    </>
  );
}

const AdminShell = lazy(() => import('@/features/admin').then((m) => ({ default: m.AdminShell })));

/**
 * A page that does not exist (or that this user may not open). Signed in, it stays inside the app's frame, so the
 * sidebar or tab bar and the account menu are still there and nobody is stranded (HIG Tab bars: "Make sure the tab bar
 * is visible"); signed out, or as a driver, it is a card on the account screens' picture. The way out goes to this
 * user's own home, not always /plans. HIG Feedback: "Show people when a command can't be carried out and help them
 * understand why"); laid out like Apple's ContentUnavailableView.
 */
function MissingPage({ kind }: { kind: Missing }) {
  const { session } = useSession();
  // Already inside the app's frame (a guard on one page of it): only the content, not a second frame.
  const inShell = useInAppShell();
  if (session.status === 'loading') {
    return <ActivityIndicator label={vi.pages.loading} className="is-page" />;
  }
  if (session.status === 'authenticated' && (canUseWeb(session.user) || session.user.role === 'ADMIN')) {
    const content = (
      <section className="pw-missing" aria-labelledby="missing-title">
        <MissingContent kind={kind} viewer="signedIn" home={homeOf(session.user)} homeLabel={vi.pages.backHome} />
      </section>
    );
    if (inShell) return content;
    return session.user.role === 'ADMIN' ? (
      <Suspense fallback={null}>
        <AdminShell>{content}</AdminShell>
      </Suspense>
    ) : (
      <DispatcherShell>{content}</DispatcherShell>
    );
  }
  const signedIn = session.status === 'authenticated';
  return (
    <AuthStage>
      <AuthLayout
        panel="wide"
        art={<MapArt />}
        pitch={
          <div className="pw-auth-pitch">
            <p className="pw-auth-headline">
              {vi.auth.brandHeadline[0]}
              <span>{vi.auth.brandHeadline[1]}</span>
              {vi.auth.brandHeadline[2]}
            </p>
            <p className="pw-auth-lead">{vi.auth.brandLead}</p>
          </div>
        }
      >
        <section className="pw-auth-card pw-missing-card" aria-labelledby="missing-title">
          <MissingContent
            kind={kind}
            viewer={signedIn ? 'signedIn' : 'signedOut'}
            home={signedIn ? homeOf(session.user) : '/login'}
            homeLabel={signedIn ? vi.pages.backHome : vi.form.toLogin}
          />
        </section>
      </AuthLayout>
    </AuthStage>
  );
}

export function NotFoundPage() {
  return <MissingPage kind="notFound" />;
}

/** 403: rendered at the address that was opened (a guard, P-75), or at /forbidden. */
export function ForbiddenPage() {
  return <MissingPage kind="forbidden" />;
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
