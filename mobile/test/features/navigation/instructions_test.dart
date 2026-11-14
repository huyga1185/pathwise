import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/features/navigation/guidance.dart';
import 'package:pathwise_driver/shared/format.dart';
import 'package:pathwise_driver/shared/i18n/vi.dart';

void main() {
  test('câu chỉ dẫn cho mọi thao tác, có và không có tên đường', () {
    const withRoad = {
      Maneuver.depart: 'Đi theo Lê Duẩn',
      Maneuver.arrive: 'Tới ĐH-1009 · Chị Lan',
      Maneuver.straight: 'Đi thẳng vào Lê Duẩn',
      Maneuver.slightLeft: 'Chếch sang trái vào Lê Duẩn',
      Maneuver.slightRight: 'Chếch sang phải vào Lê Duẩn',
      Maneuver.left: 'Rẽ trái vào Lê Duẩn',
      Maneuver.right: 'Rẽ phải vào Lê Duẩn',
      Maneuver.sharpLeft: 'Rẽ gắt sang trái vào Lê Duẩn',
      Maneuver.sharpRight: 'Rẽ gắt sang phải vào Lê Duẩn',
      Maneuver.uturn: 'Quay đầu vào Lê Duẩn',
      Maneuver.keepLeft: 'Đi về bên trái vào Lê Duẩn',
      Maneuver.keepRight: 'Đi về bên phải vào Lê Duẩn',
      Maneuver.roundabout: 'Vào vòng xoay, ra lối thứ 2 vào Lê Duẩn',
    };
    for (final MapEntry(key: maneuver, value: text) in withRoad.entries) {
      expect(Vi.instruction(maneuver.name, road: 'Lê Duẩn', exit: 2, place: 'ĐH-1009 · Chị Lan'), text);
    }
    expect(Maneuver.values, hasLength(withRoad.length), reason: 'every maneuver has a sentence');

    expect(Vi.instruction(Maneuver.depart.name), 'Bắt đầu đi');
    expect(Vi.instruction(Maneuver.left.name), 'Rẽ trái');
    expect(Vi.instruction(Maneuver.roundabout.name), 'Vào vòng xoay');
    expect(Vi.instruction(Maneuver.arrive.name), 'Tới nơi');
  });

  test('khoảng cách: làm tròn 10 m dưới 1 km, km có dấu phẩy', () {
    expect(formatDistance(0), '0 m');
    expect(formatDistance(84), '80 m');
    expect(formatDistance(996), '1000 m');
    expect(formatDistance(1234), '1,2 km');
    expect(formatDistance(12600), '13 km');
  });

  test('thời gian còn lại: ít nhất 1 phút, giờ và phút', () {
    expect(formatDuration(0), '1 phút');
    expect(formatDuration(61), '2 phút');
    expect(formatDuration(3600), '1 giờ');
    expect(formatDuration(4800), '1 giờ 20 phút');
  });
}
