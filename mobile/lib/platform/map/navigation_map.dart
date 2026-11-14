import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../shared/geo.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import 'basemap.dart';

/// The map of turn-by-turn directions (M05, P-57): the path of the drive, the destination and the driver's position
/// (an arrow toward [headingDegrees]). Only `platform/map` knows `flutter_map`/`latlong2` (P-45).
///
/// While [follow] is on the map stays centred on [position]; dragging it calls [onUserMoved] (the screen then shows
/// "Về vị trí của tôi" and turns [follow] back on).
class NavigationMap extends StatefulWidget {
  const NavigationMap({
    required this.basemap,
    required this.path,
    required this.destination,
    required this.follow,
    required this.onUserMoved,
    this.position,
    this.headingDegrees,
    super.key,
  });

  /// Zoom while following: a few streets around the driver.
  static const followZoom = 17.0;

  final Basemap basemap;
  final List<GeoPoint> path;
  final GeoPoint destination;
  final GeoPoint? position;
  final double? headingDegrees;
  final bool follow;
  final VoidCallback onUserMoved;

  @override
  State<NavigationMap> createState() => _NavigationMapState();
}

class _NavigationMapState extends State<NavigationMap> {
  final _controller = MapController();
  bool _ready = false;

  @override
  void didUpdateWidget(NavigationMap old) {
    super.didUpdateWidget(old);
    final position = widget.position;
    if (!_ready || !widget.follow || position == null) return;
    if (position != old.position || !old.follow) _controller.move(_latLng(position), NavigationMap.followZoom);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final basemap = widget.basemap;
    final start = widget.position ?? (widget.path.isEmpty ? widget.destination : widget.path.first);
    return FlutterMap(
      mapController: _controller,
      options: MapOptions(
        initialCenter: _latLng(start),
        initialZoom: NavigationMap.followZoom,
        maxZoom: basemap.maxZoom.toDouble(),
        backgroundColor: t.surfaceTint,
        // North up: the arrow turns, not the map.
        interactionOptions: const InteractionOptions(flags: InteractiveFlag.all & ~InteractiveFlag.rotate),
        onMapReady: () => _ready = true,
        onPositionChanged: (camera, hasGesture) {
          if (hasGesture) widget.onUserMoved();
        },
      ),
      children: [
        if (basemap.tileUrl.isNotEmpty)
          TileLayer(
            urlTemplate: basemap.tileUrl,
            tileDimension: basemap.tileSize,
            // 512-pixel tiles cover one zoom level more.
            zoomOffset: basemap.tileSize == 512 ? -1 : 0,
            maxZoom: basemap.maxZoom.toDouble(),
            userAgentPackageName: 'uk.huy.pathwise.pathwise_driver',
          ),
        PolylineLayer(
          polylines: [
            Polyline(
              points: [for (final point in widget.path) _latLng(point)],
              strokeWidth: 6,
              color: t.mapRoute1,
              borderStrokeWidth: 2,
              borderColor: Colors.white,
            ),
          ],
        ),
        MarkerLayer(
          markers: [
            Marker(
              point: _latLng(widget.destination),
              width: 36,
              height: 36,
              alignment: Alignment.topCenter,
              child: Icon(Icons.location_on, size: 36, color: t.danger),
            ),
            if (widget.position case final position?)
              Marker(point: _latLng(position), width: 40, height: 40, child: _PositionArrow(widget.headingDegrees)),
          ],
        ),
        SimpleAttributionWidget(
          source: Text(basemap.attribution, style: PwTypeStyles.small.copyWith(color: Colors.black87)),
          backgroundColor: Colors.white70,
        ),
      ],
    );
  }
}

/// The driver: an arrow toward the heading, a dot when the heading is unknown (standing).
class _PositionArrow extends StatelessWidget {
  const _PositionArrow(this.headingDegrees);

  final double? headingDegrees;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final heading = headingDegrees;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.info,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white, width: 3),
        boxShadow: const [BoxShadow(color: Colors.black26, blurRadius: 4)],
      ),
      child: heading == null
          ? const SizedBox.expand()
          : Transform.rotate(
              angle: heading * math.pi / 180,
              child: const Icon(Icons.navigation, color: Colors.white, size: 22),
            ),
    );
  }
}

LatLng _latLng(GeoPoint point) => LatLng(point.lat, point.lon);
