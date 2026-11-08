import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../data/api_error.dart';
import 'routes_data.dart';

/// Keeps a route fresh while its screen is open (M02): reads it again every 30 s while the app is in front and when
/// the app comes back, so a driver learns early that the plan was cancelled (the route then answers 404, T48 would
/// make that explicit).
mixin RoutePolling<T extends ConsumerStatefulWidget> on ConsumerState<T> {
  static const interval = Duration(seconds: 30);

  int get pollRouteId;

  Timer? _poll;
  AppLifecycleListener? _lifecycle;

  /// Reads the route again (also the list, which the route belongs to).
  void reloadRoute() {
    ref
      ..invalidate(driverRouteProvider(pollRouteId))
      ..invalidate(driverRoutesProvider);
  }

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: reloadRoute);
    _poll = Timer.periodic(interval, (_) {
      final lifecycle = WidgetsBinding.instance.lifecycleState;
      if (lifecycle == null || lifecycle == AppLifecycleState.resumed) reloadRoute();
    });
  }

  @override
  void dispose() {
    _poll?.cancel();
    _lifecycle?.dispose();
    super.dispose();
  }
}

/// The route to show: a reload that could not reach the server keeps what was shown (weak signal must not blank the
/// screen); an answer of the server (404 once the plan is cancelled…) replaces it with the error.
DriverRouteDetailResponse? shownRoute(AsyncValue<DriverRouteDetailResponse> route) {
  final error = route.error;
  if (error != null && !(error is ApiError && sendFailed(error))) return null;
  return route.value;
}
