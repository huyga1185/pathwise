import { useRef, useState, type KeyboardEvent, type Key, type PointerEvent, type ReactNode } from 'react';
import { cx } from './cx';
import { ChevronDownIcon, ChevronUpIcon } from './icons';

export interface DataTableColumn<Row> {
  key: string;
  header: ReactNode;
  render?: (row: Row) => ReactNode;
  /** Right-aligned tabular numbers. */
  numeric?: boolean;
  align?: 'start' | 'end';
  width?: string | number;
  /** Extra class on the column's heading and cells (e.g. hidden when the table is narrow). */
  className?: string;
  /** The heading sorts the table when clicked (HIG Lists and tables: "let people click a column heading to sort"). */
  sortable?: boolean;
}

export interface TableSort {
  key: string;
  direction: 'asc' | 'desc';
}

export interface DataTableProps<Row> {
  columns: DataTableColumn<Row>[];
  rows: Row[];
  /** Property name or function; default "id". */
  rowKey?: keyof Row | ((row: Row) => Key);
  selectedKey?: Key | null;
  onRowClick?: (row: Row) => void;
  empty?: ReactNode;
  caption?: string;
  className?: string;
  /** Current order; clicking a sortable heading calls onSort with its key. */
  sort?: TableSort;
  onSort?: (key: string) => void;
  /** Let people drag the column edges (HIG Lists and tables, macOS: "Let people resize columns"). */
  resizable?: boolean;
  /** Right click (or touch and hold) on a row: the caller opens that row's context menu. */
  onRowContextMenu?: (row: Row, point: { x: number; y: number }) => void;
}

/** Design system DataTable: selectable rows by mouse and keyboard, sticky header, numeric columns. */
export function DataTable<Row>({
  columns,
  rows,
  rowKey = 'id' as keyof Row,
  selectedKey,
  onRowClick,
  empty,
  caption,
  className,
  sort,
  onSort,
  onRowContextMenu,
  resizable = false,
}: DataTableProps<Row>) {
  const keyOf = (row: Row): Key => (typeof rowKey === 'function' ? rowKey(row) : (row[rowKey] as Key));
  const clickable = typeof onRowClick === 'function';
  const table = useRef<HTMLTableElement>(null);
  // Column widths people set by dragging; null: the browser lays the table out. The first drag freezes every column
  // at its current width (a fixed layout), so only the dragged column changes.
  const [widths, setWidths] = useState<Record<string, number> | null>(null);
  const drag = useRef<{ key: string; x: number; width: number } | null>(null);
  const freeze = (): Record<string, number> => {
    if (widths) return widths;
    const measured: Record<string, number> = {};
    // Every column but the last: the last one takes what is left, so the table always fills its group.
    table.current?.querySelectorAll<HTMLTableCellElement>('thead th').forEach((th, index) => {
      const column = columns[index];
      if (column && index < columns.length - 1) measured[column.key] = th.getBoundingClientRect().width;
    });
    return measured;
  };
  const setWidth = (key: string, width: number, base = freeze()) =>
    setWidths({ ...base, [key]: Math.max(64, Math.round(width)) });
  const handle = (key: string, label: ReactNode) => (
    <span
      className="pw-th-resize"
      role="separator"
      aria-orientation="vertical"
      aria-label={typeof label === 'string' ? `Độ rộng cột ${label}` : 'Độ rộng cột'}
      aria-valuenow={widths?.[key] ? Math.round(widths[key]) : undefined}
      tabIndex={0}
      onClick={(event) => event.stopPropagation()}
      onDoubleClick={() => setWidths(null)}
      onPointerDown={(event: PointerEvent<HTMLSpanElement>) => {
        event.preventDefault();
        event.stopPropagation();
        const base = freeze();
        const width = base[key] ?? 120;
        setWidths(base);
        drag.current = { key, x: event.clientX, width };
        event.currentTarget.setPointerCapture(event.pointerId);
      }}
      onPointerMove={(event: PointerEvent<HTMLSpanElement>) => {
        const current = drag.current;
        if (current) setWidth(current.key, current.width + event.clientX - current.x);
      }}
      onPointerUp={() => {
        drag.current = null;
      }}
      onPointerCancel={() => {
        drag.current = null;
      }}
      onKeyDown={(event: KeyboardEvent<HTMLSpanElement>) => {
        const step = event.key === 'ArrowRight' ? 16 : event.key === 'ArrowLeft' ? -16 : 0;
        if (!step) return;
        event.preventDefault();
        const base = freeze();
        setWidth(key, (base[key] ?? 120) + step, base);
      }}
    />
  );
  return (
    <div className={cx('pw-table-wrap', className)}>
      <table
        ref={table}
        className={cx('pw-table', widths && 'is-resized')}
        style={widths ? { minWidth: Object.values(widths).reduce((sum, width) => sum + width, 0) + 72 } : undefined}
      >
        {caption ? <caption className="pw-visually-hidden">{caption}</caption> : null}
        <thead>
          <tr>
            {columns.map((column, index) => (
              <th
                key={column.key}
                scope="col"
                className={cx(column.numeric && 'pw-num', column.align === 'end' && 'pw-end', column.className)}
                style={
                  widths?.[column.key]
                    ? { width: widths[column.key] }
                    : column.width
                      ? { width: column.width }
                      : undefined
                }
                aria-sort={
                  column.sortable && onSort
                    ? sort?.key === column.key
                      ? sort.direction === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : 'none'
                    : undefined
                }
              >
                {column.sortable && onSort ? (
                  <button type="button" className="pw-th-sort" onClick={() => onSort(column.key)}>
                    {column.header}
                    <span className={cx('pw-th-sort-mark', sort?.key === column.key && 'is-on')} aria-hidden="true">
                      {sort?.key === column.key && sort.direction === 'asc' ? <ChevronUpIcon /> : <ChevronDownIcon />}
                    </span>
                  </button>
                ) : (
                  column.header
                )}
                {resizable && index < columns.length - 1 ? handle(column.key, column.header) : null}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td className="pw-table-empty" colSpan={columns.length}>
                {empty ?? 'Chưa có dữ liệu.'}
              </td>
            </tr>
          ) : (
            rows.map((row) => {
              const key = keyOf(row);
              const selected = selectedKey !== undefined && selectedKey !== null && key === selectedKey;
              return (
                <tr
                  key={key}
                  className={cx(selected && 'pw-row-selected', clickable && 'pw-row-clickable')}
                  aria-selected={clickable ? selected : undefined}
                  tabIndex={clickable ? 0 : undefined}
                  onClick={clickable ? () => onRowClick(row) : undefined}
                  onContextMenu={
                    onRowContextMenu
                      ? (event) => {
                          event.preventDefault();
                          onRowContextMenu(row, { x: event.clientX, y: event.clientY });
                        }
                      : undefined
                  }
                  onKeyDown={
                    clickable
                      ? (event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            onRowClick(row);
                          }
                        }
                      : undefined
                  }
                >
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      className={cx(column.numeric && 'pw-num', column.align === 'end' && 'pw-end', column.className)}
                    >
                      {column.render ? column.render(row) : (row[column.key as keyof Row] as ReactNode)}
                    </td>
                  ))}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
