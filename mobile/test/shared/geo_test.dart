import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/shared/geo.dart';

void main() {
  test('giải mã polyline độ chính xác 6 (chuỗi mẫu của Google, giá trị chia 10)', () {
    // The documented example decodes to (38.5, -120.2), (40.7, -120.95), (43.252, -126.453) with 5 decimals.
    final points = decodePolyline6(r'_p~iF~ps|U_ulLnnqC_mqNvxq`@');

    expect(points, hasLength(3));
    expect(points[0].lat, closeTo(3.85, 1e-9));
    expect(points[0].lon, closeTo(-12.02, 1e-9));
    expect(points[1].lat, closeTo(4.07, 1e-9));
    expect(points[1].lon, closeTo(-12.095, 1e-9));
    expect(points[2].lat, closeTo(4.3252, 1e-9));
    expect(points[2].lon, closeTo(-12.6453, 1e-9));
  });

  test('polyline rỗng thì không có điểm, bị cắt ngang thì báo lỗi', () {
    expect(decodePolyline6(''), isEmpty);
    expect(() => decodePolyline6('_p~iF~ps'), throwsFormatException);
  });

  test('khoảng cách: 0,001 độ vĩ khoảng 111 m', () {
    expect(distanceMeters(const GeoPoint(16.05, 108.2), const GeoPoint(16.051, 108.2)), closeTo(111.2, 0.2));
  });

  test('chiếu vị trí lên đường: lệch bao xa và đã đi được bao nhiêu', () {
    final path = PolylinePath(const [GeoPoint(16.05, 108.2), GeoPoint(16.05, 108.21), GeoPoint(16.06, 108.21)]);
    final east = distanceMeters(const GeoPoint(16.05, 108.2), const GeoPoint(16.05, 108.21));

    final onFirstLeg = path.project(const GeoPoint(16.0505, 108.205));
    expect(onFirstLeg.offsetMeters, closeTo(55.6, 0.5));
    expect(onFirstLeg.alongMeters, closeTo(east / 2, 1));

    final onSecondLeg = path.project(const GeoPoint(16.055, 108.21));
    expect(onSecondLeg.offsetMeters, closeTo(0, 0.5));
    expect(onSecondLeg.alongMeters, closeTo(east + 556, 2));
    expect(path.lengthMeters, closeTo(east + 1112, 3));
  });

  test('chiếu không lùi về đoạn đã qua khi đường quay lại gần chính nó', () {
    // Out east and back west on the same street: the start and the end are the same place.
    final path = PolylinePath(const [GeoPoint(16.05, 108.2), GeoPoint(16.05, 108.21), GeoPoint(16.0501, 108.2)]);
    final back = path.project(const GeoPoint(16.05005, 108.201), notBeforeMeters: path.lengthMeters - 200);
    expect(back.alongMeters, greaterThan(path.lengthMeters - 200));
  });
}
