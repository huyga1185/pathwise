import { divIcon, type Map as LeafletMap, type Marker as LeafletMarker } from 'leaflet';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Marker, useMapEvents } from 'react-leaflet';
import { DEPOT_SVG } from './depotIcon';
import { DEFAULT_CENTER, MapFrame, MapOverlay, type LatLng, type MapFrameLabels } from './MapFrame';

export interface PinMapLabels extends MapFrameLabels {
  /** Name of the pin ("Kho xuất phát"). */
  pin: string;
  /** Keyboard way to place the pin: at the centre of the map. */
  placeAtCenter: string;
}

const pinIcon = divIcon({ html: DEPOT_SVG, className: 'pw-depot-marker', iconSize: [30, 30], iconAnchor: [15, 27] });
/** A delivery stop's pin: a map pin in the accent colour, its tip on the place — not the depot's house (G5). */
const STOP_PIN_SVG =
  '<svg viewBox="0 0 28 38" width="28" height="38" aria-hidden="true" class="pw-stop-pin-svg">' +
  '<path d="M14 1.5C7.1 1.5 1.5 7 1.5 13.8c0 8.6 10.6 20.6 11.6 21.7a1.2 1.2 0 0 0 1.8 0c1-1.1 11.6-13.1 11.6-21.7C26.5 7 20.9 1.5 14 1.5Z" stroke="#ffffff" stroke-width="2"/>' +
  '<circle cx="14" cy="13.8" r="4.6" fill="#ffffff"/></svg>';
const stopPinIcon = divIcon({
  html: STOP_PIN_SVG,
  className: 'pw-stop-pin',
  iconSize: [28, 38],
  iconAnchor: [14, 37],
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
        <MapOverlay className="pw-map-actions">
          <button
            type="button"
            className="pw-map-ctl pw-map-ctl-text"
            onClick={() => {
              if (!map) return;
              const centre = map.getCenter();
              onChange({ lat: centre.lat, lon: centre.lng });
            }}
          >
            {labels.placeAtCenter}
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
