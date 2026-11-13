import 'package:timezone/timezone.dart' as tz;

/// "+84905123456" → "0905 123 456" (UX_BRIEF §2); anything else unchanged.
String formatPhone(String? e164) {
  if (e164 == null || e164.isEmpty) return '';
  final match = RegExp(r'^\+84(\d{9})$').firstMatch(e164);
  if (match == null) return e164;
  final digits = '0${match.group(1)}';
  return '${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}';
}

/// An instant as "HH:mm" (24-hour) in [timezone], the IANA zone of the plan (ARCHITECTURE §6, P-50); "—" if none.
/// The time zone database must be loaded first (`initializeTimeZones`, main.dart).
String formatTime(DateTime? instant, String timezone) {
  if (instant == null) return '—';
  final local = tz.TZDateTime.from(instant, _location(timezone));
  return '${_two(local.hour)}:${_two(local.minute)}';
}

/// The day ("2026-12-01") that [instant] falls on in [timezone].
String dayIn(DateTime instant, String timezone) {
  final local = tz.TZDateTime.from(instant, _location(timezone));
  return _ymd(local.year, local.month, local.day);
}

/// The day of the phone, for when the store's timezone is not known yet.
String deviceDay(DateTime instant) {
  final local = instant.toLocal();
  return _ymd(local.year, local.month, local.day);
}

/// "2026-12-01" moved by [days] days.
String shiftDay(String day, int days) {
  final date = _parseDay(day).add(Duration(days: days));
  return _ymd(date.year, date.month, date.day);
}

/// Whole days from [from] to [to] ("2026-11-30" → "2026-12-01" is 1).
int daysBetween(String from, String to) => _parseDay(to).difference(_parseDay(from)).inDays;

/// "2026-12-01" → "Thứ Ba, 01/12/2026", like the web's formatDateLong.
String formatDateLong(String day) {
  final date = _parseDay(day);
  return '${_weekdays[date.weekday - 1]}, ${_two(date.day)}/${_two(date.month)}/${date.year}';
}

/// A time window `{start, end}` ("09:00", "11:00") as "09:00–11:00".
String formatTimeWindow(String start, String end) => '$start–$end';

const _weekdays = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật'];

tz.Location _location(String timezone) {
  try {
    return tz.getLocation(timezone);
  } on tz.LocationNotFoundException {
    return tz.UTC;
  }
}

DateTime _parseDay(String day) {
  final parts = day.split('-').map(int.parse).toList();
  return DateTime.utc(parts[0], parts[1], parts[2]);
}

String _ymd(int year, int month, int day) => '$year-${_two(month)}-${_two(day)}';

String _two(int value) => value.toString().padLeft(2, '0');

/// A driving distance: "80 m" (to 10 m) below a kilometre, "1,2 km" from there (Vietnamese decimal comma).
String formatDistance(double meters) {
  if (meters < 1000) return '${(meters / 10).round() * 10} m';
  final km = meters / 1000;
  final text = km < 10 ? km.toStringAsFixed(1).replaceAll('.', ',') : km.round().toString();
  return '$text km';
}

/// A remaining drive time: "1 phút" at least, "45 phút", "1 giờ 20 phút".
String formatDuration(int seconds) {
  final minutes = (seconds / 60).ceil().clamp(1, 1 << 30);
  if (minutes < 60) return '$minutes phút';
  final hours = minutes ~/ 60;
  final rest = minutes % 60;
  return rest == 0 ? '$hours giờ' : '$hours giờ $rest phút';
}
