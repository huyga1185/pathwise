import 'package:flutter/material.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';

/// Header of the prototype "Tài xế — đăng nhập": brand gradient, street grid, a dashed route to a pin; the logo and
/// tagline sit at the bottom right, clear of the route.
class LoginHeader extends StatelessWidget {
  const LoginHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 280,
      decoration: const BoxDecoration(gradient: PwGradients.brandGradient),
      child: Stack(
        children: [
          const Positioned.fill(
            child: ExcludeSemantics(child: CustomPaint(painter: _RoutePainter())),
          ),
          Positioned(
            right: PwSpacing.space6,
            bottom: PwSpacing.space6,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const PathwiseMarkWhite(),
                    const SizedBox(width: PwSpacing.space2),
                    Text(Vi.appName, style: PwTypeStyles.display.copyWith(color: Colors.white, fontSize: 24)),
                  ],
                ),
                const SizedBox(height: PwSpacing.space2),
                Text(Vi.appTagline, style: PwTypeStyles.bodyTouch.copyWith(color: Colors.white)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// The 390 × 280 drawing of the prototype, centred and scaled to fit.
class _RoutePainter extends CustomPainter {
  const _RoutePainter();

  static const _brand = Color(0xFF0B6A5C);

  @override
  void paint(Canvas canvas, Size size) {
    final scale = (size.width / 390).clamp(0.0, size.height / 280);
    canvas
      ..save()
      ..translate((size.width - 390 * scale) / 2, (size.height - 280 * scale) / 2)
      ..scale(scale);

    final grid = Paint()
      ..color = Colors.white.withValues(alpha: 0.09)
      ..strokeWidth = 10
      ..style = PaintingStyle.stroke;
    canvas
      ..save()
      ..translate(195, 140)
      ..rotate(-24 * 3.141592653589793 / 180)
      ..translate(-195, -140);
    for (final y in [40.0, 140.0, 240.0]) {
      canvas.drawLine(Offset(-100, y), Offset(500, y), grid);
    }
    for (final x in [40.0, 180.0, 320.0]) {
      canvas.drawLine(Offset(x, -100), Offset(x, 400), grid);
    }
    canvas.restore();

    final route = Path()
      ..moveTo(70, 230)
      ..cubicTo(110, 190, 150, 190, 190, 170)
      ..cubicTo(230, 150, 270, 120, 300, 80);
    final dash = Paint()
      ..color = Colors.white
      ..strokeWidth = 4
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;
    for (final metric in route.computeMetrics()) {
      for (var d = 0.0; d < metric.length; d += 19) {
        canvas.drawPath(metric.extractPath(d, (d + 10).clamp(0, metric.length)), dash);
      }
    }

    final pin = Path()
      ..moveTo(300, 84)
      ..cubicTo(300, 84, 278, 64, 278, 47)
      ..arcToPoint(const Offset(322, 47), radius: const Radius.circular(22))
      ..cubicTo(322, 64, 300, 84, 300, 84)
      ..close();
    canvas
      ..drawPath(pin, Paint()..color = Colors.white)
      ..drawCircle(const Offset(300, 47), 8, Paint()..color = _brand)
      ..restore();
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
