import { useEffect, useRef, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import { useStoreQuery } from '@/data/storeQuery';
import type { Schemas } from '@/data/types';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime, formatTime, todayIn } from '@/shared/format';
import { reducedMotion } from '@/shared/hooks/useExit';
import { useFitRows } from '@/shared/hooks/useFitRows';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  DataTable,
  DatePickerFilter,
  EmptyState,
  ErrorBanner,
  Pager,
  PlaceholderRows,
  PopUpFilter,
  Sheet,
  type DataTableColumn,
} from '@/shared/ui';
import { ChevronRightIcon } from '@/shared/ui/icons';
import { cx } from '@/shared/ui/cx';
import { StoreTabs } from './StoreTabs';

type StoreAuditEvent = Schemas['StoreAuditEventResponse'];
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;
/** A page on a phone (G2.9); a wide window fits its rows to the window. */
const PAGE_SIZE = 20;

/**
 * "Nhật ký cửa hàng" (W11, T58; OWNER_UI_REDESIGN §8.3): the OWNER's own store log, newest first, days in the store's
 * time zone. Paged like the plan list (OWNER_UI_REDESIGN G2.9, §8.10): the page on the URL (`?page=`), a wide window's
 * page fits the window, 20 on a phone, the pager stays put, pages slide. Filters as on the plan list; a table on a
 * wide window, rows (what happened, then when · who) on a phone. Long text is cut to two lines there, and each row
 * opens a sheet with all of it (HIG Typography: truncate in a list only when people can open a view with the rest).
 * An ADMIN's IP is never there (the server leaves it out).
 */
