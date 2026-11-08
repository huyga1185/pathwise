import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/features/driver_routes/route_list_screen.dart';

import '../support/app.dart';
import '../support/fake_api.dart';

/// `driver-route-list.json`: one route of "Giao sáng 01/12", PENDING, 0/12, start 03:38:22Z.
List<Map<String, Object?>> sampleRoutes({Map<String, Object?> change = const {}}) => [
  for (final route in sample('driver-route-list')['data']! as List<Object?>)
    {...route! as Map<String, Object?>, ...change},
];

Iterable<RequestOptions> routeRequests(FakeServer server) =>
    server.requests.where((request) => request.path == '/api/v1/driver/routes');

void main() {
  testWidgets('hôm nay: thẻ tuyến có tên plan, trạng thái, tiến độ, giờ xuất phát và số điểm', (tester) async {
    final server = driverServer((request) async => FakeReply.ok(sampleRoutes()));

    await pumpApp(tester, server, refreshToken: 'refresh-1');

    expect(find.text('Hôm nay'), findsOneWidget);
    expect(find.text('Thứ Ba, 01/12/2026'), findsOneWidget);
    expect(find.text('Giao sáng 01/12'), findsOneWidget);
    expect(find.text('CHỜ GIAO'), findsOneWidget);
    expect(find.text('Xuất phát dự kiến 10:38'), findsOneWidget);
    expect(find.text('12 điểm giao'), findsOneWidget);
    expect(find.text('Bắt đầu tuyến'), findsOneWidget);
    expect(find.bySemanticsLabel(RegExp('^Giao sáng 01/12, CHỜ GIAO, 0/12 điểm đã xong')), findsOneWidget);
    // No date: the server picks today in the store's timezone (API_SPEC §8).
    expect(routeRequests(server).single.queryParameters, isEmpty);
  });

  testWidgets('giờ xuất phát theo timezone của plan, không theo máy; nút theo trạng thái tuyến', (tester) async {
    final server = driverServer(
      (request) async =>
          FakeReply.ok(sampleRoutes(change: {'timezone': 'Asia/Tokyo', 'status': 'IN_PROGRESS', 'completedCount': 3})),
    );

    await pumpApp(tester, server, refreshToken: 'refresh-1');

    expect(find.text('Xuất phát dự kiến 12:38'), findsOneWidget);
    expect(find.text('ĐANG GIAO'), findsOneWidget);
    expect(find.text('Tiếp tục giao'), findsOneWidget);
  });

  testWidgets('đổi ngày gửi date của ngày đó; ngày không có tuyến hiện trạng thái trống', (tester) async {
    final server = driverServer(
      (request) async => FakeReply.ok(request.queryParameters['date'] == null ? sampleRoutes() : <Object>[]),
    );
    await pumpApp(tester, server, refreshToken: 'refresh-1');

    await tester.tap(find.byTooltip('Ngày sau'));
    await tester.pumpAndSettle();
    expect(routeRequests(server).last.queryParameters, {'date': '2026-12-02'});
    expect(find.text('Ngày mai'), findsOneWidget);
    expect(find.text('Thứ Tư, 02/12/2026'), findsOneWidget);
    expect(find.text('Chưa có tuyến nào'), findsOneWidget);

    await tester.tap(find.byTooltip('Ngày trước'));
    await tester.tap(find.byTooltip('Ngày trước'));
    await tester.pumpAndSettle();
    expect(routeRequests(server).last.queryParameters, {'date': '2026-11-30'});
    expect(find.text('Hôm qua'), findsOneWidget);
  });

  testWidgets('kéo xuống thì tải lại danh sách', (tester) async {
    final server = driverServer((request) async => FakeReply.ok(sampleRoutes()));
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    final before = routeRequests(server).length;

    await tester.fling(find.text('Giao sáng 01/12'), const Offset(0, 400), 1000);
    await tester.pumpAndSettle();

    expect(routeRequests(server).length, before + 1);
  });

  testWidgets('trở lại app từ nền thì tải lại danh sách', (tester) async {
    final server = driverServer((request) async => FakeReply.ok(sampleRoutes()));
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    final before = routeRequests(server).length;

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();

    expect(routeRequests(server).length, before + 1);
  });

  testWidgets('lỗi thì báo câu lỗi kèm mã tra cứu và thử lại được', (tester) async {
    var fail = true;
    final server = driverServer(
      (request) async => fail ? FakeReply.fail(500, 'INTERNAL_ERROR') : FakeReply.ok(sampleRoutes()),
    );
    await pumpApp(tester, server, refreshToken: 'refresh-1');

    expect(
      find.textContaining('Không tải được danh sách tuyến. Đã có lỗi xảy ra. Mã tra cứu: req-123.', findRichText: true),
      findsOneWidget,
    );

    fail = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();
    expect(find.text('Giao sáng 01/12'), findsOneWidget);
  });

  testWidgets('chạm thẻ tuyến thì mở chi tiết tuyến', (tester) async {
    final server = driverServer((request) async {
      if (request.path == '/api/v1/driver/routes/64') return FakeReply.ok(sample('driver-route-detail')['data']);
      return FakeReply.ok(sampleRoutes());
    });
    await pumpApp(tester, server, refreshToken: 'refresh-1');

    await tester.tap(find.text('Bắt đầu tuyến'));
    await tester.pumpAndSettle();

    expect(find.text('ĐIỂM TIẾP THEO'), findsOneWidget);
    expect(server.requests.last.path, '/api/v1/driver/routes/64');
  });

  testWidgets('chạm chữ viết tắt tên rồi xác nhận thì đăng xuất', (tester) async {
    final server = driverServer((request) async {
      if (request.path == '/api/v1/auth/log-out') return const FakeReply(204);
      return FakeReply.ok(<Object>[]);
    });
    final store = await pumpApp(tester, server, refreshToken: 'refresh-1');

    await tester.tap(find.text('TA'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(Center, 'Đăng xuất').last);
    await tester.pumpAndSettle();

    expect(find.text('Đăng nhập'), findsWidgets);
    expect(store.refreshToken, isNull);
  });

  test('chữ viết tắt lấy chữ đầu của từ đầu và từ cuối', () {
    expect(initials('Tài xế B'), 'TB');
    expect(initials('lan@example.com'), 'L');
    expect(initials(''), '');
  });
}
