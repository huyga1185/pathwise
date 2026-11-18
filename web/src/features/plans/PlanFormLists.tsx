import type { ReactNode } from 'react';
import { Controller, type Control, type FieldErrors, type UseFormRegister } from 'react-hook-form';
import { formatCapacity } from '@/features/fleet';
import { vi } from '@/shared/i18n/vi';
import { Button, ChevronRightIcon, Sheet, TextField, TimeField } from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
import { useSelectMenu } from '@/shared/ui/useSelectMenu';
import type { ActiveVehicle, AssignableMember } from './planApi';
import type { AssignmentRow, DemandUnit, PlanFormValues, StopRow } from './planForm';

const f = vi.planForm;
export const UNIT_TITLE: Record<DemandUnit, string> = { KG: 'Kg', BOX: 'Thùng', PACKAGE: 'Kiện' };
type Member = Pick<AssignableMember, 'id' | 'email'> & { fullName?: string | null };

/**
 * Phone (OWNER_UI_REDESIGN G4.8, owner's answer Q4): the assignments and the stops as rows — the main line, a secondary
 * line, a mark when the row has a field to fix, a chevron — each opening a sheet with all its fields (HIG Lists and
 * tables; Sheets; as Xe and Thành viên in G3).
 */
export function FormRows({
  label,
  rows,
  onOpen,
  rowClass,
}: {
  label: string;
  rows: Array<{ key: string; title: string; sub: string; invalid: boolean; name: string }>;
  onOpen: (index: number) => void;
  /** Arriving / leaving (useRowMotion). */
  rowClass?: (key: string) => string | undefined;
}) {
  if (rows.length === 0) return null;
  return (
    <ul className="pw-list-group pw-list-rows pw-form-rows" aria-label={label}>
      {rows.map((row, index) => (
        <li key={row.key} className={cx('pw-list-row', row.invalid && 'is-invalid', rowClass?.(row.key))}>
          <button type="button" className="pw-list-row-main" aria-label={row.name} onClick={() => onOpen(index)}>
            <span className="pw-list-row-title">{row.title}</span>
            {row.sub ? <span className="pw-list-row-sub">{row.sub}</span> : null}
            {row.invalid ? <span className="pw-form-row-fix">{f.rowToFix}</span> : null}
          </button>
          <span className="pw-list-row-chevron" aria-hidden="true">
            <ChevronRightIcon />
          </span>
        </li>
      ))}
    </ul>
  );
}

export function assignmentRows(
  values: AssignmentRow[],
  errors: FieldErrors<PlanFormValues>['assignments'],
  members: Member[],
  vehicles: ActiveVehicle[],
) {
  return values.map((row, i) => {
    const driver = members.find((member) => String(member.id) === row.driverId);
    const vehicle = vehicles.find((item) => String(item.id) === row.vehicleId);
    const title = [driver ? (driver.fullName ?? driver.email) : f.chooseDriver, vehicle?.name ?? f.chooseVehicle].join(
      ' · ',
    );
    const shift =
      row.shiftStart || row.shiftEnd ? `${f.shift} ${row.shiftStart || '--:--'}–${row.shiftEnd || '--:--'}` : '';
    const distance = row.maxDistanceKm.trim() ? `${f.upTo} ${row.maxDistanceKm.trim()} km` : '';
    return {
      key: `a${i}`,
      title,
      sub: [shift, distance].filter(Boolean).join(' · '),
      invalid: Boolean(errors?.[i]),
      name: f.openAssignment(i + 1, title),
    };
  });
}

export function stopRows(values: StopRow[], errors: FieldErrors<PlanFormValues>['stops']) {
  return values.map((row, i) => {
    const title = row.label.trim() || f.stopN(i + 1);
    const window =
      row.timeWindow.start || row.timeWindow.end
        ? `${row.timeWindow.start || '--:--'}–${row.timeWindow.end || '--:--'}`
        : '';
    return {
      key: `s${i}`,
      title,
      sub: [row.recipientName.trim(), row.address.trim(), window].filter(Boolean).join(' · '),
      invalid: Boolean(errors?.[i]),
      name: f.openStop(i + 1, title),
    };
  });
}

/** The fields of one assignment, in a sheet (G4.8): the same form paths as the table's cells. */
export function AssignmentSheet({
  index,
  isNew,
  control,
  register,
  errors,
  members,
  vehicles,
  dirty,
  onDone,
  onCancel,
  onRemove,
}: {
  index: number | null;
  isNew: boolean;
  control: Control<PlanFormValues>;
  register: UseFormRegister<PlanFormValues>;
  errors: FieldErrors<PlanFormValues>;
  members: Member[];
  vehicles: ActiveVehicle[];
  dirty: boolean;
  onDone: () => void;
  onCancel: () => void;
  onRemove: () => void;
}) {
  const at = index === null ? undefined : errors.assignments?.[index];
  return (
    <Sheet
      open={index !== null}
      title={f.assignment}
      doneLabel={f.done}
      dirty={dirty}
      onSubmit={onDone}
      onClose={onCancel}
    >
      {index !== null ? (
        <>
          <SheetSelect
            register={register}
            name={`assignments.${index}.driverId`}
            label={f.driver}
            placeholder={f.chooseDriver}
            error={at?.driverId?.message}
            options={members.map((member) => ({ value: String(member.id), label: member.fullName ?? member.email }))}
          />
          <SheetSelect
            register={register}
            name={`assignments.${index}.vehicleId`}
            label={f.vehicle}
            placeholder={f.chooseVehicle}
            error={at?.vehicleId?.message}
            options={vehicles.map((vehicle) => ({
              value: String(vehicle.id),
              label: `${vehicle.name} · ${formatCapacity(vehicle.capacity, vehicle.capacityUnit)}`,
            }))}
          />
          <div className="pw-sheet-pair">
            <TimeRow
              control={control}
              name={`assignments.${index}.shiftStart`}
              label={f.shiftStart}
              error={at?.shiftStart?.message}
            />
            <TimeRow
              control={control}
              name={`assignments.${index}.shiftEnd`}
              label={f.shiftEnd}
              error={at?.shiftEnd?.message}
            />
          </div>
          <TextField
            label={f.maxDistance}
            inputMode="decimal"
            placeholder={f.unlimited}
            error={at?.maxDistanceKm?.message}
            {...register(`assignments.${index}.maxDistanceKm`)}
          />
          {!isNew ? <RemoveButton label={f.removeThisAssignment} onRemove={onRemove} /> : null}
        </>
      ) : null}
    </Sheet>
  );
}

