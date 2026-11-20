import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/shared/ui/ui.dart';

import '../support/app.dart';
import '../support/fake_api.dart';
import '../support/routes.dart';

/// Route 64 on a server that records each result like the real one (idempotent resend, P-36) and can lose the
/// network, or lose the response after recording the result.
class RouteServer {
  RouteServer(this.route) {
    server = driverServer(_handle);
  }

  final Map<String, Object?> route;
  late final FakeServer server;

  /// False: every result request fails with no answer, as with no network.
  bool online = true;

  /// True once: the server records the result, then the answer is lost on the way back.
  bool loseResponse = false;

  /// Answers result requests instead of recording them.
  FakeReply? refuse;

  /// True: the plan was cancelled after publishing; the route answers 422 PLAN_NOT_ACTIVE (T48, API_SPEC §8).
  bool cancelled = false;

  /// False: reading the route fails with no answer.
  bool routeOnline = true;

  /// Times the app read the route.
  int get routeReads => server.requests.where((request) => request.path == '/api/v1/driver/routes/64').length;

  /// Bodies of every result request that reached the server.
  final patches = <Map<String, Object?>>[];

  List<Map<String, Object?>> get stops => (route['stops']! as List<Object?>).cast<Map<String, Object?>>();

  Future<FakeReply> _handle(RequestOptions request) async {
    final patch = RegExp(r'^/api/v1/driver/routes/64/stops/(\d+)/status$').firstMatch(request.path);
    if (patch != null) {
      if (!online) throw DioException(requestOptions: request, type: DioExceptionType.connectionError);
      final body = Map<String, Object?>.from(bodyOf(request)! as Map);
      patches.add(body);
      if (refuse != null) return refuse!;
      final stop = stops.firstWhere((stop) => stop['id'] == int.parse(patch.group(1)!));
      if (stop['status'] == 'PENDING') {
        stop['status'] = body['status'];
        stop['failureReason'] = body['status'] == 'FAILED' ? (body['failureReason']! as String).trim() : null;
      }
      final done = stops.every((stop) => stop['status'] != 'PENDING');
      route['status'] = done ? 'COMPLETED' : 'IN_PROGRESS';
      if (loseResponse) {
        loseResponse = false;
        throw DioException(requestOptions: request, type: DioExceptionType.connectionError);
      }
      return FakeReply.ok({'stop': stop, 'routeStatus': route['status'], 'planStatus': route['status']});
    }
    if (request.path == '/api/v1/driver/routes/64') {
      if (!routeOnline) throw DioException(requestOptions: request, type: DioExceptionType.connectionError);
      if (cancelled) return FakeReply.fail(422, 'PLAN_NOT_ACTIVE');
      return FakeReply.ok(jsonDecode(jsonEncode(route)));
    }
    return FakeReply.ok(<Object>[]);
  }
}

/// Opens route 64 then the report screen of its next stop through "Báo kết quả".
Future<void> openReport(WidgetTester tester, RouteServer server) async {
  await pumpApp(tester, server.server, refreshToken: 'refresh-1');
  await goTo(tester, '/routes/64');
  await tester.tap(find.text('Báo kết quả'));
  await tester.pumpAndSettle();
}

Finder inDialog(String text) => find.descendant(of: find.byType(AlertDialog), matching: find.text(text));

/// In the "Không giao được" bottom sheet.
Finder inSheet(String text) => find.descendant(of: find.byType(BottomSheet), matching: find.text(text));

