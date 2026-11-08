import { divIcon, latLngBounds, type LeafletKeyboardEvent, type Map as LeafletMap } from 'leaflet';
import { useEffect, useState } from 'react';
import { Marker, Polyline } from 'react-leaflet';
import { DEPOT_SVG, DepotIcon } from './depotIcon';
import { DEFAULT_CENTER, MapFrame, MapOverlay, type LatLng, type MapFrameLabels } from './MapFrame';

export type MarkerTone = 'route' | 'signal' | 'success' | 'danger' | 'muted';

export interface MapStop extends LatLng {
  id: string | number;
  /** Number on the marker; none for a stop without a route. */
  sequence?: number | null;
  /** Result colour (delivered, failed…); without it the stop takes its route's colour. */
  tone?: MarkerTone;
  /** Accessible name ("Điểm 3 · ĐH-1009"). */
  label: string;
}

export interface MapRoute {
  key: string | number;
  /** Driver's name: always next to the colour (DESIGN_SYSTEM "Màu route trên bản đồ"). */
  label: string;
  stops: MapStop[];
}

export interface RouteMapLabels extends MapFrameLabels {
  depot: string;
  expand: string;
  collapse: string;
  legend: string;
}

const depotMarkerIcon = divIcon({
  html: DEPOT_SVG,
  className: 'pw-depot-marker',
  iconSize: [30, 30],
  iconAnchor: [15, 27],
});

/** Route n (0-based) takes map-route-(n % 7 + 1). */
export function routeColour(index: number): number {
  return (index % 7) + 1;
}

function stopIcon(stop: MapStop, colour: number | null, selected: boolean) {
  const tone = stop.tone ? `pw-marker-${stop.tone}` : colour ? `pw-marker-r${colour}` : 'pw-marker-muted';
  const size = selected ? 36 : 28;
  const text = typeof stop.sequence === 'number' ? String(stop.sequence) : '';
  return divIcon({
    html: `<span data-theme="light" class="pw-marker ${tone}${selected ? ' pw-marker-selected' : ''}">${text}</span>`,
    className: 'pw-stop-marker',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

/**
 * Routes of a plan on the map (DESIGN_SYSTEM): stops numbered by `sequence` in the route's colour, straight lines from
 * the depot through the stops and back (the API has no road geometry yet), stops without a route apart, the depot,
 * a legend with the drivers' names, + / − and a button to make the map larger. Selecting a marker selects its row.
 */
export function RouteMap({
  depot,
  routes,
  unassigned = [],
  selectedId,
  onSelect,
  labels,
}: {
  depot: LatLng | null;
  routes: MapRoute[];
  unassigned?: MapStop[];
  selectedId?: string | number | null;
  onSelect?: (id: string | number) => void;
  labels: RouteMapLabels;
}) {
  const [map, setMap] = useState<LeafletMap | null>(null);
  const [expanded, setExpanded] = useState(false);
  const points = [...(depot ? [depot] : []), ...routes.flatMap((route) => route.stops), ...unassigned];
  const boundsKey = points.map((point) => `${point.lat},${point.lon}`).join(';');

  useEffect(() => {
    if (!map || points.length === 0) return;
    const size = map.getSize();
    // A map without a size (hidden, or jsdom) cannot compute a zoom that fits: centre on the first place instead.
    if ((points.length === 1 || !size.x || !size.y) && points[0]) map.setView([points[0].lat, points[0].lon], 15);
    else map.fitBounds(latLngBounds(points.map((point) => [point.lat, point.lon])), { padding: [32, 32] });
    // Fit again only when the set of places changes, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, boundsKey]);

  useEffect(() => {
    map?.invalidateSize();
  }, [map, expanded]);

  // Leaflet makes markers focusable but Enter only opens a popup: Enter and Space select here (keyboard users).
  const markerHandlers = (id: string | number) => ({
    click: () => onSelect?.(id),
    keypress: (event: LeafletKeyboardEvent) => {
      if (event.originalEvent.key === 'Enter' || event.originalEvent.key === ' ') onSelect?.(id);
    },
  });

  return (
    <div className={expanded ? 'pw-route-map is-expanded' : 'pw-route-map'}>
      <MapFrame center={depot ?? DEFAULT_CENTER} zoom={12} labels={labels} className="pw-map-routes" onReady={setMap}>
        {routes.map((route, index) => {
          const colour = routeColour(index);
          const line = [...(depot ? [depot] : []), ...route.stops, ...(depot ? [depot] : [])];
          return line.length > 1 ? (
            <Polyline
              key={`line-${route.key}`}
              positions={line.map((point) => [point.lat, point.lon])}
              // className is read when the line is created; react-leaflet's pathOptions only restyle.
              className={`pw-route-line pw-route-r${colour}`}
              pathOptions={{ weight: 4, opacity: 0.9 }}
            />
          ) : null;
        })}
        {routes.flatMap((route, index) =>
          route.stops.map((stop) => (
            <Marker
              key={`stop-${stop.id}`}
              position={[stop.lat, stop.lon]}
              icon={stopIcon(stop, routeColour(index), selectedId === stop.id)}
              title={stop.label}
              alt={stop.label}
              eventHandlers={markerHandlers(stop.id)}
            />
          )),
        )}
        {unassigned.map((stop) => (
          <Marker
            key={`stop-${stop.id}`}
            position={[stop.lat, stop.lon]}
            icon={stopIcon({ ...stop, sequence: null }, null, selectedId === stop.id)}
            title={stop.label}
            alt={stop.label}
            eventHandlers={markerHandlers(stop.id)}
          />
        ))}
        {depot ? (
          <Marker position={[depot.lat, depot.lon]} icon={depotMarkerIcon} title={labels.depot} alt={labels.depot} />
        ) : null}
        <MapOverlay className="pw-map-actions">
          <button
            type="button"
            className="pw-map-ctl pw-map-ctl-text"
            aria-pressed={expanded}
            onClick={() => setExpanded((open) => !open)}
          >
            {expanded ? labels.collapse : labels.expand}
          </button>
        </MapOverlay>
      </MapFrame>
      <ul className="pw-map-legend" aria-label={labels.legend}>
        <li>
          <DepotIcon />
          {labels.depot}
        </li>
        {routes.map((route, index) => (
          <li key={`legend-${route.key}`}>
            <span className={`pw-legend-swatch pw-marker-r${routeColour(index)}`} aria-hidden="true" />
            {route.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
