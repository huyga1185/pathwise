import 'package:flutter/material.dart';

import '../theme/theme.dart';
import '../theme/tokens.g.dart';
import 'pw_button.dart';

/// Design system Dialog for actions that cannot be undone: a question with the object's name, the consequence,
/// the confirm button repeats the verb, "Quay lại" closes. Resolves true when confirmed.
Future<bool> showPwDialog(
  BuildContext context, {
  required String title,
  required String body,
  required String confirmLabel,
  String cancelLabel = 'Quay lại',
  bool danger = false,
}) async {
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) {
      final t = context.pw;
      // Material 3 dialog: its own shape and colour from the theme, text buttons aligned to the end, 8 dp apart
      // (developer.android.com, Compose AlertDialog sample; P-77). Labels too long for one row stack, still at the end.
      return AlertDialog(
        title: Text(title, style: PwTypeStyles.title.copyWith(color: t.ink)),
        content: Text(body, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
        actionsOverflowAlignment: OverflowBarAlignment.end,
        actions: [
          PwButton(
            label: cancelLabel,
            variant: PwButtonVariant.text,
            onPressed: () => Navigator.of(context).pop(false),
          ),
          PwButton(
            label: confirmLabel,
            variant: danger ? PwButtonVariant.textDanger : PwButtonVariant.text,
            onPressed: () => Navigator.of(context).pop(true),
          ),
        ],
      );
    },
  );
  return confirmed ?? false;
}
