import { useStoreReadOnly } from '@/data/meQuery';
import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useSearchParams } from 'react-router';
import { errorMessages, fieldMessage, messageFor } from '@/shared/errors';
import { requiredRule } from '@/shared/forms/rules';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import { statusInfo } from '@/shared/status';
import {
  buttonClass,
  ChevronRightIcon,
  DataTable,
  ErrorBanner,
  PlaceholderRows,
  PlusIcon,
  PopUpFilter,
  RowMenu,
  SelectField,
  Sheet,
  StatusPill,
  SwitchRow,
  TextField,
  Toast,
  ToastDock,
  TruckIcon,
  useRowMenu,
  type DataTableColumn,
} from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
import { useVehicleApi, useVehicles, type CapacityUnit, type Vehicle } from './vehicleApi';
import './fleet.css';

interface VehicleForm {
  name: string;
  plateNumber: string;
  capacity: string;
  capacityUnit: CapacityUnit | '';
  active: boolean;
}

const EMPTY: VehicleForm = { name: '', plateNumber: '', capacity: '', capacityUnit: '', active: true };
const FIELDS = ['name', 'plateNumber', 'capacity', 'capacityUnit'] as const;
const LABELS = {
  name: vi.vehicles.name,
  plateNumber: vi.vehicles.plate,
  capacity: vi.vehicles.capacity,
  capacityUnit: vi.vehicles.unit,
};
const v = vi.vehicles;

/** "30 thùng": capacity with the short name of its unit. */
export function formatCapacity(capacity: number, unit: string): string {
  return `${capacity.toLocaleString('vi-VN')} ${vi.units.short[unit] ?? unit}`;
}

function VehicleStatus({ vehicle }: { vehicle: Vehicle }) {
  const info = statusInfo('vehicle', vehicle.active ? 'ACTIVE' : 'INACTIVE');
  return info ? <StatusPill tone={info.tone}>{info.label}</StatusPill> : null;
}

/**
 * "Xe" (SCREENS §1.1, OWNER_UI_REDESIGN §8.3): the store's vehicles, laid out like the plan list (G2). The add action
 * at the end of the title row; adding and changing a vehicle happen in a sheet (HIG Sheets); a status filter as a
 * pop-up button on the URL (`?active=true`: only those in use) instead of a checkbox (HIG Toggles: outside a list, not
 * a switch); a table on a wide window, rows on a phone; touch and hold or right click a row for "Sửa…" and turning it
 * off or on again. A vehicle is never deleted, only turned off. OWNER and DISPATCHER both manage vehicles (API_SPEC §5).
 */
