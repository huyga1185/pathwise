import 'package:flutter/material.dart';

import '../status/status.dart';
import '../theme/theme.dart';
import '../theme/tokens.g.dart';
import 'icon_size.dart';
import 'status_pill.dart';

/// A stop that is not the next one, as a Material list row (`ListTile`, DRIVER_UI_REDESIGN §3 "Danh sách điểm", Q6):
/// its number at the start, the title, the address, then the times, or for a finished stop its status and the failure
/// reason in full. [StopGroup] stacks them.
class StopRow extends StatelessWidget {
  const StopRow({
    super.key,
    required this.sequence,
    required this.title,
    required this.address,
    this.detail,
    this.status,
    this.danger = false,
  });

  final int sequence;
  final String title;
  final String address;

  /// The third line: "Dự kiến 10:39 · Khung 09:00–11:00", or "Lý do: …" ([danger]).
  final String? detail;
  final String? status;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final muted = PwTypeStyles.body.copyWith(color: t.inkMuted);
    final badge = scaledIconSize(context, 28);
    return ListTile(
      isThreeLine: detail != null || status != null,
      contentPadding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4),
      leading: Semantics(
        label: 'Điểm số $sequence',
        excludeSemantics: true,
        child: Container(
          width: badge,
          height: badge,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: t.surfaceTint, shape: BoxShape.circle),
          child: Text('$sequence', style: PwTypeStyles.label.copyWith(color: t.ink)),
        ),
      ),
      title: Text(
        title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: PwTypeStyles.heading.copyWith(color: t.ink),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(address, maxLines: 1, overflow: TextOverflow.ellipsis, style: muted),
          // A finished stop: its status, then the failure reason in full (the reason is what the driver needs; at the
          // end of the row the status left it a few words).
          if (status != null)
            Text.rich(
              TextSpan(
                children: [
                  WidgetSpan(
                    alignment: PlaceholderAlignment.middle,
                    child: StatusPill(kind: StatusKind.stop, status: status),
                  ),
                  if (detail != null) TextSpan(text: '  $detail'),
                ],
              ),
              style: danger ? PwTypeStyles.body.copyWith(color: t.danger) : muted,
            )
          else if (detail != null)
            Text(detail!, maxLines: 1, overflow: TextOverflow.ellipsis, style: muted),
        ],
      ),
    );
  }
}

/// Rows of a list in one container, a line between them (Flutter `ListTile`: "Card, which can be used with Column to
/// show a few ListTiles").
class StopGroup extends StatelessWidget {
  const StopGroup({super.key, required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Material(
      color: t.surfaceRaised,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(PwShapes.cornerMedium),
        side: BorderSide(color: t.line),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          for (final (index, child) in children.indexed) ...[
            if (index > 0) Divider(height: 1, indent: PwSpacing.space4, endIndent: PwSpacing.space4, color: t.line),
            child,
          ],
        ],
      ),
    );
  }
}
