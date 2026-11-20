import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../support/app.dart';
import '../support/fake_api.dart';

void main() {
  testWidgets('mở app có refresh token đã lưu thì khôi phục phiên và vào Tuyến của tôi', (tester) async {
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens(refresh: 'refresh-2'));
      if (request.path == '/api/v1/driver/routes') return FakeReply.ok(<Object>[]);
      return FakeReply.ok(meOf(driverUser));
    });

    final store = await pumpApp(tester, server, refreshToken: 'refresh-1');

    expect(find.text('Tuyến của tôi'), findsOneWidget);
    expect(find.bySemanticsLabel('Tài xế A — tài khoản'), findsOneWidget);
    expect(store.refreshToken, 'refresh-2');
    expect(bodyOf(server.requests.first), {'refreshToken': 'refresh-1'});
  });

  testWidgets('màn chờ hệ thống giữ tới khi khôi phục xong phiên, thả ra khi Tuyến của tôi đã vẽ (D2)', (tester) async {
    final refreshed = Completer<void>();
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/auth/refresh') {
        await refreshed.future;
        return FakeReply.ok(tokens(refresh: 'refresh-2'));
      }
      if (request.path == '/api/v1/driver/routes') return FakeReply.ok(<Object>[]);
      return FakeReply.ok(meOf(driverUser));
    });
    var routesShownAtDismiss = false;
    final splash = FakeSystemSplash(
      onDismiss: () => routesShownAtDismiss = find.text('Tuyến của tôi').evaluate().isNotEmpty,
    );

    await pumpApp(tester, server, refreshToken: 'refresh-1', splash: splash, settle: false);
    await tester.pump(const Duration(milliseconds: 500));
    expect(splash.dismissed, 0);

    refreshed.complete();
    await tester.pumpAndSettle();
    expect(splash.dismissed, greaterThan(0));
    expect(routesShownAtDismiss, isTrue);
  });

  testWidgets('chưa đăng nhập thì thả màn chờ hệ thống khi màn đăng nhập đã vẽ (D2)', (tester) async {
    var loginShownAtDismiss = false;
    final splash = FakeSystemSplash(
      onDismiss: () => loginShownAtDismiss = find.text('Quên mật khẩu?').evaluate().isNotEmpty,
    );

    await pumpApp(tester, FakeServer((request) async => FakeReply.ok(null)), splash: splash);

    expect(splash.dismissed, greaterThan(0));
    expect(loginShownAtDismiss, isTrue);
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

  testWidgets('biểu tượng thanh trạng thái theo theme ở màn không có thanh trên; màn đăng nhập thì sáng (D2)', (
    tester,
  ) async {
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens());
      if (request.path == '/api/v1/auth/log-out') return const FakeReply(204);
      return FakeReply.ok(meOf(ownerUser));
    });

    await pumpApp(tester, server, refreshToken: 'refresh-1');
    expect(find.text('Tài khoản này không giao hàng'), findsOneWidget);
    expect(statusBarIcons(tester), Brightness.dark);

    await tester.tap(find.text('Đăng xuất'));
    await tester.pumpAndSettle();
    expect(find.text('Quên mật khẩu?'), findsOneWidget);
    expect(statusBarIcons(tester), Brightness.light);
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
