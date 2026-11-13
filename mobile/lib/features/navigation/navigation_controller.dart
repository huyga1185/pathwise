import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../platform/location.dart';
import '../../shared/geo.dart';
import '../../shared/i18n/vi.dart';
import 'directions_source.dart';
import 'guidance.dart';

/// Why the screen cannot guide; each has its message and the way out ("Thử lại", Google/Apple Maps).
enum NavigationFailure {
  /// No position within [NavigationController.gpsWait].
  noGps,

  /// The server or the routing provider failed (502), or answered something the app cannot read.
  provider,
  network,
  rateLimited,

  /// The plan has finished or was cancelled (`PLAN_NOT_ACTIVE`).
  notActive,

  /// The route or the stop is gone (404).
  notFound,
}

sealed class NavigationState {
  const NavigationState();
}

/// Checking the location access, waiting for the first position, then for the first directions.
class NavigationStarting extends NavigationState {
  const NavigationStarting({required this.hasPosition});

  final bool hasPosition;
}

/// The phone does not let the app read the position: asking again, the settings, or GPS off.
class NavigationNeedsAccess extends NavigationState {
  const NavigationNeedsAccess(this.access);

  final LocationAccess access;
}

class NavigationGuiding extends NavigationState {
  const NavigationGuiding({
    required this.directions,
    required this.progress,
    required this.rerouting,
    this.position,
    this.headingDegrees,
  });

  final Directions directions;
  final Progress progress;

  /// New directions are being fetched from the current position; the old ones stay on screen meanwhile.
  final bool rerouting;
  final GeoPoint? position;
  final double? headingDegrees;
}

class NavigationFailed extends NavigationState {
  const NavigationFailed(this.failure);

  final NavigationFailure failure;
}

/// The stop a navigation drives to; [stopId] null for the first one not done yet.
typedef NavigationTarget = ({int routeId, int? stopId});

/// Turn-by-turn directions from the driver's position (M05, P-57): watches the position about once a second, asks the
/// server for directions from it, follows the driver along them, and asks again from wherever the driver is after
/// [Guidance.offRouteFixes] positions in a row off the route, at most once every [rerouteEvery]. Keeps the screen on
/// while it lives.
class NavigationController extends Notifier<NavigationState> {
  NavigationController(this.target);

  /// How long to wait for the first position before giving up.
  static const gpsWait = Duration(seconds: 15);

  /// Two requests for directions at least this far apart (the server allows 30 per 5 minutes, P-57).
  static const rerouteEvery = Duration(seconds: 20);

  final NavigationTarget target;

  StreamSubscription<LocationFix>? _fixes;
  Timer? _gpsTimer;
  Guidance? _guidance;
  LocationFix? _lastFix;
  DateTime? _lastRequest;
  int _offRouteCount = 0;
  bool _requesting = false;
  bool _disposed = false;

  @override
  NavigationState build() {
    // Read now: providers cannot be read while this one is being disposed.
    final screen = ref.read(screenAwakeProvider);
    ref.onDispose(() {
      _disposed = true;
      _stop();
      unawaited(screen.release());
    });
    unawaited(screen.keepOn());
    scheduleMicrotask(start);
    return const NavigationStarting(hasPosition: false);
  }

  LocationSource get _source => ref.read(locationSourceProvider);

  /// Starts over: access, first position, directions. Also "Thử lại" and the return from the phone's settings.
  Future<void> start() async {
    _stop();
    _guidance = null;
    _lastFix = null;
    _lastRequest = null;
    _offRouteCount = 0;
    _set(const NavigationStarting(hasPosition: false));
    var access = await _source.check();
    if (access == LocationAccess.denied) access = await _source.request();
    if (_disposed) return;
    if (access != LocationAccess.granted) return _set(NavigationNeedsAccess(access));
    _gpsTimer = Timer(gpsWait, () {
      if (_lastFix == null) {
        _stop();
        _set(const NavigationFailed(NavigationFailure.noGps));
      }
    });
    _fixes = _source
        .watch(title: Vi.navigationNotificationTitle, text: Vi.navigationNotificationText, precise: true)
        .listen(_onFix, onError: _onLocationError);
  }

