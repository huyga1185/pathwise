import { useRef, useState, type DragEvent } from 'react';
import { saveFile } from '@/platform/download';
import { fieldMessage, messageFor } from '@/shared/errors';
import { formatPhone } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { Button, DataTable, ErrorBanner, Sheet } from '@/shared/ui';
import { ApiError } from '@/data/apiError';
import { usePlanApi, type ImportedRow, type PlanImport } from './planApi';
import { DEMAND_UNITS, MAX_STOPS } from './planForm';

const t = vi.planImport;
const UNIT_SHORT: Record<string, string> = { KG: 'kg', BOX: 'thùng', PACKAGE: 'kiện' };

export interface ImportSheetProps {
  open: boolean;
  /** Stops the form keeps; with the valid rows they may not go past 100 (D-19). */
  keptStops: number;
  onClose: () => void;
  onAdd: (rows: ImportedRow[]) => void;
}

/**
 * "Nhập từ file" (W12, T61; OWNER_UI_REDESIGN G4.9): a sheet in two steps. First choose or drop a file ("Huỷ" only;
 * HIG Entering data: "let people provide data by dragging and dropping it"); the server reads it and saves nothing.
 * Then the rows are previewed with their errors: "Quay lại" for another file and "Thêm N điểm giao" adds only the rows
 * without errors (HIG Sheets: a Back button for a previous step, "Avoid showing all three buttons… together"). A wide
 * window gives the preview table a large sheet; a phone lists the rows.
 */