/** The fields of one stop, in a sheet (G4.8). */
export function StopSheet({
  index,
  isNew,
  control,
  register,
  errors,
  units,
  dirty,
  onDone,
  onCancel,
  onRemove,
}: {
  index: number | null;
  isNew: boolean;
  control: Control<PlanFormValues>;
  register: UseFormRegister<PlanFormValues>;
  errors: FieldErrors<PlanFormValues>;
  units: DemandUnit[];
  dirty: boolean;
  onDone: () => void;
  onCancel: () => void;
  onRemove: () => void;
}) {
  const at = index === null ? undefined : errors.stops?.[index];
  return (
    <Sheet open={index !== null} title={f.stop} doneLabel={f.done} dirty={dirty} onSubmit={onDone} onClose={onCancel}>
      {index !== null ? (
        <>
          <TextField
            label={f.label}
            required
            maxLength={150}
            error={at?.label?.message}
            {...register(`stops.${index}.label`)}
          />
          <TextField
            label={f.recipient}
            maxLength={100}
            error={at?.recipientName?.message}
            {...register(`stops.${index}.recipientName`)}
          />
          <TextField
            label={f.phone}
            type="tel"
            autoComplete="off"
            error={at?.recipientPhone?.message}
            {...register(`stops.${index}.recipientPhone`)}
          />
          <TextField
            label={f.address}
            maxLength={500}
            hint={f.addressTip}
            error={at?.address?.message}
            {...register(`stops.${index}.address`)}
          />
          <fieldset className="pw-sheet-fieldset">
            <legend className="pw-field-label">{f.window}</legend>
            <div className="pw-sheet-pair">
              <TimeRow
                control={control}
                name={`stops.${index}.timeWindow.start`}
                label={f.from}
                error={at?.timeWindow?.start?.message}
              />
              <TimeRow
                control={control}
                name={`stops.${index}.timeWindow.end`}
                label={f.to}
                error={at?.timeWindow?.end?.message ?? at?.timeWindow?.message}
              />
            </div>
          </fieldset>
          {units.length > 0 ? (
            <div className="pw-sheet-pair">
              {units.map((unit) => (
                <TextField
                  key={unit}
                  label={UNIT_TITLE[unit]}
                  inputMode="numeric"
                  error={at?.demands?.[unit]?.message}
                  {...register(`stops.${index}.demands.${unit}`)}
                />
              ))}
            </div>
          ) : null}
          <TextField label={f.note} maxLength={500} error={at?.note?.message} {...register(`stops.${index}.note`)} />
          {!isNew ? <RemoveButton label={f.removeThisStop} onRemove={onRemove} /> : null}
        </>
      ) : null}
    </Sheet>
  );
}

function TimeRow({
  control,
  name,
  label,
  error,
}: {
  control: Control<PlanFormValues>;
  name:
    | `assignments.${number}.shiftStart`
    | `assignments.${number}.shiftEnd`
    | `stops.${number}.timeWindow.start`
    | `stops.${number}.timeWindow.end`;
  label: string;
  error?: string;
}) {
  const id = `${name}-field`;
  return (
    <div className={cx('pw-field', error && 'pw-field-error')}>
      <label className="pw-field-label" htmlFor={id}>
        {label}
      </label>
      <Controller
        control={control}
        name={name}
        render={({ field }) => <TimeField {...field} id={id} aria-label={label} invalid={Boolean(error)} />}
      />
      {error ? <p className="pw-field-msg pw-field-msg-error">{error}</p> : null}
    </div>
  );
}

function SheetSelect({
  register,
  name,
  label,
  placeholder,
  error,
  options,
}: {
  register: UseFormRegister<PlanFormValues>;
  name: `assignments.${number}.driverId` | `assignments.${number}.vehicleId`;
  label: string;
  placeholder: string;
  error?: string;
  options: Array<{ value: string; label: string }>;
}): ReactNode {
  const { ref, ...field } = register(name);
  const { selectProps, menu } = useSelectMenu(label, ref);
  const id = `${name}-field`;
  return (
    <div className={cx('pw-field', error && 'pw-field-error')}>
      <label className="pw-field-label" htmlFor={id}>
        {label}
        <span aria-hidden="true" className="pw-field-req">
          {' *'}
        </span>
      </label>
      <select id={id} className="pw-field-input" aria-invalid={error ? 'true' : undefined} {...field} {...selectProps}>
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {menu}
      {error ? <p className="pw-field-msg pw-field-msg-error">{error}</p> : null}
    </div>
  );
}

/** A destructive action at the end of a sheet, as the member sheet's "Vô hiệu hoá" (G3). */
function RemoveButton({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <Button variant="secondary" block className="pw-sheet-destructive" onClick={onRemove}>
      {label}
    </Button>
  );
}
