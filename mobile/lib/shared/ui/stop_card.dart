import 'package:flutter/material.dart';

import '../format.dart';
import '../status/status.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';
import 'pw_button.dart';
import 'status_pill.dart';

/// Design system StopCard: a stop of the driver's route (M01, M02). `current` marks the next stop. With a `phone` and
/// [onCall] it has a "Gọi 0905 123 456" button; the screen dials through the Launcher port.
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
        color: current ? t.surfaceTint : t.surfaceRaised,
        border: Border.all(color: current ? t.route : t.line, width: current ? 2 : 1),
        borderRadius: BorderRadius.circular(PwRadius.radiusMd),
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
                  child: CircleAvatar(
                    radius: 14,
                    backgroundColor: t.route,
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
            if (hasCall) PwButton(label: 'Gọi ${formatPhone(phone)}', block: true, onPressed: onCall),
            for (final action in actions) ...[const SizedBox(height: PwSpacing.space2), action],
          ],
        ],
      ),
    );
  }
}
