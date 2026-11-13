import 'dart:math' as math;

/// A point in WGS 84 degrees. The app's own type, so that only `platform/map` knows `latlong2` (P-45).
class GeoPoint {
  const GeoPoint(this.lat, this.lon);

  final double lat;
  final double lon;

  @override
  bool operator ==(Object other) => other is GeoPoint && other.lat == lat && other.lon == lon;

  @override
  int get hashCode => Object.hash(lat, lon);

  @override
  String toString() => 'GeoPoint($lat, $lon)';
}

/// Decodes the Encoded Polyline Algorithm Format with 6 decimals, [lat, lon] (the `polyline` of API_SPEC §8, P-57).
/// Throws [FormatException] on a truncated string.
List<GeoPoint> decodePolyline6(String encoded) {
  const factor = 1e6;
  final points = <GeoPoint>[];
  var index = 0;
  var lat = 0;
  var lon = 0;
  int next() {
    var result = 0;
    var shift = 0;
    while (true) {
      if (index >= encoded.length) throw const FormatException('Truncated polyline');
      final byte = encoded.codeUnitAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
      if (byte < 0x20) break;
    }
    return (result & 1) != 0 ? ~(result >> 1) : result >> 1;
  }

  while (index < encoded.length) {
    lat += next();
    lon += next();
    points.add(GeoPoint(lat / factor, lon / factor));
  }
  return points;
}

const _earthRadiusMeters = 6371000.0;

/// Great-circle distance in metres.
double distanceMeters(GeoPoint a, GeoPoint b) {
  final dLat = _radians(b.lat - a.lat);
  final dLon = _radians(b.lon - a.lon);
  final h =
      math.pow(math.sin(dLat / 2), 2) +
      math.cos(_radians(a.lat)) * math.cos(_radians(b.lat)) * math.pow(math.sin(dLon / 2), 2);
  return 2 * _earthRadiusMeters * math.asin(math.min(1, math.sqrt(h)));
}

double _radians(double degrees) => degrees * math.pi / 180;

/// Where a position falls on a path: [offsetMeters] from the path, [alongMeters] from its start.
class Projection {
  const Projection({required this.offsetMeters, required this.alongMeters});

  final double offsetMeters;
  final double alongMeters;
}

/// A path with its cumulative lengths, to project positions onto it. Distances over a few kilometres are computed on
/// a local flat projection (equirectangular around each segment), precise to well under a metre at city scale.
class PolylinePath {
  PolylinePath(List<GeoPoint> points) : points = List.unmodifiable(points), _cumulative = _lengths(points) {
    if (points.isEmpty) throw ArgumentError('A path needs at least one point');
  }

  final List<GeoPoint> points;
  final List<double> _cumulative;

  double get lengthMeters => _cumulative.last;

  static List<double> _lengths(List<GeoPoint> points) {
    final lengths = <double>[0];
    for (var i = 1; i < points.length; i++) {
      lengths.add(lengths.last + distanceMeters(points[i - 1], points[i]));
    }
    return lengths;
  }

  /// The nearest point of the path to [position]. With [notBeforeMeters], segments that end before that distance
  /// are skipped, so a path that comes back near itself (a U-turn, a loop) does not send progress backwards.
  Projection project(GeoPoint position, {double notBeforeMeters = 0}) {
    if (points.length == 1) {
      return Projection(offsetMeters: distanceMeters(position, points.first), alongMeters: 0);
    }
    Projection? best;
    for (var i = 1; i < points.length; i++) {
      if (_cumulative[i] < notBeforeMeters) continue;
      final candidate = _projectOnSegment(position, i - 1);
      if (best == null || candidate.offsetMeters < best.offsetMeters) best = candidate;
    }
    return best ?? Projection(offsetMeters: distanceMeters(position, points.last), alongMeters: lengthMeters);
  }

  Projection _projectOnSegment(GeoPoint position, int start) {
    final a = points[start];
    final b = points[start + 1];
    // Metres east (x) and north (y) of a.
    final cosLat = math.cos(_radians(a.lat));
    double x(GeoPoint p) => _radians(p.lon - a.lon) * cosLat * _earthRadiusMeters;
    double y(GeoPoint p) => _radians(p.lat - a.lat) * _earthRadiusMeters;
    final bx = x(b);
    final by = y(b);
    final px = x(position);
    final py = y(position);
    final squared = bx * bx + by * by;
    final t = squared == 0 ? 0.0 : ((px * bx + py * by) / squared).clamp(0.0, 1.0);
    final dx = px - t * bx;
    final dy = py - t * by;
    final segment = _cumulative[start + 1] - _cumulative[start];
    return Projection(offsetMeters: math.sqrt(dx * dx + dy * dy), alongMeters: _cumulative[start] + t * segment);
  }
}
