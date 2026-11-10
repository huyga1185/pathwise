import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/platform/location.dart';
import 'package:pathwise_driver/platform/preferences.dart';

import '../support/app.dart';
import '../support/fake_api.dart';
import '../support/location.dart';
import '../support/routes.dart';

/// Route 64 on a server that takes positions and sharing states (T32); [route] answers the route, [location] the
/// batches of positions.
class SharingServer {
  SharingServer({Map<String, Object?>? route}) : route = route ?? sampleRoute() {
    server = driverServer((request) async {
      switch (request.path) {
        case '/api/v1/driver/routes/64':
          return FakeReply.ok(this.route);
        case '/api/v1/driver/location/sharing':
          states.add((bodyOf(request)! as Map<String, Object?>)['state']! as String);
          return sharingReply;
        case '/api/v1/driver/location':
          final reply = locationReplies.isEmpty ? const FakeReply(204) : locationReplies.removeAt(0);
          batches.add((bodyOf(request)! as Map<String, Object?>)['points']! as List<Object?>);
          return reply;
      }
      return FakeReply.ok(<Object>[]);
    });
  }

  late final FakeServer server;
  Map<String, Object?> route;
  final states = <String>[];
  final batches = <List<Object?>>[];
  final locationReplies = <FakeReply>[];
  FakeReply sharingReply = const FakeReply(204);
}

LocationFix fix(double lat, int second) => LocationFix(
  lat: lat,
  lon: 108.2,
  recordedAt: DateTime.utc(2026, 12, 1, 3, 0, second),
  headingDegrees: 90,
  speedMps: 7.5,
  accuracyMeters: 12,
);

Future<void> open(
  WidgetTester tester,
  SharingServer server, {
  required MemoryPreferences preferences,
  required FakeLocationSource location,
}) async {
  await pumpApp(tester, server.server, refreshToken: 'refresh-1', preferences: preferences, location: location);
  await goTo(tester, '/routes/64');
}

/// The driver comes back from the phone's settings.
void backToApp(WidgetTester tester) {
  for (final state in [
    AppLifecycleState.inactive,
    AppLifecycleState.hidden,
    AppLifecycleState.paused,
    AppLifecycleState.hidden,
    AppLifecycleState.inactive,
    AppLifecycleState.resumed,
  ]) {
    tester.binding.handleAppLifecycleStateChanged(state);
  }
}

