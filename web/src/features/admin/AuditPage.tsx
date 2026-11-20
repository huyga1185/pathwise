import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime, formatTime, todayIn } from '@/shared/format';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { useFitRows } from '@/shared/hooks/useFitRows';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  ClearFilters,
  DataTable,
  DatePickerFilter,
  ErrorBanner,
  ListIcon,
  Pager,
  PlaceholderRows,
  PopUpFilter,
  PresetFilter,
  Sheet,
  type DataTableColumn,
} from '@/shared/ui';
import { ChevronRightIcon } from '@/shared/ui/icons';
import { cx } from '@/shared/ui/cx';
import { PAGE_SIZE, useAdminAudit, type AuditEvent, type AuditFilter } from './adminApi';
import { idOrUndefined } from './adminFormat';

const ZONE = 'Asia/Ho_Chi_Minh';
const DAY = /^\d{4}-\d{2}-\d{2}$/;

/**
 * "Quản trị — nhật ký" (W11, T58; redesigned in G7, OWNER_UI_REDESIGN §12) as the store's own log (§8.10): action and
 * the two days as the plan list's filters (the app's calendar, not the browser's date field), the store and the
 * person only as presets from their pages (G7-Q2); a table that fits the window, rows on a phone; each row opens a
 * sheet with all of it (HIG Typography: cut in a list only when a view shows the rest). Days in Vietnam.
 */
