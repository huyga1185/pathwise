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
