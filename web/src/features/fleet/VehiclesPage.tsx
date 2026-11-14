import { useStoreReadOnly } from '@/data/meQuery';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router';
import { errorMessages, fieldMessage, messageFor } from '@/shared/errors';
import { requiredRule } from '@/shared/forms/rules';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { vi } from '@/shared/i18n/vi';
import { statusInfo } from '@/shared/status';
import {
  Button,
  Checkbox,
  DataTable,
  ErrorBanner,
  SelectField,
  StatusPill,
  TextField,
  type DataTableColumn,
} from '@/shared/ui';
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

/** "30 thùng": capacity with the short name of its unit. */
export function formatCapacity(capacity: number, unit: string): string {
  return `${capacity.toLocaleString('vi-VN')} ${vi.units.short[unit] ?? unit}`;
}

/**
 * Prototype "Xe" (SCREENS §1.1): the store's vehicles and a form to add or change one. A vehicle is never deleted,
 * only turned off. OWNER and DISPATCHER both manage vehicles (API_SPEC §5).
 */
export function VehiclesPage() {
  const [params, setParams] = useSearchParams();
  const onlyActive = params.get('active') === 'true';
  const vehicles = useVehicles(onlyActive ? { active: true } : {});
  const vehicleApi = useVehicleApi();
  const { readOnly } = useStoreReadOnly();
  const [editing, setEditing] = useState<Vehicle | null>(null);
  const [saved, setSaved] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<VehicleForm>({ defaultValues: EMPTY });

  const startAdd = () => {
    setEditing(null);
    setFailure(null);
    reset(EMPTY);
  };

  const startEdit = (vehicle: Vehicle) => {
    setEditing(vehicle);
    setSaved(null);
    setFailure(null);
    reset({
      name: vehicle.name,
      plateNumber: vehicle.plateNumber ?? '',
      capacity: String(vehicle.capacity),
      capacityUnit: vehicle.capacityUnit,
      active: vehicle.active,
    });
  };

  const onSave = handleSubmit(async (form) => {
    setSaved(null);
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
        setSaved(vi.vehicles.saved(vehicle.name));
      } else {
        await vehicleApi.create(body);
        setSaved(vi.vehicles.added);
      }
      startAdd();
    } catch (error) {
      if (errorCode(error) === 'VEHICLE_NAME_EXISTED') {
        setError('name', { type: 'server', message: errorMessages.VEHICLE_NAME_EXISTED }, { shouldFocus: true });
      } else if (!applyServerErrors(error, setError, FIELDS, LABELS)) {
        setFailure(error);
      }
    }
  });

  const columns: DataTableColumn<Vehicle>[] = [
    { key: 'name', header: vi.vehicles.name, render: (row) => <strong className="pw-fleet-name">{row.name}</strong> },
    { key: 'plate', header: vi.vehicles.plate, render: (row) => row.plateNumber ?? '—' },
    {
      key: 'capacity',
      header: vi.vehicles.capacity,
      numeric: true,
      render: (row) => formatCapacity(row.capacity, row.capacityUnit),
    },
    {
      key: 'status',
      header: vi.vehicles.status,
      render: (row) => {
        const info = statusInfo('vehicle', row.active ? 'ACTIVE' : 'INACTIVE');
        return info ? <StatusPill tone={info.tone}>{info.label}</StatusPill> : null;
      },
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{vi.vehicles.actions}</span>,
      align: 'end',
      render: (row) => (
        <Button variant="quiet" aria-label={vi.vehicles.editLabel(row.name)} onClick={() => startEdit(row)}>
          {vi.vehicles.edit}
        </Button>
      ),
    },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="vehicles-title">
      <div className="pw-fleet-head">
        <div className="pw-fleet-intro">
          <h1 id="vehicles-title" className="pw-page-title">
            {vi.vehicles.title}
          </h1>
          <p className="pw-muted">{vi.vehicles.lead}</p>
        </div>
      </div>

      <div className="pw-fleet-layout">
        <section className="pw-fleet-list" aria-label={vi.vehicles.listLabel}>
          <Checkbox
            label={vi.vehicles.showInactive}
            checked={!onlyActive}
            onChange={(event) => setParams(event.target.checked ? {} : { active: 'true' }, { replace: true })}
          />
          {vehicles.isError ? <ErrorBanner title={messageFor(vehicles.error)} /> : null}
          <DataTable
            caption={vi.vehicles.caption}
            columns={columns}
            rows={vehicles.data ?? []}
            selectedKey={editing?.id ?? null}
            className="pw-fleet-table"
            empty={vehicles.isPending ? vi.pages.loading : onlyActive ? vi.vehicles.emptyActive : vi.vehicles.empty}
          />
        </section>

        <form className="pw-card pw-fleet-form" aria-labelledby="form-title" onSubmit={onSave} noValidate>
          <h2 id="form-title">{editing ? vi.vehicles.formEdit(editing.name) : vi.vehicles.formAdd}</h2>
          {saved ? (
            <div role="status" className="pw-status-ok">
              {saved}
            </div>
          ) : null}
          {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
          <TextField
            label={vi.vehicles.name}
            required
            maxLength={100}
            hint={vi.vehicles.nameHint}
            error={errors.name?.message}
            {...register('name', requiredRule('VEHICLE_NAME_REQUIRED', vi.vehicles.name))}
          />
          <TextField
            label={vi.vehicles.plate}
            maxLength={20}
            placeholder={vi.vehicles.platePlaceholder}
            hint={vi.vehicles.plateHint}
            error={errors.plateNumber?.message}
            {...register('plateNumber')}
          />
          <div className="pw-fleet-pair">
            <TextField
              label={vi.vehicles.capacity}
              type="number"
              inputMode="numeric"
              min={1}
              step={1}
              required
              error={errors.capacity?.message}
              {...register('capacity', {
                validate: (value) =>
                  value.trim() === ''
                    ? fieldMessage('CAPACITY_REQUIRED', vi.vehicles.capacity)
                    : (Number.isInteger(Number(value)) && Number(value) > 0) || fieldMessage('INVALID_CAPACITY', ''),
              })}
            />
            <SelectField
              label={vi.vehicles.unit}
              required
              placeholder={vi.vehicles.unitPlaceholder}
              options={vi.units.options}
              hint={errors.capacityUnit ? undefined : vi.vehicles.unitHint}
              error={errors.capacityUnit?.message}
              {...register('capacityUnit', {
                validate: (value) => value !== '' || fieldMessage('CAPACITY_UNIT_REQUIRED', vi.vehicles.unit),
              })}
            />
          </div>
          {editing ? <Checkbox label={vi.vehicles.active} {...register('active')} /> : null}
          <div className="pw-fleet-actions">
            <Button variant="quiet" onClick={startAdd}>
              {vi.vehicles.cancel}
            </Button>
            <Button type="submit" variant="primary" busy={isSubmitting} disabled={readOnly}>
              {editing ? vi.vehicles.save : vi.vehicles.submitAdd}
            </Button>
          </div>
        </form>
      </div>
    </section>
  );
}
