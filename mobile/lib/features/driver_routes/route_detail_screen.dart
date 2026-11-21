import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../shared/format.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/status/status.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'directions.dart';
import 'location_sharing.dart';
import 'location_sharing_view.dart';
import 'route_error.dart';
import 'route_polling.dart';
import 'routes_data.dart';

/// "Tài xế — chi tiết tuyến" (SCREENS §2): the next stop stands out with "Gọi", "Chỉ đường" and "Báo kết quả", then
/// the stops left, then the finished ones (collapsible, with the failure reason). Opened from the list or straight by
/// id (T45). Read again every 30 s and when the app comes back (M02), so a cancelled plan shows. Opening a route not
/// finished starts sharing the driver's position, or asks first (T32, P-54); a finished or cancelled route stops it.
class RouteDetailScreen extends ConsumerStatefulWidget {
  const RouteDetailScreen({super.key, required this.routeId});

  final int routeId;

  @override
  ConsumerState<RouteDetailScreen> createState() => _RouteDetailScreenState();
}

class _RouteDetailScreenState extends ConsumerState<RouteDetailScreen> with RoutePolling {
  bool _doneOpen = true;
  bool _opened = false;
  bool _asking = false;
  late final AppLifecycleListener _sharingLifecycle;
  late final ProviderSubscription<AsyncValue<DriverRouteDetailResponse>> _routeSubscription;

  @override
  int get pollRouteId => widget.routeId;

  LocationSharingController get _sharing => ref.read(locationSharingProvider.notifier);

  @override
  void initState() {
    super.initState();
    // Back from the settings: the driver may have allowed the location or turned the GPS on.
    _sharingLifecycle = AppLifecycleListener(onResume: () => unawaited(_sharing.recheck()));
    _routeSubscription = ref.listenManual(driverRouteProvider(widget.routeId), (previous, next) {
      final route = next.value;
      if (next.error case ApiError(code: 'PLAN_NOT_ACTIVE')) {
        _sharing.routeEnded(widget.routeId);
      } else if (route != null && !next.isLoading) {
        if (!_opened || route.status == DriverRouteDetailResponseStatusEnum.COMPLETED) unawaited(_openRoute(route));
        _opened = true;
      }
    }, fireImmediately: true);
  }

  @override
  void dispose() {
    _sharingLifecycle.dispose();
    _routeSubscription.close();
    super.dispose();
  }

  Future<void> _openRoute(DriverRouteDetailResponse route) async {
    await _sharing.openRoute(route);
    final status = ref.read(locationSharingProvider);
    if (!mounted || _asking || status.phase != SharingPhase.asking || status.routeId != route.routeId) return;
    _asking = true;
    final answer = await showShareDialog(context, route.planName);
    _asking = false;
    // Dismissed without an answer: asked again the next time the route opens.
    if (answer != null) await _sharing.answer(share: answer.share, remember: answer.remember);
  }