export function VehiclesPage() {
  const [params, setParams] = useSearchParams();
  const onlyActive = params.get('active') === 'true';
  const vehicles = useVehicles(onlyActive ? { active: true } : {});
  const vehicleApi = useVehicleApi();
  const { readOnly } = useStoreReadOnly();
  const wide = useMediaQuery(WIDE_QUERY);
  const rowMenu = useRowMenu<Vehicle>();
  // The sheet: closed, adding (null) or changing a vehicle.
  const [sheet, setSheet] = useState<{ vehicle: Vehicle | null } | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    setValue,
    control,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<VehicleForm>({ defaultValues: EMPTY });
  const active = useWatch({ control, name: 'active' });

  const openAdd = () => {
    setFailure(null);
    reset(EMPTY);
    setSheet({ vehicle: null });
  };
  const openEdit = (vehicle: Vehicle) => {
    if (readOnly) return;
    setFailure(null);
    reset({
      name: vehicle.name,
      plateNumber: vehicle.plateNumber ?? '',
      capacity: String(vehicle.capacity),
      capacityUnit: vehicle.capacityUnit,
      active: vehicle.active,
    });
    setSheet({ vehicle });
  };
  const editing = sheet?.vehicle ?? null;

  const onSave = handleSubmit(async (form) => {
    setFailure(null);
    const body = {
      name: form.name.trim(),
      plateNumber: form.plateNumber.trim() || undefined,
      capacity: Number(form.capacity),
      capacityUnit: form.capacityUnit as CapacityUnit,
    };
    try {
      if (editing) {
        const vehicle = await vehicleApi.update(editing.id, { ...body, active: form.active });
        setToast(v.saved(vehicle.name));
      } else {
        await vehicleApi.create(body);
        setToast(v.added);
      }
      setSheet(null);
    } catch (error) {
      if (errorCode(error) === 'VEHICLE_NAME_EXISTED') {
        setError('name', { type: 'server', message: errorMessages.VEHICLE_NAME_EXISTED }, { shouldFocus: true });
      } else if (!applyServerErrors(error, setError, FIELDS, LABELS)) {
        setFailure(error);
      }
    }
  });

  // Turning a vehicle off or on from its context menu: the same PUT as the sheet, with every field (it replaces all).
  const setActive = async (vehicle: Vehicle, active: boolean) => {
    try {
      await vehicleApi.update(vehicle.id, {
        name: vehicle.name,
        plateNumber: vehicle.plateNumber ?? undefined,
        capacity: vehicle.capacity,
        capacityUnit: vehicle.capacityUnit,
        active,
      });
      setToast(active ? v.turnedOn(vehicle.name) : v.turnedOff(vehicle.name));
    } catch (error) {
      setToast(messageFor(error));
    }
  };

  const columns: DataTableColumn<Vehicle>[] = [
    {
      key: 'name',
      header: v.name,
      className: 'pw-cell-main',
      render: (row) => (
        <span className="pw-cell-title" title={row.name}>
          {row.name}
        </span>
      ),
    },
    { key: 'plate', header: v.plate, className: 'pw-nowrap', render: (row) => row.plateNumber ?? '—' },
    {
      key: 'capacity',
      header: v.capacity,
      numeric: true,
      render: (row) => formatCapacity(row.capacity, row.capacityUnit),
    },
    { key: 'status', header: v.status, render: (row) => <VehicleStatus vehicle={row} /> },
  ];

  const items = vehicles.data ?? [];
  const loading = vehicles.isPending;
  const empty = !loading && !vehicles.isError && items.length === 0;

  return (
    <section className="pw-page-section" aria-labelledby="vehicles-title">
      <div className="pw-list-head">
        <h1 id="vehicles-title" className="pw-page-title">
          {v.title}
        </h1>
        {!readOnly ? (
          <button
            type="button"
            className={wide ? buttonClass('primary') : 'pw-icon-button pw-list-add'}
            aria-label={wide ? undefined : v.formAdd}
            title={wide ? undefined : v.formAdd}
            onClick={openAdd}
          >
            <PlusIcon />
            {wide ? v.formAdd : null}
          </button>
        ) : null}
      </div>
      <p className="pw-muted pw-list-lead">{v.lead}</p>

      <div className="pw-list-filters" role="group" aria-label={v.filterLabel}>
        <PopUpFilter
          label={v.status}
          value={onlyActive ? 'ACTIVE' : undefined}
          emptyText={v.all}
          options={[{ value: 'ACTIVE', label: v.active }]}
          onChange={(value) => setParams(value ? { active: 'true' } : {}, { replace: true })}
        />
      </div>

      {vehicles.isError ? <ErrorBanner title={messageFor(vehicles.error)} /> : null}

      {loading ? (
        <div className="pw-list-group">
          <PlaceholderRows rows={4} label={v.loading} />
        </div>
      ) : empty ? (
        <section className="pw-list-empty" aria-labelledby="vehicles-empty">
          <span className="pw-list-empty-symbol" aria-hidden="true">
            <TruckIcon />
          </span>
          <h2 id="vehicles-empty">{onlyActive ? v.emptyActiveTitle : v.emptyTitle}</h2>
          <p>{onlyActive ? v.emptyActiveLead : v.emptyLead}</p>
          {!readOnly && !onlyActive ? (
            <div className="pw-list-empty-actions">
              <button type="button" className={buttonClass('primary', 'lg')} onClick={openAdd}>
                {v.formAdd}
              </button>
            </div>
          ) : null}
        </section>
      ) : wide ? (
        <DataTable
          caption={v.caption}
          columns={columns}
          rows={items}
          onRowClick={readOnly ? undefined : openEdit}
          onRowContextMenu={readOnly ? undefined : rowMenu.openAt}
          resizable
          className="pw-list-group"
        />
      ) : (
        <ul className="pw-list-group pw-list-rows" aria-label={v.caption}>
          {items.map((row) => (
            <li
              key={row.id}
              className={cx('pw-list-row', rowMenu.menu?.row.id === row.id && 'is-held')}
              {...(readOnly ? {} : rowMenu.holdHandlers(row))}
            >
              <button
                type="button"
                className="pw-list-row-main"
                aria-label={readOnly ? undefined : v.editLabel(row.name)}
                disabled={readOnly}
                onClick={(event) => {
                  rowMenu.consumeHold(event);
                  if (!event.defaultPrevented) openEdit(row);
                }}
              >
                <span className="pw-list-row-title">{row.name}</span>
                <span className="pw-list-row-sub">
                  {[row.plateNumber, formatCapacity(row.capacity, row.capacityUnit)].filter(Boolean).join(' · ')}
                </span>
              </button>
              <VehicleStatus vehicle={row} />
              {!readOnly ? (
                <span className="pw-list-row-chevron" aria-hidden="true">
                  <ChevronRightIcon />
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      {rowMenu.menu ? (
        <RowMenu
          rect={rowMenu.menu.rect}
          label={rowMenu.menu.row.name}
          onClose={rowMenu.close}
          items={[
            { label: v.menuEdit, onSelect: () => openEdit(rowMenu.menu!.row) },
            rowMenu.menu.row.active
              ? { label: v.menuTurnOff, onSelect: () => void setActive(rowMenu.menu!.row, false) }
              : { label: v.menuTurnOn, onSelect: () => void setActive(rowMenu.menu!.row, true) },
          ]}
        />
      ) : null}

      <Sheet
        open={sheet !== null}
        title={editing ? v.formEdit : v.formAdd}
        doneLabel={editing ? v.save : v.add}
        busy={isSubmitting}
        dirty={isDirty}
        onSubmit={onSave}
        onClose={() => setSheet(null)}
      >
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
        <TextField
          label={v.name}
          required
          maxLength={100}
          hint={v.nameHint}
          error={errors.name?.message}
          {...register('name', requiredRule('VEHICLE_NAME_REQUIRED', v.name))}
        />
        <TextField
          label={v.plate}
          maxLength={20}
          placeholder={v.platePlaceholder}
          hint={v.plateHint}
          error={errors.plateNumber?.message}
          {...register('plateNumber')}
        />
        <div className="pw-fleet-pair">
          <TextField
            label={v.capacity}
            type="number"
            inputMode="numeric"
            min={1}
            step={1}
            required
            error={errors.capacity?.message}
            {...register('capacity', {
              validate: (value) =>
                value.trim() === ''
                  ? fieldMessage('CAPACITY_REQUIRED', v.capacity)
                  : (Number.isInteger(Number(value)) && Number(value) > 0) || fieldMessage('INVALID_CAPACITY', ''),
            })}
          />
          <SelectField
            label={v.unit}
            required
            placeholder={v.unitPlaceholder}
            options={vi.units.options}
            hint={errors.capacityUnit ? undefined : v.unitHint}
            error={errors.capacityUnit?.message}
            {...register('capacityUnit', {
              validate: (value) => value !== '' || fieldMessage('CAPACITY_UNIT_REQUIRED', v.unit),
            })}
          />
        </div>
        {/* A switch in a row of the sheet (HIG Toggles: "Use the switch toggle style only in a list row"; iOS has no
            checkbox). */}
        {editing ? (
          <div className="pw-sheet-group">
            <SwitchRow
              label={v.active}
              hint={v.activeHint}
              checked={active}
              onChange={(active) => setValue('active', active, { shouldDirty: true })}
            />
          </div>
        ) : null}
      </Sheet>

      {toast ? (
        <ToastDock>
          <Toast tone="success" onClose={() => setToast(null)}>
            {toast}
          </Toast>
        </ToastDock>
      ) : null}
    </section>
  );
}
