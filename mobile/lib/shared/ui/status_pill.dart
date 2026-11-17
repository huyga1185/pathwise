import 'package:flutter/material.dart';

import '../status/status.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';

/// Design system StatusPill: uppercase label on the status colour, from [statusInfo]; nothing for a status without one.
class StatusPill extends StatelessWidget {
  const StatusPill({super.key, this.kind, this.status, this.tone, this.label});

  final StatusKind? kind;
  final String? status;
  final Tone? tone;
  final String? label;

  @override
  Widget build(BuildContext context) {
    final info = kind == null ? null : statusInfo(kind!, status);
    final text = label ?? info?.label;
    if (text == null) return const SizedBox.shrink();
    final t = context.pw;
    final colour = switch (tone ?? info?.tone ?? Tone.muted) {
      Tone.muted => t.inkMuted,
      Tone.info => t.info,
      Tone.signal => t.signal,
      Tone.danger => t.danger,
      Tone.success => t.success,
      Tone.route => t.route,
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space2, vertical: 2),
      // Light theme: 12% darker so white 12px text reaches 7:1 (HIG Dark Mode: "strive for a contrast ratio of 7:1,
      // especially in small text"; the tokens alone give 5.9–6.7:1). Dark theme already gives 7.7–9.1:1.
      decoration: BoxDecoration(
        color: Theme.of(context).brightness == Brightness.light
            ? Color.lerp(colour, const Color(0xFF000000), 0.12)
            : colour,
        borderRadius: BorderRadius.circular(PwRadius.radiusFull),
      ),
      child: Text(text, style: PwTypeStyles.label.copyWith(color: t.onAccent)),
    );
  }
}
