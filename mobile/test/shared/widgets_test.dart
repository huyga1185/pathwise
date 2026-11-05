import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/shared/status/status.dart';
import 'package:pathwise_driver/shared/theme/theme.dart';
import 'package:pathwise_driver/shared/ui/ui.dart';

Widget frame(Widget child, {Brightness brightness = Brightness.light}) => MaterialApp(
  theme: pwTheme(brightness),
  home: Scaffold(body: Center(child: child)),
);

void main() {
  testWidgets('nút đạt vùng chạm tối thiểu 48 px; nút kết quả 56 px (DESIGN_SYSTEM)', (tester) async {
    await tester.pumpWidget(frame(PwButton(label: 'Gọi', onPressed: () {})));
    expect(tester.getSize(find.byType(PwButton)).height, greaterThanOrEqualTo(48));
    expect(tester.getSize(find.byType(PwButton)).width, greaterThanOrEqualTo(48));
    await expectLater(tester, meetsGuideline(androidTapTargetGuideline));

    await tester.pumpWidget(
      frame(PwButton(label: 'Đã giao', variant: PwButtonVariant.success, size: PwButtonSize.xl, onPressed: () {})),
    );
    expect(tester.getSize(find.byType(PwButton)).height, 56);
    expect(
      tester.getSemantics(find.byType(PwButton)),
      isSemantics(label: 'Đã giao', isButton: true, isEnabled: true, hasTapAction: true),
    );
  });

  testWidgets('StatusPill lấy nhãn từ statusInfo, ở cả theme tối', (tester) async {
    await tester.pumpWidget(
      frame(
        const StatusPill(kind: StatusKind.route, status: 'IN_PROGRESS'),
        brightness: Brightness.dark,
      ),
    );
    expect(find.text('ĐANG GIAO'), findsOneWidget);
    await tester.pumpWidget(frame(const StatusPill(kind: StatusKind.geocode, status: 'RESOLVED')));
    expect(find.byType(Text), findsNothing);
  });

  testWidgets('StopCard hiện số điện thoại định dạng và lý do khi không giao được', (tester) async {
    await tester.pumpWidget(
      frame(
        const StopCard(
          sequence: 3,
          title: 'ĐH-1009 · Chị Lan',
          address: '887 Nguyễn Tri Phương',
          phone: '+84905123008',
          status: 'FAILED',
          failureReason: 'Vắng nhà',
        ),
      ),
    );
    expect(find.text('0905 123 008'), findsOneWidget);
    expect(find.text('Lý do: Vắng nhà'), findsOneWidget);
    expect(find.text('KHÔNG GIAO ĐƯỢC'), findsOneWidget);
  });
}
