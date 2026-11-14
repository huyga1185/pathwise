import { useRef, useState, type DragEvent } from 'react';
import { saveFile } from '@/platform/download';
import { fieldMessage, messageFor } from '@/shared/errors';
import { formatPhone } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, Dialog, ErrorBanner } from '@/shared/ui';
import { ApiError } from '@/data/apiError';
import { usePlanApi, type ImportedRow, type PlanImport } from './planApi';
import { DEMAND_UNITS, MAX_STOPS } from './planForm';

const t = vi.planImport;
const UNIT_SHORT: Record<string, string> = { KG: 'kg', BOX: 'thùng', PACKAGE: 'kiện' };

export interface ImportDialogProps {
  open: boolean;
  /** Stops the form keeps; with the valid rows they may not go past 100 (D-19). */
  keptStops: number;
  onClose: () => void;
  onAdd: (rows: ImportedRow[]) => void;
}

/**
 * Prototype "Kế hoạch — nhập điểm giao từ file" (W12, T61): choose or drop a file, the server reads it and saves
 * nothing, the rows are previewed with their errors, and only the rows without errors are added to the form.
 */
export function ImportDialog({ open, keptStops, onClose, onAdd }: ImportDialogProps) {
  const planApi = usePlanApi();
  const input = useRef<HTMLInputElement>(null);
  const [result, setResult] = useState<{ name: string; data: PlanImport } | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [dragging, setDragging] = useState(false);

  const close = () => {
    setResult(null);
    setFailure(null);
    onClose();
  };
  const read = async (file: File | undefined) => {
    if (!file) return;
    setBusy(true);
    setFailure(null);
    try {
      setResult({ name: file.name, data: await planApi.importStops(file) });
    } catch (error) {
      setFailure(error);
      setResult(null);
    } finally {
      setBusy(false);
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
  return (
    <Dialog
      open={open}
      size="full"
      title={t.title}
      confirmLabel={t.add(valid.length)}
      cancelLabel={t.back}
      confirmDisabled={!result || valid.length === 0 || tooMany}
      onConfirm={() => {
        onAdd(valid);
        close();
      }}
      onClose={close}
    >
      <div className="pw-import">
        {failure ? <ErrorBanner title={failureMessage(failure)} /> : null}
        {result ? (
          <Preview
            name={result.name}
            data={result.data}
            keptStops={keptStops}
            tooMany={tooMany}
            onAgain={() => setResult(null)}
          />
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
    </Dialog>
  );
}

function Preview({
  name,
  data,
  keptStops,
  tooMany,
  onAgain,
}: {
  name: string;
  data: PlanImport;
  keptStops: number;
  tooMany: boolean;
  onAgain: () => void;
}) {
  return (
    <>
      <div className="pw-import-summary">
        <span className="pw-import-file">{name}</span>
        <span>
          {t.summary(data.rows.length)} <strong className="pw-import-ok">{t.valid(data.validCount)}</strong>,{' '}
          <strong className="pw-import-bad">{t.bad(data.errorCount)}</strong>
        </span>
        <Button variant="quiet" onClick={onAgain}>
          {t.again}
        </Button>
      </div>
      {data.ignoredColumns.length > 0 || data.errorCount > 0 ? (
        <div className="pw-import-notice">
          {data.ignoredColumns.length > 0 ? <span>{t.ignored(data.ignoredColumns.join(', '))}</span> : null}
          {data.errorCount > 0 ? <span className="pw-muted">{t.badRowsHint}</span> : null}
        </div>
      ) : null}
      {tooMany ? <ErrorBanner title={t.tooMany(keptStops, data.validCount)} /> : null}
      {data.validCount === 0 ? <ErrorBanner title={t.nothingValid} /> : null}
      <DataTable<ImportedRow>
        caption={t.caption}
        rows={data.rows}
        rowKey={(row) => row.row}
        className="pw-import-table"
        columns={[
          { key: 'row', header: t.row, numeric: true, render: (row) => row.row },
          {
            key: 'result',
            header: t.result,
            render: (row) =>
              row.errors.length === 0 ? (
                <span className="pw-import-ok">{t.ok}</span>
              ) : (
                <span className="pw-import-bad">{t.error}</span>
              ),
          },
          { key: 'label', header: t.columnOf.label, render: (row) => row.stop.label ?? '' },
          { key: 'name', header: t.columnOf.recipientName, render: (row) => row.stop.recipientName ?? '' },
          {
            key: 'phone',
            header: t.columnOf.recipientPhone,
            render: (row) => <span className="pw-nowrap">{formatPhone(row.stop.recipientPhone)}</span>,
          },
          {
            key: 'address',
            header: t.columnOf.address,
            render: (row) =>
              row.stop.address ??
              (row.stop.lat != null && row.stop.lon != null ? `${row.stop.lat}, ${row.stop.lon}` : ''),
          },
          {
            key: 'window',
            header: t.window,
            render: (row) => (row.stop.timeWindow ? `${row.stop.timeWindow.start}–${row.stop.timeWindow.end}` : ''),
          },
          {
            key: 'demand',
            header: t.demand,
            render: (row) =>
              DEMAND_UNITS.filter((unit) => row.stop.demands[unit] !== undefined)
                .map((unit) => `${row.stop.demands[unit]} ${UNIT_SHORT[unit]}`)
                .join(', '),
          },
          {
            key: 'errors',
            header: t.errors,
            render: (row) =>
              row.errors.length > 0 ? (
                <span className="pw-import-err">
                  {row.errors
                    .map((error) => {
                      const field = error.field ?? '';
                      const column = t.columnOf[field] ?? field;
                      return `${column}: ${fieldMessage(error.code ?? '', column)}`;
                    })
                    .join(' ')}
                </span>
              ) : null,
          },
        ]}
      />
    </>
  );
}

/** A refused file says why on its `file` detail (UX_BRIEF §6.2); other errors use the code's sentence. */
function failureMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'INVALID_IMPORT_FILE') {
    const code = error.details.find((detail) => detail.field === 'file')?.code;
    if (code) return fieldMessage(code, '');
  }
  return messageFor(error);
}
