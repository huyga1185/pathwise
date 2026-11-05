import 'leaflet/dist/leaflet.css';
import { control, DomEvent, type Map as LeafletMap } from 'leaflet';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import { useBasemap } from './BasemapContext';
import './map.css';

export interface LatLng {
  lat: number;
  lon: number;
}

export interface MapFrameLabels {
  zoomIn: string;
  zoomOut: string;
}

/** Đà Nẵng: where a map starts when there is nothing to show yet. */
export const DEFAULT_CENTER: LatLng = { lat: 16.0544, lon: 108.2022 };

/**
 * A Leaflet map with the configured basemap, its attribution, and the + / − buttons of the prototype. Always the light
 * theme: tiles have no dark version (DESIGN_SYSTEM MapMarker).
 */
export function MapFrame({
  center,
  zoom,
  labels,
  className,
  children,
  onReady,
}: {
  center: LatLng;
  zoom: number;
  labels: MapFrameLabels;
  className?: string;
  children?: ReactNode;
  onReady?: (map: LeafletMap) => void;
}) {
  const basemap = useBasemap();
  return (
    <div data-theme="light" className={className ? `pw-map ${className}` : 'pw-map'}>
      <MapContainer
        center={[center.lat, center.lon]}
        zoom={zoom}
        maxZoom={basemap.maxZoom}
        zoomControl={false}
        attributionControl={false}
        className="pw-map-canvas"
        ref={(map) => {
          if (map) onReady?.(map);
        }}
      >
        {basemap.tileUrl ? (
          <TileLayer
            key={basemap.id}
            url={basemap.tileUrl}
            tileSize={basemap.tileSize}
            zoomOffset={basemap.tileSize === 512 ? -1 : 0}
            maxZoom={basemap.maxZoom}
          />
        ) : null}
        <Attribution text={basemap.attribution} />
        <ZoomButtons labels={labels} />
        {children}
      </MapContainer>
    </div>
  );
}

/** The provider's attribution, required on every map whatever the tiles (P-45). Text comes from basemap.ts only. */
function Attribution({ text }: { text: string }) {
  const map = useMap();
  useEffect(() => {
    const attribution = control.attribution({ position: 'bottomright', prefix: false }).addTo(map);
    if (text) attribution.addAttribution(text);
    return () => {
      attribution.remove();
    };
  }, [map, text]);
  return null;
}

/** Buttons over the map: clicks and scrolls on them must not reach the map (no pin placed, no pan). */
export function MapOverlay({ className, children }: { className: string; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!ref.current) return;
    DomEvent.disableClickPropagation(ref.current);
    DomEvent.disableScrollPropagation(ref.current);
  }, []);
  return (
    <div ref={ref} className={className}>
      {children}
    </div>
  );
}

function ZoomButtons({ labels }: { labels: MapFrameLabels }) {
  const map = useMap();
  const [zoom, setZoom] = useState(() => map.getZoom());
  useEffect(() => {
    const update = () => setZoom(map.getZoom());
    map.on('zoomend', update);
    return () => {
      map.off('zoomend', update);
    };
  }, [map]);
  return (
    <MapOverlay className="pw-map-zoom">
      <button
        type="button"
        className="pw-map-ctl"
        aria-label={labels.zoomIn}
        disabled={zoom >= map.getMaxZoom()}
        onClick={() => map.zoomIn()}
      >
        +
      </button>
      <button
        type="button"
        className="pw-map-ctl"
        aria-label={labels.zoomOut}
        disabled={zoom <= map.getMinZoom()}
        onClick={() => map.zoomOut()}
      >
        −
      </button>
    </MapOverlay>
  );
}
