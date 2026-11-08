import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:pathwise_driver/data/api_client.dart';
import 'package:pathwise_driver/platform/token_store.dart';

/// One answer of the fake server.
class FakeReply {
  const FakeReply(this.status, [this.body, this.headers = const {}]);

  /// `{data}` envelope of API_SPEC §1.
  factory FakeReply.ok(Object? data) => FakeReply(200, {'data': data});

  /// Error body `{code}` with an X-Request-Id, like the server.
  factory FakeReply.fail(int status, String code, {Map<String, String> headers = const {}}) =>
      FakeReply(status, {'code': code}, {'x-request-id': 'req-123', ...headers});

  final int status;
  final Object? body;
  final Map<String, String> headers;
}

/// A server in memory (no real network in tests, docs/client/README.md §2): `handler` answers each request.
class FakeServer implements HttpClientAdapter {
  FakeServer(this.handler);

  Future<FakeReply> Function(RequestOptions request) handler;
  final requests = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final reply = await handler(options);
    final bytes = reply.body == null ? <int>[] : utf8.encode(jsonEncode(reply.body));
    return ResponseBody.fromBytes(
      bytes,
      reply.status,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
        for (final entry in reply.headers.entries) entry.key: [entry.value],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

const driverUser = {
  'id': 451,
  'email': 'taixe.a@example.com',
  'fullName': 'Tài xế A',
  'role': 'DRIVER',
  'canDrive': true,
};
const ownerUser = {'id': 449, 'email': 'chu@example.com', 'role': 'OWNER', 'storeId': 182, 'canDrive': false};

Map<String, Object?> meOf(Map<String, Object?> user) => {
  ...user,
  'fullName': user['fullName'],
  'phoneNumber': null,
  'storeId': user['storeId'] ?? 182,
  'status': 'ACTIVE',
};

Map<String, Object?> tokens({String access = 'access-1', String refresh = 'refresh-2', Object? user}) => {
  'accessToken': access,
  'accessTokenExpiresAt': '2026-11-05T10:00:00Z',
  'refreshToken': refresh,
  'refreshTokenExpiresAt': '2026-11-19T10:00:00Z',
  'user': ?user,
};

ApiClient fakeClient(FakeServer server, MemoryTokenStore store) =>
    ApiClient(baseUrl: 'http://test', tokens: store, adapter: server);

/// The JSON body of a request, whether the client sent a map or an encoded string.
Object? bodyOf(RequestOptions request) => request.data is String ? jsonDecode(request.data as String) : request.data;

/// A response of docs/ux/samples (the API's own output, T45/T47), decoded; `data` is the envelope's content.
Map<String, Object?> sample(String name) =>
    jsonDecode(File('../docs/ux/samples/$name.json').readAsStringSync()) as Map<String, Object?>;

/// A signed-in driver (refresh → tokens, `/users/me` → driver) whose other requests go to [routes].
FakeServer driverServer(Future<FakeReply> Function(RequestOptions request) routes) => FakeServer((request) async {
  if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens());
  if (request.path == '/api/v1/users/me') return FakeReply.ok(meOf(driverUser));
  return routes(request);
});
