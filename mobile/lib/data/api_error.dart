import 'package:dio/dio.dart';

/// An error of the API, `{code, details?}` of API_SPEC §1, or a network failure (`NETWORK_ERROR`).
class ApiError implements Exception {
  const ApiError({required this.code, this.status, this.details = const [], this.retryAfter, this.requestId});

  /// Reads a failed request: the body's `code`, `Retry-After` (seconds) and `X-Request-Id` headers.
  factory ApiError.fromDio(DioException error) {
    final response = error.response;
    if (response == null) return const ApiError(code: networkError);
    final body = response.data;
    final code = body is Map && body['code'] is String ? body['code'] as String : 'HTTP_${response.statusCode}';
    final details = body is Map && body['details'] is List
        ? (body['details'] as List).whereType<Map<dynamic, dynamic>>().map(Map<String, dynamic>.from).toList()
        : const <Map<String, dynamic>>[];
    return ApiError(
      code: code,
      status: response.statusCode,
      details: details,
      retryAfter: int.tryParse(response.headers.value('retry-after') ?? ''),
      requestId: response.headers.value('x-request-id'),
    );
  }

  static const networkError = 'NETWORK_ERROR';

  final String code;
  final int? status;
  final List<Map<String, dynamic>> details;
  final int? retryAfter;
  final String? requestId;

  @override
  String toString() => 'ApiError($code${status == null ? '' : ', $status'})';
}

/// Runs an API call and turns its failure into an [ApiError].
Future<T> guard<T>(Future<T> Function() call) async {
  try {
    return await call();
  } on DioException catch (error) {
    throw ApiError.fromDio(error);
  }
}
