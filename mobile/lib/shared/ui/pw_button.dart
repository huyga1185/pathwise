import 'package:flutter/material.dart';

import '../theme/theme.dart';
import '../theme/tokens.g.dart';

/// Material 3 button emphasis (P-77, DRIVER_UI_REDESIGN §2.2): [filled] the one main action of a screen, [tonal] an
/// important second action, [outlined] medium emphasis, [text] low emphasis (links, dialog actions). [success],
/// [danger] and [signal] are the coloured result buttons of the stop report (kept on purpose, §6 Q1) and the
/// destructive confirm; [textDanger] is the destructive action of a dialog (text button in the danger colour).
enum PwButtonVariant { filled, tonal, outlined, text, textDanger, danger, success, signal }

/// lg 48 px is the app's minimum touch target; xl 56 px is for the driver's result buttons (DESIGN_SYSTEM).
enum PwButtonSize { lg, xl }

/// Design system Button on Material 3's buttons and shapes; `busy` keeps the colour, ignores taps and shows progress
/// while a request runs.
class PwButton extends StatelessWidget {
  const PwButton({
    super.key,
    required this.label,
    this.onPressed,
    this.variant = PwButtonVariant.outlined,
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
    final scheme = Theme.of(context).colorScheme;
    final enabled = onPressed != null && !busy;
    final height = size == PwButtonSize.xl ? 56.0 : 48.0;
    final (Color? fill, Color foreground) = switch (variant) {
      PwButtonVariant.filled => (null, scheme.onPrimary),
      PwButtonVariant.tonal => (null, scheme.onSecondaryContainer),
      PwButtonVariant.outlined || PwButtonVariant.text => (null, scheme.primary),
      PwButtonVariant.textDanger => (null, t.danger),
      PwButtonVariant.danger => (t.danger, t.onAccent),
      PwButtonVariant.success => (t.success, t.onAccent),
      PwButtonVariant.signal => (t.signal, t.onAccent),
    };
    final style = ButtonStyle(
      minimumSize: WidgetStatePropertyAll(Size(48, height)),
      // Side padding is Flutter's Material 3 default (24 dp, text buttons 12 dp), narrowing as system text grows.
      textStyle: const WidgetStatePropertyAll(PwTypeStyles.heading),
      tapTargetSize: MaterialTapTargetSize.padded,
      // Disabled falls through to Material's disabled colours (null resolves to the default).
      backgroundColor: fill == null ? null : _unlessDisabled(fill),
      foregroundColor: fill == null && variant != PwButtonVariant.textDanger ? null : _unlessDisabled(foreground),
    );
    final Widget content = busy
        ? SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2, color: foreground))
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Large system text wraps the label onto more lines and the button grows; it is never cut (M03).
              Flexible(child: Text(label, textAlign: TextAlign.center)),
              if (icon != null) ...[const SizedBox(width: PwSpacing.space2), Icon(icon, size: 20)],
            ],
          );
    // A busy button keeps its colour (a disabled one would grey out mid-request) but does nothing when tapped.
    final VoidCallback? tap = busy ? () {} : onPressed;
    final Widget button = switch (variant) {
      PwButtonVariant.filled ||
      PwButtonVariant.danger ||
      PwButtonVariant.success ||
      PwButtonVariant.signal => FilledButton(onPressed: tap, style: style, child: content),
      PwButtonVariant.tonal => FilledButton.tonal(onPressed: tap, style: style, child: content),
      PwButtonVariant.outlined => OutlinedButton(onPressed: tap, style: style, child: content),
      PwButtonVariant.text || PwButtonVariant.textDanger => TextButton(onPressed: tap, style: style, child: content),
    };
    final labelled = Semantics(
      button: true,
      enabled: enabled,
      label: semanticLabel ?? label,
      // The label replaces the content; the tap action must stay, or screen readers cannot press the button.
      excludeSemantics: true,
      onTap: enabled ? onPressed : null,
      child: button,
    );
    return block ? SizedBox(width: double.infinity, child: labelled) : labelled;
  }
}

WidgetStateProperty<Color?> _unlessDisabled(Color color) =>
    WidgetStateProperty.resolveWith((states) => states.contains(WidgetState.disabled) ? null : color);
