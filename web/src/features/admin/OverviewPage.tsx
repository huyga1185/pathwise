import { useState } from 'react';
import { DailyBarChart } from '@/platform/charts';
import { messageFor } from '@/shared/errors';
import {
  formatDateLong,
  formatDayTime,
  formatDuration,
  formatRate,
  shortDate,
  spokenDate,
  todayIn,
} from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { ActivityIndicator, DataTable, ErrorBanner, SegmentedControl, Stat, StatGroup } from '@/shared/ui';
import { useBusinessMetrics, useTechnicalMetrics, type BusinessMetrics, type TechnicalMetrics } from './adminApi';
import { formatBytes } from './adminFormat';

const ZONE = 'Asia/Ho_Chi_Minh';
const RANGES = [7, 30, 90] as const;
type Range = (typeof RANGES)[number];

function addDays(day: string, days: number): string {
  const date = new Date(`${day}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

/**
 * "Quản trị — tổng quan" (W11, T60; redesigned in G7, OWNER_UI_REDESIGN §12): the period as the reports' segmented
 * control, numbers in groups, the two charts by day in groups with their summary, the instance's technical numbers.
 */
export function OverviewPage() {
  const [range, setRange] = useState<Range>(30);
  const to = todayIn(ZONE);
  const from = addDays(to, -(range - 1));
  const business = useBusinessMetrics(from, to);
  const technical = useTechnicalMetrics();
  const t = vi.admin.overview;
  return (
    <section className="pw-page-section" aria-labelledby="overview-title">
      <div className="pw-admin-intro">
        <h1 id="overview-title" className="pw-page-title">
          {t.title}
        </h1>
        {business.data ? <p className="pw-muted">{t.lead(formatDayTime(business.data.computedAt, ZONE))}</p> : null}
      </div>
      {/* The period (G6.2): centred on a wide window (HIG Tab views), full width on a phone. */}
      <SegmentedControl<`${Range}`>
        label={t.range}
        idPrefix="admin-range"
        value={`${range}`}
        options={RANGES.map((n) => ({ value: `${n}` as const, label: t.days(n) }))}
        onChange={(next) => setRange(Number(next) as Range)}
      />
      {business.isError ? <ErrorBanner title={messageFor(business.error)} /> : null}
      {/* Loading: the spinner after a moment (G2.12; checklist L4). */}
      {business.isPending ? <ActivityIndicator label={vi.pages.loading} /> : null}
      {business.data ? (
        // A new period's numbers settle in as a new result (§9.6; checklist L5).
        <div key={`${business.data.from}-${business.data.to}`} className="pw-admin-body">
          <BusinessBlock data={business.data} range={range} />
        </div>
      ) : null}
      {technical.isError ? <ErrorBanner title={messageFor(technical.error)} /> : null}
      {technical.data ? <TechnicalBlock data={technical.data} /> : null}
    </section>
  );
}

function BusinessBlock({ data, range }: { data: BusinessMetrics; range: Range }) {
  const t = vi.admin.overview;
  const { now, totals, days } = data;
  const points = days.map((day) => ({
    label: shortDate(day.date),
    // The full day for assistive technologies (HIG Charts: "June 6" rather than "6/6").
    spoken: spokenDate(day.date),
    published: day.plansPublished,
    delivered: day.stopsDelivered,
    failed: day.stopsFailed,
    skipped: day.stopsSkipped,
  }));
  return (
    <>
      <section className="pw-admin-block" aria-labelledby="now-title">
        <h2 id="now-title" className="pw-section-title">
          {t.now}
        </h2>
        <StatGroup label={t.now}>
          <Stat label={t.stores} value={now.stores} sub={t.locked(now.lockedStores)} />
          <Stat label={t.active7} value={now.activeStores7d} sub={t.activeHint} />
          <Stat label={t.active30} value={now.activeStores30d} sub={t.ofStores(now.stores)} />
          <Stat
            label={t.users}
            value={now.owners + now.dispatchers + now.drivers + now.admins}
            sub={t.usersByRole(now.owners, now.dispatchers, now.drivers, now.admins)}
          />
        </StatGroup>
      </section>
      <section className="pw-admin-block" aria-labelledby="range-title">
        <h2 id="range-title" className="pw-section-title">
          {t.rangeTitle(range, shortDate(data.from), shortDate(data.to))}
        </h2>
        <StatGroup label={t.rangeTitle(range, shortDate(data.from), shortDate(data.to))}>
          <Stat label={t.newStores} value={totals.newStores} />
          <Stat
            label={t.published}
            value={totals.plansPublished}
            sub={t.plansSub(totals.plansCreated, totals.plansCompleted)}
          />
          <Stat
            label={t.delivered}
            value={formatRate(totals.deliveredRate)}
            sub={t.stopsSub(totals.stopsDelivered, totals.stopsFailed, totals.stopsSkipped)}
          />
          <Stat
            label={t.optimizations}
            value={totals.optimizations}
            sub={t.optFail(formatRate(totals.optimizationFailureRate))}
          />
        </StatGroup>
        <div className="pw-admin-charts">
          <section className="pw-group" aria-labelledby="plans-chart-title">
            <h3 id="plans-chart-title" className="pw-group-title">
              {t.plansChart}
            </h3>
            <DailyBarChart
              data={points}
              series={[{ key: 'published', label: t.seriesPublished, token: 'route' }]}
              ariaLabel={t.plansChartLabel(range, totals.plansPublished)}
              // The main message over the chart, a chosen day's number when one is chosen (HIG Charts, G6.4).
              summary={(index) => {
                const day = index === null ? null : days[index];
                return day
                  ? t.plansDay(formatDateLong(day.date), day.plansPublished)
                  : t.plansSummary(totals.plansPublished);
              }}
            />
          </section>
          <section className="pw-group" aria-labelledby="stops-chart-title">
            <h3 id="stops-chart-title" className="pw-group-title">
              {t.stopsChart}
            </h3>
            <DailyBarChart
              data={points}
              stacked
              series={[
                { key: 'delivered', label: t.seriesDelivered, token: 'success' },
                { key: 'failed', label: t.seriesFailed, token: 'danger' },
                { key: 'skipped', label: t.seriesSkipped, token: 'signal' },
              ]}
              ariaLabel={t.stopsChartLabel(range, totals.stopsDelivered, totals.stopsFailed, totals.stopsSkipped)}
              summary={(index) => {
                const day = index === null ? null : days[index];
                return day
                  ? vi.reports.chartDay(formatDateLong(day.date), day.stopsDelivered, day.stopsFailed, day.stopsSkipped)
                  : vi.reports.chartSummary(totals.stopsDelivered, totals.stopsFailed, totals.stopsSkipped);
              }}
            />
          </section>
        </div>
      </section>
    </>
  );
}

function TechnicalBlock({ data }: { data: TechnicalMetrics }) {
  const t = vi.admin.overview;
  const { http, jobs, failuresLast24h: failures, jvm, database: db } = data;
  const percent = (rate: number | null) => formatRate(rate);
  return (
    <section className="pw-admin-block" aria-labelledby="tech-title">
      <div className="pw-admin-intro">
        <h2 id="tech-title" className="pw-section-title">
          {t.tech}
        </h2>
        <p className="pw-muted">
          {t.techLead(formatDayTime(data.startedAt, ZONE), formatDuration(data.uptimeSeconds))}
        </p>
      </div>
      <StatGroup label={t.tech}>
        <Stat
          label={t.rpm}
          value={http.requestsPerMinute.toLocaleString('vi-VN')}
          sub={t.rpmSub(http.requestsLastHour)}
        />
        <Stat
          label={t.errors}
          value={`${percent(http.clientErrorRate)} / ${percent(http.serverErrorRate)}`}
          sub={t.errorsSub}
        />
        <Stat label={t.jobs} value={t.jobsValue(jobs.unfinished)} sub={t.jobsSub(jobs.failedLast24h)} />
        <Stat label={t.rateLimited} value={data.rateLimitedLast24h} sub={t.rateLimitedSub} />
      </StatGroup>
      <div className="pw-admin-charts">
        <section className="pw-group" aria-labelledby="failures-title">
          <h3 id="failures-title" className="pw-group-title">
            {t.failures}
          </h3>
          <dl className="pw-admin-kv">
            <div>
              <dt>{t.email}</dt>
              <dd>{failures.email}</dd>
            </div>
            <div>
              <dt>{t.routing}</dt>
              <dd>{failures.routing}</dd>
            </div>
            <div>
              <dt>{t.geocoding}</dt>
              <dd>{failures.geocoding}</dd>
            </div>
            <div>
              <dt>{t.job}</dt>
              <dd>{failures.job}</dd>
            </div>
          </dl>
        </section>
        <section className="pw-group" aria-labelledby="server-title">
          <h3 id="server-title" className="pw-group-title">
            {t.server}
          </h3>
          <dl className="pw-admin-kv">
            <div>
              <dt>{t.heap}</dt>
              <dd>
                {formatBytes(jvm.heapUsedBytes)}
                {jvm.heapMaxBytes ? ` / ${formatBytes(jvm.heapMaxBytes)}` : ''}
              </dd>
            </div>
            <div>
              <dt>{t.connections}</dt>
              <dd>
                {t.connectionsValue(
                  String(db.activeConnections ?? '—'),
                  String(db.idleConnections ?? '—'),
                  String(db.maxConnections ?? '—'),
                )}
              </dd>
            </div>
            <div>
              <dt>{t.dbSize}</dt>
              <dd>{formatBytes(db.sizeBytes)}</dd>
            </div>
          </dl>
        </section>
      </div>
      <div className="pw-admin-charts">
        <section className="pw-admin-table" aria-labelledby="groups-title">
          <h3 id="groups-title" className="pw-section-title">
            {t.groups}
          </h3>
          <DataTable
            caption={t.groups}
            className="pw-list-group"
            rowKey="group"
            rows={http.groups}
            columns={[
              { key: 'group', header: t.group, className: 'pw-admin-name', render: (row) => row.group },
              { key: 'requests', header: t.requests, numeric: true, render: (row) => row.requestsLastHour },
              {
                key: 'p95',
                header: t.p95,
                numeric: true,
                className: 'pw-nowrap',
                render: (row) => (row.p95Millis == null ? '—' : `${Math.round(row.p95Millis)} ms`),
              },
            ]}
          />
        </section>
        <section className="pw-admin-table" aria-labelledby="tables-title">
          <h3 id="tables-title" className="pw-section-title">
            {t.tables}
          </h3>
          <DataTable
            caption={t.tables}
            className="pw-list-group"
            rowKey="name"
            rows={db.largestTables}
            columns={[
              {
                key: 'name',
                header: t.table,
                className: 'pw-admin-name',
                render: (row) => (
                  <>
                    {row.name}
                    {/* A narrow table: the size folds under the name (G2.11; checklist L3). */}
                    <div className="pw-muted pw-tabular pw-admin-fold">{formatBytes(row.bytes)}</div>
                  </>
                ),
              },
              {
                key: 'bytes',
                header: t.size,
                numeric: true,
                className: 'pw-nowrap pw-admin-col-fold',
                render: (row) => formatBytes(row.bytes),
              },
              { key: 'rows', header: t.rows, numeric: true, render: (row) => row.rows.toLocaleString('vi-VN') },
            ]}
          />
        </section>
      </div>
    </section>
  );
}
