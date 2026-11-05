import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import '../auth/auth.dart';

/// "Tuyến hôm nay": the driver's home. M00 only greets and signs out; the routes come with M01.
class RoutesScreen extends ConsumerWidget {
  const RoutesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = context.pw;
    final session = ref.watch(sessionProvider);
    final name = session is SessionSignedIn ? (session.user.fullName ?? session.user.email) : '';
    return Scaffold(
      appBar: AppBar(
        title: Text(Vi.routesTitle, style: PwTypeStyles.title.copyWith(color: t.ink)),
        backgroundColor: t.surfaceRaised,
        actions: [
          PwButton(
            label: Vi.logout,
            variant: PwButtonVariant.quiet,
            onPressed: () => unawaited(ref.read(sessionProvider.notifier).logOut()),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(PwSpacing.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(Vi.hello(name), style: PwTypeStyles.heading.copyWith(color: t.ink)),
            const SizedBox(height: PwSpacing.space2),
            Text(Vi.routesSoon, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
          ],
        ),
      ),
    );
  }
}
