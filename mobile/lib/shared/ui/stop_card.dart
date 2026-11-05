import 'package:flutter/material.dart';

import '../format.dart';
import '../status/status.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';
import 'status_pill.dart';

/// Design system StopCard: a stop of the driver's route (M01, M02). `current` marks the next stop.
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
  final bool current;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final muted = PwTypeStyles.body.copyWith(color: t.inkMuted);
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
                CircleAvatar(
                  radius: 14,
                  backgroundColor: t.route,
                  child: Text('$sequence', style: PwTypeStyles.label.copyWith(color: t.onAccent)),
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
          if (phone != null && phone!.isNotEmpty) Text(formatPhone(phone), style: muted),
          if (note != null && note!.isNotEmpty) Text(note!, style: muted),
          if (failureReason != null && status == 'FAILED')
            Text('Lý do: $failureReason', style: PwTypeStyles.small.copyWith(color: t.danger)),
          if (actions.isNotEmpty) ...[
            const SizedBox(height: PwSpacing.space4),
            Wrap(spacing: PwSpacing.space2, runSpacing: PwSpacing.space2, children: actions),
          ],
        ],
      ),
    );
  }
}
