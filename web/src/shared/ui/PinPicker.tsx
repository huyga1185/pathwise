import { useEffect, useId, useRef, useState, type ReactNode } from 'react';
import { DepotIcon, PinMap, type LatLng } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { vi } from '@/shared/i18n/vi';
import { Dialog } from './Dialog';
import { TextField } from './TextField';

export interface Place {
  displayName: string;
  lat: number;
  lon: number;
}

/** Waits this long after the last key before searching (GET /geocoding/search is rate limited). */
const SEARCH_DELAY_MS = 400;
const MIN_QUERY = 3;

/** "45 Lê Lợi, Hải Châu, Đà Nẵng" → title "45 Lê Lợi", rest "Hải Châu, Đà Nẵng". */
function splitPlace(name: string): [string, string] {
  const cut = name.indexOf(', ');
  return cut > 0 ? [name.slice(0, cut), name.slice(cut + 2)] : [name, ''];
}

export interface PinPickerProps {
  value: LatLng | null;
  /** The pin moved: a click on the map, a drag, "place at centre", or a search result. */
  onChange?: (value: LatLng) => void;
  /** A search result was chosen (after onChange): callers may take its name as the address. */
  onPlace?: (place: Place) => void;
  /** Address search; screens pass the call of their feature (GET /geocoding/search, ARCHITECTURE §7). */
  search?: (query: string) => Promise<Place[]>;
  readOnly?: boolean;
  searchLabel?: string;
  /** Line under the map, after the legend. */
  help?: string;
  /** Name of the pin and of its legend entry. */
  pinLabel?: string;
}

/**
 * Prototype "Cửa hàng" (kho) and "Soạn plan" (sửa điểm lỗi): address search with results, a map with one pin, legend.
 * The map part comes from platform/map; this component adds the form controls, which platform may not use.
 */
export function PinPicker({
  value,
  onChange,
  onPlace,
  search,
  readOnly,
  searchLabel = vi.map.search,
  help,
  pinLabel = vi.map.depot,
}: PinPickerProps) {
  const [query, setQuery] = useState('');
  // The answer to one query; shown only while the field still holds that query.
  const [answer, setAnswer] = useState<{ query: string; places?: Place[]; error?: string } | null>(null);
  const latest = useRef(0);
  const listId = useId();
  const editable = !readOnly && onChange !== undefined;
  const text = query.trim();
  const active = search !== undefined && text.length >= MIN_QUERY;
  const current = active && answer?.query === text ? answer : null;
  const searching = active && current === null;
  const results = current?.places ?? null;
  const error = current?.error ?? null;

  useEffect(() => {
    if (!search || text.length < MIN_QUERY) return undefined;
    const run = ++latest.current;
    const timer = window.setTimeout(async () => {
      try {
        const places = await search(text);
        if (run === latest.current) setAnswer({ query: text, places });
      } catch (failure) {
        if (run === latest.current) setAnswer({ query: text, error: messageFor(failure) });
      }
    }, SEARCH_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [text, search]);

  const pick = (place: Place) => {
    onChange?.({ lat: place.lat, lon: place.lon });
    onPlace?.(place);
    setQuery('');
  };

  return (
    <div className="pw-pin-picker">
      {editable && search ? (
        <div className="pw-pin-search">
          <TextField
            label={searchLabel}
            type="search"
            autoComplete="off"
            value={query}
            hint={error ? undefined : searching ? vi.map.searching : vi.map.searchHint}
            error={error ?? undefined}
            aria-controls={results ? listId : undefined}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              // The picker often sits in a form: Enter in the search must not submit it.
              if (event.key === 'Enter') event.preventDefault();
            }}
          />
          {results ? (
            results.length > 0 ? (
              <ul id={listId} className="pw-pin-results" aria-label={vi.map.results}>
                {results.map((place) => {
                  const [title, rest] = splitPlace(place.displayName);
                  return (
                    <li key={`${place.lat},${place.lon},${place.displayName}`}>
                      <button type="button" className="pw-pin-result" onClick={() => pick(place)}>
                        <strong>{title}</strong>
                        {rest ? <span>{rest}</span> : null}
                      </button>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className="pw-muted" role="status">
                {vi.map.noResult}
              </p>
            )
          ) : null}
        </div>
      ) : null}
      <PinMap
        value={value}
        onChange={editable ? onChange : undefined}
        readOnly={!editable}
        labels={{
          zoomIn: vi.map.zoomIn,
          zoomOut: vi.map.zoomOut,
          pin: pinLabel,
          placeAtCenter: vi.map.placeAtCenter,
        }}
      />
      <div className="pw-pin-legend">
        <span className="pw-pin-legend-item">
          <DepotIcon />
          {pinLabel}
        </span>
        {help ? <span className="pw-muted">{help}</span> : null}
      </div>
    </div>
  );
}

/**
 * The picker in a near full-screen dialog (DESIGN_SYSTEM Dialog `size="full"`): used to place a stop that could not be
 * geocoded (W05). The choice is applied only on confirm.
 */
export function PinPickerDialog({
  open,
  title = vi.map.pickTitle,
  initial,
  search,
  onConfirm,
  onClose,
  busy,
  pinLabel,
  children,
}: {
  open: boolean;
  title?: string;
  initial: LatLng | null;
  search?: (query: string) => Promise<Place[]>;
  onConfirm: (value: LatLng, place: Place | null) => void;
  onClose: () => void;
  busy?: boolean;
  pinLabel?: string;
  /** Error or explanation shown above the picker. */
  children?: ReactNode;
}) {
  const [value, setValue] = useState<LatLng | null>(initial);
  const [place, setPlace] = useState<Place | null>(null);
  return (
    <Dialog
      open={open}
      size="full"
      title={title}
      confirmLabel={vi.map.pickConfirm}
      cancelLabel={vi.members.back}
      confirmDisabled={value === null}
      busy={busy}
      onConfirm={() => value && onConfirm(value, place)}
      onClose={onClose}
    >
      {children}
      {open ? (
        <PinPicker
          value={value}
          onChange={(next) => {
            setValue(next);
            setPlace(null);
          }}
          onPlace={setPlace}
          search={search}
          pinLabel={pinLabel}
        />
      ) : null}
    </Dialog>
  );
}
