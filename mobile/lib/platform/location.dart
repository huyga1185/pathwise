import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';

/// Whether the app can read the phone's position (P-54).
enum LocationAccess {
  granted,

  /// Not allowed yet; asking again shows the system dialog.
  denied,

  /// Refused for good ("Don't ask again", or iOS after one refusal): only the phone's settings can change it.
  deniedForever,

  /// Location (GPS) is turned off on the phone.
  serviceOff,
}

/// One position of the phone. Heading in degrees clockwise from north, speed in m/s, accuracy in metres.
class LocationFix {
  const LocationFix({
    required this.lat,
    required this.lon,
    required this.recordedAt,
    this.accuracyMeters,
    this.headingDegrees,
    this.speedMps,
  });

  final double lat;
  final double lon;
  final DateTime recordedAt;
  final double? accuracyMeters;
  final double? headingDegrees;
  final double? speedMps;
}

/// The stream of positions stopped because the access changed (GPS turned off, permission taken back).
class LocationUnavailable implements Exception {
  const LocationUnavailable(this.access);

  final LocationAccess access;
}

/// Location port (ARCHITECTURE §2.1): permission, GPS on/off, the phone's settings, and positions that keep coming
/// while the app is in the background (Android: a foreground service with a notification that stays). Tests use a
/// fake; only this file knows `geolocator` (P-54). Several screens can watch at once (sharing, M04; turn-by-turn
/// directions, M05): each gets every position.
abstract interface class LocationSource {
  /// The current access, without asking.
  Future<LocationAccess> check();

  /// Shows the system dialog when the permission can still be asked; the access afterwards.
  Future<LocationAccess> request();

  /// Positions until the subscription is cancelled; errors with [LocationUnavailable]. [title] and [text] are the
  /// Android notification shown while it runs. [precise]: about one position a second, for turn-by-turn directions;
  /// otherwise one every 15 s or 10 m, enough for sharing and lighter on the battery.
  Stream<LocationFix> watch({required String title, required String text, bool precise = false});

  /// The app's page in the phone's settings (permission refused for good).
  Future<bool> openAppSettings();

  /// The phone's location settings (GPS off).
  Future<bool> openLocationSettings();
}

/// Positions with one settings: [precise] or not, and the Android notification.
typedef PositionStreamFactory = Stream<LocationFix> Function({
  required bool precise,
  required String title,
  required String text,
});

/// `geolocator` keeps one stream of positions per app on Android: asking for a second one returns the first, with its
/// settings. So every watcher listens to one shared stream, opened with the finest settings any watcher needs (and the
/// notification of a sharing watcher if there is one), and reopened when that changes.
class GeolocatorLocationSource implements LocationSource {
  GeolocatorLocationSource({PositionStreamFactory? positions}) : _positions = positions ?? _geolocatorPositions;

  final PositionStreamFactory _positions;
  final _watchers = <_Watcher>[];
  StreamSubscription<LocationFix>? _subscription;
  ({bool precise, String title, String text})? _settings;

  /// Sharing: one point at least every 15 s, or every 10 m, whichever comes first (FR-D04: sent every 15–30 s).
  static const _interval = Duration(seconds: 15);
  static const _distanceMeters = 10;

  /// Directions: about one point a second.
  static const _preciseInterval = Duration(seconds: 1);
  static const _preciseDistanceMeters = 2;

  /// Slower than this (about 2 km/h) the driver is standing: no direction.
  static const _movingMps = 0.5;

