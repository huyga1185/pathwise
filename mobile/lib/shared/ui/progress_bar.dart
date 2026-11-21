import 'package:flutter/material.dart';

import '../i18n/vi.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';

/// Design system ProgressBar: background work or a share; leave value/max out for an indeterminate bar.
class PwProgressBar extends StatelessWidget {
  const PwProgressBar({super.key, required this.label, this.value, this.max});

  final String label;
  final int? value;
  final int? max;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final known = value != null && max != null && max! > 0;
    // Read as one sentence, "2 trên 6 điểm đã xong": "2/6" and the bar's own percentage were read as "33" (H5,
    // DRIVER_UI_REDESIGN §3 "Thanh tiến độ", WCAG 1.1.1).
    return Semantics(
      container: true,
      label: known ? Vi.progressOf(value!, max!, label) : label,
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ExcludeSemantics(
            child: Row(
              children: [
                Expanded(
                  child: Text(label, style: PwTypeStyles.body.copyWith(color: t.ink)),
                ),
                if (known)
                  Text(
                    '$value/$max',
                    style: PwTypeStyles.body.copyWith(
                      color: t.inkMuted,
                      fontFeatures: const [FontFeature.tabularFigures()],
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: PwSpacing.space1),
          ClipRRect(
            borderRadius: BorderRadius.circular(PwRadius.radiusFull),
            child: LinearProgressIndicator(
              value: known ? value! / max! : null,
              minHeight: 8,
              color: t.route,
              backgroundColor: t.line,
            ),
          ),
        ],
      ),
    );
  }
}
