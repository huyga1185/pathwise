import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:pathwise_driver/platform/location.dart';
import 'package:pathwise_driver/platform/preferences.dart';

import 'features/stop_report_test.dart' show RouteServer, inDialog;
import 'support/app.dart';
import 'support/fake_api.dart';
import 'support/location.dart';
import 'support/routes.dart';

/// M03 acceptance (docs/client/PLAN.md): on a small phone (360 × 640) with the system text at 130 %, every screen of
/// the driver app lays out without overflow (a cut line fails the test), and in both themes every tap target is
/// labelled for TalkBack/VoiceOver, at least 48 px (44 on iOS), and text keeps a 4.5:1 contrast.
void main() {
  Future<void> check(WidgetTester tester, Future<void> Function() open, {bool dark = false}) async {
    final semantics = tester.ensureSemantics();
    tester.view.physicalSize = const Size(360 * 3, 640 * 3);
    tester.view.devicePixelRatio = 3;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    if (dark) tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(() {
      tester.view.reset();
      tester.platformDispatcher.clearTextScaleFactorTestValue();
      tester.platformDispatcher.clearPlatformBrightnessTestValue();
    });

    await open();

    await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
    await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
    await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
    semantics.dispose();
  }

  FakeServer listServer(List<Object?> routes) => driverServer((request) async => FakeReply.ok(routes));

  Future<void> openRoute(WidgetTester tester, RouteServer server) async {
    await pumpApp(tester, server.server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/64');
  }

  Future<void> openReport(WidgetTester tester, RouteServer server) async {
    await openRoute(tester, server);
    await tester.scrollUntilVisible(find.text('Báo kết quả'), 200);
    await tester.tap(find.text('Báo kết quả'));
    await tester.pumpAndSettle();
  }

  final inProgress = {1: ('FAILED', 'Vắng nhà'), 2: ('DELIVERED', null)};

  for (final dark in [false, true]) {
    final theme = dark ? 'tối' : 'sáng';

    testWidgets('đăng nhập ($theme)', (tester) async {
      await check(tester, () => pumpApp(tester, FakeServer((request) async => const FakeReply(401))), dark: dark);
    });

    testWidgets('tuyến của tôi có tuyến ($theme)', (tester) async {
      await check(
        tester,
        () => pumpApp(tester, listServer(sample('driver-route-list')['data']! as List<Object?>), refreshToken: 'r'),
        dark: dark,
      );
    });

    testWidgets('tuyến của tôi trống ($theme)', (tester) async {
      await check(tester, () => pumpApp(tester, listServer(<Object>[]), refreshToken: 'r'), dark: dark);
    });

    testWidgets('menu tài khoản ($theme)', (tester) async {
      await check(tester, () async {
        await pumpApp(tester, listServer(<Object>[]), refreshToken: 'r');
        await tester.tap(find.text('TA'));
        await tester.pumpAndSettle();
      }, dark: dark);
    });

    // T32: the question, then each box of the route detail (declined is the default of pumpApp).
    testWidgets('hỏi chia sẻ vị trí ($theme)', (tester) async {
      await check(tester, () async {
        await pumpApp(tester, RouteServer(sampleRoute()).server, refreshToken: 'r', preferences: MemoryPreferences());
        await goTo(tester, '/routes/64');
      }, dark: dark);
    });

    for (final (name, access) in [
      ('đang chia sẻ', LocationAccess.granted),
      ('máy chưa cho dùng vị trí', LocationAccess.deniedForever),
      ('GPS tắt', LocationAccess.serviceOff),
    ]) {
      testWidgets('chi tiết tuyến, $name ($theme)', (tester) async {
        await check(tester, () async {
          await pumpApp(
            tester,
            RouteServer(sampleRoute()).server,
            refreshToken: 'r',
            preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}),
            location: FakeLocationSource(access: access),
          );
          await goTo(tester, '/routes/64');
        }, dark: dark);
      });
    }

    testWidgets('chi tiết tuyến đang giao ($theme)', (tester) async {
      await check(
        tester,
        () => openRoute(tester, RouteServer(sampleRoute(finished: inProgress, status: 'IN_PROGRESS'))),
        dark: dark,
      );
    });

    testWidgets('chi tiết tuyến đã xong ($theme)', (tester) async {
      final all = {for (var sequence = 1; sequence <= 12; sequence++) sequence: ('DELIVERED', null)};
      await check(
        tester,
        () => openRoute(tester, RouteServer(sampleRoute(finished: all, status: 'COMPLETED'))),
        dark: dark,
      );
    });

    testWidgets('báo kết quả ($theme)', (tester) async {
      await check(tester, () => openReport(tester, RouteServer(sampleRoute())), dark: dark);
    });

    testWidgets('hộp lý do không giao được ($theme)', (tester) async {
      await check(tester, () async {
        await openReport(tester, RouteServer(sampleRoute()));
        await tester.tap(find.text('Không giao được'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Vắng nhà'));
        await tester.pumpAndSettle();
      }, dark: dark);
    });

    testWidgets('đã gửi kết quả ($theme)', (tester) async {
      await check(tester, () async {
        await openReport(tester, RouteServer(sampleRoute()));
        await tester.tap(find.text('Đã giao'));
        await tester.pumpAndSettle();
        await tester.tap(inDialog('Đã giao'));
        await tester.pumpAndSettle();
      }, dark: dark);
    });

    testWidgets('chưa gửi được vì mất mạng ($theme)', (tester) async {
      await check(tester, () async {
        await openReport(tester, RouteServer(sampleRoute())..online = false);
        await tester.tap(find.text('Đã giao'));
        await tester.pumpAndSettle();
        await tester.tap(inDialog('Đã giao'));
        await tester.pumpAndSettle();
      }, dark: dark);
    });
  }
}
