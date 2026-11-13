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
/// fake; only this file knows `geolocator` (P-54).
abstract interface class LocationSource {
  /// The current access, without asking.
  Future<LocationAccess> check();

  /// Shows the system dialog when the permission can still be asked; the access afterwards.
  Future<LocationAccess> request();

  /// Positions until the subscription is cancelled; errors with [LocationUnavailable]. [title] and [text] are the
  /// Android notification shown while it runs.
  Stream<LocationFix> watch({required String title, required String text});

  /// The app's page in the phone's settings (permission refused for good).
  Future<bool> openAppSettings();

  /// The phone's location settings (GPS off).
  Future<bool> openLocationSettings();
}

class GeolocatorLocationSource implements LocationSource {
  const GeolocatorLocationSource();

  /// One point at least every 15 s, or every 10 m, whichever comes first (FR-D04: sent every 15–30 s).
  static const _interval = Duration(seconds: 15);
  static const _distanceMeters = 10;

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
  Stream<LocationFix> watch({required String title, required String text}) {
    final LocationSettings settings = switch (defaultTargetPlatform) {
      TargetPlatform.android => AndroidSettings(
        distanceFilter: _distanceMeters,
        intervalDuration: _interval,
        foregroundNotificationConfig: ForegroundNotificationConfig(
          notificationTitle: title,
          notificationText: text,
          notificationIcon: const AndroidResource(name: 'ic_launcher', defType: 'mipmap'),
          enableWakeLock: true,
          setOngoing: true,
        ),
      ),
      TargetPlatform.iOS || TargetPlatform.macOS => AppleSettings(
        distanceFilter: _distanceMeters,
        activityType: ActivityType.automotiveNavigation,
        pauseLocationUpdatesAutomatically: false,
        showBackgroundLocationIndicator: true,
        allowBackgroundLocationUpdates: true,
      ),
      _ => const LocationSettings(distanceFilter: _distanceMeters),
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
