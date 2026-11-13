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

    return Scaffold(
      backgroundColor: t.surface,
      body: SafeArea(
        child: Column(
          children: [
            _Header(route: detail),
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
          current: true,
          onCall: next.recipientPhone == null ? null : () => unawaited(_open(callUri(next.recipientPhone!))),
        ),
        const SizedBox(height: PwSpacing.space2),
        Row(
          children: [
            Expanded(
              child: PwButton(
                label: Vi.directions,
                block: true,
                onPressed: () => context.push('/routes/${route.routeId}/navigate?stopId=${next.id}'),
              ),
            ),
            const SizedBox(width: PwSpacing.space2),
            Expanded(
              child: PwButton(
                label: Vi.report,
                variant: PwButtonVariant.primary,
                block: true,
                onPressed: () => context.push('/routes/${route.routeId}/stops/${next.id}'),
              ),
            ),
          ],
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
              borderRadius: BorderRadius.circular(PwRadius.radiusMd),
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
        for (final stop in pending.skip(1)) ...[
          _StopView(stop: stop, timezone: route.timezone),
          const SizedBox(height: PwSpacing.space2),
        ],
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
        if (_doneOpen)
          for (final stop in done) ...[
            _StopView(stop: stop, timezone: route.timezone, showStatus: true),
            const SizedBox(height: PwSpacing.space2),
          ],
      ],
      gap,
      Text(_startAndEnd(route), style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
    ];
  }

  static String _startAndEnd(DriverRouteDetailResponse route) {
    final start = _place(route.start);
    final end = _place(route.end);
    return start == end ? Vi.startAndEnd(start) : Vi.startThenEnd(start, end);
  }

  /// The depot's address, or its coordinates when it has none.
  static String _place(LocationResponse location) => location.address ?? '${location.lat}, ${location.lon}';
}

class _Header extends StatelessWidget {
  const _Header({required this.route});

  final DriverRouteDetailResponse? route;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final route = this.route;
    return Container(
      padding: const EdgeInsets.all(PwSpacing.space2),
      decoration: BoxDecoration(
        color: t.surfaceRaised,
        border: Border(bottom: BorderSide(color: t.line)),
      ),
      child: Row(
        children: [
          IconButton(
            tooltip: Vi.backToRoutes,
            icon: Icon(Icons.chevron_left, color: t.ink),
            constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
            onPressed: () => context.canPop() ? context.pop() : context.go('/routes'),
          ),
          const SizedBox(width: PwSpacing.space1),
          Expanded(
            child: route == null
                ? const SizedBox.shrink()
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Semantics(
                        header: true,
                        child: Text(route.planName, style: PwTypeStyles.heading.copyWith(color: t.ink, fontSize: 18)),
                      ),
                      Text(formatDateLong(route.planDate), style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
                    ],
                  ),
          ),
          if (route != null) StatusPill(kind: StatusKind.route, status: route.status.value),
        ],
      ),
    );
  }
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

/// A stop as a StopCard: the next one with its phone, the finished ones with their status and reason.
class _StopView extends StatelessWidget {
  const _StopView({
    required this.stop,
    required this.timezone,
    this.current = false,
    this.showStatus = false,
    this.onCall,
  });

  final DriverStopResponse stop;
  final String timezone;
  final bool current;
  final bool showStatus;
  final VoidCallback? onCall;

  @override
  Widget build(BuildContext context) {
    final window = stop.timeWindow;
    return StopCard(
      sequence: stop.sequence,
      title: [stop.label, ?stop.recipientName].join(' · '),
      address: stop.address ?? Vi.noAddress,
      eta: showStatus || stop.plannedArrival == null ? null : formatTime(stop.plannedArrival, timezone),
      timeWindow: showStatus || window == null ? null : formatTimeWindow(window.start, window.end),
      note: showStatus ? null : stop.note,
      status: showStatus ? stop.status.value : null,
      failureReason: showStatus ? stop.failureReason : null,
      phone: current ? stop.recipientPhone : null,
      onCall: onCall,
      current: current,
    );
  }
}
