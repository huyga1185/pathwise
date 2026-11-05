import 'dart:async';

import 'package:dio/dio.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../platform/token_store.dart';

/// The HTTP layer (ARCHITECTURE §3, §4, P-46): the generated client on a Dio that sends `X-Client-Platform: mobile`
/// and the access token, and on a 401 refreshes once (shared by every request waiting, F-29) then sends the request
/// again exactly once. The refresh token travels in the body (mobile) and is kept by [TokenStore].
class ApiClient {
  ApiClient({required String baseUrl, required this.tokens, HttpClientAdapter? adapter, this.onSessionExpired})
    : dio = Dio(_options(baseUrl)),
      _refreshDio = Dio(_options(baseUrl)) {
    if (adapter != null) {
      dio.httpClientAdapter = adapter;
      _refreshDio.httpClientAdapter = adapter;
    }
    dio.interceptors.add(_AuthInterceptor(this));
    api = PathwiseApi(dio: dio, interceptors: const []);
  }

  static BaseOptions _options(String baseUrl) => BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 30),
    headers: const {platformHeader: 'mobile'},
  );

  static const platformHeader = 'X-Client-Platform';
  static const _authPaths = {'/api/v1/auth/log-in', '/api/v1/auth/refresh', '/api/v1/auth/log-out'};

  final TokenStore tokens;
  final Dio dio;
  final Dio _refreshDio;
  late final PathwiseApi api;

  /// Called once when a refresh fails while signed in: the session is over.
  void Function()? onSessionExpired;

  Future<bool>? _refreshing;

  /// Gets a new access token with the stored refresh token. Concurrent callers share one request (single flight).
  Future<bool> refresh() => _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);

  Future<bool> _doRefresh() async {
    final refreshToken = await tokens.readRefreshToken();
    if (refreshToken == null) return false;
    try {
      final response = await _refreshDio.post<Map<String, dynamic>>(
        '/api/v1/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      final data = response.data?['data'];
      if (data is! Map) return false;
      tokens.accessToken = data['accessToken'] as String?;
      // The refresh token rotates on every use (P-04).
      await tokens.writeRefreshToken(data['refreshToken'] as String?);
      return tokens.accessToken != null;
    } on DioException catch (error) {
      if (error.response?.statusCode == 401) {
        tokens.accessToken = null;
        await tokens.writeRefreshToken(null);
      }
      return false;
    }
  }

  bool _isAuthPath(RequestOptions options) => _authPaths.contains(options.path);
}

class _AuthInterceptor extends Interceptor {
  _AuthInterceptor(this.client);

  final ApiClient client;
  static const _retried = 'pathwise.retried';

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = client.tokens.accessToken;
    if (token != null && !client._isAuthPath(options)) options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }

  @override
  Future<void> onError(DioException error, ErrorInterceptorHandler handler) async {
    final options = error.requestOptions;
    final retry = error.response?.statusCode == 401 && !client._isAuthPath(options) && options.extra[_retried] != true;
    if (!retry) return handler.next(error);
    final hadToken = client.tokens.accessToken != null;
    if (!await client.refresh()) {
      if (hadToken) client.onSessionExpired?.call();
      return handler.next(error);
    }
    try {
      options.extra[_retried] = true;
      options.headers['Authorization'] = 'Bearer ${client.tokens.accessToken}';
      handler.resolve(await client.dio.fetch<dynamic>(options));
    } on DioException catch (again) {
      handler.next(again);
    }
  }
}