  @override
  Future<LocationAccess> check() async {
    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.deniedForever) return LocationAccess.deniedForever;
    if (permission != LocationPermission.whileInUse && permission != LocationPermission.always) {
      return LocationAccess.denied;
    }
    return await Geolocator.isLocationServiceEnabled() ? LocationAccess.granted : LocationAccess.serviceOff;
  }

  @override
  Future<LocationAccess> request() async {
    if (await check() != LocationAccess.denied) return check();
    await Geolocator.requestPermission();
    return check();
  }

  @override
  Stream<LocationFix> watch({required String title, required String text, bool precise = false}) {
    late final _Watcher watcher;
    final controller = StreamController<LocationFix>(
      onListen: () {
        _watchers.add(watcher);
        _reopen();
      },
      onCancel: () {
        _watchers.remove(watcher);
        _reopen();
      },
    );
    watcher = _Watcher(controller, precise: precise, title: title, text: text);
    return controller.stream;
  }

  /// Opens, reopens or closes the shared stream for the current watchers.
  void _reopen() {
    if (_watchers.isEmpty) {
      unawaited(_subscription?.cancel());
      _subscription = null;
      _settings = null;
      return;
    }
    final notification = _watchers.firstWhere((watcher) => !watcher.precise, orElse: () => _watchers.first);
    final settings = (
      precise: _watchers.any((watcher) => watcher.precise),
      title: notification.title,
      text: notification.text,
    );
    if (_subscription != null && settings == _settings) return;
    // Cancelling makes geolocator drop its cached stream, so the next one gets the new settings.
    unawaited(_subscription?.cancel());
    _settings = settings;
    _subscription = _positions(precise: settings.precise, title: settings.title, text: settings.text).listen(
      (fix) {
        for (final watcher in List.of(_watchers)) {
          watcher.controller.add(fix);
        }
      },
      onError: (Object error, StackTrace stackTrace) {
        for (final watcher in List.of(_watchers)) {
          watcher.controller.addError(error, stackTrace);
        }
      },
    );
  }

  static Stream<LocationFix> _geolocatorPositions({
    required bool precise,
    required String title,
    required String text,
  }) {
    final distance = precise ? _preciseDistanceMeters : _distanceMeters;
    final LocationSettings settings = switch (defaultTargetPlatform) {
      TargetPlatform.android => AndroidSettings(
        distanceFilter: distance,
        intervalDuration: precise ? _preciseInterval : _interval,
        foregroundNotificationConfig: ForegroundNotificationConfig(
          notificationTitle: title,
          notificationText: text,
          notificationIcon: const AndroidResource(name: 'ic_launcher', defType: 'mipmap'),
          enableWakeLock: true,
          setOngoing: true,
        ),
      ),
      TargetPlatform.iOS || TargetPlatform.macOS => AppleSettings(
        accuracy: precise ? LocationAccuracy.bestForNavigation : LocationAccuracy.best,
        distanceFilter: distance,
        activityType: ActivityType.automotiveNavigation,
        pauseLocationUpdatesAutomatically: false,
        showBackgroundLocationIndicator: true,
        allowBackgroundLocationUpdates: true,
      ),
      _ => LocationSettings(distanceFilter: distance),
    };
    return Geolocator.getPositionStream(locationSettings: settings)
        .handleError(
          (Object error) => throw LocationUnavailable(
            error is LocationServiceDisabledException ? LocationAccess.serviceOff : LocationAccess.denied,
          ),
          test: (error) => error is LocationServiceDisabledException || error is PermissionDeniedException,
        )
        .map(
          (position) => LocationFix(
            lat: position.latitude,
            lon: position.longitude,
            recordedAt: position.timestamp.toUtc(),
            accuracyMeters: position.accuracy < 0 ? null : position.accuracy,
            // -1 when unknown (iOS); standing still, the platforms report 0 or the last bearing, which says nothing.
            headingDegrees: position.heading < 0 || (position.speed >= 0 && position.speed < _movingMps)
                ? null
                : position.heading % 360,
            speedMps: position.speed < 0 ? null : position.speed,
          ),
        );
  }

  @override
  Future<bool> openAppSettings() => Geolocator.openAppSettings();

  @override
  Future<bool> openLocationSettings() => Geolocator.openLocationSettings();
}

class _Watcher {
  _Watcher(this.controller, {required this.precise, required this.title, required this.text});

  final StreamController<LocationFix> controller;
  final bool precise;
  final String title;
  final String text;
}
