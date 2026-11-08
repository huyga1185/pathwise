import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';

import '../support/app.dart';
import '../support/fake_api.dart';
import '../support/routes.dart';

/// Opens route 64 straight away (as a notification would), the server answering [route].
Future<FakeServer> openRoute(WidgetTester tester, Map<String, Object?> route, {FakeLauncher? launcher}) async {
  final server = driverServer((request) async {
    if (request.path == '/api/v1/driver/routes/64') return FakeReply.ok(route);
    return FakeReply.ok(<Object>[]);
  });
  await pumpApp(tester, server, refreshToken: 'refresh-1', launcher: launcher);
  await goTo(tester, '/routes/64');
  return server;
}

void main() {
  testWidgets('tuyến chưa giao: tiêu đề planName, điểm tiếp theo nổi bật, giờ theo timezone, các điểm còn lại', (
    tester,
  ) async {
    await openRoute(tester, sampleRoute());

    expect(find.text('Giao sáng 01/12'), findsOneWidget);
    expect(find.text('Thứ Ba, 01/12/2026'), findsOneWidget);
    expect(find.text('CHỜ GIAO'), findsOneWidget);
    expect(find.text('ĐIỂM TIẾP THEO'), findsOneWidget);
    expect(find.text('ĐH-1009 · Chị Lan'), findsOneWidget);
    expect(find.text('Dự kiến 10:39 · Khung 09:00–11:00'), findsOneWidget);
    expect(find.text('Gọi 0905 123 008'), findsOneWidget);
    expect(find.text('Chỉ đường'), findsOneWidget);
    expect(find.text('CÒN 11 ĐIỂM SAU ĐÓ'), findsOneWidget);
    expect(find.textContaining('ĐÃ XONG'), findsNothing);

    await tester.scrollUntilVisible(find.text('ĐH-1021 · Chị Mai'), 300);
    // Stop 3 has no address: the driver goes by its coordinates.
    expect(find.text('Không có địa chỉ, chỉ đường theo toạ độ'), findsOneWidget);
    await tester.scrollUntilVisible(find.textContaining('Xuất phát và quay về'), 300);
    expect(find.text('Xuất phát và quay về: Kho 45 Lê Lợi, Hải Châu, Đà Nẵng.'), findsOneWidget);
  });

  testWidgets('"Chỉ đường" mở Google Maps trên Android, "Gọi" mở trình gọi số E.164', (tester) async {
    final launcher = FakeLauncher();
    await openRoute(tester, sampleRoute(), launcher: launcher);

    await tester.tap(find.text('Chỉ đường'));
    await tester.tap(find.text('Gọi 0905 123 008'));
    await tester.pumpAndSettle();

    expect(launcher.opened.map((uri) => uri.toString()), [
      'https://www.google.com/maps/dir/?api=1&destination=16.048,108.196',
      'tel:+84905123008',
    ]);
  });

  testWidgets('"Chỉ đường" mở Apple Maps trên iOS', (tester) async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    final launcher = FakeLauncher();
    await openRoute(tester, sampleRoute(), launcher: launcher);

    await tester.tap(find.text('Chỉ đường'));
    await tester.pumpAndSettle();

    expect(launcher.opened.single.toString(), 'https://maps.apple.com/?daddr=16.048,108.196');
    debugDefaultTargetPlatformOverride = null;
  });

  testWidgets('máy không mở được ứng dụng thì báo', (tester) async {
    await openRoute(tester, sampleRoute(), launcher: FakeLauncher()..succeeds = false);

    await tester.tap(find.text('Chỉ đường'));
    await tester.pumpAndSettle();

    expect(find.text('Không mở được ứng dụng trên máy.'), findsOneWidget);
  });

  testWidgets('đang giao: điểm tiếp theo là điểm chờ đầu tiên; nhóm đã xong có lý do và thu gọn được', (tester) async {
    await openRoute(
      tester,
      sampleRoute(finished: {1: ('FAILED', 'Vắng nhà'), 2: ('DELIVERED', null)}, status: 'IN_PROGRESS'),
    );

    expect(find.text('ĐANG GIAO'), findsOneWidget);
    expect(find.text('2/12'), findsOneWidget);
    expect(find.text('ĐH-1021 · Chị Mai'), findsOneWidget);
    expect(find.text('CÒN 9 ĐIỂM SAU ĐÓ'), findsOneWidget);

    await tester.scrollUntilVisible(find.text('ĐÃ XONG (2)'), 400);
    await tester.scrollUntilVisible(find.text('Lý do: Vắng nhà'), 200);
    expect(find.text('KHÔNG GIAO ĐƯỢC'), findsOneWidget);
    expect(find.text('ĐÃ GIAO'), findsOneWidget);

    await tester.tap(find.text('Thu gọn'));
    await tester.pumpAndSettle();
    expect(find.text('Lý do: Vắng nhà'), findsNothing);
    expect(find.text('Xem'), findsOneWidget);
  });

  testWidgets('tuyến đã xong: báo xong tuyến và quay về kho, không còn điểm tiếp theo', (tester) async {
    await openRoute(
      tester,
      sampleRoute(
        finished: {for (var sequence = 1; sequence <= 12; sequence++) sequence: ('DELIVERED', null)},
        status: 'COMPLETED',
      ),
    );

    expect(find.text('HOÀN THÀNH'), findsOneWidget);
    expect(find.text('Bạn đã xong tuyến này.'), findsOneWidget);
    expect(find.text('Quay về kho: Kho 45 Lê Lợi, Hải Châu, Đà Nẵng.'), findsOneWidget);
    expect(find.text('ĐIỂM TIẾP THEO'), findsNothing);
    expect(find.text('ĐÃ XONG (12)'), findsOneWidget);
  });

  testWidgets('tuyến không còn (404, ví dụ plan bị huỷ) thì bảo dừng giao, nút quay lại về danh sách', (tester) async {
    final server = driverServer((request) async {
      if (request.path == '/api/v1/driver/routes/99') return FakeReply.fail(404, 'NOT_FOUND');
      return FakeReply.ok(<Object>[]);
    });
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/99');

    expect(find.textContaining('Tuyến này không còn.', findRichText: true), findsOneWidget);
    expect(
      find.textContaining('Kế hoạch có thể đã bị huỷ. Dừng giao và liên hệ điều phối viên.', findRichText: true),
      findsOneWidget,
    );

    await tester.tap(find.byTooltip('Về danh sách tuyến'));
    await tester.pumpAndSettle();
    expect(find.text('Tuyến của tôi'), findsOneWidget);
  });
}
