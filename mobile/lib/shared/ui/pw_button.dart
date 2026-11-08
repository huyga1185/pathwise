import 'package:flutter/material.dart';

import '../theme/theme.dart';
import '../theme/tokens.g.dart';

enum PwButtonVariant { primary, secondary, quiet, danger, success, signal }

/// lg 48 px is the app's minimum touch target; xl 56 px is for the driver's result buttons (DESIGN_SYSTEM).
enum PwButtonSize { lg, xl }

/// Design system Button: one primary per screen; `busy` disables it and shows progress while a request runs.
class PwButton extends StatelessWidget {
  const PwButton({
    super.key,
    required this.label,
    this.onPressed,
    this.variant = PwButtonVariant.secondary,
    this.size = PwButtonSize.lg,
    this.busy = false,
    this.block = false,
    this.icon,
    this.semanticLabel,
  });

  final String label;
  final VoidCallback? onPressed;
  final PwButtonVariant variant;
  final PwButtonSize size;
  final bool busy;
  final bool block;
  final IconData? icon;

  /// What screen readers say instead of [label] when the label alone is vague ("Gọi khách" → "Gọi 0905 123 008").
  final String? semanticLabel;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final (Color background, Color foreground, Color? border) = switch (variant) {
      PwButtonVariant.primary => (t.route, t.onAccent, null),
      PwButtonVariant.danger => (t.danger, t.onAccent, null),
      PwButtonVariant.success => (t.success, t.onAccent, null),
      PwButtonVariant.signal => (t.signal, t.onAccent, null),
      PwButtonVariant.secondary => (t.surfaceRaised, t.ink, t.inkMuted),
      PwButtonVariant.quiet => (Colors.transparent, t.route, null),
    };
    final enabled = onPressed != null && !busy;
    final height = size == PwButtonSize.xl ? 56.0 : 48.0;
    final child = busy
        ? SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2, color: foreground))
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Large system text wraps the label onto more lines and the button grows; it is never cut (M03).
              Flexible(
                child: Text(
                  label,
                  textAlign: TextAlign.center,
                  style: PwTypeStyles.heading.copyWith(color: foreground),
                ),
              ),
              if (icon != null) ...[const SizedBox(width: PwSpacing.space2), Icon(icon, size: 20, color: foreground)],
            ],
          );
    final button = Semantics(
      button: true,
      enabled: enabled,
      label: semanticLabel ?? label,
      // The label replaces the content; the tap action must stay, or screen readers cannot press the button.
      excludeSemantics: true,
      onTap: enabled ? onPressed : null,
      child: Opacity(
        opacity: enabled || busy ? 1 : 0.5,
        child: Material(
          color: background,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(PwRadius.radiusSm),
            side: border == null ? BorderSide.none : BorderSide(color: border),
          ),
          child: InkWell(
            onTap: enabled ? onPressed : null,
            borderRadius: BorderRadius.circular(PwRadius.radiusSm),
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: height, minWidth: 48),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4),
                child: Center(widthFactor: 1, heightFactor: 1, child: child),
              ),
            ),
          ),
        ),
      ),
    );
    return block ? SizedBox(width: double.infinity, child: button) : button;
  }
}
