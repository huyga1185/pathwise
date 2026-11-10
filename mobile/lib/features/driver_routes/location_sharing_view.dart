import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'location_sharing.dart';

/// "Chia sẻ vị trí cho tuyến này?" (prototype `ask`): resolves the answer and whether to remember it; null when the
/// dialog was dismissed without answering (asked again next time the route opens).
Future<({bool share, bool remember})?> showShareDialog(BuildContext context, String planName) {
  return showDialog<({bool share, bool remember})>(
    context: context,
    builder: (context) {
      var remember = false;
      return StatefulBuilder(
        builder: (context, setState) {
          final t = context.pw;
          return AlertDialog(
            backgroundColor: t.surfaceRaised,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(PwRadius.radiusLg)),
            title: Text(Vi.shareAskTitle, style: PwTypeStyles.title.copyWith(color: t.ink)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(Vi.shareAskBody(planName), style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
                  const SizedBox(height: PwSpacing.space2),
                  CheckboxListTile(
                    value: remember,
                    onChanged: (value) => setState(() => remember = value ?? false),
                    controlAffinity: ListTileControlAffinity.leading,
                    contentPadding: EdgeInsets.zero,
                    activeColor: t.route,
                    title: Text(Vi.shareRemember, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
                  ),
                  Text(Vi.shareRememberHint, style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
                ],
              ),
            ),
            actions: [
              PwButton(
                label: Vi.shareDecline,
                onPressed: () => Navigator.of(context).pop((share: false, remember: remember)),
              ),
              PwButton(
                label: Vi.shareConfirm,
                variant: PwButtonVariant.primary,
                onPressed: () => Navigator.of(context).pop((share: true, remember: remember)),
              ),
            ],
          );
        },
      );
    },
  );
}

/// The line or box under the route's progress (prototype `sharing`, `declined`, `denied`, `gps-off`); nothing while
/// another route is shared or the dialog is open.
class LocationSharingBanner extends ConsumerWidget {
  const LocationSharingBanner({super.key, required this.routeId});

  final int routeId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(locationSharingProvider);
    if (!status.isFor(routeId)) return const SizedBox.shrink();
    final controller = ref.read(locationSharingProvider.notifier);
    return switch (status.phase) {
      SharingPhase.sharing => const _Sharing(),
      SharingPhase.declined => _Warning(
        title: Vi.sharingDeclinedTitle,
        body: Vi.sharingDeclinedBody,
        action: Vi.sharingEnable,
        onAction: () => unawaited(controller.enable()),
      ),
      SharingPhase.noPermission => _Warning(
        alert: true,
        title: Vi.sharingDeniedTitle,
        body: status.canAsk ? Vi.sharingDeniedBody : Vi.sharingDeniedForeverBody,
        action: status.canAsk ? Vi.sharingEnable : Vi.openSettings,
        onAction: () => unawaited(status.canAsk ? controller.enable() : controller.openSettings()),
      ),
      SharingPhase.gpsOff => _Warning(
        alert: true,
        title: Vi.gpsOffTitle,
        body: Vi.gpsOffBody,
        action: Vi.gpsTurnOn,
        onAction: () => unawaited(controller.openSettings()),
      ),
      SharingPhase.off || SharingPhase.asking => const SizedBox.shrink(),
    };
  }
}

class _Sharing extends StatelessWidget {
  const _Sharing();

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Semantics(
      liveRegion: true,
      child: Container(
        constraints: const BoxConstraints(minHeight: 48),
        padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4, vertical: PwSpacing.space2),
        decoration: BoxDecoration(
          color: t.surfaceRaised,
          border: Border.all(color: t.line),
          borderRadius: BorderRadius.circular(PwRadius.radiusMd),
        ),
        child: Row(
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                color: t.success,
                shape: BoxShape.circle,
                boxShadow: [BoxShadow(color: t.success.withValues(alpha: 0.25), spreadRadius: 4)],
              ),
            ),
            const SizedBox(width: PwSpacing.space2),
            Expanded(
              child: Text(Vi.sharingOn, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
            ),
          ],
        ),
      ),
    );
  }
}

class _Warning extends StatelessWidget {
  const _Warning({
    required this.title,
    required this.body,
    required this.action,
    required this.onAction,
    this.alert = false,
  });

  final String title;
  final String body;
  final String action;
  final VoidCallback onAction;
  final bool alert;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Semantics(
      liveRegion: alert,
      container: true,
      child: Container(
        padding: const EdgeInsets.all(PwSpacing.space4),
        decoration: BoxDecoration(
          color: t.surfaceRaised,
          border: Border.all(color: t.signal),
          borderRadius: BorderRadius.circular(PwRadius.radiusMd),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              title,
              style: PwTypeStyles.bodyTouch.copyWith(color: t.ink, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: PwSpacing.space2),
            Text(body, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
            const SizedBox(height: PwSpacing.space2),
            PwButton(label: action, block: true, onPressed: onAction),
          ],
        ),
      ),
    );
  }
}
