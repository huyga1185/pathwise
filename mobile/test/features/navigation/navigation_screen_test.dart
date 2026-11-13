import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_driver/data/api_error.dart';
import 'package:pathwise_driver/platform/clock.dart';
import 'package:pathwise_driver/platform/location.dart';
import 'package:pathwise_driver/shared/geo.dart';

import '../../support/app.dart';
import '../../support/fake_api.dart';
import '../../support/location.dart';
import '../../support/navigation.dart';
import '../../support/routes.dart';

/// A clock the test moves forward.
class MovingClock implements Clock {
  DateTime instant = sampleNow;

  @override
  DateTime now() => instant;
}

/// Stop 116 of the sample route (ĐH-1009 · Chị Lan).
const stop = GeoPoint(16.048, 108.196);

/// West of the stop on Lê Duẩn: the drive goes east, then left (south) at 108.196.
const start = GeoPoint(16.05, 108.19);

LocationFix at(GeoPoint point, {double accuracy = 5, double? heading}) => LocationFix(
  lat: point.lat,
  lon: point.lon,
  accuracyMeters: accuracy,
  headingDegrees: heading,
  recordedAt: sampleNow,
);

typedef Navigation = ({
  FakeLocationSource location,
  FakeDirectionsSource directions,
  FakeScreenAwake awake,
  FakeLauncher launcher,
  MovingClock clock,
});

/// Opens the directions to stop 116 over the route detail, as "Chỉ đường" does.
Future<Navigation> openNavigation(
  WidgetTester tester, {
  FakeLocationSource? location,
  FakeDirectionsSource? directions,
  Map<String, Object?>? route,
}) async {
  final nav = (
    location: location ?? FakeLocationSource(),
    directions: directions ?? FakeDirectionsSource(),
    awake: FakeScreenAwake(),
    launcher: FakeLauncher(),
    clock: MovingClock(),
  );
  final server = driverServer((request) async {
    if (request.path == '/api/v1/driver/routes/64') return FakeReply.ok(route ?? sampleRoute());
    return FakeReply.ok(<Object>[]);
  });
  await pumpApp(
    tester,
    server,
    refreshToken: 'refresh-1',
    clock: nav.clock,
    launcher: nav.launcher,
    location: nav.location,
    directions: nav.directions,
    screenAwake: nav.awake,
  );
  await goTo(tester, '/routes/64');
  unawaited(GoRouter.of(tester.element(find.byType(Scaffold).first)).push('/routes/64/navigate?stopId=116'));
  await settle(tester);
  return nav;
}

/// Lets requests and the page transition finish; a spinner never settles, so no pumpAndSettle.
Future<void> settle(WidgetTester tester) async {
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 500));
}

Future<void> emit(WidgetTester tester, FakeLocationSource location, LocationFix fix) async {
  location.emit(fix);
  await settle(tester);
}

