import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';

/// Errors reach the screen at once (its "Thử lại" and pull-to-refresh retry): Riverpod's automatic retry would keep
/// a refused request (404, 401) spinning.
Duration? _noRetry(int retryCount, Object error) => null;

/// The caller's routes of a day (`GET /driver/routes`), by planned start. A null day lets the server pick today in
/// the store's timezone (API_SPEC §8).
final driverRoutesProvider = FutureProvider.autoDispose.family<List<DriverRouteSummaryResponse>, String?>((
  ref,
  day,
) async {
  final api = ref.watch(apiClientProvider).api.getDriverRouteControllerApi();
  final response = await guard(() => api.listRoutes(date: day));
  return response.data!.data;
}, retry: _noRetry);

/// One of the caller's routes with its stops in visiting order (`GET /driver/routes/{routeId}`).
final driverRouteProvider = FutureProvider.autoDispose.family<DriverRouteDetailResponse, int>((ref, routeId) async {
  final api = ref.watch(apiClientProvider).api.getDriverRouteControllerApi();
  final response = await guard(() => api.getRoute(routeId: routeId));
  return response.data!.data;
}, retry: _noRetry);
