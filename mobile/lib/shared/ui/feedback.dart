import 'package:flutter/material.dart';

import '../theme/theme.dart';
import '../theme/tokens.g.dart';

/// Error box at the top of a form (announced to screen readers); field errors go under the field instead. [action]
/// (a button such as "Gửi lại") sits under the text.
class ErrorBanner extends StatelessWidget {
  const ErrorBanner({super.key, required this.title, this.detail, this.action});

  final String title;
  final String? detail;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4, vertical: PwSpacing.space2),
        decoration: BoxDecoration(
          color: t.surfaceRaised,
          border: Border.all(color: t.danger),
          borderRadius: BorderRadius.circular(PwRadius.radiusMd),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: title,
                    style: TextStyle(color: t.danger, fontWeight: FontWeight.w600),
                  ),
                  if (detail != null) TextSpan(text: ' $detail'),
                ],
              ),
              style: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
            ),
            if (action != null) ...[const SizedBox(height: PwSpacing.space2), action!],
          ],
        ),
      ),
    );
  }
}

/// The white PathWise mark (brand/logo, rasterised by scripts/build_mobile_brand.sh).
class PathwiseMarkWhite extends StatelessWidget {
  const PathwiseMarkWhite({super.key, this.height = 40});

  final double height;

  @override
  Widget build(BuildContext context) =>
      Image.asset('assets/brand/pathwise-mark-white.png', height: height, excludeFromSemantics: true);
}
