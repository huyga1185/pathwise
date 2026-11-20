import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:pathwise_driver/shared/ui/ui.dart';

import '../support/app.dart';
import '../support/fake_api.dart';

/// A signed-in driver whose `/users/me` answers GET with [me] and PATCH with [onPatch] (M06).
FakeServer profileServer({
  Map<String, Object?> me = const {'phoneNumber': '+84905123456'},
  Future<FakeReply> Function(RequestOptions request)? onPatch,
}) => FakeServer((request) async {
  if (request.path == '/api/v1/auth/refresh') return FakeReply.ok(tokens());
  if (request.path == '/api/v1/users/me') {
    if (request.method == 'PATCH') {
      if (onPatch != null) return onPatch(request);
      final body = bodyOf(request)! as Map<String, Object?>;
      return FakeReply.ok({...meOf(driverUser), ...body, 'phoneNumber': null});
    }
    return FakeReply.ok({...meOf(driverUser), ...me});
  }
  return FakeReply.ok(<Object>[]);
});

Iterable<RequestOptions> patches(FakeServer server) =>
    server.requests.where((request) => request.path == '/api/v1/users/me' && request.method == 'PATCH');

Future<void> openProfile(WidgetTester tester) async {
  await tester.tap(find.text('TA'));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Thông tin của tôi'));
  await tester.pumpAndSettle();
}

Finder input(String label) =>
    find.descendant(of: find.widgetWithText(PwTextField, label), matching: find.byType(EditableText));

EditableText field(WidgetTester tester, String label) => tester.widget<EditableText>(input(label));

void main() {
  testWidgets('menu tài khoản mở "Thông tin của tôi" có họ tên và số điện thoại hiện tại (M06)', (tester) async {
    await pumpApp(tester, profileServer(), refreshToken: 'refresh-1');
    await openProfile(tester);

    expect(find.widgetWithText(AppBar, 'Thông tin của tôi'), findsOneWidget);
    expect(field(tester, 'Họ tên').controller.text, 'Tài xế A');
    expect(field(tester, 'Số điện thoại').controller.text, '0905 123 456');
    expect(find.text('Tuỳ chọn. Chỉ nhận số Việt Nam.'), findsOneWidget);
  });

  testWidgets('lưu gửi merge patch; ô để trống gửi null; xong báo "Đã lưu thông tin." và về danh sách', (tester) async {
    final server = profileServer();
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    await openProfile(tester);

    await tester.enterText(input('Họ tên'), '  Trần Văn Bình ');
    await tester.enterText(input('Số điện thoại'), '');
    await tester.tap(find.text('Lưu'));
    await tester.pumpAndSettle();

    expect(bodyOf(patches(server).single), {'fullName': 'Trần Văn Bình', 'phoneNumber': null});
    expect(find.text('Đã lưu thông tin.'), findsOneWidget);
    expect(find.widgetWithText(AppBar, 'Tuyến của tôi'), findsOneWidget);
    // The initials follow the new name.
    expect(find.text('TB'), findsOneWidget);
  });

  testWidgets('lỗi số điện thoại hiện dưới ô và ở lại màn', (tester) async {
    final server = profileServer(
      onPatch: (request) async => const FakeReply(400, {
        'code': 'VALIDATION_ERROR',
        'details': [
          {'field': 'phoneNumber', 'code': 'PHONE_NUMBER_WRONG_REGION'},
        ],
      }),
    );
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    await openProfile(tester);

    await tester.enterText(input('Số điện thoại'), '+1 202 555 0100');
    await tester.tap(find.text('Lưu'));
    await tester.pumpAndSettle();

    expect(find.text('Chỉ nhận số Việt Nam.'), findsOneWidget);
    expect(find.text('Tuỳ chọn. Chỉ nhận số Việt Nam.'), findsNothing);
    expect(find.widgetWithText(AppBar, 'Thông tin của tôi'), findsOneWidget);
  });

  testWidgets('quay lại khi chưa lưu thì hỏi; "Bỏ thay đổi" về danh sách mà không gửi gì', (tester) async {
    final server = profileServer();
    await pumpApp(tester, server, refreshToken: 'refresh-1');
    await openProfile(tester);

    await tester.enterText(input('Họ tên'), 'Tên mới');
    // The frame that rebuilds the form as changed (a person never taps back within the same frame).
    await tester.pump();
    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(find.text('Bỏ thay đổi chưa lưu?'), findsOneWidget);

    await tester.tap(find.text('Ở lại'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(AppBar, 'Thông tin của tôi'), findsOneWidget);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Bỏ thay đổi'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(AppBar, 'Tuyến của tôi'), findsOneWidget);
    expect(patches(server), isEmpty);
  });

  testWidgets('chưa sửa gì thì quay lại không hỏi', (tester) async {
    await pumpApp(tester, profileServer(), refreshToken: 'refresh-1');
    await openProfile(tester);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();

    expect(find.text('Bỏ thay đổi chưa lưu?'), findsNothing);
    expect(find.widgetWithText(AppBar, 'Tuyến của tôi'), findsOneWidget);
  });
}
