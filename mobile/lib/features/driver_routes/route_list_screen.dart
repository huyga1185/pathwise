import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../app/theme_mode.dart';
import '../../shared/errors/errors.dart';
import '../../shared/format.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/status/status.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import '../auth/auth.dart';
import 'routes_data.dart';

/// "Tài xế — tuyến hôm nay" (SCREENS §2): the driver's routes of one day, today by default. Pull down or come back to
/// the app to reload (ARCHITECTURE §5).
class RouteListScreen extends ConsumerStatefulWidget {
  const RouteListScreen({super.key});

  @override
  ConsumerState<RouteListScreen> createState() => _RouteListScreenState();
}

class _RouteListScreenState extends ConsumerState<RouteListScreen> {
  /// The day asked for; null until the driver changes day, so the server picks today in the store's timezone.
  String? _day;

  /// The store's timezone, once a route has told it (T47). Until then "today" is the phone's day.
  String? _timezone;

  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: () => ref.invalidate(driverRoutesProvider));
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final routes = ref.watch(driverRoutesProvider(_day));
    final loaded = routes.value;
    if (loaded != null && loaded.isNotEmpty) _timezone = loaded.first.timezone;

    final now = ref.watch(clockProvider).now();
    final today = _timezone == null ? deviceDay(now) : dayIn(now, _timezone!);
    // Without a day asked for, the server's today is the plan date of what it returned.
    final shown = _day ?? (loaded != null && loaded.isNotEmpty ? loaded.first.planDate : today);

    return Scaffold(
      backgroundColor: t.surface,
      body: SafeArea(
        child: Column(
          children: [
            const _Header(),
            _DaySwitcher(
              day: shown,
              today: today,
              // From _day once set: two quick taps both move, even before the screen rebuilds.
              onMove: (days) => setState(() => _day = shiftDay(_day ?? shown, days)),
            ),
            Expanded(
              child: RefreshIndicator(
                color: t.route,
                onRefresh: () => ref.refresh(driverRoutesProvider(_day).future).then((_) {}, onError: (_) {}),
                child: switch (routes) {
                  AsyncValue(:final value?) when value.isEmpty => const _Scroll(children: [_Empty()]),
                  AsyncValue(:final value?) => _Scroll(
                    children: [
                      for (final route in value) ...[
                        _RouteCard(route: route, onOpen: () => context.push('/routes/${route.routeId}')),
                        const SizedBox(height: PwSpacing.space4),
                      ],
                    ],
                  ),
                  AsyncValue(:final error?) => _Scroll(
                    children: [
                      ErrorBanner(title: Vi.routesLoadFailed, detail: messageFor(error)),
                      const SizedBox(height: PwSpacing.space4),
                      PwButton(
                        label: Vi.retry,
                        block: true,
                        onPressed: () => ref.invalidate(driverRoutesProvider(_day)),
                      ),
                    ],
                  ),
                  _ => const Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// "Tuyến của tôi" and the driver's initials, which open the account sheet: appearance and sign-out (M03).
class _Header extends ConsumerWidget {
  const _Header();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = context.pw;
    final session = ref.watch(sessionProvider);
    final user = session is SessionSignedIn ? session.user : null;
    final name = user == null ? '' : (user.fullName ?? user.email);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4, vertical: PwSpacing.space2),
      decoration: BoxDecoration(
        color: t.surfaceRaised,
        border: Border(bottom: BorderSide(color: t.line)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Semantics(
              header: true,
              child: Text(Vi.myRoutes, style: PwTypeStyles.title.copyWith(color: t.ink)),
            ),
          ),
          Semantics(
            button: true,
            label: Vi.accountMenu(name),
            excludeSemantics: true,
            child: InkResponse(
              radius: 28,
              onTap: () => unawaited(_openAccount(context, ref, name, user?.email ?? '')),
              child: SizedBox(
                width: 48,
                height: 48,
                child: Center(
                  child: CircleAvatar(
                    radius: 20,
                    backgroundColor: t.surfaceTint,
                    child: Text(
                      initials(name),
                      style: PwTypeStyles.body.copyWith(color: t.ink, fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _openAccount(BuildContext context, WidgetRef ref, String name, String email) async {
    final t = context.pw;
    final logOut = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: t.surfaceRaised,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (sheet) => _AccountSheet(name: name, email: email),
    );
    if ((logOut ?? false) && context.mounted) await _confirmLogOut(context, ref);
  }

  Future<void> _confirmLogOut(BuildContext context, WidgetRef ref) async {
    final confirmed = await showPwDialog(context, title: Vi.logoutTitle, body: Vi.logoutBody, confirmLabel: Vi.logout);
    if (confirmed) await ref.read(sessionProvider.notifier).logOut();
  }
}

/// "Tài xế B" → "TB", "lan@example.com" → "L".
@visibleForTesting
String initials(String name) {
  final words = name.split('@').first.trim().split(RegExp(r'\s+')).where((word) => word.isNotEmpty).toList();
  if (words.isEmpty) return '';
  final first = words.first.characters.first;
  return (words.length == 1 ? first : first + words.last.characters.first).toUpperCase();
}

class _DaySwitcher extends StatelessWidget {
  const _DaySwitcher({required this.day, required this.today, required this.onMove});

  final String day;
  final String today;
  final ValueChanged<int> onMove;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final relative = switch (daysBetween(today, day)) {
      0 => Vi.today,
      -1 => Vi.yesterday,
      1 => Vi.tomorrow,
      _ => null,
    };
    return Padding(
      padding: const EdgeInsets.all(PwSpacing.space2),
      child: Row(
        children: [
          _DayButton(icon: Icons.chevron_left, label: Vi.previousDay, onPressed: () => onMove(-1)),
          Expanded(
            child: Semantics(
              liveRegion: true,
              child: Column(
                children: [
                  Text(relative ?? formatDateLong(day), style: PwTypeStyles.heading.copyWith(color: t.ink)),
                  if (relative != null) Text(formatDateLong(day), style: PwTypeStyles.body.copyWith(color: t.inkMuted)),
                ],
              ),
            ),
          ),
          _DayButton(icon: Icons.chevron_right, label: Vi.nextDay, onPressed: () => onMove(1)),
        ],
      ),
    );
  }
}

class _DayButton extends StatelessWidget {
  const _DayButton({required this.icon, required this.label, required this.onPressed});

  final IconData icon;
  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => IconButton(
    onPressed: onPressed,
    tooltip: label,
    icon: Icon(icon, color: context.pw.ink),
    constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
  );
}

/// Always scrollable, so pull-to-refresh works on a short or empty list.
class _Scroll extends StatelessWidget {
  const _Scroll({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) => ListView(
    physics: const AlwaysScrollableScrollPhysics(),
    padding: const EdgeInsets.fromLTRB(PwSpacing.space4, PwSpacing.space2, PwSpacing.space4, PwSpacing.space6),
    children: children,
  );
}

class _RouteCard extends StatelessWidget {
  const _RouteCard({required this.route, required this.onOpen});

  final DriverRouteSummaryResponse route;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final status = route.status.value;
    final cta = switch (route.status) {
      DriverRouteSummaryResponseStatusEnum.IN_PROGRESS => Vi.continueRoute,
      DriverRouteSummaryResponseStatusEnum.COMPLETED => Vi.reviewRoute,
      _ => Vi.startRoute,
    };
    final start = route.plannedStart == null ? null : Vi.plannedStart(formatTime(route.plannedStart, route.timezone));
    final muted = PwTypeStyles.body.copyWith(color: t.inkMuted);
    final label = [
      route.planName,
      statusInfo(StatusKind.route, status)?.label,
      '${route.completedCount}/${route.stopCount} ${Vi.stopsDone.toLowerCase()}',
      ?start,
      cta,
    ].whereType<String>().join(', ');

    return Semantics(
      button: true,
      label: label,
      excludeSemantics: true,
      onTap: onOpen,
      child: Material(
        color: t.surfaceRaised,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(PwRadius.radiusMd),
          side: BorderSide(color: t.line),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onOpen,
          child: Padding(
            padding: const EdgeInsets.all(PwSpacing.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(route.planName, style: PwTypeStyles.heading.copyWith(color: t.ink, fontSize: 18)),
                    ),
                    StatusPill(kind: StatusKind.route, status: status),
                  ],
                ),
                const SizedBox(height: PwSpacing.space2),
                PwProgressBar(label: Vi.stopsDone, value: route.completedCount, max: route.stopCount),
                const SizedBox(height: PwSpacing.space2),
                if (start != null) Text(start, style: muted),
                Text(Vi.stopCount(route.stopCount), style: muted),
                const SizedBox(height: PwSpacing.space4),
                PwButton(label: cta, variant: PwButtonVariant.primary, block: true, onPressed: onOpen),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Empty extends StatelessWidget {
  const _Empty();

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Padding(
      padding: const EdgeInsets.fromLTRB(PwSpacing.space4, 48, PwSpacing.space4, 0),
      child: Column(
        children: [
          ExcludeSemantics(
            child: CustomPaint(size: const Size(160, 120), painter: _EmptyPainter(t)),
          ),
          const SizedBox(height: PwSpacing.space4),
          Text(
            Vi.noRoutesTitle,
            textAlign: TextAlign.center,
            style: PwTypeStyles.title.copyWith(color: t.ink),
          ),
          const SizedBox(height: PwSpacing.space4),
          Text(
            Vi.noRoutesLead,
            textAlign: TextAlign.center,
            style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted),
          ),
        ],
      ),
    );
  }
}

/// The empty state's picture (prototype): a tinted card with a dashed route leaving a dot.
class _EmptyPainter extends CustomPainter {
  _EmptyPainter(this.t);

  final PwColorTokens t;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(8, 10, 144, 100), const Radius.circular(8)),
      Paint()..color = t.surfaceTint,
    );
    final path = Path()
      ..moveTo(30, 90)
      ..cubicTo(60, 90, 56, 56, 86, 54)
      ..cubicTo(116, 52, 120, 34, 128, 30);
    final stroke = Paint()
      ..color = t.route
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    for (final metric in path.computeMetrics()) {
      for (var distance = 0.0; distance < metric.length; distance += 13) {
        canvas.drawPath(metric.extractPath(distance, distance + 7), stroke);
      }
    }
    canvas.drawCircle(const Offset(30, 90), 7, Paint()..color = t.ink);
  }

  @override
  bool shouldRepaint(_EmptyPainter oldDelegate) => oldDelegate.t != t;
}

/// The account sheet: who is signed in, the theme (system, light, dark; remembered on the phone), sign out.
class _AccountSheet extends ConsumerWidget {
  const _AccountSheet({required this.name, required this.email});

  final String name;
  final String email;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = context.pw;
    final mode = ref.watch(themeModeProvider);
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(PwSpacing.space4, 0, PwSpacing.space4, PwSpacing.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(name, style: PwTypeStyles.title.copyWith(color: t.ink)),
            if (email.isNotEmpty && email != name)
              Text(email, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
            const SizedBox(height: PwSpacing.space4),
            Semantics(
              header: true,
              child: Text(Vi.appearance.toUpperCase(), style: PwTypeStyles.label.copyWith(color: t.inkMuted)),
            ),
            const SizedBox(height: PwSpacing.space1),
            for (final (option, label) in const [
              (ThemeMode.system, Vi.themeSystem),
              (ThemeMode.light, Vi.themeLight),
              (ThemeMode.dark, Vi.themeDark),
            ])
              _ThemeOption(
                label: label,
                selected: mode == option,
                onTap: () => unawaited(ref.read(themeModeProvider.notifier).choose(option)),
              ),
            const SizedBox(height: PwSpacing.space4),
            PwButton(label: Vi.logout, block: true, onPressed: () => Navigator.of(context).pop(true)),
          ],
        ),
      ),
    );
  }
}

class _ThemeOption extends StatelessWidget {
  const _ThemeOption({required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Semantics(
      inMutuallyExclusiveGroup: true,
      checked: selected,
      button: true,
      label: label,
      excludeSemantics: true,
      onTap: onTap,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(PwRadius.radiusSm),
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 48),
          child: Row(
            children: [
              Icon(
                selected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                color: selected ? t.route : t.inkMuted,
              ),
              const SizedBox(width: PwSpacing.space2),
              Expanded(
                child: Text(
                  label,
                  style: PwTypeStyles.bodyTouch.copyWith(
                    color: t.ink,
                    fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
