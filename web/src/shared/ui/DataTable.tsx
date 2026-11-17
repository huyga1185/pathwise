import type { Key, ReactNode } from 'react';
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
}: DataTableProps<Row>) {
  const keyOf = (row: Row): Key => (typeof rowKey === 'function' ? rowKey(row) : (row[rowKey] as Key));
  const clickable = typeof onRowClick === 'function';
  return (
    <div className={cx('pw-table-wrap', className)}>
      <table className="pw-table">
        {caption ? <caption className="pw-visually-hidden">{caption}</caption> : null}
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cx(column.numeric && 'pw-num', column.align === 'end' && 'pw-end', column.className)}
                style={column.width ? { width: column.width } : undefined}
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
