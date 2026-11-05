import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'session.dart';

/// Signed in, but not a driver (neither DRIVER nor `canDrive`, P-44): the app has nothing for them.
class NoAccessScreen extends ConsumerWidget {
  const NoAccessScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = context.pw;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(PwSpacing.space6),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Semantics(
                header: true,
                child: Text(Vi.noAccessTitle, style: PwTypeStyles.title.copyWith(color: t.ink)),
              ),
              const SizedBox(height: PwSpacing.space2),
              Text(Vi.noAccessLead, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
              const SizedBox(height: PwSpacing.space6),
              PwButton(
                label: Vi.logout,
                variant: PwButtonVariant.primary,
                block: true,
                onPressed: () => unawaited(ref.read(sessionProvider.notifier).logOut()),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