export function StoreAuditPage() {
  const api = useApi();
  const t = vi.audit;
  const [params, setParams] = useSearchParams();
  const filter = {
    action: (params.get('action') as StoreAuditEvent['action']) || undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    page: Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0),
  };
  const wide = useMediaQuery(WIDE_QUERY);
  const result = useRef<HTMLDivElement>(null);
  const probe = useRef<HTMLDivElement>(null);
  const filters = useRef<HTMLDivElement>(null);
  const fit = useFitRows(wide, result, probe, filters, PAGE_SIZE);
  const size = wide ? (fit?.rows ?? null) : PAGE_SIZE;
  const [opened, setOpened] = useState<StoreAuditEvent | null>(null);
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;
  const audit = useQuery({
    queryKey: queryKeys.storeAudit({ ...filter, size }),
    enabled: size !== null,
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/store/audit', {
          params: {
            query: {
              ...filter,
              from: filter.from || undefined,
              to: filter.to || undefined,
              size: size ?? PAGE_SIZE,
              sort: ['occurredAt,desc'],
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
  const change = (next: Partial<Record<'action' | 'from' | 'to' | 'page', string | undefined>>) => {
    const merged = new URLSearchParams(params);
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value);
      else merged.delete(key);
    }
    if (!('page' in next)) merged.delete('page');
    setParams(merged, { replace: true });
  };
  const goToPage = (target: number) => {
    change({ page: target > 0 ? String(target) : undefined });
    // A phone's next page starts at the top of the list: scroll back only as far as needed (G2.9).
    const top = result.current?.getBoundingClientRect().top ?? 0;
    if (!wide && top < 0) {
      result.current?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' });
    }
  };
  // A wider or narrower window changes the rows a page holds: keep the first row shown on the page shown.
  const [lastSize, setLastSize] = useState(size);
  if (size !== lastSize) {
    setLastSize(size);
    if (size && lastSize && filter.page > 0) {
      const moved = Math.floor((filter.page * lastSize) / size);
      const next = new URLSearchParams(params);
      if (moved > 0) next.set('page', String(moved));
      else next.delete('page');
      setParams(next, { replace: true });
    }
  }
  const page = audit.data;
  const totalPages = page?.totalPages ?? 0;
  const loading = page === undefined && !audit.isError;
  const empty = page !== undefined && page.items.length === 0 && totalPages === 0;
  // A page past the end (an old link, a narrower window): show the last page.
  const pastEnd = page !== undefined && !audit.isPlaceholderData && totalPages > 0 && filter.page >= totalPages;
  useEffect(() => {
    if (!pastEnd) return;
    const next = new URLSearchParams(params);
    if (totalPages > 1) next.set('page', String(totalPages - 1));
    else next.delete('page');
    setParams(next, { replace: true });
  }, [pastEnd, params, setParams, totalPages]);
  // Another page slides in from its side; new filters settle in from below (G2.9, G2.10).
  const filterKey = [loading ? 'loading' : empty ? 'empty' : 'list', filter.action, filter.from, filter.to].join('|');
  const resultKey = `${filterKey}|${filter.page}`;
  const [shown, setShown] = useState({ key: resultKey, filterKey, page: filter.page, motion: '' });
  if (!audit.isPlaceholderData && resultKey !== shown.key) {
    const motion = filterKey !== shown.filterKey ? 'is-changed' : filter.page > shown.page ? 'is-next' : 'is-prev';
    setShown({ key: resultKey, filterKey, page: filter.page, motion });
  }
  const actor = (row: StoreAuditEvent) => (row.actorRole === 'ADMIN' ? t.admin : (row.actorEmail ?? '—'));
  const columns: DataTableColumn<StoreAuditEvent>[] = [
    {
      key: 'at',
      header: t.at,
      render: (row) => <span className="pw-nowrap pw-tabular">{formatDayTime(row.occurredAt, timeZone)}</span>,
    },
    {
      key: 'actor',
      header: t.actor,
      className: 'pw-store-audit-col-actor',
      // One line each (email, role); the whole address is in the row's sheet.
      render: (row) =>
        row.actorRole === 'ADMIN' ? (
          <span className="pw-cell-sub pw-store-audit-actor">{t.admin}</span>
        ) : (
          <div>
            <div className="pw-cell-sub pw-store-audit-actor" title={row.actorEmail ?? undefined}>
              {row.actorEmail ?? '—'}
            </div>
            <div className="pw-cell-sub">{row.actorRole ? (vi.roles[row.actorRole] ?? row.actorRole) : ''}</div>
          </div>
        ),
    },
    {
      key: 'action',
      header: t.action,
      className: 'pw-cell-main',
      render: (row) => (
        <span className="pw-cell-title pw-cell-plain" title={auditSentence(row)}>
          {auditSentence(row)}
        </span>
      ),
    },
    { key: 'ip', header: t.ip, className: 'pw-nowrap', render: (row) => row.ip ?? '—' },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="store-audit-title">
      <div className="pw-list-head pw-store-head">
        <h1 id="store-audit-title" className="pw-page-title">
          {vi.store.title}
        </h1>
      </div>
      <StoreTabs />
      <p className="pw-muted pw-list-lead-tight">{t.storeLead}</p>
      {/* Filters like the plan list (G2): pop-up button and compact date pickers, no card. */}
      <div ref={filters} className="pw-list-filters" role="group" aria-label={t.filterLabel}>
        <PopUpFilter
          label={t.action}
          value={filter.action}
          emptyText={t.all}
          options={AUDIT_ACTIONS.map((a) => ({ value: a, label: AUDIT_ACTION_LABELS[a] ?? a }))}
          onChange={(value) => change({ action: value })}
        />
        <DatePickerFilter
          label={t.from}
          value={filter.from}
          emptyText={t.any}
          today={todayIn(timeZone)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => change({ from: value })}
        />
        <DatePickerFilter
          label={t.to}
          value={filter.to}
          emptyText={t.any}
          today={todayIn(timeZone)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => change({ to: value })}
        />
      </div>
      {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}

      {/* A hidden row as tall as a real one can be (one line of email over the role, two of the sentence) measures how
          many rows fit the window (G2.9). */}
      <div ref={probe} className="pw-fit-probe" aria-hidden="true" inert>
        {wide ? (
          <>
            <div className="pw-list-group pw-table-wrap">
              <table className="pw-table">
                <thead>
                  <tr>
                    <th>{t.at}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <div className="pw-cell-sub pw-store-audit-actor">{t.actor}</div>
                      <div className="pw-cell-sub">{t.actor}</div>
                    </td>
                    <td>
                      <span className="pw-cell-title pw-cell-plain">
                        {t.action}
                        <br />
                        {t.action}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pager label={t.pagerLabel} page={0} totalPages={2} onPage={() => {}} />
          </>
        ) : null}
      </div>
      <div
        ref={result}
        key={shown.key}
        className={cx('pw-list-result', shown.motion, audit.isPlaceholderData && 'is-stale')}
        // A full page's height on a wide window, so the pager stays put on a short last page (G2.9).
        style={wide && fit && !empty ? { minHeight: fit.height } : undefined}
        aria-busy={audit.isPlaceholderData || loading}
      >
        {loading ? (
          <div className="pw-list-group">
            <PlaceholderRows rows={6} label={t.loading} />
          </div>
        ) : empty ? (
          <EmptyState title={t.empty} />
        ) : wide ? (
          <DataTable<StoreAuditEvent>
            caption={t.title}
            rows={page?.items ?? []}
            columns={columns}
            resizable
            className="pw-list-group"
            onRowClick={setOpened}
          />
        ) : (
          <ul className="pw-list-group pw-list-rows" aria-label={t.title}>
            {(page?.items ?? []).map((row) => (
              <li key={row.id} className="pw-list-row">
                <button type="button" className="pw-list-row-main" onClick={() => setOpened(row)}>
                  <span className="pw-list-row-title pw-store-audit-sentence">{auditSentence(row)}</span>
                  <span className="pw-list-row-sub">
                    {formatDayTime(row.occurredAt, timeZone)} · {actor(row)}
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
      {/* Outside the result, which is drawn again on each page: the pager keeps its focus while pages turn. */}
      {page && !empty ? (
        <Pager label={t.pagerLabel} page={filter.page} totalPages={totalPages} onPage={goToPage} />
      ) : null}

      {/* All of a row: what a list cuts to two lines is read here (HIG Typography). Close only: nothing to edit. */}
      <Sheet open={opened !== null} title={t.detailTitle} cancelLabel={t.close} onClose={() => setOpened(null)}>
        {opened ? (
          <dl className="pw-store-audit-detail">
            <div>
              <dt>{t.action}</dt>
              <dd>{auditSentence(opened)}</dd>
            </div>
            <div>
              <dt>{t.at}</dt>
              <dd className="pw-tabular">
                {`${formatDate(opened.occurredAt, timeZone)} ${formatTime(opened.occurredAt, timeZone)}`}
              </dd>
            </div>
            <div>
              <dt>{t.actor}</dt>
              <dd>
                {actor(opened)}
                {opened.actorRole && opened.actorRole !== 'ADMIN' ? (
                  <span className="pw-muted"> · {vi.roles[opened.actorRole] ?? opened.actorRole}</span>
                ) : null}
              </dd>
            </div>
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
