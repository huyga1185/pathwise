import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { DailyBarChart } from '@/platform/charts';
import { saveFile } from '@/platform/download';
import { fieldMessage, messageFor } from '@/shared/errors';
import { formatDayTime, formatDistance, formatRate, formatTime, shortDate, todayIn } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, EmptyState, ErrorBanner, Stat, TextField } from '@/shared/ui';
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

  const setPeriod = (next: { from: string; to: string }) => setParams(next, { replace: true });
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
  const isToday = from === today && to === today;
  const isWeek = from === addDays(today, -6) && to === today;

  return (
    <section className="pw-page-section" aria-labelledby="reports-title">
      <div className="pw-reports-head">
        <div className="pw-reports-intro">
          <h1 id="reports-title" className="pw-page-title">
            {t.title}
          </h1>
          <p className="pw-muted">{t.lead}</p>
        </div>
        <Button variant="secondary" busy={exporting} disabled={rangeError !== null} onClick={() => void exportCsv()}>
          {exporting ? t.exporting : t.exportCsv}
        </Button>
      </div>
      <form aria-label={t.range} className="pw-reports-filters" onSubmit={(event) => event.preventDefault()}>
        <div className="pw-segmented" role="group" aria-label={t.range}>
          <button
            type="button"
            className="pw-segmented-btn"
            aria-pressed={isToday}
            onClick={() => setPeriod({ from: today, to: today })}
          >
            {t.today}
          </button>
          <button
            type="button"
            className="pw-segmented-btn"
            aria-pressed={isWeek}
            onClick={() => setPeriod({ from: addDays(today, -6), to: today })}
          >
            {t.week}
          </button>
        </div>
        <TextField
          label={t.from}
          type="date"
          value={from}
          error={rangeError ?? undefined}
          onChange={(event) => event.target.value && setPeriod({ from: event.target.value, to })}
        />
        <TextField
          label={t.to}
          type="date"
          value={to}
          hint={t.maxHint}
          onChange={(event) => event.target.value && setPeriod({ from, to: event.target.value })}
        />
      </form>
      {exportFailure ? <ErrorBanner title={messageFor(exportFailure)} /> : null}
      {report.isError ? <ErrorBanner title={messageFor(report.error)} /> : null}
      {rangeError === null && report.data ? (
        report.data.plans.total === 0 ? (
          <EmptyState title={t.empty}>{t.emptyLead}</EmptyState>
        ) : (
          <ReportBody report={report.data} timeZone={timeZone} />
        )
      ) : null}
    </section>
  );
}

function ReportBody({ report, timeZone }: { report: StoreReport; timeZone: string }) {
  const { plans, stops } = report;
  const oneDay = report.from === report.to;
  const when = (instant: string | null | undefined) =>
    !instant ? '—' : oneDay ? formatTime(instant, timeZone) : formatDayTime(instant, timeZone);
  const points = report.days.map((day) => ({
    label: shortDate(day.date),
    delivered: day.delivered,
    failed: day.failed,
    skipped: day.skipped,
  }));
  return (
    <>
      <div className="pw-reports-grid">
        <Stat
          label={t.plans}
          value={plans.published + plans.inProgress + plans.completed}
          sub={t.plansSub(plans.completed, plans.draft, plans.cancelled)}
        />
        <Stat
          label={t.stops}
          value={stops.total}
          sub={t.stopsSub(stops.delivered, stops.failed, stops.skipped, stops.pending)}
        />
        <Stat label={t.rate} value={formatRate(stops.deliveredRate)} sub={t.rateSub} />
        <Stat label={t.distance} value={formatDistance(report.plannedDistanceMeters)} sub={t.distanceSub} />
      </div>
      {!oneDay ? (
        <div className="pw-reports-card">
          <h2 className="pw-reports-h2">{t.chart}</h2>
          <DailyBarChart
            data={points}
            stacked
            series={[
              { key: 'delivered', label: t.delivered, token: 'success' },
              { key: 'failed', label: t.failed, token: 'danger' },
              { key: 'skipped', label: t.skipped, token: 'signal' },
            ]}
            ariaLabel={t.chartLabel(shortDate(report.from), shortDate(report.to))}
          />
        </div>
      ) : null}
      <div className="pw-reports-tables">
        <section className="pw-reports-drivers" aria-labelledby="drivers-title">
          <h2 id="drivers-title" className="pw-reports-h2">
            {t.drivers}
          </h2>
          <DataTable<ReportDriver>
            caption={t.drivers}
            rows={report.drivers}
            rowKey="driverId"
            empty={t.noDrivers}
            columns={[
              { key: 'name', header: t.driver, render: (row) => <strong>{row.fullName || t.unnamed}</strong> },
              { key: 'routes', header: t.routes, numeric: true, render: (row) => row.routes },
              { key: 'delivered', header: t.delivered, numeric: true, render: (row) => row.delivered },
              { key: 'failed', header: t.failed, numeric: true, render: (row) => row.failed },
              { key: 'skipped', header: t.skipped, numeric: true, render: (row) => row.skipped },
              { key: 'pending', header: t.pending, numeric: true, render: (row) => row.pending },
              { key: 'km', header: t.km, numeric: true, render: (row) => formatDistance(row.plannedDistanceMeters) },
              { key: 'first', header: t.first, numeric: true, render: (row) => when(row.firstResultAt) },
              { key: 'last', header: t.last, numeric: true, render: (row) => when(row.lastResultAt) },
            ]}
          />
        </section>
        <section aria-labelledby="reasons-title">
          <h2 id="reasons-title" className="pw-reports-h2">
            {t.reasons}
          </h2>
          <DataTable<ReportFailureReason>
            caption={t.reasons}
            rows={report.failureReasons}
            rowKey="reason"
            empty={t.noReasons}
            columns={[
              { key: 'reason', header: t.reason, render: (row) => row.reason },
              { key: 'count', header: t.count, numeric: true, render: (row) => row.count },
            ]}
          />
        </section>
      </div>
    </>
  );
}
