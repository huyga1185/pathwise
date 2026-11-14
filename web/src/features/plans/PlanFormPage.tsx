import { useStoreReadOnly } from '@/data/meQuery';
import { useMemo, useState, type ReactNode } from 'react';
import { useFieldArray, useForm, useWatch, type FieldPath, type UseFormRegister } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { useStoreQuery } from '@/data/storeQuery';
import { formatCapacity } from '@/features/fleet';
import { errorMessages, fieldErrors, messageFor } from '@/shared/errors';
import { todayIn } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, Dialog, ErrorBanner, TextField, TrashIcon } from '@/shared/ui';
import {
  useActiveVehicles,
  useAssignableMembers,
  usePlan,
  usePlanApi,
  type ActiveVehicle,
  type AssignableMember,
  type PlanDetail,
} from './planApi';
import {
  demandUnits,
  emptyAssignment,
  emptyStop,
  formPath,
  importedToRow,
  isBlankStop,
  MAX_ASSIGNMENTS,
  MAX_STOPS,
  newPlanValues,
  planToValues,
  validatePlan,
  valuesToRequest,
  type DemandUnit,
  type PlanFormValues,
} from './planForm';
import { ImportDialog } from './ImportDialog';
import './plans.css';

const f = vi.planForm;
const LABELS: Record<string, string> = {
  name: f.name,
  planDate: f.date,
  driverId: f.driver,
  vehicleId: f.vehicle,
  shiftStart: f.shiftStart,
  shiftEnd: f.shiftEnd,
  maxDistanceKm: f.maxDistance,
  maxDistanceMeters: f.maxDistance,
  label: f.label,
  recipientName: f.recipient,
  recipientPhone: f.phone,
  address: f.address,
  windowFrom: f.windowFrom,
  windowTo: f.windowTo,
  start: f.windowFrom,
  end: f.windowTo,
  note: f.note,
};
const UNIT_TITLE: Record<DemandUnit, string> = { KG: 'Kg', BOX: 'Thùng', PACKAGE: 'Kiện' };

/** `/plans/new` and `/plans/:id/edit` (prototype "Tạo kế hoạch"): one form, a PUT keeps the stops' ids. */
export function PlanFormPage() {
  const { id } = useParams();
  const planId = id ? Number(id) : null;
  const store = useStoreQuery();
  const members = useAssignableMembers();
  const vehicles = useActiveVehicles();
  const failure = store.error ?? members.error ?? vehicles.error;
  if (failure) return <ErrorBanner title={messageFor(failure)} />;
  // The selects get their values when they mount: wait for their options.
  if (!store.data || !members.data || !vehicles.data) return <p className="pw-muted">{vi.pages.loading}</p>;
  return planId ? <EditPlan id={planId} /> : <PlanForm plan={null} />;
}

function EditPlan({ id }: { id: number }) {
  const plan = usePlan(id);
  if (plan.isError) return <ErrorBanner title={messageFor(plan.error)} />;
  if (!plan.data) return <p className="pw-muted">{vi.pages.loading}</p>;
  if (plan.data.status !== 'DRAFT') {
    return (
      <section className="pw-page-section">
        <ErrorBanner title={f.notEditable} />
        <Link className={buttonClass('secondary')} to={`/plans/${id}`}>
          {f.toPlan}
        </Link>
      </section>
    );
  }
  return <PlanForm key={`${plan.data.id}`} plan={plan.data} />;
}

