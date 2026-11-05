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
      return AlertDialog(
        backgroundColor: t.surfaceRaised,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(PwRadius.radiusLg)),
        title: Text(title, style: PwTypeStyles.title.copyWith(color: t.ink)),
        content: Text(body, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
        actions: [
          PwButton(label: cancelLabel, onPressed: () => Navigator.of(context).pop(false)),
          PwButton(
            label: confirmLabel,
            variant: danger ? PwButtonVariant.danger : PwButtonVariant.primary,
            onPressed: () => Navigator.of(context).pop(true),
          ),
        ],
      );
    },
  );
  return confirmed ?? false;
}