Future<void> report(WidgetTester tester, String result, {String confirm = ''}) async {
  await tester.tap(find.text(result));
  await tester.pumpAndSettle();
  await tester.tap(inDialog(confirm.isEmpty ? result : confirm));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('báo kết quả: điểm và nút gọi, chỉ đường, ba nút kết quả cao 56 px', (tester) async {
    await openReport(tester, RouteServer(sampleRoute()));

    expect(find.text('Điểm 1/12'), findsOneWidget);
    expect(find.text('ĐH-1009 · Chị Lan'), findsOneWidget);
    expect(find.text('Dự kiến 10:39 · Khung nhận 09:00–11:00'), findsOneWidget);
    expect(find.bySemanticsLabel('Gọi 0905 123 008'), findsOneWidget);
    expect(find.text('Chỉ đường'), findsOneWidget);
    for (final result in ['Đã giao', 'Không giao được', 'Bỏ qua']) {
      expect(tester.getSize(find.ancestor(of: find.text(result), matching: find.byType(InkWell))).height, 56);
    }
  });

  testWidgets('đã giao có xác nhận, gửi DELIVERED rồi về tuyến với snackbar, điểm tiếp theo ở thẻ đầu (D5)', (
    tester,
  ) async {
    final server = RouteServer(sampleRoute());
    await openReport(tester, server);

    await tester.tap(find.text('Đã giao'));
    await tester.pumpAndSettle();
    expect(find.text('Xác nhận đã giao ĐH-1009?'), findsOneWidget);
    expect(find.text('Kết quả gửi về cho điều phối viên ngay và không sửa được sau khi gửi.'), findsOneWidget);
    await tester.tap(inDialog('Đã giao'));
    await tester.pumpAndSettle();

    expect(server.patches, [
      {'status': 'DELIVERED'},
    ]);
    expect(find.widgetWithText(SnackBar, 'Đã giao điểm 1.'), findsOneWidget);
    expect(find.text('1/12'), findsOneWidget);
    expect(find.text('ĐH-1015 · Anh Khoa'), findsOneWidget);
  });

  testWidgets('quay lại ở hộp xác nhận thì không gửi gì', (tester) async {
    final server = RouteServer(sampleRoute());
    await openReport(tester, server);

    await tester.tap(find.text('Bỏ qua'));
    await tester.pumpAndSettle();
    await tester.tap(inDialog('Quay lại'));
    await tester.pumpAndSettle();

    expect(server.patches, isEmpty);
    expect(find.text('Điểm 1/12'), findsOneWidget);
  });

  testWidgets('bỏ qua hỏi bằng hộp danger rồi gửi SKIPPED', (tester) async {
    final server = RouteServer(sampleRoute());
    await openReport(tester, server);

    await tester.tap(find.text('Bỏ qua'));
    await tester.pumpAndSettle();
    expect(find.text('Bỏ qua ĐH-1009?'), findsOneWidget);
    await tester.tap(inDialog('Bỏ qua'));
    await tester.pumpAndSettle();

    expect(server.patches, [
      {'status': 'SKIPPED'},
    ]);
    expect(find.widgetWithText(SnackBar, 'Đã bỏ qua điểm 1.'), findsOneWidget);
  });

  testWidgets('không giao được: bắt buộc lý do, chọn nhanh được, gửi FAILED kèm lý do', (tester) async {
    final server = RouteServer(sampleRoute());
    await openReport(tester, server);

    await tester.tap(find.text('Không giao được'));
    await tester.pumpAndSettle();
    expect(find.text('Không giao được ĐH-1009'), findsOneWidget);
    expect(find.text('0/255'), findsOneWidget);

    // Blank or spaces only: the send button stays off.
    await tester.enterText(find.byType(TextField), '   ');
    await tester.pump();
    await tester.tap(inSheet('Gửi kết quả'));
    await tester.pumpAndSettle();
    expect(server.patches, isEmpty);

    await tester.tap(find.text('Vắng nhà'));
    await tester.pump();
    expect(find.text('8/255'), findsOneWidget);
    await tester.tap(inSheet('Gửi kết quả'));
    await tester.pumpAndSettle();

    expect(server.patches, [
      {'status': 'FAILED', 'failureReason': 'Vắng nhà'},
    ]);
    expect(find.widgetWithText(SnackBar, 'Đã báo không giao được điểm 1.'), findsOneWidget);
    expect(find.text('ĐH-1015 · Anh Khoa'), findsOneWidget);
  });

  testWidgets('lý do dừng ở 255 đơn vị UTF-16 như server đếm', (tester) async {
    await openReport(tester, RouteServer(sampleRoute()));
    await tester.tap(find.text('Không giao được'));
    await tester.pumpAndSettle();

    // "ệ" typed as e + two combining marks: 3 code units for one letter.
    await tester.enterText(find.byType(TextField), 'ệ' * 100);
    await tester.pump();

    final text = tester.widget<TextField>(find.byType(TextField)).controller!.text;
    expect(text.length, 255);
    expect(find.text('255/255'), findsOneWidget);
  });

  testWidgets('mất mạng: báo chưa gửi được, khoá kết quả khác, "Gửi lại" gửi đúng nội dung cũ', (tester) async {
    final server = RouteServer(sampleRoute())..online = false;
    await openReport(tester, server);

    await tester.tap(find.text('Không giao được'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Sai địa chỉ'));
    await tester.pump();
    await tester.tap(inSheet('Gửi kết quả'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Chưa gửi được: mất kết nối.', findRichText: true), findsOneWidget);
    expect(
      find.textContaining('Chờ có sóng rồi bấm Gửi lại; gửi lại cùng kết quả không bị trùng.', findRichText: true),
      findsOneWidget,
    );
    // Only the same result may go: the others stay locked.
    await tester.tap(find.text('Đã giao'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsNothing);

    // The network comes back, but the first answer is lost after the server recorded the result.
    server
      ..online = true
      ..loseResponse = true;
    await tester.tap(find.text('Gửi lại'));
    await tester.pumpAndSettle();
    expect(find.text('Gửi lại'), findsOneWidget);

    await tester.tap(find.text('Gửi lại'));
    await tester.pumpAndSettle();

    // The same body both times; the server answers the second with 200 without changing anything (P-36).
    expect(server.patches, [
      {'status': 'FAILED', 'failureReason': 'Sai địa chỉ'},
      {'status': 'FAILED', 'failureReason': 'Sai địa chỉ'},
    ]);
    expect(find.widgetWithText(SnackBar, 'Đã báo không giao được điểm 1.'), findsOneWidget);
  });

  testWidgets('mất response nhưng server đã ghi: lần tải lại tuyến 30 giây sau cho thấy đã xong', (tester) async {
    final server = RouteServer(sampleRoute())..loseResponse = true;
    await openReport(tester, server);
    await report(tester, 'Đã giao');
    expect(find.text('Gửi lại'), findsOneWidget);

    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();

    expect(find.text('Đã báo giao thành công ĐH-1009'), findsOneWidget);
    expect(server.patches, hasLength(1));
  });

  testWidgets('mở màn báo kết quả thì đọc lại tuyến; plan vừa bị huỷ (PLAN_NOT_ACTIVE) thì bảo dừng giao', (
    tester,
  ) async {
    final server = RouteServer(sampleRoute());
    await pumpApp(tester, server.server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/64');
    final reads = server.routeReads;

    server.cancelled = true;
    await tester.tap(find.text('Báo kết quả'));
    await tester.pumpAndSettle();

    expect(server.routeReads, reads + 1);
    expect(find.textContaining('Plan đã kết thúc hoặc đã bị huỷ.', findRichText: true), findsOneWidget);
    expect(find.textContaining('Dừng giao và liên hệ điều phối viên.', findRichText: true), findsOneWidget);
    expect(find.text('Đã giao'), findsNothing);

    await tester.tap(find.text('Về danh sách tuyến'));
    await tester.pumpAndSettle();
    expect(find.text('Tuyến của tôi'), findsOneWidget);
  });

  testWidgets('chi tiết tuyến tự tải lại mỗi 30 giây; plan bị huỷ giữa chừng thì bảo dừng giao', (tester) async {
    final server = RouteServer(sampleRoute());
    await pumpApp(tester, server.server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/64');
    final reads = server.routeReads;

    await tester.pump(const Duration(seconds: 29));
    expect(server.routeReads, reads);
    server.cancelled = true;
    await tester.pump(const Duration(seconds: 1));
    await tester.pumpAndSettle();

    expect(server.routeReads, reads + 1);
    expect(find.textContaining('Plan đã kết thúc hoặc đã bị huỷ.', findRichText: true), findsOneWidget);
    expect(find.textContaining('Dừng giao và liên hệ điều phối viên.', findRichText: true), findsOneWidget);
  });

  testWidgets('tải lại không tới được server (sóng yếu) thì giữ nguyên tuyến đang hiện', (tester) async {
    final server = RouteServer(sampleRoute());
    await pumpApp(tester, server.server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/64');

    server.routeOnline = false;
    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();

    expect(find.text('ĐH-1009 · Chị Lan'), findsOneWidget);
    expect(find.text('Báo kết quả'), findsOneWidget);
  });

  testWidgets('app chạy nền thì không tải lại; trở lại app thì tải lại', (tester) async {
    final server = RouteServer(sampleRoute());
    await pumpApp(tester, server.server, refreshToken: 'refresh-1');
    await goTo(tester, '/routes/64');
    final reads = server.routeReads;

    for (final state in [AppLifecycleState.inactive, AppLifecycleState.hidden, AppLifecycleState.paused]) {
      tester.binding.handleAppLifecycleStateChanged(state);
    }
    await tester.pump(const Duration(minutes: 2));
    expect(server.routeReads, reads);

    for (final state in [AppLifecycleState.hidden, AppLifecycleState.inactive, AppLifecycleState.resumed]) {
      tester.binding.handleAppLifecycleStateChanged(state);
    }
    await tester.pumpAndSettle();
    expect(server.routeReads, reads + 1);
  });

  testWidgets('PLAN_NOT_ACTIVE: báo plan đã kết thúc hoặc bị huỷ, khoá nút', (tester) async {
    final server = RouteServer(sampleRoute())..refuse = FakeReply.fail(422, 'PLAN_NOT_ACTIVE');
    await openReport(tester, server);
    await report(tester, 'Đã giao');

    expect(find.textContaining('Plan đã kết thúc hoặc đã bị huỷ.', findRichText: true), findsOneWidget);
    expect(
      find.textContaining('Không báo kết quả được nữa. Liên hệ điều phối viên nếu cần.', findRichText: true),
      findsOneWidget,
    );

    await tester.tap(find.text('Bỏ qua'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsNothing);

    await tester.tap(find.text('Về danh sách tuyến'));
    await tester.pumpAndSettle();
    expect(find.text('Tuyến của tôi'), findsOneWidget);
  });

  testWidgets('INVALID_STOP_TRANSITION: báo điểm đã có kết quả', (tester) async {
    final server = RouteServer(sampleRoute())..refuse = FakeReply.fail(422, 'INVALID_STOP_TRANSITION');
    await openReport(tester, server);
    await report(tester, 'Đã giao');

    expect(find.textContaining('Điểm giao đã được báo kết quả, không đổi được.', findRichText: true), findsOneWidget);
    expect(find.text('Gửi lại'), findsNothing);
  });

  testWidgets('điểm cuối: gửi xong về tuyến đã hoàn thành, có snackbar', (tester) async {
    final server = RouteServer(
      sampleRoute(
        finished: {for (var sequence = 1; sequence <= 11; sequence++) sequence: ('DELIVERED', null)},
        status: 'IN_PROGRESS',
      ),
    );
    await openReport(tester, server);
    expect(find.text('Điểm 12/12'), findsOneWidget);

    await report(tester, 'Đã giao');

    expect(find.widgetWithText(SnackBar, 'Đã giao điểm 12.'), findsOneWidget);
    expect(find.text('HOÀN THÀNH'), findsOneWidget);
    expect(find.text('Bạn đã xong tuyến này.'), findsOneWidget);
  });

  testWidgets('thanh trên M3 có nút back và "Điểm 1/12"; "Gọi khách", "Chỉ đường" là nút tonal (D5)', (tester) async {
    await openReport(tester, RouteServer(sampleRoute()));

    expect(find.descendant(of: find.byType(AppBar), matching: find.text('Điểm 1/12')), findsOneWidget);
    expect(find.byTooltip('Về chi tiết tuyến'), findsOneWidget);
    for (final label in ['Gọi khách', 'Chỉ đường']) {
      expect(tester.widget<PwButton>(find.widgetWithText(PwButton, label)).variant, PwButtonVariant.tonal);
    }

    await tester.tap(find.byTooltip('Về chi tiết tuyến'));
    await tester.pumpAndSettle();
    expect(find.text('ĐIỂM TIẾP THEO'), findsOneWidget);
  });

  testWidgets('lý do không giao được là bottom sheet: gợi ý dưới ô, sheet nổi trên bàn phím (D5, Q3)', (tester) async {
    // A phone (Pixel 8, 412 × 914 dp).
    tester.view
      ..physicalSize = const Size(1080, 2400)
      ..devicePixelRatio = 2.625;
    addTearDown(tester.view.reset);
    await openReport(tester, RouteServer(sampleRoute()));
    await tester.tap(find.text('Không giao được'));
    await tester.pumpAndSettle();

    expect(find.byType(BottomSheet), findsOneWidget);
    expect(find.byType(AlertDialog), findsNothing);
    expect(find.byType(ActionChip), findsNWidgets(4));
    expect(tester.getTopLeft(find.text('Vắng nhà')).dy, greaterThan(tester.getBottomLeft(find.byType(TextField)).dy));

    // The keyboard takes the bottom 300 px: "Gửi kết quả" stays above it.
    tester.view.viewInsets = FakeViewPadding(bottom: 300 * tester.view.devicePixelRatio);
    addTearDown(tester.view.resetViewInsets);
    await tester.pumpAndSettle();
    final screen = tester.view.physicalSize.height / tester.view.devicePixelRatio;
    expect(tester.getBottomLeft(inSheet('Gửi kết quả')).dy, lessThanOrEqualTo(screen - 300));
  });
}
