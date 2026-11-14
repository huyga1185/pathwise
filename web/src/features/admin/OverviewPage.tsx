import { useState } from 'react';
import { DailyBarChart } from '@/platform/charts';
import { messageFor } from '@/shared/errors';
import { formatDayTime, formatDuration, formatRate, shortDate, todayIn } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { ErrorBanner, DataTable, Stat } from '@/shared/ui';
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

/** Prototype "Quản trị — tổng quan" (W11, T60): cards, two charts by day, technical numbers of the instance. */
export function OverviewPage() {
  const [range, setRange] = useState<Range>(30);
  const to = todayIn(ZONE);
  const from = addDays(to, -(range - 1));
  const business = useBusinessMetrics(from, to);
  const technical = useTechnicalMetrics();
  const t = vi.admin.overview;
  return (
    <section className="pw-page-section" aria-labelledby="overview-title">
      <div className="pw-admin-head">
        <div className="pw-admin-intro">
          <h1 id="overview-title" className="pw-page-title">
            {t.title}
          </h1>
          {business.data ? <p className="pw-muted">{t.lead(formatDayTime(business.data.computedAt, ZONE))}</p> : null}
        </div>
        <div className="pw-segmented" role="group" aria-label={t.range}>
          {RANGES.map((n) => (
            <button
              key={n}
              type="button"
              className="pw-segmented-btn"
              aria-pressed={range === n}
              onClick={() => setRange(n)}
            >
              {t.days(n)}
            </button>
          ))}
        </div>
      </div>
      {business.isError ? <ErrorBanner title={messageFor(business.error)} /> : null}
      {business.data ? <BusinessBlock data={business.data} range={range} /> : null}
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
    published: day.plansPublished,
    delivered: day.stopsDelivered,
    failed: day.stopsFailed,
    skipped: day.stopsSkipped,
  }));
  return (
    <>
      <section className="pw-admin-block" aria-labelledby="now-title">
        <h2 id="now-title" className="pw-admin-h2">
          {t.now}
        </h2>
        <div className="pw-admin-grid">
          <Stat label={t.stores} value={now.stores} sub={t.locked(now.lockedStores)} />
          <Stat label={t.active7} value={now.activeStores7d} sub={t.activeHint} />
          <Stat label={t.active30} value={now.activeStores30d} sub={t.ofStores(now.stores)} />
          <Stat
            label={t.users}
            value={now.owners + now.dispatchers + now.drivers + now.admins}
            sub={t.usersByRole(now.owners, now.dispatchers, now.drivers, now.admins)}
          />
        </div>
      </section>
      <section className="pw-admin-block" aria-labelledby="range-title">
        <h2 id="range-title" className="pw-admin-h2">
          {t.rangeTitle(range, shortDate(data.from), shortDate(data.to))}
        </h2>
        <div className="pw-admin-grid">
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
        </div>
        <div className="pw-admin-charts">
          <div className="pw-admin-card">
            <h3 className="pw-admin-h3">{t.plansChart}</h3>
            <DailyBarChart
              data={points}
              series={[{ key: 'published', label: t.seriesPublished, token: 'route' }]}
              ariaLabel={t.plansChartLabel(range, totals.plansPublished)}
            />
          </div>
          <div className="pw-admin-card">
            <h3 className="pw-admin-h3">{t.stopsChart}</h3>
            <DailyBarChart
              data={points}
              stacked
              series={[
                { key: 'delivered', label: t.seriesDelivered, token: 'success' },
                { key: 'failed', label: t.seriesFailed, token: 'danger' },
                { key: 'skipped', label: t.seriesSkipped, token: 'signal' },
              ]}
              ariaLabel={t.stopsChartLabel(range, totals.stopsDelivered, totals.stopsFailed, totals.stopsSkipped)}
            />
          </div>
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
        <h2 id="tech-title" className="pw-admin-h2">
          {t.tech}
        </h2>
        <p className="pw-muted">
          {t.techLead(formatDayTime(data.startedAt, ZONE), formatDuration(data.uptimeSeconds))}
        </p>
      </div>
      <div className="pw-admin-grid">
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
      </div>
      <div className="pw-admin-charts">
        <div className="pw-admin-card">
          <h3 className="pw-admin-h3">{t.failures}</h3>
          <dl className="pw-admin-kv">
            <dt>{t.email}</dt>
            <dd>{failures.email}</dd>
            <dt>{t.routing}</dt>
            <dd>{failures.routing}</dd>
            <dt>{t.geocoding}</dt>
            <dd>{failures.geocoding}</dd>
            <dt>{t.job}</dt>
            <dd>{failures.job}</dd>
          </dl>
        </div>
        <div className="pw-admin-card">
          <h3 className="pw-admin-h3">{t.server}</h3>
          <dl className="pw-admin-kv">
            <dt>{t.heap}</dt>
            <dd>
              {formatBytes(jvm.heapUsedBytes)}
              {jvm.heapMaxBytes ? ` / ${formatBytes(jvm.heapMaxBytes)}` : ''}
            </dd>
            <dt>{t.connections}</dt>
            <dd>
              {t.connectionsValue(
                String(db.activeConnections ?? '—'),
                String(db.idleConnections ?? '—'),
                String(db.maxConnections ?? '—'),
              )}
            </dd>
            <dt>{t.dbSize}</dt>
            <dd>{formatBytes(db.sizeBytes)}</dd>
          </dl>
        </div>
      </div>
      <div className="pw-admin-charts">
        <DataTable
          caption={t.groups}
          rowKey="group"
          rows={http.groups}
          columns={[
            { key: 'group', header: t.group, render: (row) => row.group },
            { key: 'requests', header: t.requests, numeric: true, render: (row) => row.requestsLastHour },
            {
              key: 'p95',
              header: t.p95,
              numeric: true,
              render: (row) => (row.p95Millis == null ? '—' : `${Math.round(row.p95Millis)} ms`),
            },
          ]}
        />
        <DataTable
          caption={t.tables}
          rowKey="name"
          rows={db.largestTables}
          columns={[
            { key: 'name', header: t.table, render: (row) => row.name },
            { key: 'bytes', header: t.size, numeric: true, render: (row) => formatBytes(row.bytes) },
            { key: 'rows', header: t.rows, numeric: true, render: (row) => row.rows.toLocaleString('vi-VN') },
          ]}
        />
      </div>
    </section>
  );
}
