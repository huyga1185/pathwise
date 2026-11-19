import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { DailyBarChart } from '@/platform/charts';
import { saveFile } from '@/platform/download';
import { fieldMessage, messageFor } from '@/shared/errors';
import {
  formatDateLong,
  formatDayTime,
  formatDistance,
  formatRate,
  formatTime,
  shortDate,
  spokenDate,
  todayIn,
  weekdayShort,
} from '@/shared/format';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  ActivityIndicator,
  Button,
  DataTable,
  DateField,
  ErrorBanner,
  Pager,
  SegmentedControl,
  Sheet,
} from '@/shared/ui';
import { ChartIcon, ChevronRightIcon, ShareIcon } from '@/shared/ui/icons';
import {
  MAX_DAYS,
  useReport,
  useReportApi,
  type ReportDriver,
  type ReportFailureReason,
  type StoreReport,
} from './reportsApi';
import './reports.css';

const t = vi.reports;
const DAY = /^\d{4}-\d{2}-\d{2}$/;

/** "2026-12-07" − 6 days → "2026-12-01". */
function addDays(day: string, days: number): string {
  const date = new Date(`${day}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000) + 1;
}

/**
 * Prototype "Báo cáo" (W12, T63): one day or a period of at most 31 days by plan date, on the URL (`?from=&to=`).
 * Cards, results by day, drivers, frequent failure reasons, and the stops as CSV. Readable while the store is locked.
 */
export function ReportsPage() {
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? 'Asia/Ho_Chi_Minh';
  const today = todayIn(timeZone);
  const [params, setParams] = useSearchParams();
  const to = DAY.test(params.get('to') ?? '') ? (params.get('to') as string) : today;
  const from = DAY.test(params.get('from') ?? '') ? (params.get('from') as string) : to;
  const days = daysBetween(from, to);
  const rangeError = days < 1 ? fieldMessage('INVALID_DATE_RANGE', '') : days > MAX_DAYS ? t.tooLong : null;
  const report = useReport(from, to, rangeError === null);
  const reportApi = useReportApi();
  const [exporting, setExporting] = useState(false);
  const [exportFailure, setExportFailure] = useState<unknown>(null);

  // A new period starts the drivers' list on its first page (G2.9).
  const setPeriod = (next: { from: string; to: string }) => setParams(next, { replace: true });
  const page = Math.max(1, Math.floor(Number(params.get('page')) || 1));
  const setPage = (next: number) => {
    const all = new URLSearchParams(params);
    if (next > 1) all.set('page', String(next));
    else all.delete('page');
    setParams(all);
  };
  const exportCsv = async () => {
    setExporting(true);
    setExportFailure(null);
    try {
      const file = await reportApi.stopsCsv(from, to);
      saveFile(file.blob, file.filename);
    } catch (error) {
      setExportFailure(error);
    } finally {
      setExporting(false);
    }
  };
  // The period (G6-Q1): a preset when the URL's range is one, else "Tùy chọn"; choosing "Tùy chọn" keeps the range
  // and shows the two dates.
  const presets: Record<Exclude<Period, 'custom'>, { from: string; to: string }> = {
    today: { from: today, to: today },
    week: { from: addDays(today, -6), to: today },
    month: { from: addDays(today, -29), to: today },
  };
  const matching = (Object.keys(presets) as Array<Exclude<Period, 'custom'>>).find(
    (key) => presets[key].from === from && presets[key].to === to,
  );
  const [customChosen, setCustomChosen] = useState(false);
  const period: Period = customChosen || !matching ? 'custom' : matching;
  const wide = useMediaQuery(WIDE_QUERY);

  return (
    <section className="pw-page-section" aria-labelledby="reports-title">
      {/* Title and the action at its end (G6.1; G2.1): a button on a wide window, a round icon on a phone. */}
      <div className="pw-reports-intro">
        <div className="pw-list-head">
          <h1 id="reports-title" className="pw-page-title">
            {t.title}
          </h1>
          {/* The page's one action, filled like "+ Tạo kế hoạch" in the same place on the other pages (G2.1; HIG
              Buttons: style marks the preferred action; C13 consistency). */}
          <Button
            variant="primary"
            className={wide ? undefined : 'pw-icon-button pw-list-add pw-reports-export'}
            busy={exporting}
            busyLabel={wide ? t.exporting : undefined}
            disabled={rangeError !== null}
            aria-label={wide ? undefined : t.exportCsv}
            title={wide ? undefined : t.exportCsv}
            onClick={() => void exportCsv()}
          >
            <ShareIcon />
            {wide ? t.exportCsv : null}
          </Button>
        </div>
        <p className="pw-muted">{t.lead}</p>
      </div>
      {/* The period (G6-Q1): a segmented control; "Tùy chọn" shows the two dates (G4 date fields). */}
      <form aria-label={t.range} className="pw-reports-filters" onSubmit={(event) => event.preventDefault()}>
        <SegmentedControl<Period>
          label={t.range}
          idPrefix="report-period"
          value={period}
          options={[
            { value: 'today', label: t.today },
            { value: 'week', label: t.week },
            { value: 'month', label: t.month },
            { value: 'custom', label: t.custom },
          ]}
          onChange={(next) => {
            if (next === 'custom') {
              setCustomChosen(true);
              return;
            }
            setCustomChosen(false);
            setPeriod(presets[next]);
          }}
        />
        {period === 'custom' ? (
          <div className="pw-reports-dates">
            <DateField
              label={t.from}
              value={from}
              today={today}
              required
              error={rangeError ?? undefined}
              onChange={(value) => value && setPeriod({ from: value, to })}
            />
            <DateField
              label={t.to}
              value={to}
              today={today}
              required
              hint={t.maxHint}
              onChange={(value) => value && setPeriod({ from, to: value })}
            />
          </div>
        ) : null}
      </form>
      {exportFailure ? <ErrorBanner title={messageFor(exportFailure)} /> : null}
      {report.isError ? <ErrorBanner title={messageFor(report.error)} /> : null}
      {/* Loading: the spinner after a moment (G2.12; checklist L4). */}
      {rangeError === null && report.isPending ? <ActivityIndicator label={vi.pages.loading} /> : null}
      {rangeError === null && report.data ? (
        report.data.plans.total === 0 ? (
          // Empty like ContentUnavailableView, no card (G2.7): a symbol, a title, a line.
          <section className="pw-list-empty" aria-labelledby="reports-empty">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <ChartIcon />
            </span>
            <h2 id="reports-empty">{t.empty}</h2>
            <p>{t.emptyLead}</p>
          </section>
        ) : (
          // A new period's report settles in as a new result (§9.6; checklist L5).
          <div key={`${report.data.from}-${report.data.to}`} className="pw-reports-body">
            <ReportBody report={report.data} timeZone={timeZone} page={page} onPage={setPage} />
          </div>
        )
      ) : null}
    </section>
  );
}

/** Drivers per page (G2.9 with a fixed size: the table sits under the chart, "fits one screen" does not apply; §11.8). */
const DRIVERS_PER_PAGE = 10;

function ReportBody({
  report,
  timeZone,
  page: asked,
  onPage,
}: {
  report: StoreReport;
  timeZone: string;
  page: number;
  onPage: (page: number) => void;
}) {
  const { plans, stops } = report;
  const wide = useMediaQuery(WIDE_QUERY);
  const [opened, setOpened] = useState<ReportDriver | null>(null);
  const totalPages = Math.max(1, Math.ceil(report.drivers.length / DRIVERS_PER_PAGE));
  // A page past the end (an old link) shows the last one (G2.9).
  const page = Math.min(asked, totalPages);
  const drivers = report.drivers.slice((page - 1) * DRIVERS_PER_PAGE, page * DRIVERS_PER_PAGE);
  const pager = (
    <Pager
      label={t.driversPager}
      // The pager counts from 0; the URL and this page from 1.
      page={page - 1}
      totalPages={totalPages}
      onPage={(index) => onPage(index + 1)}
    />
  );
  const oneDay = report.from === report.to;
  const when = (instant: string | null | undefined) =>
    !instant ? '—' : oneDay ? formatTime(instant, timeZone) : formatDayTime(instant, timeZone);
  // Day labels as Health's: a week by day of the week, longer periods by date (G6, §11.7).
  const byWeekday = report.days.length <= 7;
  const points = report.days.map((day) => ({
    label: byWeekday ? weekdayShort(day.date) : shortDate(day.date),
    spoken: spokenDate(day.date),
    delivered: day.delivered,
    failed: day.failed,
    skipped: day.skipped,
  }));
  return (
    <>
      {/* One group of four totals, no outline (G6-Q2; G2/G3 groups). */}
      <section className="pw-reports-totals" aria-label={t.totals}>
        <Total
          label={t.plans}
          value={String(plans.published + plans.inProgress + plans.completed)}
          sub={t.plansSub(plans.completed, plans.draft, plans.cancelled)}
        />
        <Total
          label={t.stops}
          value={String(stops.total)}
          sub={t.stopsSub(stops.delivered, stops.failed, stops.skipped, stops.pending)}
        />
        <Total label={t.rate} value={formatRate(stops.deliveredRate)} sub={t.rateSub} />
        <Total label={t.distance} value={formatDistance(report.plannedDistanceMeters)} sub={t.distanceSub} />
      </section>
      {!oneDay ? (
        <section className="pw-reports-group" aria-labelledby="chart-title">
          <h2 id="chart-title" className="pw-reports-h2">
            {t.chart}
          </h2>
          {/* The main message over the chart, a chosen day's numbers when one is chosen (HIG Charts). */}
          <DailyBarChart
            data={points}
            stacked
            series={[
              { key: 'delivered', label: t.delivered, token: 'success' },
              { key: 'failed', label: t.failed, token: 'danger' },
              { key: 'skipped', label: t.skipped, token: 'signal' },
            ]}
            ariaLabel={t.chartLabel(shortDate(report.from), shortDate(report.to))}
            summary={(index) => {
              const day = index === null ? null : report.days[index];
              return day
                ? t.chartDay(formatDateLong(day.date), day.delivered, day.failed, day.skipped)
                : t.chartSummary(stops.delivered, stops.failed, stops.skipped);
            }}
          />
        </section>
      ) : null}
      <div className="pw-reports-tables">
        <section className="pw-reports-drivers" aria-labelledby="drivers-title">
          <h2 id="drivers-title" className="pw-reports-h2">
            {t.drivers}
          </h2>
          {/* Wide: a table in its group; the routes and kilometres under the name, the first and last results in one
              column, so it fits (G2.11). Phone: rows, each opening the driver's numbers in a sheet (G3). */}
          {wide ? (
            <DataTable<ReportDriver>
              key={page}
              caption={t.drivers}
              rows={drivers}
              // A long name is cut to two lines: the whole row opens its sheet with all of it (HIG Typography).
              onRowClick={setOpened}
              rowKey="driverId"
              empty={t.noDrivers}
              className="pw-list-group"
              columns={[
                {
                  key: 'name',
                  header: t.driver,
                  render: (row) => (
                    <>
                      <strong className="pw-cell-title" title={row.fullName || undefined}>
                        {row.fullName || t.unnamed}
                      </strong>
                      <div className="pw-muted pw-tabular pw-reports-driver-sub">
                        {t.driverSub(row.routes, formatDistance(row.plannedDistanceMeters))}
                      </div>
                      {/* A narrow table: the first and last results fold under the name (G2.11), as another line. */}
                      {row.firstResultAt ? (
                        <div className="pw-muted pw-tabular pw-reports-driver-reported">
                          {t.reportedLine(t.reportedRange(when(row.firstResultAt), when(row.lastResultAt)))}
                        </div>
                      ) : null}
                    </>
                  ),
                },
                { key: 'delivered', header: t.delivered, numeric: true, render: (row) => row.delivered },
                { key: 'failed', header: t.failed, numeric: true, render: (row) => row.failed },
                { key: 'skipped', header: t.skipped, numeric: true, render: (row) => row.skipped },
                { key: 'pending', header: t.pending, numeric: true, render: (row) => row.pending },
                {
                  key: 'reported',
                  header: t.reported,
                  numeric: true,
                  className: 'pw-nowrap pw-reports-col-reported',
                  render: (row) =>
                    row.firstResultAt ? t.reportedRange(when(row.firstResultAt), when(row.lastResultAt)) : '—',
                },
              ]}
            />
          ) : report.drivers.length === 0 ? (
            <p className="pw-muted">{t.noDrivers}</p>
          ) : (
            <>
              {/* Phone: the pager above the list (§8.17), not pinned — the page has other parts under it (§11.8). */}
              {pager}
              <ul key={page} className="pw-list-group pw-list-rows pw-reports-page" aria-label={t.drivers}>
                {drivers.map((row) => (
                  <li key={row.driverId} className="pw-list-row">
                    <button
                      type="button"
                      className="pw-list-row-main"
                      aria-label={t.driverOpen(row.fullName || t.unnamed)}
                      onClick={() => setOpened(row)}
                    >
                      <span className="pw-list-row-title">{row.fullName || t.unnamed}</span>
                      <span className="pw-list-row-sub pw-tabular">
                        {t.driverRow(row.routes, row.delivered, row.failed, row.skipped, row.pending)}
                      </span>
                    </button>
                    <span className="pw-list-row-chevron" aria-hidden="true">
                      <ChevronRightIcon />
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
          {wide ? pager : null}
        </section>
        <section aria-labelledby="reasons-title">
          <h2 id="reasons-title" className="pw-reports-h2">
            {t.reasons}
          </h2>
          {wide ? (
            <DataTable<ReportFailureReason>
              caption={t.reasons}
              rows={report.failureReasons}
              rowKey="reason"
              empty={t.noReasons}
              className="pw-list-group"
              columns={[
                // All of the reason, wrapped (not cut: nothing else shows it; G2.11, HIG Typography).
                { key: 'reason', header: t.reason, className: 'pw-reports-col-reason', render: (row) => row.reason },
                { key: 'count', header: t.count, numeric: true, render: (row) => row.count },
              ]}
            />
          ) : report.failureReasons.length === 0 ? (
            <p className="pw-muted">{t.noReasons}</p>
          ) : (
            // Read only: nothing more to open, so no chevron.
            <ul className="pw-list-group pw-list-rows" aria-label={t.reasons}>
              {report.failureReasons.map((row) => (
                <li key={row.reason} className="pw-list-row pw-reports-reason">
                  {/* Not cut: nothing else shows the whole reason (HIG Typography). */}
                  <span className="pw-list-row-title pw-reports-reason-text">{row.reason}</span>
                  <span className="pw-tabular">{row.count}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
      {/* A driver's numbers on a phone (G3: a sheet with all of a row). Close only: nothing to edit. */}
      <Sheet open={opened !== null} title={t.driver} cancelLabel={t.close} onClose={() => setOpened(null)}>
        {opened ? (
          <dl className="pw-reports-detail">
            {(
              [
                [t.driver, opened.fullName || t.unnamed],
                [t.routes, opened.routes],
                [t.delivered, opened.delivered],
                [t.failed, opened.failed],
                [t.skipped, opened.skipped],
                [t.pending, opened.pending],
                [t.km, formatDistance(opened.plannedDistanceMeters)],
                [t.first, when(opened.firstResultAt)],
                [t.last, when(opened.lastResultAt)],
              ] as Array<[string, string | number]>
            ).map(([term, value]) => (
              <div key={term}>
                <dt>{term}</dt>
                <dd className="pw-tabular">{value}</dd>
              </div>
            ))}
          </dl>
        ) : null}
      </Sheet>
    </>
  );
}

type Period = 'today' | 'week' | 'month' | 'custom';

/** One of the four totals: a label, the number, a line under it. */
function Total({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div className="pw-reports-total">
      <span className="pw-reports-total-label">{label}</span>
      <span className="pw-reports-total-value pw-tabular">{value}</span>
      <span className="pw-muted pw-reports-total-sub">{sub}</span>
    </div>
  );
}