void main() {
  testWidgets('dẫn đường: xin đường từ vị trí hiện tại tới điểm, hiện thao tác tiếp theo, giữ màn hình sáng', (
    tester,
  ) async {
    final nav = await openNavigation(tester);
    expect(find.text('Đang lấy vị trí của bạn…'), findsOneWidget);
    expect(nav.location.watchingPrecisely, isTrue);
    expect(nav.awake.on, isTrue);

    await emit(tester, nav.location, at(start, heading: 90));

    final request = nav.directions.requests.single;
    expect(request.routeId, 64);
    expect(request.toStopId, 116);
    expect(request.from, start);
    expect(find.text('Rẽ trái vào Nguyễn Văn Linh'), findsOneWidget);
    expect(find.text('640 m'), findsOneWidget);
    expect(find.text('ĐH-1009 · Chị Lan'), findsOneWidget);
    expect(find.text('Mở Google Maps'), findsOneWidget);
    expect(find.text('Báo kết quả'), findsOneWidget);

    await tester.tap(find.byTooltip('Về chi tiết tuyến'));
    await tester.pumpAndSettle();
    expect(find.text('ĐIỂM TIẾP THEO'), findsOneWidget);
    expect(nav.awake.on, isFalse, reason: 'the screen may sleep again');
    expect(nav.location.watchingPrecisely, isFalse);
  });

  testWidgets('lệch đường 3 lần liên tiếp thì xin đường mới từ vị trí đó, nhưng không quá 1 lần mỗi 20 giây', (
    tester,
  ) async {
    final nav = await openNavigation(tester);
    await emit(tester, nav.location, at(start));
    expect(nav.directions.requests, hasLength(1));

    // About 330 m north of Lê Duẩn, three times, right after the first request: too soon to ask again.
    const off = GeoPoint(16.053, 108.192);
    for (var i = 0; i < 3; i++) {
      await emit(tester, nav.location, at(off));
    }
    expect(nav.directions.requests, hasLength(1));

    nav.clock.instant = sampleNow.add(const Duration(seconds: 21));
    await emit(tester, nav.location, at(off));

    expect(nav.directions.requests, hasLength(2));
    expect(nav.directions.requests.last.from, off);
    expect(nav.directions.requests.last.toStopId, 116);
  });

  testWidgets('một điểm GPS lệch thì không xin đường mới', (tester) async {
    final nav = await openNavigation(tester);
    await emit(tester, nav.location, at(start));
    nav.clock.instant = sampleNow.add(const Duration(minutes: 1));

    await emit(tester, nav.location, at(const GeoPoint(16.053, 108.192)));
    await emit(tester, nav.location, at(const GeoPoint(16.05, 108.193)));
    await emit(tester, nav.location, at(const GeoPoint(16.053, 108.192)));

    expect(nav.directions.requests, hasLength(1));
  });

  testWidgets('tới nơi thì báo đã tới, "Báo kết quả" mở màn báo kết quả của điểm', (tester) async {
    final nav = await openNavigation(tester);
    await emit(tester, nav.location, at(start));

    await emit(tester, nav.location, at(const GeoPoint(16.0481, 108.196)));
    expect(find.text('Bạn đã tới ĐH-1009 · Chị Lan'), findsOneWidget);

    await tester.tap(find.text('Báo kết quả'));
    await tester.pumpAndSettle();
    expect(find.text('Điểm 1/12'), findsOneWidget);
  });

  testWidgets('máy chủ không tìm được đường (502): báo, "Thử lại" và mở Google Maps tới điểm', (tester) async {
    final directions = FakeDirectionsSource()..error = const ApiError(code: 'ROUTING_PROVIDER_ERROR', status: 502);
    final nav = await openNavigation(tester, directions: directions);
    await emit(tester, nav.location, at(start));

    expect(find.text('Không dẫn đường được lúc này.'), findsOneWidget);
    await tester.tap(find.text('Mở Google Maps'));
    await tester.pump();
    expect(nav.launcher.opened.single.toString(), 'https://www.google.com/maps/dir/?api=1&destination=16.048,108.196');

    directions.error = null;
    await tester.tap(find.text('Thử lại'));
    await settle(tester);
    await emit(tester, nav.location, at(start));
    expect(find.text('Rẽ trái vào Nguyễn Văn Linh'), findsOneWidget);
    expect(directions.requests, hasLength(2));
  });

  testWidgets('chưa có quyền vị trí: xin quyền; bị chặn hẳn thì "Mở cài đặt"', (tester) async {
    final location = FakeLocationSource(access: LocationAccess.denied, afterRequest: LocationAccess.deniedForever);
    final nav = await openNavigation(tester, location: location);

    expect(location.requests, 1, reason: 'opening the directions asks for the permission');
    expect(find.text('Máy đang chặn quyền vị trí của PathWise. Mở cài đặt để cho phép.'), findsOneWidget);
    await tester.tap(find.text('Mở cài đặt'));
    await tester.pump();
    expect(location.settingsOpened, 1);
    expect(nav.directions.requests, isEmpty);
  });

  testWidgets('GPS tắt: "Bật định vị" mở cài đặt định vị', (tester) async {
    final location = FakeLocationSource(access: LocationAccess.serviceOff);
    await openNavigation(tester, location: location);

    expect(find.text('Định vị (GPS) đang tắt.'), findsOneWidget);
    await tester.tap(find.text('Bật định vị'));
    await tester.pump();
    expect(location.locationSettingsOpened, 1);
  });

  testWidgets('plan đã kết thúc hoặc bị huỷ: báo dừng giao, không còn nút dẫn đường', (tester) async {
    final directions = FakeDirectionsSource()..error = const ApiError(code: 'PLAN_NOT_ACTIVE', status: 422);
    final nav = await openNavigation(tester, directions: directions);
    await emit(tester, nav.location, at(start));

    expect(find.textContaining('Dừng giao và liên hệ điều phối viên.'), findsOneWidget);
    expect(find.text('Về danh sách tuyến'), findsOneWidget);
    expect(find.text('Mở Google Maps'), findsNothing);
  });

  testWidgets('15 giây chưa có vị trí thì báo, vẫn mở được Google Maps', (tester) async {
    await openNavigation(tester);

    await tester.pump(const Duration(seconds: 16));

    expect(find.text('Chưa lấy được vị trí của bạn.'), findsOneWidget);
    expect(find.text('Thử lại'), findsOneWidget);
    expect(find.text('Mở Google Maps'), findsOneWidget);
  });
}
