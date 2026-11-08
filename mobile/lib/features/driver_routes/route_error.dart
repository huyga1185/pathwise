import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../data/api_error.dart';
import '../../shared/errors/errors.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';

/// A route that could not be shown. Gone (404: another driver's, or its plan cancelled, API_SPEC §8) → tell the
/// driver to stop and call the dispatcher, back to the list; anything else → the error and "Thử lại".
List<Widget> routeErrorView(BuildContext context, Object error, VoidCallback onRetry) {
  if (error is ApiError && error.status == 404) {
    return [
      ErrorBanner(
        title: Vi.routeGone,
        detail: Vi.routeGoneDetail,
        action: PwButton(label: Vi.backToRoutes, block: true, onPressed: () => context.go('/routes')),
      ),
    ];
  }
  return [
    ErrorBanner(title: Vi.routeLoadFailed, detail: messageFor(error)),
    const SizedBox(height: PwSpacing.space4),
    PwButton(label: Vi.retry, block: true, onPressed: onRetry),
  ];
}
