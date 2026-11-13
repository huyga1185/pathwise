import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../platform/location.dart';
import '../../platform/map/navigation_map.dart';
import '../../shared/errors/errors.dart';
import '../../shared/format.dart';
import '../../shared/geo.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import '../driver_routes/directions.dart';
import '../driver_routes/routes_data.dart';
import 'guidance.dart';
import 'navigation_controller.dart';

/// Tài xế — dẫn đường (M05, P-57, SCREENS §2): turn-by-turn directions from the driver's position to [stopId], or to
/// the first stop not done yet. Google/Apple Maps stays one tap away (D-07), and is the way out of every failure.
class NavigationScreen extends ConsumerStatefulWidget {
  const NavigationScreen({required this.routeId, this.stopId, super.key});

  final int routeId;
  final int? stopId;

  @override
  ConsumerState<NavigationScreen> createState() => _NavigationScreenState();
}

class _NavigationScreenState extends ConsumerState<NavigationScreen> {
  late final AppLifecycleListener _lifecycle;
  bool _follow = true;

  NavigationTarget get _target => (routeId: widget.routeId, stopId: widget.stopId);

  @override
  void initState() {
    super.initState();
    // Back from the phone's settings: try again with the new permission or GPS.
    _lifecycle = AppLifecycleListener(
      onResume: () {
        if (ref.read(navigationProvider(_target)) is NavigationNeedsAccess) {
          unawaited(ref.read(navigationProvider(_target).notifier).start());
        }
      },
    );
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  Future<void> _openExternal(GeoPoint destination) async {
    final uri = directionsUri(destination.lat, destination.lon, Theme.of(context).platform);
    final opened = await ref.read(launcherProvider).open(uri);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text(Vi.cannotOpen)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final state = ref.watch(navigationProvider(_target));
    final route = ref.watch(driverRouteProvider(widget.routeId)).value;
    final fallback = _fallbackDestination(route, widget.stopId);

    final Widget body = switch (state) {
      NavigationGuiding() => _guiding(state, route),
      NavigationStarting(:final hasPosition) => _message(
        busy: true,
        title: hasPosition ? Vi.findingRoute : Vi.waitingForGps,
        fallback: fallback,
      ),
      NavigationNeedsAccess(:final access) => _needsAccess(access, fallback),
      NavigationFailed(:final failure) => _failed(failure, fallback),
    };

    return Scaffold(
      backgroundColor: t.surface,
      body: Stack(
        children: [
          Positioned.fill(child: body),
          // Positioned: a plain child would size the Stack, and so the whole screen, to this button.
          Positioned(
            top: 0,
            left: 0,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(PwSpacing.space2),
                child: Material(
                  color: t.surfaceRaised,
                  shape: const CircleBorder(),
                  elevation: 2,
                  child: IconButton(
                    tooltip: Vi.backToRoute,
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () => context.canPop() ? context.pop() : context.go('/routes/${widget.routeId}'),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _guiding(NavigationGuiding state, DriverRouteDetailResponse? route) {
    final t = context.pw;
    final directions = state.directions;
    final progress = state.progress;
    final place = _placeName(route, directions.destinationStopId);
    final steps = directions.steps;
    final next = steps.isEmpty ? null : steps[progress.nextStep];
    final after = progress.nextStep + 1 < steps.length ? steps[progress.nextStep + 1] : null;
    final arrival = ref.read(clockProvider).now().add(Duration(seconds: progress.remainingSeconds));
    final stopId = directions.destinationStopId;
    final basemap = ref.watch(basemapProvider);

    return Stack(
      children: [
        Positioned.fill(
          child: NavigationMap(
            basemap: basemap,
            path: directions.path,
            destination: directions.destination,
            position: state.position,
            headingDegrees: state.headingDegrees,
            follow: _follow,
            onUserMoved: () {
              if (_follow) setState(() => _follow = false);
            },
          ),
        ),
        // Top: the maneuver ahead.
        Positioned(
          left: 0,
          right: 0,
          top: 0,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(64, PwSpacing.space2, PwSpacing.space2, 0),
              child: Semantics(
                liveRegion: true,
                child: _Panel(
                  color: t.route,
                  child: progress.arrived
                      ? _Instruction(icon: Icons.flag, text: Vi.arrivedAt(place), color: t.onAccent)
                      : next == null
                      ? const SizedBox.shrink()
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            _Instruction(
                              icon: _icon(next.maneuver),
                              text: _text(next, place),
                              distance: formatDistance(progress.metersToNextStep),
                              color: t.onAccent,
                            ),
                            if (after != null && !state.rerouting) ...[
                              const SizedBox(height: PwSpacing.space1),
                              Text(
                                Vi.thenStep(_text(after, place)),
                                style: PwTypeStyles.body.copyWith(color: t.onAccent),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                            if (state.rerouting) ...[
                              const SizedBox(height: PwSpacing.space1),
                              Text(Vi.rerouting, style: PwTypeStyles.body.copyWith(color: t.onAccent)),
                            ],
                          ],
                        ),
                ),
              ),
            ),
          ),
        ),
        // Bottom: what is left, and the way out.
        Positioned(
          left: 0,
          right: 0,
          bottom: 0,
          child: SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.all(PwSpacing.space2),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  if (!_follow) ...[
                    PwButton(
                      label: Vi.recenter,
                      icon: Icons.my_location,
                      onPressed: () => setState(() => _follow = true),
                    ),
                    const SizedBox(height: PwSpacing.space2),
                  ],
                  _Panel(
                    color: t.surfaceRaised,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(place, style: PwTypeStyles.heading.copyWith(color: t.ink)),
                        const SizedBox(height: PwSpacing.space1),
                        Text(
                          Vi.remaining(
                            formatDistance(progress.remainingMeters),
                            formatDuration(progress.remainingSeconds),
                            formatTime(arrival, route?.timezone ?? 'Asia/Ho_Chi_Minh'),
                          ),
                          style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted),
                        ),
                        const SizedBox(height: PwSpacing.space2),
                        Row(
                          children: [
                            Expanded(
                              child: PwButton(
                                label: Vi.openExternalMaps(_apple),
                                block: true,
                                onPressed: () => unawaited(_openExternal(directions.destination)),
                              ),
                            ),
                            if (stopId != null) ...[
                              const SizedBox(width: PwSpacing.space2),
                              Expanded(
                                child: PwButton(
                                  label: Vi.report,
                                  block: true,
                                  variant: progress.arrived ? PwButtonVariant.primary : PwButtonVariant.secondary,
                                  // The report screen with the route detail under it, whatever led here.
                                  onPressed: () => context.go('/routes/${widget.routeId}/stops/$stopId'),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  bool get _apple => switch (Theme.of(context).platform) {
    TargetPlatform.iOS || TargetPlatform.macOS => true,
    _ => false,
  };

  Widget _needsAccess(LocationAccess access, GeoPoint? fallback) {
    final controller = ref.read(navigationProvider(_target).notifier);
    return switch (access) {
      LocationAccess.serviceOff => _message(
        title: Vi.navigationGpsOff,
        action: PwButton(label: Vi.gpsTurnOn, block: true, onPressed: () => unawaited(controller.openSettings())),
        fallback: fallback,
      ),
      LocationAccess.deniedForever => _message(
        title: Vi.navigationPermissionForever,
        action: PwButton(label: Vi.openSettings, block: true, onPressed: () => unawaited(controller.openSettings())),
        fallback: fallback,
      ),
      _ => _message(
        title: Vi.navigationNeedsPermission,
        action: PwButton(label: Vi.allowLocation, block: true, onPressed: () => unawaited(controller.start())),
        fallback: fallback,
      ),
    };
  }

  Widget _failed(NavigationFailure failure, GeoPoint? fallback) {
    final back = PwButton(label: Vi.backToRoutes, block: true, onPressed: () => context.go('/routes'));
    final retry = PwButton(
      label: Vi.retry,
      block: true,
      onPressed: () => unawaited(ref.read(navigationProvider(_target).notifier).start()),
    );
    return switch (failure) {
      NavigationFailure.notActive => _message(
        title: messageFor(const ApiError(code: 'PLAN_NOT_ACTIVE', status: 422)),
        detail: Vi.stopDelivering,
        action: back,
      ),
      NavigationFailure.notFound => _message(title: Vi.routeGone, detail: Vi.routeGoneDetail, action: back),
      NavigationFailure.noGps => _message(title: Vi.navigationNoGps, action: retry, fallback: fallback),
      NavigationFailure.rateLimited => _message(title: Vi.navigationRateLimited, action: retry, fallback: fallback),
      NavigationFailure.network => _message(
        title: Vi.navigationFailed,
        detail: messageFor(const ApiError(code: ApiError.networkError)),
        action: retry,
        fallback: fallback,
      ),
      NavigationFailure.provider => _message(title: Vi.navigationFailed, action: retry, fallback: fallback),
    };
  }

  /// A full screen message: [title], [detail], [action], and Google/Apple Maps to [fallback] when known.
  Widget _message({required String title, String? detail, Widget? action, GeoPoint? fallback, bool busy = false}) {
    final t = context.pw;
    return SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(PwSpacing.space4, 64, PwSpacing.space4, PwSpacing.space4),
          // Full width: inside Center and a scroll view the column would otherwise shrink to its narrowest child.
          child: SizedBox(
            width: double.infinity,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (busy) ...[
                  const Center(child: CircularProgressIndicator()),
                  const SizedBox(height: PwSpacing.space4),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      title,
                      textAlign: TextAlign.center,
                      style: PwTypeStyles.heading.copyWith(color: t.ink),
                    ),
                  ),
                ] else
                  ErrorBanner(title: title, detail: detail),
                if (action != null) ...[const SizedBox(height: PwSpacing.space4), action],
                if (fallback != null) ...[
                  const SizedBox(height: PwSpacing.space2),
                  PwButton(
                    label: Vi.openExternalMaps(_apple),
                    block: true,
                    onPressed: () => unawaited(_openExternal(fallback)),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Where Google/Apple Maps should go while there are no directions: the stop asked for, else the first stop not
  /// done, else the end of the route.
  static GeoPoint? _fallbackDestination(DriverRouteDetailResponse? route, int? stopId) {
    if (route == null) return null;
    final stop = stopId != null
        ? route.stops.where((candidate) => candidate.id == stopId).firstOrNull
        : route.stops.where((candidate) => candidate.status == DriverStopResponseStatusEnum.PENDING).firstOrNull;
    if (stop != null) return GeoPoint(stop.lat, stop.lon);
    return GeoPoint(route.end.lat, route.end.lon);
  }

  /// "ĐH-1024 · Chị Lan" for a stop, "Điểm kết thúc tuyến" for the end of the route.
  static String _placeName(DriverRouteDetailResponse? route, int? stopId) {
    if (stopId == null) return Vi.routeEnd;
    final stop = route?.stops.where((candidate) => candidate.id == stopId).firstOrNull;
    if (stop == null) return Vi.nextStop;
    return [stop.label, ?stop.recipientName].join(' · ');
  }

  static String _text(DirectionsStep step, String place) =>
      Vi.instruction(step.maneuver.name, road: step.roadName, exit: step.exitNumber, place: place);

  static IconData _icon(Maneuver maneuver) => switch (maneuver) {
    Maneuver.depart => Icons.navigation,
    Maneuver.arrive => Icons.flag,
    Maneuver.straight => Icons.straight,
    Maneuver.slightLeft => Icons.turn_slight_left,
    Maneuver.slightRight => Icons.turn_slight_right,
    Maneuver.left => Icons.turn_left,
    Maneuver.right => Icons.turn_right,
    Maneuver.sharpLeft => Icons.turn_sharp_left,
    Maneuver.sharpRight => Icons.turn_sharp_right,
    Maneuver.uturn => Icons.u_turn_left,
    Maneuver.keepLeft => Icons.fork_left,
    Maneuver.keepRight => Icons.fork_right,
    Maneuver.roundabout => Icons.roundabout_right,
  };
}

class _Panel extends StatelessWidget {
  const _Panel({required this.color, required this.child});

  final Color color;
  final Widget child;

  @override
  Widget build(BuildContext context) => Material(
    color: color,
    elevation: 3,
    borderRadius: BorderRadius.circular(PwRadius.radiusLg),
    child: Padding(padding: const EdgeInsets.all(PwSpacing.space4), child: child),
  );
}

class _Instruction extends StatelessWidget {
  const _Instruction({required this.icon, required this.text, required this.color, this.distance});

  final IconData icon;
  final String text;
  final String? distance;
  final Color color;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Icon(icon, size: 40, color: color),
      const SizedBox(width: PwSpacing.space2),
      Expanded(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (distance != null) Text(distance!, style: PwTypeStyles.display.copyWith(color: color)),
            Text(text, style: PwTypeStyles.title.copyWith(color: color)),
          ],
        ),
      ),
    ],
  );
}
