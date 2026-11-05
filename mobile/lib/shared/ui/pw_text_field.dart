import 'package:flutter/material.dart';

import '../theme/theme.dart';
import '../theme/tokens.g.dart';

/// Design system TextField: label always above, hint and error below; `body-touch` 16 px, 48 px high (driver app).
class PwTextField extends StatelessWidget {
  const PwTextField({
    super.key,
    required this.label,
    this.controller,
    this.hint,
    this.error,
    this.required = false,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.autofillHints,
    this.onSubmitted,
    this.suffix,
  });

  final String label;
  final TextEditingController? controller;
  final String? hint;
  final String? error;
  final bool required;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final ValueChanged<String>? onSubmitted;
  final Widget? suffix;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    OutlineInputBorder border(Color colour, double width) => OutlineInputBorder(
      borderRadius: BorderRadius.circular(PwRadius.radiusSm),
      borderSide: BorderSide(color: colour, width: width),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text.rich(
          TextSpan(
            text: label,
            children: [
              if (required)
                TextSpan(
                  text: ' *',
                  style: TextStyle(color: t.danger),
                ),
            ],
          ),
          style: PwTypeStyles.heading.copyWith(color: t.ink),
        ),
        const SizedBox(height: PwSpacing.space1),
        Semantics(
          label: label,
          child: TextField(
            controller: controller,
            obscureText: obscureText,
            keyboardType: keyboardType,
            textInputAction: textInputAction,
            autofillHints: autofillHints,
            onSubmitted: onSubmitted,
            style: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
            decoration: InputDecoration(
              filled: true,
              fillColor: t.surfaceRaised,
              constraints: const BoxConstraints(minHeight: 48),
              contentPadding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4, vertical: 12),
              enabledBorder: border(error == null ? t.inkMuted : t.danger, error == null ? 1 : 2),
              focusedBorder: border(error == null ? t.route : t.danger, 2),
              suffixIcon: suffix,
            ),
          ),
        ),
        if (error != null) ...[
          const SizedBox(height: PwSpacing.space1),
          Text(error!, style: PwTypeStyles.small.copyWith(color: t.danger)),
        ],
        if (hint != null && error == null) ...[
          const SizedBox(height: PwSpacing.space1),
          Text(hint!, style: PwTypeStyles.small.copyWith(color: t.inkMuted)),
        ],
      ],
    );
  }
}
