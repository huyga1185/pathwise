import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/features/driver_routes/directions.dart';

void main() {
  test('chỉ đường mở Google Maps trên Android và Apple Maps trên iOS theo toạ độ (D-07, UX_BRIEF §4.2)', () {
    expect(
      directionsUri(16.048, 108.196, TargetPlatform.android).toString(),
      'https://www.google.com/maps/dir/?api=1&destination=16.048,108.196',
    );
    expect(
      directionsUri(16.048, 108.196, TargetPlatform.iOS).toString(),
      'https://maps.apple.com/?daddr=16.048,108.196',
    );
  });

  test('gọi điện bằng số E.164 của API', () {
    expect(callUri('+84905123008').toString(), 'tel:+84905123008');
  });
}