export function AuditPage() {
  const t = vi.audit;
  const a = vi.admin.audit;
  const wide = useMediaQuery(WIDE_QUERY);
  const [params, setParams] = useSearchParams();
  const storeId = idOrUndefined(params.get('storeId'));
  const actorId = idOrUndefined(params.get('actorId'));
  const rawAction = params.get('action') ?? '';
  const action = (AUDIT_ACTIONS as readonly string[]).includes(rawAction)
    ? (rawAction as AuditFilter['action'])
    : undefined;
  const from = DAY.test(params.get('from') ?? '') ? (params.get('from') as string) : undefined;
  const to = DAY.test(params.get('to') ?? '') ? (params.get('to') as string) : undefined;
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);

  const result = useRef<HTMLDivElement>(null);
  const probe = useRef<HTMLDivElement>(null);
  const filters = useRef<HTMLDivElement>(null);
  const fit = useFitRows(wide, result, probe, filters, PAGE_SIZE);
  const size = wide ? (fit?.rows ?? null) : PAGE_SIZE;
  const audit = useAdminAudit({ storeId, actorId, action, from, to, page }, size ?? PAGE_SIZE, size !== null);
  const [opened, setOpened] = useState<AuditEvent | null>(null);

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    if (!('page' in changes)) next.delete('page'); // another filter starts at page 1 (G2.9)
    setParams(next, { replace: true });
  };
  const goToPage = (target: number) => {
    update({ page: target > 0 ? String(target) : undefined });
    const top = result.current?.getBoundingClientRect().top ?? 0;
    const pinned = Number.parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
    if (!wide && top < pinned) {
      result.current?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' });
    }
  };
  const [lastSize, setLastSize] = useState(size);
  if (size !== lastSize) {
    setLastSize(size);
    if (size && lastSize && page > 0) {
      const next = new URLSearchParams(params);
      const moved = Math.floor((page * lastSize) / size);
      if (moved > 0) next.set('page', String(moved));
      else next.delete('page');
      setParams(next, { replace: true });
    }
  }

  const data = audit.data;
  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 0;
  const filtered = Boolean(storeId || actorId || action || from || to);
  const loading = data === undefined && !audit.isError;
  const empty = data !== undefined && data.totalElements === 0;
  const pastEnd = data !== undefined && !audit.isPlaceholderData && totalPages > 0 && page >= totalPages;
  useEffect(() => {
    if (!pastEnd) return;
    const next = new URLSearchParams(params);
    if (totalPages > 1) next.set('page', String(totalPages - 1));
    else next.delete('page');
    setParams(next, { replace: true });
  }, [pastEnd, params, setParams, totalPages]);
  const filterKey = [loading ? 'loading' : empty ? 'empty' : 'list', storeId, actorId, action, from, to].join('|');
  const resultKey = `${filterKey}|${page}`;
  const [shown, setShown] = useState({ key: resultKey, filterKey, page, motion: '' });
  if (!audit.isPlaceholderData && resultKey !== shown.key) {
    const motion = filterKey !== shown.filterKey ? 'is-changed' : page > shown.page ? 'is-next' : 'is-prev';
    setShown({ key: resultKey, filterKey, page, motion });
  }
  const clearPresence = useExit(filtered, 140);
  const foldLine = (row: AuditEvent) =>
    [row.storeId == null ? null : vi.admin.users.storeNumber(row.storeId), row.ip ? `${t.ip} ${row.ip}` : null]
      .filter(Boolean)
      .join(' · ');
  const role = (row: AuditEvent) => (row.actorRole ? (vi.roles[row.actorRole] ?? row.actorRole) : '');

  const columns: DataTableColumn<AuditEvent>[] = [
    {
      key: 'at',
      header: t.at,
      render: (row) => <span className="pw-nowrap pw-tabular">{formatDayTime(row.occurredAt, ZONE)}</span>,
    },
    {
      key: 'actor',
      header: t.actor,
      className: 'pw-admin-audit-col-actor',
      // One line each (email, role); all of it in the row's sheet.
      render: (row) => (
        <div>
          <div className="pw-cell-sub pw-admin-audit-actor" title={row.actorEmail ?? undefined}>
            {row.actorEmail ?? '—'}
          </div>
          <div className="pw-cell-sub">{role(row)}</div>
        </div>
      ),
    },
    {
      key: 'action',
      header: t.action,
      className: 'pw-cell-main',
      render: (row) => (
        <>
          <span className="pw-cell-title pw-cell-plain" title={auditSentence(row)}>
            {auditSentence(row)}
          </span>
          {/* A narrow table: the store and the IP fold under the sentence (G2.11; checklist L3). */}
          {foldLine(row) ? <span className="pw-cell-sub pw-admin-audit-fold">{foldLine(row)}</span> : null}
        </>
      ),
    },
    {
      key: 'store',
      header: t.store,
      className: 'pw-nowrap pw-admin-audit-col-fold',
      render: (row) => (row.storeId == null ? '—' : vi.admin.users.storeNumber(row.storeId)),
    },
    {
      key: 'ip',
      header: t.ip,
      className: 'pw-nowrap pw-admin-audit-col-fold',
      render: (row) => row.ip ?? '—',
    },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="audit-title">
      <div className="pw-admin-intro">
        <h1 id="audit-title" className="pw-page-title">
          {a.title}
        </h1>
        <p className="pw-muted">{a.lead}</p>
      </div>

      <div ref={filters} className="pw-list-filters" role="group" aria-label={a.filterLabel}>
        {storeId ? (
          <PresetFilter
            label={t.store}
            value={params.get('storeName') || vi.admin.users.storeNumber(storeId)}
            clearText={vi.admin.users.clearStore}
            onClear={() => update({ storeId: undefined, storeName: undefined })}
          />
        ) : null}
        {actorId ? (
          <PresetFilter
            label={t.actor}
            value={params.get('actor') || a.actorNumber(actorId)}
            clearText={a.clearActor}
            onClear={() => update({ actorId: undefined, actor: undefined })}
          />
        ) : null}
        <PopUpFilter
          label={t.action}
          value={action}
          emptyText={t.all}
          options={AUDIT_ACTIONS.map((value) => ({ value, label: AUDIT_ACTION_LABELS[value] ?? value }))}
          onChange={(value) => update({ action: value })}
        />
        <DatePickerFilter
          label={t.from}
          value={from}
          emptyText={t.any}
          today={todayIn(ZONE)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => update({ from: value })}
        />
        <DatePickerFilter
          label={t.to}
          value={to}
          emptyText={t.any}
          today={todayIn(ZONE)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => update({ to: value })}
        />
        {clearPresence !== 'closed' ? (
          <ClearFilters
            text={vi.plans.clearFilters}
            closing={clearPresence === 'closing'}
            onClear={() =>
              update({
                storeId: undefined,
                storeName: undefined,
                actorId: undefined,
                actor: undefined,
                action: undefined,
                from: undefined,
                to: undefined,
              })
            }
          />
        ) : null}
      </div>

      {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}

      {/* A row as tall as a real one can be (email over role, a two-line sentence) measures a page (G2.9). */}
      <div ref={probe} className="pw-fit-probe" aria-hidden="true" inert>
        {wide ? (
          <>
            <div className="pw-list-group pw-table-wrap pw-admin-audit-table">
              <table className="pw-table">
                <thead>
                  <tr>
                    <th>{t.at}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <div className="pw-cell-sub pw-admin-audit-actor">{t.actor}</div>
                      <div className="pw-cell-sub">{t.actor}</div>
                    </td>
                    <td>
                      <span className="pw-cell-title pw-cell-plain">
                        {t.action}
                        <br />
                        {t.action}
                      </span>
                      <span className="pw-cell-sub pw-admin-audit-fold">{t.action}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pager label={t.pagerLabel} page={0} totalPages={2} onPage={() => {}} />
          </>
        ) : null}
      </div>
      {!wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} pinned />
      ) : null}
      <div
        ref={result}
        key={shown.key}
        className={cx('pw-list-result', shown.motion, audit.isPlaceholderData && 'is-stale')}
        style={wide && fit && !empty ? { minHeight: fit.height } : undefined}
        aria-busy={audit.isPlaceholderData || undefined}
      >
        {loading ? (
          <div className="pw-list-group">
            <PlaceholderRows rows={Math.min(size ?? 6, 12)} label={t.loading} />
          </div>
        ) : empty ? (
          <section className="pw-list-empty" aria-labelledby="audit-empty-title">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <ListIcon />
            </span>
            <h2 id="audit-empty-title">{t.empty}</h2>
            {filtered ? <p>{vi.admin.users.emptyHint}</p> : null}
          </section>
        ) : wide ? (
          <DataTable<AuditEvent>
            caption={a.title}
            rows={items}
            columns={columns}
            resizable
            className="pw-list-group pw-admin-audit-table"
            onRowClick={setOpened}
          />
        ) : (
          <ul className="pw-list-group pw-list-rows" aria-label={a.title}>
            {items.map((row) => (
              <li key={row.id} className="pw-list-row">
                <button type="button" className="pw-list-row-main" onClick={() => setOpened(row)}>
                  <span className="pw-list-row-title pw-admin-audit-sentence">{auditSentence(row)}</span>
                  <span className="pw-list-row-sub">
                    {[formatDayTime(row.occurredAt, ZONE), row.actorEmail].filter(Boolean).join(' · ')}
                  </span>
                </button>
                <span className="pw-list-row-chevron" aria-hidden="true">
                  <ChevronRightIcon />
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
      {wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} />
      ) : null}

      {/* All of a row (§8.10): what the list cuts is read here. Close only: nothing to edit. */}
      <Sheet open={opened !== null} title={t.detailTitle} cancelLabel={t.close} onClose={() => setOpened(null)}>
        {opened ? (
          <dl className="pw-admin-detail">
            <div>
              <dt>{t.action}</dt>
              <dd>{auditSentence(opened)}</dd>
            </div>
            <div>
              <dt>{t.at}</dt>
              <dd className="pw-tabular">
                {`${formatDate(opened.occurredAt, ZONE)} ${formatTime(opened.occurredAt, ZONE)}`}
              </dd>
            </div>
            <div>
              <dt>{t.actor}</dt>
              <dd>
                {opened.actorEmail ?? '—'}
                {role(opened) ? <span className="pw-muted"> · {role(opened)}</span> : null}
              </dd>
            </div>
            {opened.storeId != null ? (
              <div>
                <dt>{t.store}</dt>
                <dd>
                  <Link to={`/admin/stores/${opened.storeId}`}>{vi.admin.users.storeNumber(opened.storeId)}</Link>
                </dd>
              </div>
            ) : null}
            {opened.ip ? (
              <div>
                <dt>{t.ip}</dt>
                <dd className="pw-tabular">{opened.ip}</dd>
              </div>
            ) : null}
          </dl>
        ) : null}
      </Sheet>
    </section>
  );
}
