import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useGeocodingSearch } from '@/data/geocoding';
import { useStoreQuery } from '@/data/storeQuery';
import { useSession } from '@/features/auth';
import type { LatLng } from '@/platform/map';
import { errorMessages, fieldErrors, messageFor } from '@/shared/errors';
import { requiredRule } from '@/shared/forms/rules';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { vi } from '@/shared/i18n/vi';
import { Button, ErrorBanner, SelectField, TextField } from '@/shared/ui';
import { PinPicker } from '@/shared/ui/PinPicker';
import { useStoreApi, type Store } from './storeApi';
import './store.css';

/** "16,06110 / 108,21175": coordinates with five decimals (about one metre), vi-VN decimal comma. */
export function formatCoordinates(point: LatLng): string {
  const format = (value: number) =>
    value.toLocaleString('vi-VN', { minimumFractionDigits: 5, maximumFractionDigits: 5, useGrouping: false });
  return `${format(point.lat)} / ${format(point.lon)}`;
}

/**
 * Prototype "Cửa hàng" (SCREENS §1.1): name, timezone and the depot every route starts from. The depot is set with the
 * pin (coordinates required), its address is optional. OWNER edits; others read.
 */
export function StorePage() {
  const store = useStoreQuery();
  const { session } = useSession();
  const isOwner = session.status === 'authenticated' && session.user.role === 'OWNER';

  return (
    <section className="pw-page-section" aria-labelledby="store-title">
      {store.data ? (
        <StoreForm key={store.data.id} store={store.data} editable={isOwner} />
      ) : (
        <>
          <h1 id="store-title" className="pw-page-title">
            {vi.store.title}
          </h1>
          {store.isError ? (
            <ErrorBanner title={messageFor(store.error)} />
          ) : (
            <p className="pw-muted">{vi.pages.loading}</p>
          )}
        </>
      )}
    </section>
  );
}

interface StoreFormValues {
  name: string;
  timezone: string;
  address: string;
}

const FIELDS = ['name', 'timezone', 'address'] as const;
const LABELS = { name: vi.store.name, timezone: vi.store.timezone, address: vi.store.depotAddress };

function StoreForm({ store, editable }: { store: Store; editable: boolean }) {
  const storeApi = useStoreApi();
  const search = useGeocodingSearch();
  const [pin, setPin] = useState<LatLng | null>(store.depot ? { lat: store.depot.lat, lon: store.depot.lon } : null);
  const [saved, setSaved] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<StoreFormValues>({
    defaultValues: { name: store.name, timezone: store.timezone, address: store.depot?.address ?? '' },
  });

  const timezones = vi.store.timezones.some((zone) => zone.value === store.timezone)
    ? vi.store.timezones
    : [...vi.store.timezones, { value: store.timezone, label: store.timezone }];

  const onSave = handleSubmit(async (form) => {
    if (!pin) return;
    setSaved(false);
    setFailure(null);
    try {
      await storeApi.update({
        name: form.name.trim(),
        timezone: form.timezone,
        depot: { lat: pin.lat, lon: pin.lon, address: form.address.trim() || undefined },
      });
      setSaved(true);
    } catch (error) {
      if (errorCode(error) === 'INVALID_TIMEZONE') {
        setError('timezone', { type: 'server', message: errorMessages.INVALID_TIMEZONE }, { shouldFocus: true });
        return;
      }
      // depot.address is the address field; a coordinate error (depot.lat) has no field of its own.
      const depotAddress = fieldErrors(error, LABELS)['depot.address'];
      if (depotAddress) setError('address', { type: 'server', message: depotAddress });
      if (!applyServerErrors(error, setError, FIELDS, LABELS) && !depotAddress) setFailure(error);
    }
  });

  const movePin = (next: LatLng) => {
    setPin(next);
    setSaved(false);
  };

  return (
    <form className="pw-store" aria-labelledby="store-title" onSubmit={onSave} noValidate>
      <div className="pw-store-head">
        <div className="pw-store-intro">
          <h1 id="store-title" className="pw-page-title">
            {vi.store.title}
          </h1>
          <p className="pw-muted">{editable ? vi.store.leadOwner : vi.store.leadViewer}</p>
        </div>
        {editable ? (
          <Button type="submit" variant="primary" busy={isSubmitting} disabled={!pin}>
            {vi.store.save}
          </Button>
        ) : null}
      </div>

      {!pin ? (
        <section role="alert" className="pw-store-warning">
          {editable ? vi.store.noDepot : vi.store.noDepotViewer}
        </section>
      ) : null}
      {saved ? (
        <section role="status" className="pw-store-saved">
          {vi.store.saved}
        </section>
      ) : null}
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

      <div className="pw-store-layout">
        <section className="pw-card pw-store-info" aria-labelledby="info-title">
          <h2 id="info-title">{vi.store.info}</h2>
          <TextField
            label={vi.store.name}
            required
            maxLength={150}
            disabled={!editable}
            error={errors.name?.message}
            {...register('name', requiredRule('STORE_NAME_REQUIRED', vi.store.name))}
          />
          <SelectField
            label={vi.store.timezone}
            required
            disabled={!editable}
            options={timezones}
            hint={vi.store.timezoneHint}
            error={errors.timezone?.message}
            {...register('timezone')}
          />
          <TextField
            label={vi.store.depotAddress}
            maxLength={500}
            disabled={!editable}
            hint={vi.store.depotAddressHint}
            error={errors.address?.message}
            {...register('address', { onChange: () => setSaved(false) })}
          />
          <div className="pw-field">
            <span className="pw-field-label">{vi.store.coordinates}</span>
            <span className="pw-store-coords" aria-live="polite">
              {pin ? formatCoordinates(pin) : vi.store.notSet}
            </span>
          </div>
        </section>

        <section className="pw-card pw-store-map" aria-labelledby="map-title">
          <h2 id="map-title">{vi.store.depotTitle}</h2>
          <PinPicker
            value={pin}
            onChange={editable ? movePin : undefined}
            onPlace={(place) => {
              // The found address helps people recognise the depot; keep one the owner typed.
              if (!getValues('address').trim()) setValue('address', place.displayName);
            }}
            search={editable ? search : undefined}
            readOnly={!editable}
            searchLabel={vi.store.searchDepot}
            help={editable ? vi.store.mapHelp : undefined}
          />
        </section>
      </div>
    </form>
  );
}
