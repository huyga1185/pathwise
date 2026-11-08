import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../data/api_error.dart';
import '../../shared/errors/errors.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';

/// A route that could not be shown. Its plan was cancelled after the driver got it (`PLAN_NOT_ACTIVE`, T48) → say so
/// and tell them to stop; gone (404: not theirs, or never published, API_SPEC §8) → say it is no longer there; both
/// go back to the list. Anything else → the error and "Thử lại".
List<Widget> routeErrorView(BuildContext context, Object error, VoidCallback onRetry) {
  final back = PwButton(label: Vi.backToRoutes, block: true, onPressed: () => context.go('/routes'));
  if (error is ApiError && error.code == 'PLAN_NOT_ACTIVE') {
    return [ErrorBanner(title: messageFor(error), detail: Vi.stopDelivering, action: back)];
  }
  if (error is ApiError && error.status == 404) {
    return [ErrorBanner(title: Vi.routeGone, detail: Vi.routeGoneDetail, action: back)];
  }
  return [
    ErrorBanner(title: Vi.routeLoadFailed, detail: messageFor(error)),
    const SizedBox(height: PwSpacing.space4),
    PwButton(label: Vi.retry, block: true, onPressed: onRetry),
  ];
}