function PlanForm({ plan }: { plan: PlanDetail | null }) {
  const navigate = useNavigate();
  const { readOnly } = useStoreReadOnly();
  const planApi = usePlanApi();
  const store = useStoreQuery();
  const members = useAssignableMembers();
  const vehicles = useActiveVehicles();
  const [failure, setFailure] = useState<unknown>(null);
  const [modified, setModified] = useState(false);
  const [importing, setImporting] = useState(false);
  const [imported, setImported] = useState<number | null>(null);
  const live = usePlan(plan?.id ?? 0, plan !== null);
  const timeZone = store.data?.timezone ?? 'Asia/Ho_Chi_Minh';
  const {
    control,
    register,
    handleSubmit,
    setError,
    clearErrors,
    reset,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<PlanFormValues>({
    defaultValues: plan ? planToValues(plan) : newPlanValues(todayIn(timeZone)),
  });
  const assignments = useFieldArray({ control, name: 'assignments' });
  const stops = useFieldArray({ control, name: 'stops' });
  const watchedAssignments = useWatch({ control, name: 'assignments' });
  const watchedStops = useWatch({ control, name: 'stops' });

  const vehicleById = useMemo(
    () => new Map((vehicles.data ?? []).map((vehicle) => [String(vehicle.id), vehicle])),
    [vehicles.data],
  );
  const units = demandUnits(
    (watchedAssignments ?? []).flatMap((row) => {
      const vehicle = vehicleById.get(row.vehicleId);
      return vehicle ? [vehicle.capacityUnit] : [];
    }),
    watchedStops ?? [],
  );
  const depot = plan ? plan.depot : store.data?.depot;
  const noDepot = !plan && store.data !== undefined && store.data.depot === null;
  const busy = plan !== null && live.data !== undefined && live.data.processingStatus !== 'IDLE';

  const onSave = handleSubmit(async (values) => {
    setFailure(null);
    clearErrors();
    const instant = validatePlan(values, LABELS);
    if (Object.keys(instant).length > 0) {
      Object.entries(instant).forEach(([path, message], index) =>
        setError(path as FieldPath<PlanFormValues>, { type: 'client', message }, { shouldFocus: index === 0 }),
      );
      return;
    }
    try {
      const body = valuesToRequest(values);
      const saved = plan
        ? await planApi.replace(plan.id, { ...body, version: (live.data ?? plan).version })
        : await planApi.create(body);
      navigate(`/plans/${saved.id}`);
    } catch (error) {
      if (!(error instanceof ApiError)) {
        setFailure(error);
        return;
      }
      if (error.code === 'PLAN_MODIFIED') {
        setModified(true);
        return;
      }
      const fields = fieldErrors(error, LABELS);
      let placed = 0;
      Object.entries(fields).forEach(([field, message]) => {
        if (!field) return;
        // Server paths are the request's; the form keeps kilometres under its own name.
        const path = formPath(field)
          .replace(/\.maxDistanceMeters$/, '.maxDistanceKm')
          .replace(/\.(lat|lon)$/, '.address');
        setError(path as FieldPath<PlanFormValues>, { type: 'server', message }, { shouldFocus: placed === 0 });
        placed += 1;
      });
      if (placed === 0 || error.code !== 'VALIDATION_ERROR') setFailure(error);
    }
  });

  const errorCount = countErrors(errors);
  // A driver disabled or a vehicle turned off since the draft was saved stays selectable in that draft.
  const memberOptions = withCurrent(
    members.data ?? [],
    (plan?.routes ?? []).map((route) => ({ id: route.driver.id, fullName: route.driver.fullName, email: '' })),
  );
  const vehicleOptions = withCurrent(
    vehicles.data ?? [],
    (plan?.routes ?? []).map((route) => ({ ...route.vehicle, plateNumber: null, active: false })),
  );

  return (
    <form className="pw-plan-form" aria-labelledby="plan-form-title" onSubmit={onSave} noValidate>
      <div className="pw-plans-head">
        <div className="pw-plans-intro">
          <Link to={plan ? `/plans/${plan.id}` : '/plans'} className="pw-back-link">
            {vi.plans.back}
          </Link>
          <h1 id="plan-form-title" className="pw-page-title">
            {plan ? f.editTitle(plan.name) : f.createTitle}
          </h1>
        </div>
        <FormActions plan={plan} disabled={noDepot || busy || readOnly} busy={isSubmitting} />
      </div>

      {noDepot || (failure instanceof ApiError && failure.code === 'STORE_DEPOT_REQUIRED') ? (
        <section role="alert" className="pw-plans-alert">
          <span>{f.noDepot}</span>
          <Link className={buttonClass('secondary')} to="/store">
            {f.chooseDepot}
          </Link>
        </section>
      ) : failure ? (
        <ErrorBanner title={messageFor(failure)} />
      ) : null}
      {busy ? <ErrorBanner tone="info" title={errorMessages.PLAN_BUSY} /> : null}
      {errorCount > 0 ? (
        <section role="alert" className="pw-plans-alert pw-plans-alert-plain">
          {f.cellsToFix(errorCount)}
        </section>
      ) : null}

      <section className="pw-card" aria-labelledby="sec-info">
        <h2 id="sec-info">{f.info}</h2>
        <div className="pw-plan-info">
          <TextField label={f.name} required maxLength={150} error={errors.name?.message} {...register('name')} />
          <TextField label={f.date} type="date" required error={errors.planDate?.message} {...register('planDate')} />
          <div className="pw-field">
            <span className="pw-field-label">{f.depot}</span>
            <div className="pw-plan-depot">
              <span>{depot ? (depot.address ?? `${depot.lat}, ${depot.lon}`) : f.noDepotShort}</span>
              {!plan ? <Link to="/store">{f.change}</Link> : null}
            </div>
            <p className="pw-field-msg">{f.depotHint}</p>
          </div>
        </div>
      </section>

      <section className="pw-card" aria-labelledby="sec-assign">
        <div className="pw-card-head">
          <h2 id="sec-assign">{f.assignments}</h2>
          <span className="pw-muted">{f.assignmentsCount(assignments.fields.length)}</span>
        </div>
        {members.data && vehicles.data && (memberOptions.length === 0 || vehicleOptions.length === 0) ? (
          <p className="pw-muted">{f.noAssignable}</p>
        ) : null}
        {assignments.fields.length > 0 ? (
          <div className="pw-table-wrap">
            <table className="pw-table pw-edit-table pw-assign-table">
              <caption className="pw-visually-hidden">{f.assignmentsCaption}</caption>
              <thead>
                <tr>
                  <th scope="col">{f.driver}</th>
                  <th scope="col">{f.vehicle}</th>
                  <th scope="col">{f.shiftStart}</th>
                  <th scope="col">{f.shiftEnd}</th>
                  <th scope="col">{f.maxDistance}</th>
                  <th scope="col">
                    <span className="pw-visually-hidden">{f.remove}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {assignments.fields.map((field, i) => {
                  const n = i + 1;
                  const at = errors.assignments?.[i];
                  return (
                    <tr key={field.id}>
                      <Cell error={at?.driverId?.message}>
                        <MemberSelect
                          register={register}
                          name={`assignments.${i}.driverId`}
                          label={f.rowLabel(f.driver, n)}
                          members={memberOptions}
                          invalid={Boolean(at?.driverId)}
                        />
                      </Cell>
                      <Cell error={at?.vehicleId?.message}>
                        <VehicleSelect
                          register={register}
                          name={`assignments.${i}.vehicleId`}
                          label={f.rowLabel(f.vehicle, n)}
                          vehicles={vehicleOptions}
                          invalid={Boolean(at?.vehicleId)}
                        />
                      </Cell>
                      <Cell error={at?.shiftStart?.message}>
                        <input
                          className="pw-field-input"
                          type="time"
                          aria-label={f.rowLabel(f.shiftStart, n)}
                          aria-invalid={at?.shiftStart ? 'true' : undefined}
                          {...register(`assignments.${i}.shiftStart`)}
                        />
                      </Cell>
                      <Cell error={at?.shiftEnd?.message}>
                        <input
                          className="pw-field-input"
                          type="time"
                          aria-label={f.rowLabel(f.shiftEnd, n)}
                          aria-invalid={at?.shiftEnd ? 'true' : undefined}
                          {...register(`assignments.${i}.shiftEnd`)}
                        />
                      </Cell>
                      <Cell error={at?.maxDistanceKm?.message}>
                        <input
                          className="pw-field-input"
                          type="number"
                          min={1}
                          step="any"
                          inputMode="decimal"
                          placeholder={f.unlimited}
                          aria-label={f.rowLabel(f.maxDistance, n)}
                          aria-invalid={at?.maxDistanceKm ? 'true' : undefined}
                          {...register(`assignments.${i}.maxDistanceKm`)}
                        />
                      </Cell>
                      <td>
                        <button
                          type="button"
                          className="pw-icon-button pw-row-remove"
                          aria-label={f.removeAssignment(n)}
                          onClick={() => assignments.remove(i)}
                        >
                          <TrashIcon />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
        <div>
          <Button
            variant="quiet"
            disabled={assignments.fields.length >= MAX_ASSIGNMENTS}
            onClick={() => assignments.append(emptyAssignment())}
          >
            {f.addAssignment}
          </Button>
        </div>
      </section>

      <section className="pw-card" aria-labelledby="sec-stops">
        <div className="pw-card-head">
          <h2 id="sec-stops">{f.stops}</h2>
          <span className="pw-muted pw-tabular">{f.stopsCount(stops.fields.length)}</span>
        </div>
        {stops.fields.length > 0 ? (
          <div className="pw-table-wrap">
            <table className="pw-table pw-edit-table pw-stops-table">
              <caption className="pw-visually-hidden">{f.stopsCaption}</caption>
              <thead>
                <tr>
                  <th scope="col" className="pw-col-label">
                    {f.label} *
                  </th>
                  <th scope="col">{f.recipient}</th>
                  <th scope="col" className="pw-col-phone">
                    {f.phone}
                  </th>
                  <th scope="col" className="pw-col-address">
                    {f.address}
                  </th>
                  <th scope="col" className="pw-col-window">
                    {f.window}
                  </th>
                  {units.map((unit) => (
                    <th key={unit} scope="col" className="pw-col-demand">
                      {UNIT_TITLE[unit]}
                    </th>
                  ))}
                  <th scope="col">{f.note}</th>
                  <th scope="col">
                    <span className="pw-visually-hidden">{f.remove}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {stops.fields.map((field, i) => {
                  const n = i + 1;
                  const at = errors.stops?.[i];
                  const windowError =
                    at?.timeWindow?.end?.message ?? at?.timeWindow?.start?.message ?? at?.timeWindow?.message;
                  return (
                    <tr key={field.id}>
                      <Cell error={at?.label?.message}>
                        <input
                          className="pw-field-input"
                          maxLength={150}
                          aria-label={f.stopField(f.label, n)}
                          aria-invalid={at?.label ? 'true' : undefined}
                          {...register(`stops.${i}.label`)}
                        />
                      </Cell>
                      <Cell error={at?.recipientName?.message}>
                        <input
                          className="pw-field-input"
                          maxLength={100}
                          aria-label={f.stopField(f.recipient, n)}
                          aria-invalid={at?.recipientName ? 'true' : undefined}
                          {...register(`stops.${i}.recipientName`)}
                        />
                      </Cell>
                      <Cell error={at?.recipientPhone?.message}>
                        <input
                          className="pw-field-input"
                          type="tel"
                          aria-label={f.stopField(f.phone, n)}
                          aria-invalid={at?.recipientPhone ? 'true' : undefined}
                          {...register(`stops.${i}.recipientPhone`)}
                        />
                      </Cell>
                      <Cell error={at?.address?.message}>
                        <input
                          className="pw-field-input"
                          maxLength={500}
                          aria-label={f.stopField(f.address, n)}
                          aria-invalid={at?.address ? 'true' : undefined}
                          {...register(`stops.${i}.address`)}
                        />
                      </Cell>
                      <Cell error={windowError}>
                        <div className="pw-window">
                          <input
                            className="pw-field-input"
                            type="time"
                            aria-label={f.stopField(f.windowFrom, n)}
                            aria-invalid={at?.timeWindow?.start ? 'true' : undefined}
                            {...register(`stops.${i}.timeWindow.start`)}
                          />
                          <span aria-hidden="true">–</span>
                          <input
                            className="pw-field-input"
                            type="time"
                            aria-label={f.stopField(f.windowTo, n)}
                            aria-invalid={at?.timeWindow?.end ? 'true' : undefined}
                            {...register(`stops.${i}.timeWindow.end`)}
                          />
                        </div>
                      </Cell>
                      {units.map((unit) => (
                        <Cell key={unit} error={at?.demands?.[unit]?.message ?? at?.demands?.message}>
                          <input
                            className="pw-field-input"
                            type="number"
                            min={0}
                            step={1}
                            inputMode="numeric"
                            aria-label={f.demandOf(UNIT_TITLE[unit].toLocaleLowerCase('vi-VN'), n)}
                            aria-invalid={at?.demands?.[unit] ? 'true' : undefined}
                            {...register(`stops.${i}.demands.${unit}`)}
                          />
                        </Cell>
                      ))}
                      <Cell error={at?.note?.message}>
                        <input
                          className="pw-field-input"
                          maxLength={500}
                          aria-label={f.stopField(f.note, n)}
                          aria-invalid={at?.note ? 'true' : undefined}
                          {...register(`stops.${i}.note`)}
                        />
                      </Cell>
                      <td>
                        <button
                          type="button"
                          className="pw-icon-button pw-row-remove"
                          aria-label={f.removeStop(n)}
                          onClick={() => stops.remove(i)}
                        >
                          <TrashIcon />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="pw-plan-add">
          <Button variant="quiet" disabled={stops.fields.length >= MAX_STOPS} onClick={() => stops.append(emptyStop())}>
            {f.addStop}
          </Button>
          {!readOnly ? (
            <Button variant="secondary" onClick={() => setImporting(true)}>
              {vi.planImport.open}
            </Button>
          ) : null}
          <span className="pw-muted">{f.addressTip}</span>
        </div>
        {imported !== null ? (
          <p role="status" className="pw-import-added">
            {vi.planImport.added(imported)}
          </p>
        ) : null}
        <ImportDialog
          open={importing}
          keptStops={(watchedStops ?? []).filter((row) => !isBlankStop(row)).length}
          onClose={() => setImporting(false)}
          onAdd={(rows) => {
            // Blank rows (the first row of a new plan) give way to the file's stops.
            const kept = getValues('stops').filter((row) => !isBlankStop(row));
            stops.replace([...kept, ...rows.map((row) => importedToRow(row.stop))]);
            setImported(rows.length);
          }}
        />
      </section>

      <div className="pw-plan-footer">
        <FormActions plan={plan} disabled={noDepot || busy || readOnly} busy={isSubmitting} />
      </div>

      <Dialog
        open={modified}
        title={f.modifiedTitle}
        confirmLabel={f.reload}
        cancelLabel={vi.members.back}
        onConfirm={async () => {
          if (!plan) return;
          const fresh = await live.refetch();
          if (fresh.data) reset(planToValues(fresh.data));
          setModified(false);
        }}
        onClose={() => setModified(false)}
      >
        <p>{errorMessages.PLAN_MODIFIED}</p>
      </Dialog>
    </form>
  );
}

function FormActions({ plan, disabled, busy }: { plan: PlanDetail | null; disabled: boolean; busy: boolean }) {
  return (
    <div className="pw-plan-actions">
      <Link className={buttonClass('quiet')} to={plan ? `/plans/${plan.id}` : '/plans'}>
        {f.cancel}
      </Link>
      <Button type="submit" variant="primary" busy={busy} disabled={disabled}>
        {f.save}
      </Button>
    </div>
  );
}

/** A table cell with its error under the control (prototype ".cell-err"). */
function Cell({ error, children }: { error?: string; children: ReactNode }) {
  return (
    <td className={error ? 'pw-cell-error' : undefined}>
      {children}
      {error ? <p className="pw-cell-msg">{error}</p> : null}
    </td>
  );
}

function MemberSelect({
  register,
  name,
  label,
  members,
  invalid,
}: {
  register: UseFormRegister<PlanFormValues>;
  name: `assignments.${number}.driverId`;
  label: string;
  members: Array<Pick<AssignableMember, 'id' | 'email'> & { fullName?: string | null }>;
  invalid: boolean;
}) {
  return (
    <select
      className="pw-field-input"
      aria-label={label}
      aria-invalid={invalid ? 'true' : undefined}
      {...register(name)}
    >
      <option value="">{f.chooseDriver}</option>
      {members.map((member) => (
        <option key={member.id} value={String(member.id)}>
          {member.fullName ?? member.email}
        </option>
      ))}
    </select>
  );
}

function VehicleSelect({
  register,
  name,
  label,
  vehicles,
  invalid,
}: {
  register: UseFormRegister<PlanFormValues>;
  name: `assignments.${number}.vehicleId`;
  label: string;
  vehicles: ActiveVehicle[];
  invalid: boolean;
}) {
  return (
    <select
      className="pw-field-input"
      aria-label={label}
      aria-invalid={invalid ? 'true' : undefined}
      {...register(name)}
    >
      <option value="">{f.chooseVehicle}</option>
      {vehicles.map((vehicle) => (
        <option key={vehicle.id} value={String(vehicle.id)}>
          {`${vehicle.name} · ${formatCapacity(vehicle.capacity, vehicle.capacityUnit)}`}
        </option>
      ))}
    </select>
  );
}

function withCurrent<T extends { id: number }>(options: T[], current: Array<Partial<T> & { id: number }>): T[] {
  const ids = new Set(options.map((option) => option.id));
  const extra = current.filter((item) => !ids.has(item.id)) as T[];
  return [...options, ...extra];
}

/** Fields with an error, nested lists included. */
function countErrors(errors: unknown): number {
  if (!errors || typeof errors !== 'object') return 0;
  if ('message' in errors && typeof (errors as { message?: unknown }).message === 'string') return 1;
  return Object.entries(errors as Record<string, unknown>)
    .filter(([key]) => key !== 'ref' && key !== 'root')
    .reduce((sum, [, value]) => sum + countErrors(value), 0);
}
