import '../../data/api_error.dart';
import 'messages.dart';

export 'messages.dart';

const _networkMessage = 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.';

/// "4 phút 12 giây", "45 giây", "2 giờ 5 phút" for a wait in seconds.
String formatWait(int seconds) {
  final s = seconds < 0 ? 0 : seconds;
  final hours = s ~/ 3600;
  final minutes = (s % 3600) ~/ 60;
  final rest = s % 60;
  if (hours > 0) return minutes > 0 ? '$hours giờ $minutes phút' : '$hours giờ';
  if (minutes > 0) return rest > 0 ? '$minutes phút $rest giây' : '$minutes phút';
  return '$rest giây';
}

/// The sentence for an error (ARCHITECTURE §3.2, UX_BRIEF §2, §6.1); a code without one gets the generic sentence
/// with the lookup code, never the raw code alone.
String messageFor(Object error) {
  if (error is! ApiError || error.code == ApiError.networkError) return _networkMessage;
  final requestId = error.requestId ?? error.code;
  final template = errorMessages[error.code];
  if (template == null) return 'Đã có lỗi xảy ra. Mã tra cứu: $requestId.';
  return template
      .replaceFirst('{Retry-After}', error.retryAfter != null ? formatWait(error.retryAfter!) : 'ít phút')
      .replaceFirst('{X-Request-Id}', requestId);
}

/// Sentences of `details[].code` that do not follow a pattern (UX_BRIEF §6.2; same words as the web).
const _fieldSentences = <String, String>{
  'PHONE_NUMBER_INVALID': 'Số điện thoại không hợp lệ.',
  'PHONE_NUMBER_WRONG_REGION': 'Chỉ nhận số Việt Nam.',
  'PHONE_NUMBER_TOO_SHORT': 'Số điện thoại quá ngắn.',
  'PHONE_NUMBER_TOO_LONG': 'Số điện thoại quá dài.',
  'PHONE_NUMBER_TYPE_NOT_ALLOWED': 'Số điện thoại không hợp lệ.',
};

/// The sentence for one `details[].code` under a field whose name on screen is [label] (UX_BRIEF §6.2).
String fieldMessage(String code, String label) {
  final sentence = _fieldSentences[code];
  if (sentence != null) return sentence;
  if (code.endsWith('_REQUIRED')) return 'Vui lòng nhập ${label.toLowerCase()}.';
  if (RegExp(r'^INVALID_.*_LENGTH$').hasMatch(code)) return '$label quá dài hoặc quá ngắn.';
  return 'Giá trị không hợp lệ.';
}

/// The first field error of [error] for each field name (`details[].field`), as sentences; [labels] names the fields
/// on screen. Empty when [error] is not a validation error.
Map<String, String> fieldErrors(Object error, Map<String, String> labels) {
  if (error is! ApiError) return const {};
  final result = <String, String>{};
  for (final detail in error.details) {
    final field = detail['field'];
    final code = detail['code'];
    if (field is! String || code is! String || result.containsKey(field)) continue;
    result[field] = fieldMessage(code, labels[field] ?? 'giá trị này');
  }
  return result;
}
