import { divIcon, type Map as LeafletMap, type Marker as LeafletMarker } from 'leaflet';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Marker, useMapEvents } from 'react-leaflet';
import { DEPOT_SVG } from './depotIcon';
import { pinSvg } from './RouteMap';
import { DEFAULT_CENTER, MapFrame, MapOverlay, type LatLng, type MapFrameLabels } from './MapFrame';

export interface PinMapLabels extends MapFrameLabels {
  /** Name of the pin ("Kho xuất phát"). */
  pin: string;
  /** Keyboard way to place the pin: at the centre of the map. */
  placeAtCenter: string;
}

const pinIcon = divIcon({ html: DEPOT_SVG, className: 'pw-depot-marker', iconSize: [30, 30], iconAnchor: [15, 27] });
/** A delivery stop's pin: the stop pin of the route map (fb_17) in the accent colour, empty circle — not the house. */
const stopPinIcon = divIcon({
  html: pinSvg('var(--route)', '', 28, 'pw-pin-route'),
  className: 'pw-stop-pin',
  iconSize: [28, 40],
  iconAnchor: [14, 39],
});

/**
 * A map with one pin (the depot of DESIGN_SYSTEM): click the map, drag the pin or use "place at centre" to move it.
 * When `value` changes from outside (an address picked from search) the map moves to it.
 */
export function PinMap({
  value,
  onChange,
  readOnly,
  labels,
  className,
  start: startAt,
  kind = 'depot',
}: {
  value: LatLng | null;
  /** Where to open without a pin (the depot), instead of the city's default centre. */
  start?: LatLng;
  /** What the pin marks: the depot (a house) or a delivery stop (a map pin). */
  kind?: 'depot' | 'stop';
  onChange?: (value: LatLng) => void;
  readOnly?: boolean;
  labels: PinMapLabels;
  className?: string;
}) {
  const [map, setMap] = useState<LeafletMap | null>(null);
  const editable = !readOnly && onChange !== undefined;

  useEffect(() => {
    // Only when the pin is out of sight (picked from search); a click on the map keeps the view.
    if (map && value && !map.getBounds().contains([value.lat, value.lon])) {
      map.setView([value.lat, value.lon], Math.max(map.getZoom(), 15));
    }
  }, [map, value]);

  const start = value ?? startAt ?? DEFAULT_CENTER;
  return (
    <MapFrame
      center={start}
      zoom={value ? 15 : 12}
      labels={labels}
      className={editable ? `pw-map-pick ${className ?? ''}` : className}
      onReady={setMap}
    >
      {editable ? <ClickToPlace onPlace={onChange} /> : null}
      {value ? (
        <Pin
          value={value}
          label={labels.pin}
          draggable={editable}
          onMove={onChange}
          icon={kind === 'stop' ? stopPinIcon : pinIcon}
        />
      ) : null}
      {editable ? (
        // A pin button under + / − (owner's choice, §10.7): the symbol of dropping a pin (HIG Buttons: "associate
        // familiar actions with familiar icons"); its full name for assistive technologies and as a tooltip.
        <MapOverlay className="pw-map-pin-action">
          <button
            type="button"
            className="pw-map-ctl"
            aria-label={labels.placeAtCenter}
            title={labels.placeAtCenter}
            onClick={() => {
              if (!map) return;
              const centre = map.getCenter();
              onChange({ lat: centre.lat, lon: centre.lng });
            }}
          >
            <svg
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              strokeWidth={1.7}
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z" />
              <circle cx="12" cy="9.5" r="2.5" />
            </svg>
          </button>
        </MapOverlay>
      ) : null}
    </MapFrame>
  );
}

function ClickToPlace({ onPlace }: { onPlace: (value: LatLng) => void }) {
  useMapEvents({ click: (event) => onPlace({ lat: event.latlng.lat, lon: event.latlng.lng }) });
  return null;
}

function Pin({
  value,
  label,
  draggable,
  onMove,
  icon,
}: {
  value: LatLng;
  label: string;
  draggable: boolean;
  onMove?: (value: LatLng) => void;
  icon: ReturnType<typeof divIcon>;
}) {
  const ref = useRef<LeafletMarker>(null);
  const handlers = useMemo(
    () => ({
      dragend: () => {
        const at = ref.current?.getLatLng();
        if (at && onMove) onMove({ lat: at.lat, lon: at.lng });
      },
    }),
    [onMove],
  );
  return (
    <Marker
      ref={ref}
      position={[value.lat, value.lon]}
      icon={icon}
      draggable={draggable}
      keyboard={false}
      title={label}
      alt={label}
      eventHandlers={handlers}
    />
  );
}
