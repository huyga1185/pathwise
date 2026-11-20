// Stand-in for the map services during E2E (W07): the backend's Nominatim and OSRM clients (matrix, and directions
// since D6) point here (compose.e2e.yaml), so the end-to-end run never calls a real third-party service
// (docs/client/README.md §2).
// Plain Node, no dependencies; answers are deterministic.
import { createServer } from 'node:http';

const PORT = Number(process.env.PORT ?? 8090);
// Đà Nẵng, around the sample data.
const SOUTH = 16.03;
const NORTH = 16.08;
const WEST = 108.18;
const EAST = 108.24;
const ROAD_FACTOR = 1.3;
const SPEED_MPS = 8.3; // about 30 km/h in town

function hash(text) {
  let value = 2166136261;
  for (const char of text) value = Math.imul(value ^ char.codePointAt(0), 16777619) >>> 0;
  return value;
}

/** Nominatim /search?format=json: one place for any address, none for one containing "không tồn tại". */
function search(query) {
  if (!query || query.toLocaleLowerCase('vi-VN').includes('không tồn tại')) return [];
  const h = hash(query);
  const lat = SOUTH + ((h % 1000) / 1000) * (NORTH - SOUTH);
  const lon = WEST + ((Math.floor(h / 1000) % 1000) / 1000) * (EAST - WEST);
  return [{ display_name: `${query}, Đà Nẵng, Việt Nam`, lat: lat.toFixed(6), lon: lon.toFixed(6) }];
}

function metres(a, b) {
  const rad = (degrees) => (degrees * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLon = rad(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371000 * Math.asin(Math.sqrt(h)) * ROAD_FACTOR;
}

/** OSRM /table/v1/{profile}/{lon,lat;…}?annotations=distance,duration[&sources=…&destinations=…]. */
function table(coordinates, params) {
  const points = coordinates.split(';').map((pair) => {
    const [lon, lat] = pair.split(',').map(Number);
    return { lat, lon };
  });
  const pick = (name) => (params.get(name) ? params.get(name).split(';').map(Number) : points.map((_, index) => index));
  const sources = pick('sources');
  const destinations = pick('destinations');
  const distances = sources.map((i) => destinations.map((j) => metres(points[i], points[j])));
  const durations = distances.map((row) => row.map((distance) => distance / SPEED_MPS));
  return { code: 'Ok', distances, durations };
}

/** Google's encoded polyline at 1e6 precision (OSRM geometries=polyline6), of {lat, lon} points. */
function polyline6(points) {
  let out = '';
  let lastLat = 0;
  let lastLon = 0;
  const encode = (value) => {
    let v = value < 0 ? ~(value << 1) : value << 1;
    while (v >= 0x20) {
      out += String.fromCharCode((0x20 | (v & 0x1f)) + 63);
      v >>= 5;
    }
    out += String.fromCharCode(v + 63);
  };
  for (const { lat, lon } of points) {
    const latE6 = Math.round(lat * 1e6);
    const lonE6 = Math.round(lon * 1e6);
    encode(latE6 - lastLat);
    encode(lonE6 - lastLon);
    lastLat = latE6;
    lastLon = lonE6;
  }
  return out;
}

/**
 * OSRM /route/v1/{profile}/{lon,lat;lon,lat}?steps=true&geometries=polyline6 (driver directions, T33): an L-shaped
 * drive, east or west along one road then north or south along another, with depart, one turn and arrive steps.
 */
function route(coordinates) {
  const [from, to] = coordinates.split(';').map((pair) => {
    const [lon, lat] = pair.split(',').map(Number);
    return { lat, lon };
  });
  const corner = { lat: from.lat, lon: to.lon };
  const first = metres(from, corner) / ROAD_FACTOR;
  const second = metres(corner, to) / ROAD_FACTOR;
  const east = to.lon >= from.lon;
  const north = to.lat >= from.lat;
  const step = (distance, name, type, modifier, at) => ({
    distance,
    duration: distance / SPEED_MPS,
    name,
    maneuver: { type, ...(modifier ? { modifier } : {}), location: [at.lon, at.lat] },
  });
  const steps = [
    step(first, 'Nguyễn Văn Linh', 'depart', undefined, from),
    step(second, 'Lê Duẩn', 'turn', east === north ? 'left' : 'right', corner),
    step(0, '', 'arrive', undefined, to),
  ];
  const distance = first + second;
  return {
    code: 'Ok',
    routes: [{ distance, duration: distance / SPEED_MPS, geometry: polyline6([from, corner, to]), legs: [{ steps }] }],
  };
}

createServer((request, response) => {
  const url = new URL(request.url ?? '/', 'http://stub');
  const send = (status, body) => {
    response.writeHead(status, { 'content-type': 'application/json' });
    response.end(JSON.stringify(body));
  };
  if (url.pathname === '/health') return send(200, { status: 'UP' });
  if (url.pathname === '/search') return send(200, search(url.searchParams.get('q') ?? ''));
  const osrm = /^\/table\/v1\/[^/]+\/(.+)$/.exec(url.pathname);
  if (osrm) return send(200, table(decodeURIComponent(osrm[1]), url.searchParams));
  const directions = /^\/route\/v1\/[^/]+\/(.+)$/.exec(url.pathname);
  if (directions) return send(200, route(decodeURIComponent(directions[1])));
  return send(404, { code: 'NotFound' });
}).listen(PORT, () => console.log(`map stub on ${PORT}`));
