import 'package:flutter/material.dart';

import '../format.dart';
import '../status/status.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';
import 'icon_size.dart';
import 'pw_button.dart';
import 'status_pill.dart';

/// Design system StopCard: the next stop of the driver's route, one card with all of it (M01, M02; the other stops are
/// [StopRow]s, DRIVER_UI_REDESIGN Q6). `current` marks the next stop. With a `phone` and [onCall] it has a "Gọi 0905 123
/// 456" button, filled tonal (an important second action, M3); the screen dials through the Launcher port.
class StopCard extends StatelessWidget {
  const StopCard({
    super.key,
    this.sequence,
    required this.title,
    required this.address,
    this.eta,
    this.timeWindow,
    this.note,
    this.status,
    this.failureReason,
    this.phone,
    this.onCall,
    this.current = false,
    this.actions = const [],
  });

  final int? sequence;
  final String title;
  final String address;
  final String? eta;
  final String? timeWindow;
  final String? note;
  final String? status;
  final String? failureReason;
  final String? phone;
  final VoidCallback? onCall;
  final bool current;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final muted = PwTypeStyles.body.copyWith(color: t.inkMuted);
    final hasCall = phone != null && phone!.isNotEmpty && onCall != null;
    return Container(
      padding: const EdgeInsets.all(PwSpacing.space4),
      decoration: BoxDecoration(
        // The 2 dp route border marks the next stop; a tinted fill hid the filled tonal "Gọi" button, which is the same
        // tint (D4).
        color: t.surfaceRaised,
        border: Border.all(color: current ? t.route : t.line, width: current ? 2 : 1),
        borderRadius: BorderRadius.circular(PwShapes.cornerMedium),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              if (sequence != null) ...[
                Semantics(
                  label: 'Điểm số $sequence',
                  excludeSemantics: true,
                  // Grows with the system text; a CircleAvatar keeps its number at 100 % (D4).
                  child: Container(
                    width: scaledIconSize(context, 28),
                    height: scaledIconSize(context, 28),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(color: t.route, shape: BoxShape.circle),
                    child: Text('$sequence', style: PwTypeStyles.label.copyWith(color: t.onAccent)),
                  ),
                ),
                const SizedBox(width: PwSpacing.space2),
              ],
              Expanded(
                child: Text(title, style: PwTypeStyles.heading.copyWith(color: t.ink)),
              ),
              StatusPill(kind: StatusKind.stop, status: status),
            ],
          ),
          const SizedBox(height: PwSpacing.space2),
          Text(address, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
          if (eta != null || timeWindow != null)
            Text(
              [if (eta != null) 'Dự kiến $eta', if (timeWindow != null) 'Khung $timeWindow'].join(' · '),
              style: muted,
            ),
          if (note != null && note!.isNotEmpty) Text(note!, style: muted),
          if (failureReason != null && failureReason!.isNotEmpty)
            Text('Lý do: $failureReason', style: PwTypeStyles.body.copyWith(color: t.danger)),
          if (hasCall || actions.isNotEmpty) ...[
            const SizedBox(height: PwSpacing.space2),
            if (hasCall)
              PwButton(
                label: 'Gọi ${formatPhone(phone)}',
                variant: PwButtonVariant.tonal,
                block: true,
                onPressed: onCall,
              ),
            for (final action in actions) ...[const SizedBox(height: PwSpacing.space2), action],
          ],
        ],
      ),
    );
  }
}
