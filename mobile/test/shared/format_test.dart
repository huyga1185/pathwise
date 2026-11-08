import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/shared/format.dart';

void main() {
  test('giờ hiện theo timezone của plan khi có timezone, kể cả khác giờ máy', () {
    final start = DateTime.utc(2026, 12, 1, 3, 38, 22);
    expect(formatTime(start, 'Asia/Ho_Chi_Minh'), '10:38');
    expect(formatTime(start, 'Asia/Tokyo'), '12:38');
    expect(formatTime(start, 'Không/CóThật'), '03:38');
    expect(formatTime(null, 'Asia/Ho_Chi_Minh'), '—');
  });

  test('ngày theo timezone khi giờ UTC đã sang ngày khác', () {
    final lateEvening = DateTime.utc(2026, 11, 30, 18);
    expect(dayIn(lateEvening, 'Asia/Ho_Chi_Minh'), '2026-12-01');
    expect(dayIn(lateEvening, 'UTC'), '2026-11-30');
  });

  test('đổi ngày qua tháng, khoảng cách ngày và tên ngày giống web', () {
    expect(shiftDay('2026-11-30', 1), '2026-12-01');
    expect(shiftDay('2027-01-01', -1), '2026-12-31');
    expect(daysBetween('2026-12-01', '2026-11-30'), -1);
    expect(daysBetween('2026-12-01', '2026-12-08'), 7);
    expect(formatDateLong('2026-12-01'), 'Thứ Ba, 01/12/2026');
    expect(formatDateLong('2026-12-06'), 'Chủ nhật, 06/12/2026');
    expect(formatTimeWindow('09:00', '11:00'), '09:00–11:00');
  });
}
