import { useId } from 'react';
import { SortableBoard, type DragHandleProps } from '@/platform/dnd';
import { vi } from '@/shared/i18n/vi';
import { cx } from '@/shared/ui/cx';
import { draftLoad, moveStop, routeColumn, UNASSIGNED, type Draft } from './manualEdit';
import type { PlanDetail, PlanStop } from './planApi';
import { driverName, RouteBadge, stopPlace, stopTitle, vehicleText, windowText } from './planShared';

const m = vi.manual;

/**
 * The routes as drag-and-drop columns while editing by hand (W10, P-58): a grab handle and a "Chuyển sang…" menu on
 * each stop, the load of each route recomputed from the draft (over capacity in danger), and a column of stops on no
 * route. Times and distances are the server's, after saving.
 */
export function ManualBoard({
  plan,
  draft,
  onChange,
}: {
  plan: PlanDetail;
  draft: Draft;
  onChange: (draft: Draft) => void;
}) {
  const stops = new Map<string, PlanStop>();
  for (const stop of [...plan.routes.flatMap((route) => route.stops), ...plan.unassignedStops])
    stops.set(String(stop.id), stop);
  const routes = new Map(plan.routes.map((route, index) => [routeColumn(route.id), { route, index }]));
  const columnLabel = (column: string) => {
    const entry = routes.get(column);
    return entry ? driverName(entry.route) : m.unassignedColumn;
  };
  const itemLabel = (id: string) => {
    const stop = stops.get(id);
    return stop ? stopTitle(stop) : id;
  };
  const columns = Object.keys(draft);

  return (
    <SortableBoard
      items={draft}
      onChange={onChange}
      columnLabel={columnLabel}
      itemLabel={itemLabel}
      texts={m.dnd}
      renderColumn={(column, list) => {
        const ids = draft[column] ?? [];
        const entry = routes.get(column);
        const columnStops = ids.flatMap((id) => (stops.has(id) ? [stops.get(id) as PlanStop] : []));
        const load = entry ? draftLoad(entry.route, columnStops) : 0;
        const over = entry ? load > entry.route.vehicle.capacity : false;
        const unit = entry
          ? (vi.units.short[entry.route.vehicle.capacityUnit] ?? entry.route.vehicle.capacityUnit)
          : '';
        return (
          <section
            className={cx('pw-route-block', 'pw-manual-column', over && 'pw-manual-over')}
            aria-label={columnLabel(column)}
          >
            <div className="pw-manual-column-head">
              {entry ? <RouteBadge index={entry.index} /> : null}
              <span className="pw-route-driver">{columnLabel(column)}</span>
              <span className="pw-muted">{entry ? vehicleText(entry.route) : m.unassignedLead}</span>
              <span className="pw-grow" />
              <span className={cx('pw-tabular', over ? 'pw-manual-load-over' : 'pw-muted')}>
                {entry
                  ? m.columnLoad(
                      ids.length,
                      `${load.toLocaleString('vi-VN')}/${entry.route.vehicle.capacity.toLocaleString('vi-VN')} ${unit}`,
                    )
                  : m.stopsCount(ids.length)}
              </span>
            </div>
            {list}
            {ids.length === 0 ? (
              <p className="pw-manual-empty pw-muted">{column === UNASSIGNED ? m.emptyUnassigned : m.emptyRoute}</p>
            ) : null}
          </section>
        );
      }}
      renderItem={(id, column, handle, dragging) => {
        const stop = stops.get(id);
        if (!stop) return null;
        const position = (draft[column] ?? []).indexOf(id) + 1;
        return (
          <StopRow
            stop={stop}
            position={column === UNASSIGNED ? null : position}
            handle={handle}
            dragging={dragging}
            handleLabel={m.handle(stopTitle(stop), columnLabel(column), position)}
            targets={columns
              .filter((other) => other !== column)
              .map((other) => ({ id: other, label: other === UNASSIGNED ? m.takeOff : columnLabel(other) }))}
            onMove={(to) => onChange(moveStop(draft, id, to))}
          />
        );
      }}
    />
  );
}

function StopRow({
  stop,
  position,
  handle,
  dragging,
  handleLabel,
  targets,
  onMove,
}: {
  stop: PlanStop;
  position: number | null;
  handle: DragHandleProps;
  dragging: boolean;
  handleLabel: string;
  targets: { id: string; label: string }[];
  onMove: (to: string) => void;
}) {
  const selectId = useId();
  const window = windowText(stop);
  const demand = Object.entries(stop.demands as Record<string, number>)
    .map(([unit, quantity]) => `${quantity.toLocaleString('vi-VN')} ${vi.units.short[unit] ?? unit}`)
    .join(', ');
  return (
    <div className={cx('pw-manual-row', dragging && 'pw-manual-row-lifted')}>
      <button type="button" className="pw-drag-handle" aria-label={handleLabel} {...handle}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="9" cy="6" r="1.6" />
          <circle cx="15" cy="6" r="1.6" />
          <circle cx="9" cy="12" r="1.6" />
          <circle cx="15" cy="12" r="1.6" />
          <circle cx="9" cy="18" r="1.6" />
          <circle cx="15" cy="18" r="1.6" />
        </svg>
      </button>
      <span className="pw-manual-seq pw-tabular">{position ?? '–'}</span>
      <div className="pw-manual-stop">
        <span className="pw-manual-title">{stopTitle(stop)}</span>
        <span className="pw-muted pw-manual-detail">
          {[stopPlace(stop), demand, window ? `khung ${window}` : ''].filter(Boolean).join(' · ')}
        </span>
      </div>
      <label className="pw-visually-hidden" htmlFor={selectId}>
        {vi.manual.moveLabel(stopTitle(stop))}
      </label>
      <select
        id={selectId}
        className="pw-manual-move"
        value=""
        onChange={(event) => {
          if (event.target.value) onMove(event.target.value);
        }}
      >
        <option value="">{vi.manual.movePlaceholder}</option>
        {targets.map((target) => (
          <option key={target.id} value={target.id}>
            {target.label}
          </option>
        ))}
      </select>
    </div>
  );
}