  /// The phone's settings (permission refused for good) or its location settings (GPS off).
  Future<void> openSettings() async {
    final current = state;
    if (current is NavigationNeedsAccess && current.access == LocationAccess.serviceOff) {
      await _source.openLocationSettings();
    } else {
      await _source.openAppSettings();
    }
  }

  void _onFix(LocationFix fix) {
    final first = _lastFix == null;
    _lastFix = fix;
    final guidance = _guidance;
    final current = state;
    if (guidance == null || current is! NavigationGuiding) {
      if (first) {
        _gpsTimer?.cancel();
        _set(const NavigationStarting(hasPosition: true));
        unawaited(_request(fix));
      }
      return;
    }
    final position = GeoPoint(fix.lat, fix.lon);
    final progress = guidance.update(position, accuracyMeters: fix.accuracyMeters, previous: current.progress);
    _offRouteCount = progress.offRoute ? _offRouteCount + 1 : 0;
    _set(
      NavigationGuiding(
        directions: current.directions,
        progress: progress,
        rerouting: current.rerouting,
        position: position,
        headingDegrees: fix.headingDegrees ?? current.headingDegrees,
      ),
    );
    if (_offRouteCount >= Guidance.offRouteFixes && !progress.arrived && _mayRequest()) unawaited(_request(fix));
  }

  bool _mayRequest() {
    final last = _lastRequest;
    return !_requesting && (last == null || ref.read(clockProvider).now().difference(last) >= rerouteEvery);
  }

  Future<void> _request(LocationFix fix) async {
    _requesting = true;
    _lastRequest = ref.read(clockProvider).now();
    final current = state;
    if (current is NavigationGuiding) {
      _set(
        NavigationGuiding(
          directions: current.directions,
          progress: current.progress,
          rerouting: true,
          position: current.position,
          headingDegrees: current.headingDegrees,
        ),
      );
    }
    try {
      final directions = await ref
          .read(directionsSourceProvider)
          .directions(routeId: target.routeId, from: GeoPoint(fix.lat, fix.lon), toStopId: target.stopId);
      if (_disposed) return;
      final guidance = Guidance(directions);
      _guidance = guidance;
      _offRouteCount = 0;
      final latest = _lastFix ?? fix;
      final position = GeoPoint(latest.lat, latest.lon);
      _set(
        NavigationGuiding(
          directions: directions,
          progress: guidance.update(position, accuracyMeters: latest.accuracyMeters),
          rerouting: false,
          position: position,
          headingDegrees: latest.headingDegrees,
        ),
      );
    } on Object catch (error) {
      if (_disposed) return;
      final failure = _failureOf(error);
      final now = state;
      // Off the route with directions already on screen: keep them and try again later, unless the plan is over.
      if (now is NavigationGuiding && failure != NavigationFailure.notActive && failure != NavigationFailure.notFound) {
        _set(
          NavigationGuiding(
            directions: now.directions,
            progress: now.progress,
            rerouting: false,
            position: now.position,
            headingDegrees: now.headingDegrees,
          ),
        );
      } else {
        _stop();
        _set(NavigationFailed(failure));
      }
    } finally {
      _requesting = false;
    }
  }

  static NavigationFailure _failureOf(Object error) {
    if (error is! ApiError) return NavigationFailure.provider;
    if (error.code == ApiError.networkError) return NavigationFailure.network;
    if (error.code == 'PLAN_NOT_ACTIVE') return NavigationFailure.notActive;
    return switch (error.status) {
      404 => NavigationFailure.notFound,
      429 => NavigationFailure.rateLimited,
      _ => NavigationFailure.provider,
    };
  }

  void _onLocationError(Object error) {
    _stop();
    _set(
      error is LocationUnavailable
          ? NavigationNeedsAccess(error.access)
          : const NavigationFailed(NavigationFailure.noGps),
    );
  }

  void _stop() {
    _gpsTimer?.cancel();
    _gpsTimer = null;
    unawaited(_fixes?.cancel());
    _fixes = null;
  }

  void _set(NavigationState next) {
    if (!_disposed) state = next;
  }
}

final navigationProvider = NotifierProvider.autoDispose.family<NavigationController, NavigationState, NavigationTarget>(
  NavigationController.new,
);
