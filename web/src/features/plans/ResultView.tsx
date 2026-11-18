import { useStoreReadOnly } from '@/data/meQuery';
import { useState } from 'react';
import { useBlocker, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { RouteMap } from '@/platform/map';
import { jobMessageFor, messageFor } from '@/shared/errors';
import { formatDateLong, formatDayTime, formatDistance, formatDuration, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { BackLink } from '@/shared/ui/BackLink';
import {
  Alert,
  Button,
  ErrorBanner,
  MapStage,
  PageActionSheet,
  ProgressBar,
  StatusPill,
  type RowMenuItem,
} from '@/shared/ui';
import { ManualBoard } from './ManualBoard';
import {
  draftChanged,
  draftFits,
  draftOf,
  draftPlan,
  lateMinutes,
  replaceRequest,
  violationStop,
  violationText,
  type Draft,
} from './manualEdit';
import { CopySheet } from './CopySheet';
import { MoreMenu, RouteGroup, StopCard } from './PlanStage';
import { CancelAlert, PublishSheet, routesWithStops } from './PlanDialogs';
import { allStops, useJob, usePlanApi, type Job, type PlanDetail, type PlanStop } from './planApi';
import { driverName, loadText, MAP_LABELS, mapData, spanText, stopPlace, stopTitle, windowText } from './planShared';

const r = vi.result;
const m = vi.manual;

/**
 * Prototype "Soạn plan — sau khi tối ưu" (SCREENS §1.1): overview, the stops the solver could not place (a warning,
 * publishing is still allowed), one collapsible block per route with its table, and the map; a row and its marker
 * select each other. Re-optimize follows `OPTIMIZING` by polling; a failed job keeps the previous result.
 * "Chỉnh tay" (W10, P-58) turns the routes into drag-and-drop columns; saving recomputes times and distances on the
 * server, which reports the constraints the edit breaks (`violations`).
 */
export function ResultView({
  plan,
  onPublished,
  onSaved,
}: {
  plan: PlanDetail;
  onPublished: (drivers: number) => void;
  onSaved: () => void;
}) {
  const navigate = useNavigate();
  const planApi = usePlanApi();
  const { readOnly } = useStoreReadOnly();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [job, setJob] = useState<Job | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [dialog, setDialog] = useState<'publish' | 'cancel' | 'discard' | 'reoptimize' | null>(null);
  const [savedDraft, setDraft] = useState<Draft | null>(null);
  const [copying, setCopying] = useState(false);
  // The plan changed under the draft (another dispatcher, a new optimization): a draft that no longer fits is dropped.
  const draft = savedDraft !== null && draftFits(plan, savedDraft) ? savedDraft : null;
  const [saving, setSaving] = useState(false);
  // A sentence of the manual edit (UX_BRIEF §6.3) rather than an error code.
  const [notice, setNotice] = useState<string | null>(null);
  const editing = draft !== null;
  const changed = draft !== null && draftChanged(plan, draft);
  // Leaving the page with an unsaved draft asks first.
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) => changed && currentLocation.pathname !== nextLocation.pathname,
  );
  const jobState = useJob(job);
  const busy = plan.processingStatus !== 'IDLE';
  const stops = allStops(plan);
  const routed = plan.routes.reduce((sum, route) => sum + route.stops.length, 0);
  const drivers = routesWithStops(plan).length;
  const infeasible = plan.unassignedStops.filter((stop) => stop.dropReason === 'INFEASIBLE');
  const takenOff = plan.unassignedStops.filter((stop) => stop.dropReason === 'MANUAL');
  const late = lateMinutes(plan);
  const shown = draft ? draftPlan(plan, draft) : plan;
  const failedJob = jobState.data?.status === 'FAILED' ? jobState.data : null;

  const save = async () => {
    if (!draft) return;
    setSaving(true);
    setFailure(null);
    setNotice(null);
    try {
      await planApi.replaceRoutes(plan.id, replaceRequest(plan, draft));
      setDraft(null);
      onSaved();
    } catch (error) {
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        setNotice(m.modified);
        await planApi.reload(plan.id);
      } else if (error instanceof ApiError && error.code === 'VALIDATION_ERROR') {
        // The draft no longer matches the plan: a bug or a change the app missed.
        setNotice(m.staleList);
        setDraft(null);
        await planApi.reload(plan.id);
      } else if (error instanceof ApiError && error.code === 'ROUTING_PROVIDER_ERROR') {
        setNotice(m.recomputeFailed);
      } else {
        setFailure(error);
      }
    } finally {
      setSaving(false);
    }
  };

  const cancelEdit = () => {
    if (changed) setDialog('discard');
    else setDraft(null);
  };

  // Optimizing again replaces manual edits: ask when there is something to lose.
  const askReoptimize = () => {
    if (changed || takenOff.length > 0 || plan.violations.length > 0) setDialog('reoptimize');
    else void reoptimize();
  };

  const reoptimize = async () => {
    setDraft(null);
    setNotice(null);
    setFailure(null);
    setJob(null);
    try {
      setJob(await planApi.optimize(plan.id, plan.version));
    } catch (error) {
      setFailure(error);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        await planApi.reload(plan.id);
      }
    }
  };

  const select = (id: number) => setSelectedId(id);
  const { routes, unassigned } = mapData(shown, false);
  const selected = allStops(shown).find((stop) => stop.id === selectedId) ?? null;
  const closeCard = () => {
    const id = selectedId;
    setSelectedId(null);
    // Back to the list: focus returns to the stop's row (WCAG 2.4.3).
    window.requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-stop="${id}"]`)?.focus());
  };
  const moreItems: RowMenuItem[] = readOnly
    ? []
    : [
        ...(busy ? [] : [{ label: r.editStops, onSelect: () => navigate(`/plans/${plan.id}/edit`) }]),
        ...(busy ? [] : [{ label: r.reoptimize, onSelect: askReoptimize }]),
        { label: vi.planCopy.action, onSelect: () => setCopying(true) },
        ...(busy ? [] : [{ label: r.cancel, destructive: true, onSelect: () => setDialog('cancel') }]),
      ];
  const eta = (stop: PlanStop) => (
    <>
      <span className="pw-tabular">{stop.plannedArrival ? formatTime(stop.plannedArrival, plan.timezone) : '—'}</span>
      {late.has(stop.id) ? <span className="pw-chip-signal">{m.late(late.get(stop.id) ?? 0)}</span> : null}
    </>
  );
  const unplaced = [...takenOff, ...infeasible];

  return (
    <MapStage
      label={r.panel}
      map={(inset) => (
        <RouteMap
          staged
          inset={inset}
          depot={{ lat: plan.depot.lat, lon: plan.depot.lon }}
          routes={routes}
          unassigned={unassigned}
          selectedId={selectedId}
          onSelect={(id) => select(Number(id))}
          labels={MAP_LABELS}
        />
      )}
    >
      {/* The sheet's low detent shows this header (G5.4): title, state, summary, actions. */}
      <div className="pw-stage-head" data-peek>
        <BackLink to="/plans" label={vi.plans.title} />
        <div className="pw-plan-title">
          <h1 id="plan-title" className="pw-page-title pw-stage-title">
            {plan.name}
          </h1>
          <StatusPill kind="plan" status={plan.status} />
        </div>
        <p className="pw-muted">
          {r.subtitle(
            formatDateLong(plan.planDate),
            plan.depot.address ?? vi.map.depot,
            plan.optimizedAt ? formatDayTime(plan.optimizedAt, plan.timezone) : '—',
          )}
        </p>
        <p className="pw-stage-summary pw-tabular">
          {r.summary(
            routed,
            stops.length,
            formatDistance(plan.totalDistanceMeters),
            formatDuration(plan.totalDurationSeconds),
            drivers,
          )}
        </p>
        {editing ? (
          <section className="pw-manual-banner" aria-label={m.regionLabel}>
            <div className="pw-manual-banner-text">
              <strong>{m.bannerTitle}</strong>
              <span className="pw-muted">{m.bannerLead}</span>
            </div>
            <Button variant="quiet" disabled={saving} onClick={cancelEdit}>
              {m.cancel}
            </Button>
            <Button variant="primary" busy={saving} disabled={!changed} onClick={() => void save()}>
              {saving ? m.saving : m.save}
            </Button>
          </section>
        ) : (
          // More, then the edit, then the primary action last (G5-Q5; HIG Toolbars).
          <div className="pw-stage-actions">
            <MoreMenu items={moreItems} />
            <Button variant="secondary" disabled={busy || readOnly} onClick={() => setDraft(draftOf(plan))}>
              {m.start}
            </Button>
            <Button variant="primary" disabled={busy || drivers === 0 || readOnly} onClick={() => setDialog('publish')}>
              {r.publish(drivers)}
            </Button>
          </div>
        )}
      </div>

      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      {notice ? <ErrorBanner title={notice} /> : null}
      {failedJob ? <ErrorBanner title={jobMessageFor(failedJob.errorCode)}>{r.jobFailed}</ErrorBanner> : null}

      {busy ? (
        <section className="pw-plans-progress" aria-label={r.progressLabel}>
          <ProgressBar label={r.progressLabel} />
          <p className="pw-muted">{r.progressNote}</p>
        </section>
      ) : null}

      {!editing && plan.violations.length > 0 ? (
        <section className="pw-violations" aria-labelledby="violations-title">
          <h2 id="violations-title">{m.violationsTitle(plan.violations.length)}</h2>
          <p className="pw-muted">{m.violationsLead}</p>
          <ul>
            {plan.violations.map((violation, index) => (
              <li key={`${violation.code}-${violation.routeId}-${violation.stopId ?? ''}-${index}`}>
                <button
                  type="button"
                  className="pw-link-button"
                  onClick={() => {
                    const stop = violationStop(plan, violation);
                    if (stop !== null) select(stop);
                  }}
                >
                  {violationText(plan, violation)}
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {draft ? (
        <ManualBoard plan={plan} draft={draft} onChange={setDraft} />
      ) : selected ? (
        <StopCard plan={shown} stop={selected} onBack={closeCard} />
      ) : (
        <div className="pw-stage-list">
          {plan.routes.map((route, index) => {
            const load = loadText(route);
            return (
              <RouteGroup
                key={route.id}
                route={route}
                index={index}
                summary={r.routeSummary(
                  route.stops.length,
                  formatDistance(route.plannedDistanceMeters),
                  formatDuration(route.plannedDurationSeconds),
                  spanText(route, plan.timezone),
                  load ? r.load(load) : '',
                )}
                extra={
                  plan.violations.some((violation) => violation.routeId === route.id) ? (
                    <span className="pw-chip-signal pw-stage-group-chip">{m.violationBadge}</span>
                  ) : null
                }
                stops={route.stops}
                trailing={eta}
                onOpen={(stop) => select(stop.id)}
              />
            );
          })}
          {unplaced.length > 0 ? (
            // Not placed in any route (G5.10): a group in the attention colour, publishing still allowed.
            <RouteGroup
              title={r.unassignedGroup}
              tone="signal"
              summary={infeasible.length > 0 ? r.unassignedLead : m.manualLead}
              stops={unplaced}
              trailing={(stop) => <StatusPill kind="dropReason" status={stop.dropReason ?? 'INFEASIBLE'} />}
              onOpen={(stop) => select(stop.id)}
            />
          ) : null}
        </div>
      )}

      <PublishSheet
        plan={plan}
        open={dialog === 'publish'}
        onClose={() => setDialog(null)}
        onPublished={(n) => {
          setDialog(null);
          onPublished(n);
        }}
      />
      <CancelAlert
        plan={plan}
        open={dialog === 'cancel'}
        onClose={() => setDialog(null)}
        onCancelled={() => setDialog(null)}
      />
      {/* Cancelling an edit with changes: an action sheet (HIG Alerts: "Use an action sheet — not an alert — to offer
          choices related to an intentional action"). */}
      <PageActionSheet
        open={dialog === 'discard'}
        title={m.discardTitle}
        message={m.discardLead}
        choices={[
          {
            label: m.discardConfirm,
            destructive: true,
            onSelect: () => {
              setDialog(null);
              setDraft(null);
            },
          },
          { label: m.keepEditing, cancel: true, onSelect: () => setDialog(null) },
        ]}
        onDismiss={() => setDialog(null)}
      />
      <Alert
        open={dialog === 'reoptimize'}
        title={m.reoptimizeTitle}
        message={m.reoptimizeLead}
        actionLabel={m.reoptimizeConfirm}
        onAction={() => {
          setDialog(null);
          void reoptimize();
        }}
        onCancel={() => setDialog(null)}
      />
      <Alert
        open={blocker.state === 'blocked'}
        title={m.leaveTitle}
        message={m.leaveLead}
        actionLabel={m.leaveConfirm}
        destructive
        onAction={() => blocker.proceed?.()}
        onCancel={() => blocker.reset?.()}
      />
      {copying ? <CopySheet planId={plan.id} planName={plan.name} open onClose={() => setCopying(false)} /> : null}
    </MapStage>
  );
}

/** The line under the map for the stop selected in a table or on the map. */
export function describeSelection(plan: PlanDetail, stop: PlanStop | null): { title: string; detail: string } | null {
  if (!stop) return null;
  const route = plan.routes.find((item) => item.stops.some((candidate) => candidate.id === stop.id));
  return {
    title: route ? r.selection(driverName(route), stop.sequence, stopTitle(stop)) : `${r.noRoute} · ${stopTitle(stop)}`,
    detail: r.selectionDetail(
      stopPlace(stop),
      stop.plannedArrival ? formatTime(stop.plannedArrival, plan.timezone) : '',
      windowText(stop),
    ),
  };
}