  Future<void> _open(Uri uri) async {
    final opened = await ref.read(launcherProvider).open(uri);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text(Vi.cannotOpen)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final provider = driverRouteProvider(widget.routeId);
    final route = ref.watch(provider);
    final detail = shownRoute(route);

    // Material 3 small top app bar, pinned (DRIVER_UI_REDESIGN §3 "Thanh trên"): back and the plan's name only; the
    // status label sits in the page next to the progress, where the bar had no room for it (H2).
    return Scaffold(
      appBar: AppBar(
        // The platform's back icon, named for where it goes; opened straight by id there is nothing to pop.
        leading: IconButton(
          icon: const BackButtonIcon(),
          tooltip: Vi.backToRoutes,
          onPressed: () => context.canPop() ? context.pop() : context.go('/routes'),
        ),
        title: detail == null ? null : Text(detail.planName),
      ),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Expanded(
              child: RefreshIndicator(
                color: t.route,
                onRefresh: () => ref.refresh(provider.future).then((_) {}, onError: (_) {}),
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(PwSpacing.space4, PwSpacing.space4, PwSpacing.space4, 32),
                  children: switch (route) {
                    _ when detail != null => _content(detail),
                    AsyncValue(:final error?) => routeErrorView(context, error, () => ref.invalidate(provider)),
                    _ => const [
                      Padding(
                        padding: EdgeInsets.only(top: 48),
                        child: Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
                      ),
                    ],
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _content(DriverRouteDetailResponse route) {
    final t = context.pw;
    final pending = route.stops.where((stop) => stop.status == DriverStopResponseStatusEnum.PENDING).toList();
    final done = route.stops.where((stop) => stop.status != DriverStopResponseStatusEnum.PENDING).toList();
    final next = pending.isEmpty ? null : pending.first;
    const gap = SizedBox(height: PwSpacing.space6);

    return [
      Wrap(
        spacing: PwSpacing.space2,
        runSpacing: PwSpacing.space1,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          StatusPill(kind: StatusKind.route, status: route.status.value),
          Text(formatDateLong(route.planDate), style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
        ],
      ),
      const SizedBox(height: PwSpacing.space4),
      PwProgressBar(label: Vi.stopsDone, value: done.length, max: route.stops.length),
      if (ref.watch(locationSharingProvider).isFor(route.routeId)) ...[
        const SizedBox(height: PwSpacing.space4),
        LocationSharingBanner(routeId: route.routeId),
      ],
      if (next != null) ...[
        gap,
        _SectionTitle(Vi.nextStop),
        _StopView(
          stop: next,
          timezone: route.timezone,
          onCall: next.recipientPhone == null ? null : () => unawaited(_open(callUri(next.recipientPhone!))),
        ),
        const SizedBox(height: PwSpacing.space2),
        PwButtonPair(
          first: PwButton(
            label: Vi.directions,
            variant: PwButtonVariant.tonal,
            block: true,
            onPressed: () => context.push('/routes/${route.routeId}/navigate?stopId=${next.id}'),
          ),
          second: PwButton(
            label: Vi.report,
            variant: PwButtonVariant.filled,
            block: true,
            onPressed: () => context.push('/routes/${route.routeId}/stops/${next.id}'),
          ),
          labels: const [Vi.directions, Vi.report],
        ),
      ],
      if (route.status == DriverRouteDetailResponseStatusEnum.COMPLETED) ...[
        gap,
        Semantics(
          liveRegion: true,
          child: Container(
            padding: const EdgeInsets.all(PwSpacing.space4),
            decoration: BoxDecoration(
              color: t.surfaceRaised,
              border: Border.all(color: t.success),
              borderRadius: BorderRadius.circular(PwShapes.cornerMedium),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  Vi.routeDone,
                  style: PwTypeStyles.bodyTouch.copyWith(color: t.ink, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: PwSpacing.space1),
                Text(Vi.returnTo(_place(route.end)), style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
              ],
            ),
          ),
        ),
      ],
      if (pending.length > 1) ...[
        gap,
        _SectionTitle(Vi.restStops(pending.length - 1)),
        StopGroup(children: [for (final stop in pending.skip(1)) _stopRow(stop, route.timezone)]),
      ],
      if (done.isNotEmpty) ...[
        gap,
        Semantics(
          button: true,
          expanded: _doneOpen,
          child: InkWell(
            onTap: () => setState(() => _doneOpen = !_doneOpen),
            child: ConstrainedBox(
              constraints: const BoxConstraints(minHeight: 48),
              child: Row(
                children: [
                  Expanded(child: _SectionTitle(Vi.doneStops(done.length), padded: false)),
                  Text(
                    _doneOpen ? Vi.collapse : Vi.expand,
                    style: PwTypeStyles.body.copyWith(color: t.route, fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
          ),
        ),
        if (_doneOpen) StopGroup(children: [for (final stop in done) _stopRow(stop, route.timezone)]),
      ],
      gap,
      Text(_startAndEnd(route), style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
    ];
  }

  /// A stop after the next one: the address and times; a finished one: its status and failure reason.
  static Widget _stopRow(DriverStopResponse stop, String timezone) {
    final finished = stop.status != DriverStopResponseStatusEnum.PENDING;
    final reason = stop.failureReason;
    return StopRow(
      sequence: stop.sequence,
      title: [stop.label, ?stop.recipientName].join(' · '),
      address: stop.address ?? Vi.noAddress,
      detail: finished
          ? (reason == null || reason.isEmpty ? null : Vi.failureReasonLine(reason))
          : _times(stop, timezone),
      status: finished ? stop.status.value : null,
      danger: finished,
    );
  }

  /// "Dự kiến 10:39 · Khung 09:00–11:00", or null without either.
  static String? _times(DriverStopResponse stop, String timezone) {
    final window = stop.timeWindow;
    final parts = [
      if (stop.plannedArrival != null) 'Dự kiến ${formatTime(stop.plannedArrival, timezone)}',
      if (window != null) 'Khung ${formatTimeWindow(window.start, window.end)}',
    ];
    return parts.isEmpty ? null : parts.join(' · ');
  }

  static String _startAndEnd(DriverRouteDetailResponse route) {
    final start = _place(route.start);
    final end = _place(route.end);
    return start == end ? Vi.startAndEnd(start) : Vi.startThenEnd(start, end);
  }

  /// The depot's address, or its coordinates when it has none.
  static String _place(LocationResponse location) => location.address ?? '${location.lat}, ${location.lon}';
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text, {this.padded = true});

  final String text;
  final bool padded;

  @override
  Widget build(BuildContext context) => Padding(
    padding: EdgeInsets.only(bottom: padded ? PwSpacing.space2 : 0),
    child: Semantics(
      header: true,
      child: Text(text.toUpperCase(), style: PwTypeStyles.label.copyWith(color: context.pw.inkMuted)),
    ),
  );
}

/// The next stop as a StopCard, with its phone.
class _StopView extends StatelessWidget {
  const _StopView({required this.stop, required this.timezone, this.onCall});

  final DriverStopResponse stop;
  final String timezone;
  final VoidCallback? onCall;

  @override
  Widget build(BuildContext context) {
    final window = stop.timeWindow;
    return StopCard(
      sequence: stop.sequence,
      title: [stop.label, ?stop.recipientName].join(' · '),
      address: stop.address ?? Vi.noAddress,
      eta: stop.plannedArrival == null ? null : formatTime(stop.plannedArrival, timezone),
      timeWindow: window == null ? null : formatTimeWindow(window.start, window.end),
      note: stop.note,
      phone: stop.recipientPhone,
      onCall: onCall,
      current: true,
    );
  }
}
