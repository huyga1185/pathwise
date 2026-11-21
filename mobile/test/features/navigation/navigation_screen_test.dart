import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_driver/data/api_error.dart';
import 'package:pathwise_driver/platform/clock.dart';
import 'package:pathwise_driver/platform/location.dart';
import 'package:pathwise_driver/platform/map/basemap.dart';
import 'package:pathwise_driver/shared/geo.dart';
import 'package:pathwise_driver/shared/ui/ui.dart';

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
  String attribution = '',
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
    basemap: blankBasemap(attribution),
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

  testWidgets('đứng yên, luồng vị trí chưa gửi gì: xin một vị trí riêng rồi xin đường từ đó', (tester) async {
    // A stream filtered by distance may say nothing while the phone stands still (the bug on a real phone).
    final location = FakeLocationSource()..currentFix = at(start);
    final nav = await openNavigation(tester, location: location);

    expect(location.currentRequests, 1);
    expect(nav.directions.requests.single.from, start);
    expect(find.text('Rẽ trái vào Nguyễn Văn Linh'), findsOneWidget);

    // The stream keeps the guidance going once it starts.
    await tester.pump(const Duration(seconds: 16));
    expect(find.text('Chưa lấy được vị trí của bạn.'), findsNothing);
  });

  testWidgets('"Thử lại" sau 15 giây cũng xin lại một vị trí riêng', (tester) async {
    final location = FakeLocationSource();
    final nav = await openNavigation(tester, location: location);
    await tester.pump(const Duration(seconds: 16));
    expect(find.text('Chưa lấy được vị trí của bạn.'), findsOneWidget);

    location.currentFix = at(start);
    await tester.tap(find.text('Thử lại'));
    await settle(tester);

    expect(location.currentRequests, 2);
    expect(nav.directions.requests.single.from, start);
  });

  PwButtonVariant variantOf(WidgetTester tester, String label) =>
      tester.widget<PwButton>(find.widgetWithText(PwButton, label)).variant;

  testWidgets('mức nhấn nút: "Báo kết quả" tonal khi đang đi, filled khi đã tới; Google Maps viền (D6)', (
    tester,
  ) async {
    final nav = await openNavigation(tester);
    await emit(tester, nav.location, at(start));
    expect(variantOf(tester, 'Báo kết quả'), PwButtonVariant.tonal);
    expect(variantOf(tester, 'Mở Google Maps'), PwButtonVariant.outlined);

    await emit(tester, nav.location, at(const GeoPoint(16.0481, 108.196)));
    expect(variantOf(tester, 'Báo kết quả'), PwButtonVariant.filled);
  });

  testWidgets('màn báo lỗi: việc chính ("Thử lại") filled, Google Maps viền (D6)', (tester) async {
    final directions = FakeDirectionsSource()..error = const ApiError(code: 'ROUTING_PROVIDER_ERROR', status: 502);
    final nav = await openNavigation(tester, directions: directions);
    await emit(tester, nav.location, at(start));

    expect(variantOf(tester, 'Thử lại'), PwButtonVariant.filled);
    expect(variantOf(tester, 'Mở Google Maps'), PwButtonVariant.outlined);
  });

  testWidgets('tên điểm dài ở thanh dưới tối đa hai dòng, không che bản đồ (D6)', (tester) async {
    final route = sampleRoute();
    final stops = (route['stops']! as List<Object?>).cast<Map<String, Object?>>();
    stops.firstWhere((stop) => stop['id'] == 116)['label'] =
        'Văn phòng Công ty TNHH Thương mại Dịch vụ và Xuất nhập khẩu Hoàng Gia Phát (tầng 7, toà nhà Vĩnh Trung Plaza)';
    final nav = await openNavigation(tester, route: route);
    await emit(tester, nav.location, at(start));

    final name = tester.widget<Text>(
      find.text(
        'Văn phòng Công ty TNHH Thương mại Dịch vụ và Xuất nhập khẩu Hoàng Gia Phát (tầng 7, toà nhà Vĩnh Trung Plaza) '
        '· Chị Lan',
      ),
    );
    expect(name.maxLines, 2);
    expect(name.overflow, TextOverflow.ellipsis);
  });

  testWidgets('ghi công bản đồ nằm trong thanh dưới, không bị thanh che (D6)', (tester) async {
    final nav = await openNavigation(tester, attribution: '© OpenStreetMap contributors');
    await emit(tester, nav.location, at(start));

    final credit = find.text('© OpenStreetMap contributors');
    expect(credit, findsOneWidget);
    expect(find.ancestor(of: credit, matching: find.byType(Material)), findsWidgets);
    expect(tester.getBottomLeft(credit).dy, greaterThan(tester.getTopLeft(find.text('Báo kết quả')).dy));
  });

  testWidgets('bản đồ sáng ở cả theme tối: biểu tượng thanh trạng thái màu tối (D6)', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    final nav = await openNavigation(tester);
    await emit(tester, nav.location, at(start));

    final regions = tester.widgetList<AnnotatedRegion<SystemUiOverlayStyle>>(
      find.byType(AnnotatedRegion<SystemUiOverlayStyle>),
    );
    expect(regions.map((region) => region.value.statusBarIconBrightness), contains(Brightness.dark));
  });

  // geolocator's foreground service takes a partial wake lock (enableWakeLock in platform/location.dart); without the
  // permission its position stream failed at once on Android, so no position was ever shared or used to navigate (D6).
  test('AndroidManifest khai báo WAKE_LOCK vì dịch vụ vị trí giữ wake lock', () {
    final manifest = File('android/app/src/main/AndroidManifest.xml').readAsStringSync();
    final location = File('lib/platform/location.dart').readAsStringSync();
    expect(location, contains('enableWakeLock: true'));
    expect(manifest, contains('<uses-permission android:name="android.permission.WAKE_LOCK" />'));
  });
}
