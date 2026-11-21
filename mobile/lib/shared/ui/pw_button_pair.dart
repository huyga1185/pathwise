import 'package:flutter/material.dart';

import '../theme/tokens.g.dart';

/// Two buttons side by side, or one above the other when a label would not fit on one line in half the width (large
/// system text): two halves of different heights, one label broken in two (D4). [labels] are the two buttons' labels,
/// measured in the button's style.
class PwButtonPair extends StatelessWidget {
  const PwButtonPair({super.key, required this.first, required this.second, required this.labels});

  final Widget first;
  final Widget second;
  final List<String> labels;

  @override
  Widget build(BuildContext context) {
    final scaler = MediaQuery.textScalerOf(context);
    return LayoutBuilder(
      builder: (context, constraints) {
        final half = (constraints.maxWidth - PwSpacing.space2) / 2;
        final fits = labels.every((label) {
          final painter = TextPainter(
            text: TextSpan(text: label, style: PwTypeStyles.heading),
            textDirection: TextDirection.ltr,
            textScaler: scaler,
          )..layout();
          final width = painter.width;
          painter.dispose();
          // Material 3 buttons pad 24 dp each side, less as text grows; 48 is the most it can take.
          return width + 48 <= half;
        });
        if (fits) {
          return Row(
            children: [
              Expanded(child: first),
              const SizedBox(width: PwSpacing.space2),
              Expanded(child: second),
            ],
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            first,
            const SizedBox(height: PwSpacing.space2),
            second,
          ],
        );
      },
    );
  }
}
