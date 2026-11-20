import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:pathwise_driver/features/auth/login_header.dart';
import 'package:pathwise_driver/shared/ui/ui.dart';

import '../support/app.dart';
import '../support/fake_api.dart';

Finder field(String label) => find.descendant(of: find.bySemanticsLabel(label), matching: find.byType(TextField));

Future<void> logIn(WidgetTester tester, {String email = 'taixe.a@example.com', String password = 'matkhau123'}) async {
  await tester.enterText(field('Email'), email);
  await tester.enterText(field('Mật khẩu'), password);
  final submit = find.widgetWithText(InkWell, 'Đăng nhập').last;
  await tester.ensureVisible(submit);
  await tester.pumpAndSettle();
  await tester.tap(submit);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('chữ hệ thống 200%: logo giữ chỗ như cỡ 100%, phần đầu cao thêm thay vì chữ đè lên hình (D2)', (
    tester,
  ) async {
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    Future<(double, double)> measure(double scale) async {
      tester.platformDispatcher.textScaleFactorTestValue = scale;
      await pumpApp(tester, FakeServer((request) async => FakeReply.ok(null)));
      final drawing = tester.getRect(
        find.descendant(of: find.byType(LoginHeader), matching: find.byType(CustomPaint)).first,
      );
      final logo = tester
          .getTopLeft(find.ancestor(of: find.byType(PathwiseMarkWhite), matching: find.byType(Row)).first)
          .dy;
      return (logo - drawing.top, tester.getSize(find.byType(LoginHeader)).height);
    }

    final (logoAt100, headerAt100) = await measure(1);
    final (logoAt200, headerAt200) = await measure(2);
    expect(logoAt200, closeTo(logoAt100, 1));
    expect(headerAt200, greaterThan(headerAt100));
  });

  testWidgets('biểu tượng thanh trạng thái màu sáng trên phần đầu xanh, ở cả theme tối (D2)', (tester) async {
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    for (final brightness in Brightness.values) {
      tester.platformDispatcher.platformBrightnessTestValue = brightness;
      await pumpApp(tester, FakeServer((request) async => FakeReply.ok(null)));
      expect(statusBarIcons(tester), Brightness.light, reason: '$brightness');
    }
  });

  testWidgets('đăng nhập là tài xế: gửi X-Client-Platform, lưu refresh token, vào Tuyến của tôi', (tester) async {
    final server = FakeServer((request) async {
      if (request.path == '/api/v1/driver/routes') return FakeReply.ok(<Object>[]);
      return FakeReply.ok(tokens(user: driverUser));
    });
    final store = await pumpApp(tester, server);

    await logIn(tester);

    expect(find.text('Tuyến của tôi'), findsOneWidget);
    final request = server.requests.first;
    expect(request.path, '/api/v1/auth/log-in');
    expect(request.headers['X-Client-Platform'], 'mobile');
    expect(bodyOf(request), {'email': 'taixe.a@example.com', 'password': 'matkhau123'});
    expect(store.refreshToken, 'refresh-2');
    expect(store.accessToken, 'access-1');
  });

  testWidgets('bỏ trống thì báo dưới ô, không gọi API', (tester) async {
    final server = FakeServer((request) async => FakeReply.ok(null));
    await pumpApp(tester, server);

    await logIn(tester, email: '', password: '');

    expect(find.text('Vui lòng nhập email.'), findsOneWidget);
    expect(find.text('Vui lòng nhập mật khẩu.'), findsOneWidget);
    expect(server.requests, isEmpty);
  });

  testWidgets('sai mật khẩu và tài khoản bị vô hiệu hoá hiện câu của UX_BRIEF ở đầu form', (tester) async {
    var code = 'INVALID_EMAIL_OR_PASSWORD';
    final server = FakeServer((request) async => FakeReply.fail(code == 'ACCOUNT_DISABLED' ? 403 : 401, code));
    await pumpApp(tester, server);

    await logIn(tester);
    expect(find.textContaining('Email hoặc mật khẩu không đúng.', findRichText: true), findsOneWidget);

    code = 'ACCOUNT_DISABLED';
    await logIn(tester);
    expect(
      find.textContaining('Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng.', findRichText: true),
      findsOneWidget,
    );
    expect(find.text('Tuyến của tôi'), findsNothing);
  });

  testWidgets('bị giới hạn thì báo và khoá nút theo Retry-After', (tester) async {
    final server = FakeServer((request) async => FakeReply.fail(429, 'RATE_LIMITED', headers: {'retry-after': '120'}));
    await pumpApp(tester, server);

    await logIn(tester);

    expect(find.textContaining('Bạn thao tác quá nhiều lần. Thử lại sau 2 phút.', findRichText: true), findsOneWidget);
    expect(find.text('Thử lại sau 2 phút'), findsOneWidget);
    await tester.ensureVisible(find.text('Thử lại sau 2 phút'));
    await tester.tap(find.text('Thử lại sau 2 phút'), warnIfMissed: false);
    await tester.pump();
    expect(server.requests, hasLength(1));
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Thử lại sau 1 phút 59 giây'), findsOneWidget);
  });

  testWidgets('"Quên mật khẩu?" và "Nhập mã mời" mở màn web tương ứng trong trình duyệt (M03)', (tester) async {
    final launcher = FakeLauncher();
    await pumpApp(tester, FakeServer((request) async => const FakeReply(401)), launcher: launcher);

    await tester.tap(find.text('Quên mật khẩu?'));
    await tester.pumpAndSettle();
    final invite = find.text('Nhập mã mời');
    await tester.ensureVisible(invite);
    await tester.tap(invite);
    await tester.pumpAndSettle();

    // Default WEB_BASE: API_BASE + /app (web and API share an origin, P-47).
    expect(launcher.opened.map((uri) => uri.toString()), [
      'http://10.0.2.2:8080/app/forgot-password',
      'http://10.0.2.2:8080/app/invite',
    ]);
  });

  testWidgets('máy không mở được trình duyệt thì báo', (tester) async {
    await pumpApp(
      tester,
      FakeServer((request) async => const FakeReply(401)),
      launcher: FakeLauncher()..succeeds = false,
    );

    await tester.tap(find.text('Quên mật khẩu?'));
    await tester.pumpAndSettle();

    expect(find.text('Không mở được ứng dụng trên máy.'), findsOneWidget);
  });
}
