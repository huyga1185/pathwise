import 'package:flutter/foundation.dart';

/// "Chỉ đường" (D-07, UX_BRIEF §4.2): Apple Maps on iOS, Google Maps elsewhere, to the stop's coordinates. The
/// address is not used: coordinates are what the route was planned on, and a stop may have no address.
Uri directionsUri(double lat, double lon, TargetPlatform platform) {
  return switch (platform) {
    TargetPlatform.iOS || TargetPlatform.macOS => Uri.parse('https://maps.apple.com/?daddr=$lat,$lon'),
    _ => Uri.parse('https://www.google.com/maps/dir/?api=1&destination=$lat,$lon'),
  };
}

/// The phone dialer for an E.164 number.
Uri callUri(String e164) => Uri(scheme: 'tel', path: e164);
