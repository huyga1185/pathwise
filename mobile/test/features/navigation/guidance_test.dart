import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/features/navigation/guidance.dart';
import 'package:pathwise_driver/shared/geo.dart';

import '../../support/navigation.dart';

void main() {
  // East along Lê Duẩn for about 1 km, left at the corner, north about 1.1 km to the stop.
  const from = GeoPoint(16.05, 108.2);
  const to = GeoPoint(16.06, 108.21);
  final guidance = Guidance(straightDirections(from, to, stopId: 116));
  const corner = GeoPoint(16.05, 108.21);

  test('lúc bắt đầu: thao tác tiếp theo là rẽ trái ở góc phố, còn cả quãng', () {
    final start = guidance.start;
    expect(start.nextStep, 1);
    expect(start.metersToNextStep, closeTo(distanceMeters(from, corner), 1));
    expect(start.remainingMeters, closeTo(guidance.path.lengthMeters, 0.1));
    expect(start.remainingSeconds, 180);
    expect(start.arrived, isFalse);
  });

  test('đi được nửa đoạn đầu: còn nửa đường tới chỗ rẽ, thời gian còn lại giảm theo', () {
    final progress = guidance.update(const GeoPoint(16.05, 108.205), accuracyMeters: 5);
    expect(progress.nextStep, 1);
    expect(progress.metersToNextStep, closeTo(distanceMeters(from, corner) / 2, 2));
    expect(progress.remainingSeconds, lessThan(180));
    expect(progress.offRoute, isFalse);
  });

  test('đã qua góc phố: thao tác tiếp theo là tới nơi', () {
    final progress = guidance.update(const GeoPoint(16.052, 108.21), accuracyMeters: 5);
    expect(progress.nextStep, 2);
  });

  test('lệch quá 50 m là lệch đường; GPS kém chính xác thì nới ngưỡng, quá kém thì không tính', () {
    // About 110 m north of the street.
    const off = GeoPoint(16.051, 108.203);
    expect(guidance.update(off, accuracyMeters: 10).offRoute, isTrue);
    expect(guidance.update(off, accuracyMeters: 120).offRoute, isFalse, reason: 'worse than 100 m says nothing');
    // About 60 m north: off with a precise fix, on the route when the fix is only good to 80 m.
    const near = GeoPoint(16.05054, 108.203);
    expect(guidance.update(near, accuracyMeters: 5).offRoute, isTrue);
    expect(guidance.update(near, accuracyMeters: 80).offRoute, isFalse);
  });

  test('cách đích dưới 40 m là tới nơi, và giữ nguyên khi GPS nhảy ra xa', () {
    final arrived = guidance.update(const GeoPoint(16.0598, 108.21), accuracyMeters: 5);
    expect(arrived.arrived, isTrue);
    expect(arrived.offRoute, isFalse);
    expect(guidance.update(const GeoPoint(16.0595, 108.21), previous: arrived).arrived, isTrue);
  });
}
