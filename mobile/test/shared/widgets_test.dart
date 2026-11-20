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

  testWidgets('nút theo kiểu Material 3: tô nền, tonal, viền, chữ; hình viên thuốc mặc định của M3 (P-77)', (
    tester,
  ) async {
    for (final (variant, type) in [
      (PwButtonVariant.filled, FilledButton),
      (PwButtonVariant.tonal, FilledButton),
      (PwButtonVariant.outlined, OutlinedButton),
      (PwButtonVariant.text, TextButton),
      (PwButtonVariant.danger, FilledButton),
    ]) {
      await tester.pumpWidget(frame(PwButton(label: 'Lưu', variant: variant, onPressed: () {})));
      expect(find.byType(type), findsOneWidget, reason: '$variant');
      final material = tester.widget<Material>(find.descendant(of: find.byType(type), matching: find.byType(Material)));
      expect(material.shape, isA<StadiumBorder>(), reason: '$variant');
    }
  });

  testWidgets('nút kết quả bị tắt thì mang màu tắt của M3, không giữ màu đỏ', (tester) async {
    await tester.pumpWidget(frame(const PwButton(label: 'Gửi kết quả', variant: PwButtonVariant.danger)));
    final material = tester.widget<Material>(
      find.descendant(of: find.byType(FilledButton), matching: find.byType(Material)),
    );
    final context = tester.element(find.byType(PwButton));
    expect(material.color, isNot(context.pw.danger));
    expect(
      tester.getSemantics(find.byType(PwButton)),
      isSemantics(label: 'Gửi kết quả', isButton: true, isEnabled: false),
    );
  });

  testWidgets('nút đang chạy giữ màu, hiện vòng chờ, bấm không gọi lại', (tester) async {
    var taps = 0;
    await tester.pumpWidget(
      frame(PwButton(label: 'Lưu', variant: PwButtonVariant.filled, busy: true, onPressed: () => taps++)),
    );
    final material = tester.widget<Material>(
      find.descendant(of: find.byType(FilledButton), matching: find.byType(Material)),
    );
    expect(material.color, Theme.of(tester.element(find.byType(PwButton))).colorScheme.primary);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    await tester.tap(find.byType(PwButton));
    expect(taps, 0);
    expect(tester.getSemantics(find.byType(PwButton)), isSemantics(label: 'Lưu', isButton: true, isEnabled: false));
  });

  testWidgets('hộp xác nhận: hai nút chữ canh cuối; việc phá huỷ màu danger', (tester) async {
    await tester.pumpWidget(
      frame(
        Builder(
          builder: (context) => PwButton(
            label: 'Mở',
            onPressed: () =>
                showPwDialog(context, title: 'Đăng xuất?', body: 'B', confirmLabel: 'Đăng xuất', danger: true),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Mở'));
    await tester.pumpAndSettle();
    expect(find.descendant(of: find.byType(AlertDialog), matching: find.byType(TextButton)), findsNWidgets(2));
    final confirm = tester.widget<Text>(find.descendant(of: find.byType(TextButton), matching: find.text('Đăng xuất')));
    final context = tester.element(find.byType(AlertDialog));
    expect(DefaultTextStyle.of(tester.element(find.byWidget(confirm))).style.color, context.pw.danger);
    expect(
      tester.getTopRight(find.text('Đăng xuất').last).dx,
      greaterThan(tester.getTopRight(find.text('Quay lại')).dx),
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

  testWidgets('StopCard có nút gọi với số định dạng, đọc số thứ tự và lý do khi không giao được', (tester) async {
    var calls = 0;
    await tester.pumpWidget(
      frame(
        StopCard(
          sequence: 3,
          title: 'ĐH-1009 · Chị Lan',
          address: '887 Nguyễn Tri Phương',
          phone: '+84905123008',
          onCall: () => calls++,
          status: 'FAILED',
          failureReason: 'Vắng nhà',
        ),
      ),
    );
    expect(find.text('Lý do: Vắng nhà'), findsOneWidget);
    expect(find.text('KHÔNG GIAO ĐƯỢC'), findsOneWidget);
    expect(find.bySemanticsLabel('Điểm số 3'), findsOneWidget);
    await tester.tap(find.text('Gọi 0905 123 008'));
    expect(calls, 1);
  });

  testWidgets('StopCard không có nút gọi khi không có số hoặc không có hành động gọi', (tester) async {
    await tester.pumpWidget(frame(const StopCard(title: 'ĐH-1015', address: '893 Lê Duẩn', phone: '+84905123014')));
    expect(find.textContaining('Gọi'), findsNothing);
  });
}
