import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../shared/errors/errors.dart';
import '../../shared/format.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/status/status.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'directions.dart';
import 'failure_reason_sheet.dart';
import 'route_error.dart';
import 'route_polling.dart';
import 'routes_data.dart';

/// "Tài xế — báo kết quả" (SCREENS §2): the stop, "Gọi khách" and "Chỉ đường", then the three results at the bottom
/// (56 px, thumb reach), each confirmed first because none can be undone (P-34). A result goes straight to the server;
/// on its answer the driver is back on the route with a snackbar (D5) (ARCHITECTURE §5); without network the driver waits and presses
/// "Gửi lại", which sends the same result (P-51). The route is read again on opening and every 30 s, so a cancelled
/// plan shows before the goods are handed over.
class StopReportScreen extends ConsumerStatefulWidget {
  const StopReportScreen({super.key, required this.routeId, required this.stopId});

  final int routeId;
  final int stopId;

  @override
  ConsumerState<StopReportScreen> createState() => _StopReportScreenState();
}

class _StopReportScreenState extends ConsumerState<StopReportScreen> with RoutePolling {
  /// The result being sent (its button shows progress, the others wait).
  StopResult? _sending;

  /// A result that did not reach the server: only this one can be sent again ("Gửi lại"), the same as before.
  (StopResult, String?)? _unsent;

  /// Why the last send failed.
  ApiError? _error;

  @override
  int get pollRouteId => widget.routeId;

  bool get _planNotActive => _error?.code == 'PLAN_NOT_ACTIVE';