void main() {
  testWidgets('mặc định hỏi khi mở tuyến; chia sẻ thì báo SHARING, gửi điểm đầu ngay, hiện "Đang chia sẻ"', (
    tester,
  ) async {
    final server = SharingServer();
    final location = FakeLocationSource();
    await open(tester, server, preferences: MemoryPreferences(), location: location);

    expect(find.text('Chia sẻ vị trí cho tuyến này?'), findsOneWidget);
    expect(find.textContaining('Trong lúc giao tuyến Giao sáng 01/12'), findsOneWidget);
    await tester.tap(find.text('Chia sẻ vị trí'));
    await tester.pumpAndSettle();

    expect(server.states, ['SHARING']);
    expect(find.text('Đang chia sẻ vị trí với điều phối viên'), findsOneWidget);
    expect(location.watching, isTrue);
    expect(location.notificationText, 'Tuyến Giao sáng 01/12. Tự dừng khi tuyến xong hoặc bạn đăng xuất.');

    location.emit(fix(16.01, 0));
    await tester.pumpAndSettle();
    expect(server.batches, hasLength(1));
    expect(server.batches.single.single, {
      'lat': 16.01,
      'lon': 108.2,
      'accuracyMeters': 12.0,
      'headingDegrees': 90.0,
      'speedMps': 7.5,
      'recordedAt': '2026-12-01T03:00:00.000Z',
    });

    // Later points wait for the next send (every 30 s), in one batch.
    location
      ..emit(fix(16.02, 10))
      ..emit(fix(16.03, 20));
    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();
    expect(server.batches, hasLength(2));
    expect(server.batches.last, hasLength(2));
  });

  testWidgets('ghi nhớ "Không chia sẻ": lưu lựa chọn, báo DECLINED, bật lại được ngay trong tuyến', (tester) async {
    final server = SharingServer();
    final preferences = MemoryPreferences();
    final location = FakeLocationSource();
    await open(tester, server, preferences: preferences, location: location);

    await tester.tap(find.text('Ghi nhớ lựa chọn, không hỏi lại'));
    await tester.pump();
    await tester.tap(find.text('Không chia sẻ'));
    await tester.pumpAndSettle();

    expect(preferences.values['pw-location-sharing'], 'NEVER');
    expect(server.states, ['DECLINED']);
    expect(find.text('Bạn đang không chia sẻ vị trí cho tuyến này.'), findsOneWidget);
    expect(location.watching, isFalse);

    await tester.tap(find.text('Bật chia sẻ vị trí'));
    await tester.pumpAndSettle();
    expect(server.states, ['DECLINED', 'SHARING']);
    expect(find.text('Đang chia sẻ vị trí với điều phối viên'), findsOneWidget);
    expect(preferences.values['pw-location-route'], '64:1');
  });

  testWidgets('đã trả lời cho tuyến này thì mở lại không hỏi nữa', (tester) async {
    final server = SharingServer();
    await open(
      tester,
      server,
      preferences: MemoryPreferences({'pw-location-route': '64:0'}),
      location: FakeLocationSource(),
    );

    expect(find.text('Chia sẻ vị trí cho tuyến này?'), findsNothing);
    expect(find.text('Bạn đang không chia sẻ vị trí cho tuyến này.'), findsOneWidget);
  });

  testWidgets('"Luôn chia sẻ": không hỏi, tự bật khi mở tuyến', (tester) async {
    final server = SharingServer();
    final location = FakeLocationSource();
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);

    expect(find.text('Chia sẻ vị trí cho tuyến này?'), findsNothing);
    expect(server.states, ['SHARING']);
    expect(location.watching, isTrue);
  });

  testWidgets('menu tài khoản đổi lựa chọn chia sẻ, mặc định là "Hỏi"', (tester) async {
    final preferences = MemoryPreferences();
    await pumpApp(
      tester,
      driverServer((request) async => FakeReply.ok(<Object>[])),
      refreshToken: 'refresh-1',
      preferences: preferences,
    );

    await tester.tap(find.text('TA'));
    await tester.pumpAndSettle();
    expect(find.text('CHIA SẺ VỊ TRÍ KHI GIAO'), findsOneWidget);
    expect(
      tester.getSemantics(find.bySemanticsLabel(RegExp('^Hỏi khi bắt đầu mỗi tuyến'))),
      matchesSemantics(
        isChecked: true,
        hasCheckedState: true,
        isInMutuallyExclusiveGroup: true,
        isButton: true,
        hasTapAction: true,
      ),
    );

    await tester.tap(find.text('Luôn chia sẻ'));
    await tester.pumpAndSettle();
    expect(preferences.values['pw-location-sharing'], 'ALWAYS');

    await tester.tap(find.text('Hỏi khi bắt đầu mỗi tuyến'));
    await tester.pumpAndSettle();
    expect(preferences.values.containsKey('pw-location-sharing'), isFalse);
  });

  testWidgets('máy từ chối quyền: báo NO_PERMISSION; từ chối hẳn thì "Mở cài đặt", quay lại app thì kiểm lại', (
    tester,
  ) async {
    final server = SharingServer();
    final location = FakeLocationSource(access: LocationAccess.denied, afterRequest: LocationAccess.deniedForever);
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);

    expect(location.requests, 1);
    expect(server.states, ['NO_PERMISSION']);
    expect(find.text('Chưa chia sẻ vị trí.'), findsOneWidget);
    expect(find.textContaining('Trong cài đặt, cho phép PathWise dùng vị trí.'), findsOneWidget);

    await tester.tap(find.text('Mở cài đặt'));
    await tester.pumpAndSettle();
    expect(location.settingsOpened, 1);

    location.access = LocationAccess.granted;
    backToApp(tester);
    await tester.pumpAndSettle();
    expect(server.states.last, 'SHARING');
    expect(find.text('Đang chia sẻ vị trí với điều phối viên'), findsOneWidget);
  });

  testWidgets('GPS tắt: báo GPS_OFF, "Bật định vị" mở cài đặt định vị; tắt giữa chừng cũng vậy', (tester) async {
    final server = SharingServer();
    final location = FakeLocationSource(access: LocationAccess.serviceOff);
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);

    expect(server.states, ['GPS_OFF']);
    await tester.tap(find.text('Bật định vị'));
    await tester.pumpAndSettle();
    expect(location.locationSettingsOpened, 1);

    location.access = LocationAccess.granted;
    backToApp(tester);
    await tester.pumpAndSettle();
    expect(server.states.last, 'SHARING');

    location.fail(LocationAccess.serviceOff);
    await tester.pumpAndSettle();
    expect(server.states.last, 'GPS_OFF');
    expect(find.text('Định vị (GPS) đang tắt.'), findsOneWidget);
  });

  testWidgets('mất mạng: điểm chờ trong máy, lần gửi sau gửi cả lô', (tester) async {
    final server = SharingServer()..locationReplies.add(FakeReply.fail(503, 'INTERNAL_ERROR'));
    final location = FakeLocationSource();
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);

    location.emit(fix(16.01, 0));
    await tester.pumpAndSettle();
    location.emit(fix(16.02, 10));
    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();

    expect(server.batches, hasLength(2));
    expect(server.batches.last.map((point) => (point! as Map<String, Object?>)['lat']), [16.01, 16.02]);
  });

  testWidgets('tuyến xong thì dừng chia sẻ', (tester) async {
    final server = SharingServer();
    final location = FakeLocationSource();
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);
    expect(location.watching, isTrue);

    server.route = sampleRoute(
      finished: {for (var sequence = 1; sequence <= 12; sequence++) sequence: ('DELIVERED', null)},
      status: 'COMPLETED',
    );
    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();

    expect(location.watching, isFalse);
    expect(find.text('Đang chia sẻ vị trí với điều phối viên'), findsNothing);
  });

  testWidgets('plan không còn chạy (PLAN_NOT_ACTIVE khi báo trạng thái) thì dừng chia sẻ', (tester) async {
    final server = SharingServer()..sharingReply = FakeReply.fail(422, 'PLAN_NOT_ACTIVE');
    final location = FakeLocationSource();
    await open(tester, server, preferences: MemoryPreferences({'pw-location-sharing': 'ALWAYS'}), location: location);

    expect(location.watching, isFalse);
  });
}
