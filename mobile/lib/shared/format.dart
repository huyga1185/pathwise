/// "+84905123456" → "0905 123 456" (UX_BRIEF §2); anything else unchanged.
String formatPhone(String? e164) {
  if (e164 == null || e164.isEmpty) return '';
  final match = RegExp(r'^\+84(\d{9})$').firstMatch(e164);
  if (match == null) return e164;
  final digits = '0${match.group(1)}';
  return '${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}';
}