  @override
  void initState() {
    super.initState();
    // The list or the route may have been read minutes ago: check the plan still runs before the driver hands over.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) reloadRoute();
    });
  }

  Future<void> _open(Uri uri) async {
    final opened = await ref.read(launcherProvider).open(uri);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text(Vi.cannotOpen)));
    }
  }

  void _backToRoute() => context.go('/routes/${widget.routeId}');

  Future<void> _ask(DriverStopResponse stop, StopResult result) async {
    String? reason;
    switch (result) {
      case StopResult.delivered:
        final confirmed = await showPwDialog(
          context,
          title: Vi.confirmDeliveredTitle(stop.label),
          body: Vi.confirmDeliveredBody,
          confirmLabel: Vi.delivered,
        );
        if (!confirmed) return;
      case StopResult.skipped:
        final confirmed = await showPwDialog(
          context,
          title: Vi.confirmSkippedTitle(stop.label),
          body: Vi.confirmSkippedBody,
          confirmLabel: Vi.skipped,
          danger: true,
        );
        if (!confirmed) return;
      case StopResult.failed:
        reason = await showFailureReasonSheet(context, label: stop.label);
        if (reason == null) return;
    }
    if (mounted) await _send(result, reason);
  }

  Future<void> _send(StopResult result, String? reason) async {
    setState(() {
      _sending = result;
      _error = null;
    });
    try {
      final response = await ref
          .read(stopReporterProvider)
          .report(routeId: widget.routeId, stopId: widget.stopId, result: result, failureReason: reason);
      if (!mounted) return;
      // Back to the route, whose first card is now the next stop (or the end of the route), with a short note of what
      // was recorded (§3 "Kết quả việc", Material 3: "a brief notification"; chủ dự án chọn, D5).
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(switch (result) {
              StopResult.delivered => Vi.reportedDelivered(response.stop.sequence),
              StopResult.failed => Vi.reportedFailed(response.stop.sequence),
              StopResult.skipped => Vi.reportedSkipped(response.stop.sequence),
            }),
          ),
        );
      _backToRoute();
    } on ApiError catch (error) {
      if (!mounted) return;
      setState(() {
        _sending = null;
        _error = error;
        _unsent = sendFailed(error) ? (result, reason) : null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final route = ref.watch(driverRouteProvider(widget.routeId));
    final detail = shownRoute(route);
    final stop = detail?.stops.where((stop) => stop.id == widget.stopId).firstOrNull;

    final List<Widget> body;
    Widget? footer;
    if (detail == null) {
      body = switch (route) {
        AsyncValue(:final error?) => routeErrorView(context, error, reloadRoute),
        _ => const [
          Padding(
            padding: EdgeInsets.only(top: 48),
            child: Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
          ),
        ],
      };
    } else if (stop == null) {
      body = [
        const ErrorBanner(title: Vi.stopNotFound),
        const SizedBox(height: PwSpacing.space4),
        PwButton(label: Vi.backToRoute, block: true, onPressed: _backToRoute),
      ];
    } else {
      final next = detail.stops
          .where((other) => other.id != stop.id && other.status == DriverStopResponseStatusEnum.PENDING)
          .firstOrNull;
      // The server's word wins: a send whose answer was lost shows as done once the route is read again.
      if (stop.status != DriverStopResponseStatusEnum.PENDING) {
        body = _result(detail, stop, next);
        footer = PwButton(
          label: next == null ? Vi.backToRoute : Vi.goNext,
          variant: PwButtonVariant.filled,
          size: PwButtonSize.xl,
          block: true,
          onPressed: _backToRoute,
        );
      } else {
        body = _stop(detail, stop);
        footer = _results(stop);
      }
    }

    return Scaffold(
      backgroundColor: t.surface,
      // Material 3 small top app bar, as on the route (D4); back goes to the route, also when the screen was opened
      // straight from a link.
      appBar: AppBar(
        leading: IconButton(icon: const BackButtonIcon(), tooltip: Vi.backToRoute, onPressed: _backToRoute),
        title: stop == null ? null : Text(Vi.stopOf(stop.sequence, detail!.stops.length)),
      ),
      // The footer's fill runs under the gesture bar, its buttons stay above it (edge-to-edge; a strip of the page
      // colour showed under the footer, D5).
      body: SafeArea(
        top: false,
        bottom: footer == null,
        child: Column(
          children: [
            Expanded(
              child: ListView(padding: const EdgeInsets.all(PwSpacing.space4), children: body),
            ),
            if (footer != null)
              Container(
                padding:
                    const EdgeInsets.all(PwSpacing.space4) +
                    EdgeInsets.only(bottom: MediaQuery.paddingOf(context).bottom),
                decoration: BoxDecoration(
                  color: t.surfaceRaised,
                  border: Border(top: BorderSide(color: t.line)),
                ),
                child: footer,
              ),
          ],
        ),
      ),
    );
  }

  /// Before the result: who and where, call and directions, and why the last send failed.
  List<Widget> _stop(DriverRouteDetailResponse route, DriverStopResponse stop) {
    final t = context.pw;
    final window = stop.timeWindow;
    final when = [
      if (stop.plannedArrival != null) Vi.expectedAt(formatTime(stop.plannedArrival, route.timezone)),
      if (window != null) Vi.receiveWindow(formatTimeWindow(window.start, window.end)),
    ].join(' · ');
    final phone = stop.recipientPhone;
    final directions = PwButton(
      label: Vi.directions,
      variant: PwButtonVariant.tonal,
      block: true,
      onPressed: () => context.push('/routes/${route.routeId}/navigate?stopId=${stop.id}'),
    );
    final error = _error;
    final unsent = _unsent;
    return [
      Row(
        children: [
          Semantics(
            label: 'Điểm số ${stop.sequence}',
            excludeSemantics: true,
            // Grows with the system text, as on the route's card (D4).
            child: Container(
              width: scaledIconSize(context, 28),
              height: scaledIconSize(context, 28),
              alignment: Alignment.center,
              decoration: BoxDecoration(color: t.route, shape: BoxShape.circle),
              child: Text('${stop.sequence}', style: PwTypeStyles.label.copyWith(color: t.onAccent)),
            ),
          ),
          const SizedBox(width: PwSpacing.space2),
          Expanded(
            child: Semantics(
              header: true,
              child: Text(
                [stop.label, ?stop.recipientName].join(' · '),
                style: PwTypeStyles.title.copyWith(color: t.ink),
              ),
            ),
          ),
        ],
      ),
      const SizedBox(height: PwSpacing.space2),
      Text(stop.address ?? Vi.noAddress, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
      if (when.isNotEmpty) Text(when, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
      if (stop.note != null && stop.note!.isNotEmpty)
        Text(stop.note!, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
      const SizedBox(height: PwSpacing.space4),
      if (phone == null)
        directions
      else
        // Filled tonal, the second level: the results below are what the screen is for (§3 "Nút").
        PwButtonPair(
          first: PwButton(
            label: Vi.callCustomer,
            semanticLabel: Vi.callNumber(formatPhone(phone)),
            variant: PwButtonVariant.tonal,
            block: true,
            onPressed: () => unawaited(_open(callUri(phone))),
          ),
          second: directions,
          labels: const [Vi.callCustomer, Vi.directions],
        ),
      if (unsent != null && error != null) ...[
        const SizedBox(height: PwSpacing.space4),
        ErrorBanner(
          title: error.code == ApiError.networkError ? Vi.notSentOffline : Vi.notSent,
          detail: [if (error.code != ApiError.networkError) messageFor(error), Vi.waitAndResend].join(' '),
          action: PwButton(
            label: Vi.resend,
            block: true,
            busy: _sending != null,
            onPressed: () => unawaited(_send(unsent.$1, unsent.$2)),
          ),
        ),
      ] else if (_planNotActive) ...[
        const SizedBox(height: PwSpacing.space4),
        ErrorBanner(
          title: messageFor(error!),
          detail: Vi.planNotActiveDetail,
          action: PwButton(label: Vi.backToRoutes, block: true, onPressed: () => context.go('/routes')),
        ),
      ] else if (error != null) ...[
        const SizedBox(height: PwSpacing.space4),
        ErrorBanner(title: Vi.reportFailed, detail: messageFor(error)),
      ],
    ];
  }

  /// The three results, stacked; locked while one is sent, while one waits to be sent again (only the same result
  /// may go, P-36) and once the plan has ended.
  Widget _results(DriverStopResponse stop) {
    final locked = _sending != null || _unsent != null || _planNotActive;
    PwButton button(StopResult result, String label, PwButtonVariant variant) => PwButton(
      label: label,
      variant: variant,
      size: PwButtonSize.xl,
      block: true,
      busy: _sending == result && _unsent == null,
      onPressed: locked ? null : () => unawaited(_ask(stop, result)),
    );
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        button(StopResult.delivered, Vi.delivered, PwButtonVariant.success),
        const SizedBox(height: PwSpacing.space2),
        button(StopResult.failed, Vi.failed, PwButtonVariant.danger),
        const SizedBox(height: PwSpacing.space2),
        button(StopResult.skipped, Vi.skipped, PwButtonVariant.signal),
      ],
    );
  }

  /// A stop that already has its result (opened again, or a send whose answer was lost shows as done once the route is
  /// read again): what was recorded, then the next stop or the end of the route.
  List<Widget> _result(DriverRouteDetailResponse route, DriverStopResponse stop, DriverStopResponse? next) {
    final t = context.pw;
    final result = StopResult.of(stop.status.value)!;
    final reason = stop.failureReason;
    final routeDone = route.status == DriverRouteDetailResponseStatusEnum.COMPLETED;
    final title = switch (result) {
      StopResult.delivered => Vi.sentDelivered(stop.label),
      StopResult.failed => Vi.sentFailed(stop.label),
      StopResult.skipped => Vi.sentSkipped(stop.label),
    };
    final error = _error;
    return [
      if (error != null && !sendFailed(error)) ...[
        ErrorBanner(title: Vi.reportFailed, detail: messageFor(error)),
        const SizedBox(height: PwSpacing.space4),
      ],
      Semantics(
        liveRegion: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: PwSpacing.space6),
            StatusPill(kind: StatusKind.stop, status: result.value),
            const SizedBox(height: PwSpacing.space4),
            Text(
              title,
              style: PwTypeStyles.title.copyWith(color: t.ink, fontSize: 24, fontWeight: FontWeight.w700),
            ),
            if (result == StopResult.failed && reason != null && reason.isNotEmpty) ...[
              const SizedBox(height: PwSpacing.space2),
              Text(Vi.reasonWas(reason), style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
            ],
          ],
        ),
      ),
      const SizedBox(height: PwSpacing.space4),
      if (routeDone || next == null)
        Container(
          padding: const EdgeInsets.all(PwSpacing.space4),
          decoration: BoxDecoration(
            color: t.surfaceRaised,
            border: Border.all(color: t.success),
            borderRadius: BorderRadius.circular(PwRadius.radiusMd),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                routeDone ? Vi.routeDone : Vi.noNextStop,
                style: PwTypeStyles.bodyTouch.copyWith(color: t.ink, fontWeight: FontWeight.w700),
              ),
              if (routeDone) ...[
                const SizedBox(height: PwSpacing.space1),
                Text(
                  Vi.returnTo(route.end.address ?? '${route.end.lat}, ${route.end.lon}'),
                  style: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
                ),
              ],
            ],
          ),
        )
      else
        Container(
          padding: const EdgeInsets.all(PwSpacing.space4),
          decoration: BoxDecoration(color: t.surfaceTint, borderRadius: BorderRadius.circular(PwRadius.radiusMd)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(Vi.nextStop, style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
              const SizedBox(height: PwSpacing.space1),
              Text(
                ['${next.sequence}', next.label, ?next.recipientName].join(' · '),
                style: PwTypeStyles.bodyTouch.copyWith(color: t.ink, fontWeight: FontWeight.w700),
              ),
              Text(
                [
                  next.address ?? Vi.noAddress,
                  if (next.plannedArrival != null) Vi.expectedAtLower(formatTime(next.plannedArrival, route.timezone)),
                ].join(' · '),
                style: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
              ),
            ],
          ),
        ),
    ];
  }
}
