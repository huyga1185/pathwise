import 'package:flutter_test/flutter_test.dart';

import '../support/app.dart';
import '../support/fake_api.dart';

void main() {
  testWidgets('mở app có refresh token đã lưu thì khôi phục phiên và vào Tuyến hôm nay', (tester) async {
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens(refresh: 'refresh-2'));
      return FakeReply.ok(meOf(driverUser));
    });

    final store = await pumpApp(tester, server, refreshToken: 'refresh-1');

    expect(find.text('Tuyến hôm nay'), findsOneWidget);
    expect(find.text('Xin chào, Tài xế A'), findsOneWidget);
    expect(store.refreshToken, 'refresh-2');
    expect(bodyOf(server.requests.first), {'refreshToken': 'refresh-1'});
  });

  testWidgets('không có refresh token thì về màn đăng nhập, không gọi API', (tester) async {
    final server = FakeServer((request) async => FakeReply.ok(null));

    await pumpApp(tester, server);

    expect(find.text('Đăng nhập'), findsWidgets);
    expect(server.requests, isEmpty);
  });

  testWidgets('refresh token đã hết hạn thì xoá và về đăng nhập', (tester) async {
    final server = FakeServer((request) async => FakeReply.fail(401, 'INVALID_TOKEN'));

    final store = await pumpApp(tester, server, refreshToken: 'refresh-old');

    expect(find.text('Đăng nhập'), findsWidgets);
    expect(store.refreshToken, isNull);
  });

  testWidgets('người không phải tài xế thì thấy màn không có quyền và đăng xuất được', (tester) async {
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens());
      if (request.path == '/api/v1/auth/log-out') return const FakeReply(204);
      return FakeReply.ok(meOf(ownerUser));
    });

    final store = await pumpApp(tester, server, refreshToken: 'refresh-1');
    expect(find.text('Tài khoản này không giao hàng'), findsOneWidget);

    await tester.tap(find.text('Đăng xuất'));
    await tester.pumpAndSettle();

    expect(find.text('Đăng nhập'), findsWidgets);
    expect(store.refreshToken, isNull);
    expect(server.requests.last.path, '/api/v1/auth/log-out');
    expect(bodyOf(server.requests.last), {'refreshToken': 'refresh-2'});
  });
}
