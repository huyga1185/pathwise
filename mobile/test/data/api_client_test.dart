import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/data/api_error.dart';
import 'package:pathwise_driver/platform/token_store.dart';

import '../support/fake_api.dart';

void main() {
  group('ApiClient', () {
    test('gửi X-Client-Platform: mobile và access token', () async {
      final server = FakeServer((request) async => FakeReply.ok(meOf(driverUser)));
      final client = fakeClient(server, MemoryTokenStore(accessToken: 'access-1'));

      await client.api.getUserControllerApi().getUserProfile();

      expect(server.requests.single.headers['X-Client-Platform'], 'mobile');
      expect(server.requests.single.headers['Authorization'], 'Bearer access-1');
    });

    test(
      'nhiều request cùng gặp 401 thì refresh một lần (refresh token trong body), mỗi request gửi lại đúng một lần',
      () async {
        var refreshes = 0;
        final server = FakeServer((request) async {
          if (request.path == '/api/v1/auth/refresh') {
            refreshes += 1;
            expect((request.data as Map)['refreshToken'], 'refresh-1');
            await Future<void>.delayed(const Duration(milliseconds: 20));
            return FakeReply.ok(tokens(access: 'access-2', refresh: 'refresh-2'));
          }
          return request.headers['Authorization'] == 'Bearer access-2'
              ? FakeReply.ok(meOf(driverUser))
              : FakeReply.fail(401, 'UNAUTHORIZED');
        });
        final store = MemoryTokenStore(accessToken: 'access-old', refreshToken: 'refresh-1');
        final client = fakeClient(server, store);

        final results = await Future.wait([
          client.api.getUserControllerApi().getUserProfile(),
          client.api.getUserControllerApi().getUserProfile(),
        ]);

        expect(results.map((response) => response.data!.data.id), [451, 451]);
        expect(refreshes, 1);
        expect(store.accessToken, 'access-2');
        expect(store.refreshToken, 'refresh-2');
        expect(server.requests.where((request) => request.path == '/api/v1/users/me'), hasLength(4));
      },
    );

    test('refresh bị từ chối: xoá token, báo phiên hết hạn đúng một lần, request trả lỗi 401', () async {
      final server = FakeServer(
        (request) async => request.path == '/api/v1/auth/refresh'
            ? FakeReply.fail(401, 'INVALID_TOKEN')
            : FakeReply.fail(401, 'UNAUTHORIZED'),
      );
      final store = MemoryTokenStore(accessToken: 'access-old', refreshToken: 'refresh-1');
      final client = fakeClient(server, store);
      var expired = 0;
      client.onSessionExpired = () => expired += 1;

      final error = await guard(() => client.api.getUserControllerApi().getUserProfile())
          .then<Object?>((_) => null, onError: (Object error) => error);

      expect(error, isA<ApiError>().having((e) => e.code, 'code', 'UNAUTHORIZED'));
      expect(expired, 1);
      expect(store.accessToken, isNull);
      expect(store.refreshToken, isNull);
    });

    test('lỗi của API đọc được mã, Retry-After và X-Request-Id; mất mạng là NETWORK_ERROR', () async {
      final server = FakeServer((request) async => FakeReply.fail(429, 'RATE_LIMITED', headers: {'retry-after': '90'}));
      final client = fakeClient(server, MemoryTokenStore());

      final error = await guard(() => client.api.getUserControllerApi().getUserProfile())
          .then<Object?>((_) => null, onError: (Object error) => error);

      expect(error, isA<ApiError>().having((e) => e.code, 'code', 'RATE_LIMITED'));
      final apiError = error! as ApiError;
      expect(apiError.retryAfter, 90);
      expect(apiError.requestId, 'req-123');

      server.handler = (request) => Future<FakeReply>.error(Exception('offline'));
      final offline = await guard(() => client.api.getUserControllerApi().getUserProfile())
          .then<Object?>((_) => null, onError: (Object error) => error);
      expect(offline, isA<ApiError>().having((e) => e.code, 'code', ApiError.networkError));
    });
  });
}