export function ImportSheet({ open, keptStops, onClose, onAdd }: ImportSheetProps) {
  const planApi = usePlanApi();
  const input = useRef<HTMLInputElement>(null);
  const [result, setResult] = useState<{ name: string; data: PlanImport } | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [dragging, setDragging] = useState(false);
  // Each read gets a number; a read that ends after the dialog closed or another file was chosen is dropped.
  const reading = useRef(0);

  const close = () => {
    reading.current += 1;
    setBusy(false);
    setResult(null);
    setFailure(null);
    onClose();
  };
  const read = async (file: File | undefined) => {
    if (!file) return;
    const id = ++reading.current;
    setBusy(true);
    setFailure(null);
    try {
      const data = await planApi.importStops(file);
      if (id === reading.current) setResult({ name: file.name, data });
    } catch (error) {
      if (id !== reading.current) return;
      setFailure(error);
      setResult(null);
    } finally {
      if (id === reading.current) setBusy(false);
      if (input.current) input.current.value = '';
    }
  };
  const download = async (kind: 'xlsx' | 'csv') => {
    try {
      const file = await planApi.template(kind);
      saveFile(file.blob, file.filename);
    } catch (error) {
      setFailure(error);
    }
  };
  const onDrop = (event: DragEvent) => {
    event.preventDefault();
    setDragging(false);
    void read(event.dataTransfer.files[0]);
  };

  const valid = result ? result.data.rows.filter((row) => row.errors.length === 0) : [];
  const tooMany = keptStops + valid.length > MAX_STOPS;
  const wide = useMediaQuery(WIDE_QUERY);
  return (
    <Sheet
      open={open}
      size="large"
      title={t.title}
      doneLabel={result ? t.add(valid.length) : undefined}
      doneDisabled={!result || valid.length === 0 || tooMany}
      onSubmit={(event) => {
        event.preventDefault();
        onAdd(valid);
        close();
      }}
      onBack={result ? () => setResult(null) : undefined}
      backLabel={t.back}
      onClose={close}
    >
      <div className="pw-import">
        {failure ? <ErrorBanner title={failureMessage(failure)} /> : null}
        {result ? (
          <Preview name={result.name} data={result.data} keptStops={keptStops} tooMany={tooMany} wide={wide} />
        ) : (
          <>
            <div
              className={dragging ? 'pw-import-drop pw-import-drop-over' : 'pw-import-drop'}
              onDragOver={(event) => {
                event.preventDefault();
                setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={onDrop}
            >
              <span className="pw-import-drop-title">{t.drop}</span>
              <span className="pw-muted">{t.dropHint}</span>
              <label className="pw-visually-hidden" htmlFor="import-file">
                {t.fileInput}
              </label>
              <input
                ref={input}
                id="import-file"
                type="file"
                accept=".xlsx,.csv,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                className="pw-visually-hidden"
                onChange={(event) => void read(event.target.files?.[0])}
              />
              <Button variant="primary" busy={busy} onClick={() => input.current?.click()}>
                {busy ? t.reading : t.choose}
              </Button>
            </div>
            <div className="pw-import-templates">
              <span className="pw-muted">{t.noTemplate}</span>
              <Button variant="quiet" onClick={() => void download('xlsx')}>
                {t.templateXlsx}
              </Button>
              <Button variant="quiet" onClick={() => void download('csv')}>
                {t.templateCsv}
              </Button>
            </div>
            <p className="pw-muted">{t.columns}</p>
          </>
        )}
      </div>
    </Sheet>
  );
}

function Preview({
  name,
  data,
  keptStops,
  tooMany,
  wide,
}: {
  name: string;
  data: PlanImport;
  keptStops: number;
  tooMany: boolean;
  wide: boolean;
}) {
  return (
    <>
      <div className="pw-import-summary">
        <span className="pw-import-file">{name}</span>
        <span>
          {t.summary(data.rows.length)} <strong className="pw-import-ok">{t.valid(data.validCount)}</strong>,{' '}
          <strong className="pw-import-bad">{t.bad(data.errorCount)}</strong>
        </span>
      </div>
      {data.ignoredColumns.length > 0 || data.errorCount > 0 ? (
        <div className="pw-import-notice">
          {data.ignoredColumns.length > 0 ? <span>{t.ignored(data.ignoredColumns.join(', '))}</span> : null}
          {data.errorCount > 0 ? <span className="pw-muted">{t.badRowsHint}</span> : null}
        </div>
      ) : null}
      {tooMany ? <ErrorBanner title={t.tooMany(keptStops, data.validCount)} /> : null}
      {data.validCount === 0 ? <ErrorBanner title={t.nothingValid} /> : null}
      {wide ? null : (
        <ul className="pw-list-group pw-list-rows pw-import-rows" aria-label={t.caption}>
          {data.rows.map((row) => (
            <li key={row.row} className={row.errors.length > 0 ? 'pw-list-row is-invalid' : 'pw-list-row'}>
              <span className="pw-list-row-main">
                <span className="pw-list-row-title">{row.stop.label || `${t.row} ${row.row}`}</span>
                <span className="pw-list-row-sub">
                  {[
                    `${t.row} ${row.row}`,
                    row.stop.recipientName,
                    row.stop.address,
                    row.stop.timeWindow ? `${row.stop.timeWindow.start}–${row.stop.timeWindow.end}` : '',
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
                {row.errors.length > 0 ? <span className="pw-import-err">{errorText(row)}</span> : null}
              </span>
              <span className={row.errors.length === 0 ? 'pw-import-ok' : 'pw-import-bad'}>
                {row.errors.length === 0 ? t.ok : t.error}
              </span>
            </li>
          ))}
        </ul>
      )}
      {wide ? (
        <DataTable<ImportedRow>
          caption={t.caption}
          rows={data.rows}
          rowKey={(row) => row.row}
          className="pw-import-table"
          columns={[
            // The file's line and whether it can be added, in one cell (the result under the line number).
            {
              key: 'row',
              header: t.rowAndResult,
              className: 'pw-nowrap',
              render: (row) => (
                <>
                  <div className="pw-tabular">{`${t.row} ${row.row}`}</div>
                  {row.errors.length === 0 ? (
                    <span className="pw-import-ok">{t.ok}</span>
                  ) : (
                    <span className="pw-import-bad">{t.error}</span>
                  )}
                </>
              ),
            },
            { key: 'label', header: t.columnOf.label, render: (row) => row.stop.label ?? '' },
            // The recipient and their phone in one cell, the phone under the name: the preview fits the sheet
            // (HIG Lists and tables: "Choose a row style that fits the information you need to display").
            {
              key: 'name',
              header: t.columnOf.recipientName,
              render: (row) => (
                <>
                  <div>{row.stop.recipientName ?? ''}</div>
                  {row.stop.recipientPhone ? (
                    <div className="pw-muted pw-nowrap">{formatPhone(row.stop.recipientPhone)}</div>
                  ) : null}
                </>
              ),
            },
            {
              key: 'address',
              header: t.columnOf.address,
              className: 'pw-import-col-address',
              render: (row) =>
                row.stop.address ??
                (row.stop.lat != null && row.stop.lon != null ? `${row.stop.lat}, ${row.stop.lon}` : ''),
            },
            // The time window and the amounts in one cell (amounts under the window), as the recipient and phone.
            {
              key: 'window',
              header: t.windowAndDemand,
              className: 'pw-nowrap',
              render: (row) => {
                const demand = DEMAND_UNITS.filter((unit) => row.stop.demands[unit] !== undefined)
                  .map((unit) => `${row.stop.demands[unit]} ${UNIT_SHORT[unit]}`)
                  .join(', ');
                return (
                  <>
                    <div>{row.stop.timeWindow ? `${row.stop.timeWindow.start}–${row.stop.timeWindow.end}` : ''}</div>
                    {demand ? <div className="pw-muted">{demand}</div> : null}
                  </>
                );
              },
            },
            {
              key: 'errors',
              header: t.errors,
              className: 'pw-import-col-errors',
              render: (row) => (row.errors.length > 0 ? <span className="pw-import-err">{errorText(row)}</span> : null),
            },
          ]}
        />
      ) : null}
    </>
  );
}

/** A row's errors as "Column: sentence" (the template's column names). */
function errorText(row: ImportedRow): string {
  return row.errors
    .map((error) => {
      const field = error.field ?? '';
      const column = t.columnOf[field] ?? field;
      return `${column}: ${fieldMessage(error.code ?? '', column)}`;
    })
    .join(' ');
}

/** A refused file says why on its `file` detail (UX_BRIEF §6.2); other errors use the code's sentence. */
function failureMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'INVALID_IMPORT_FILE') {
    const code = error.details.find((detail) => detail.field === 'file')?.code;
    if (code) return fieldMessage(code, '');
  }
  return messageFor(error);
}
