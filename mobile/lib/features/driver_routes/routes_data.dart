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

/// Why the caller's store is locked by an ADMIN (`GET /users/me`, T56), or null when it is not. A locked store still
/// lets the driver report results and send the location (P-59): the route screen only shows a banner. An error is
/// treated as not locked: the banner is information, never a reason to stop delivering.
final storeLockProvider = FutureProvider.autoDispose<String?>((ref) async {
  try {
    final response = await guard(() => ref.watch(apiClientProvider).api.getUserControllerApi().getUserProfile());
    final user = response.data!.data;
    if (user.storeStatus != GetUserResponseStoreStatusEnum.READ_ONLY) return null;
    return user.storeLockReason ?? '';
  } on Object {
    return null;
  }
}, retry: _noRetry);

/// The three results a driver reports; none can be undone (P-34).
enum StopResult {
  delivered('DELIVERED'),
  failed('FAILED'),
  skipped('SKIPPED');

  const StopResult(this.value);

  final String value;

  static StopResult? of(String value) => values.where((result) => result.value == value).firstOrNull;
}

/// No answer from the server, or one saying to try later (5xx, 429): the driver waits and sends the same result again
/// ("Gửi lại"); the server takes a resend as the same report (P-36).
bool sendFailed(ApiError error) =>
    error.code == ApiError.networkError || error.status == null || error.status! >= 500 || error.status == 429;

/// Reports a stop's result (`PATCH …/stops/{stopId}/status`). Sent straight away, never queued on the phone (P-51):
/// the driver waits for the server, so a cancelled plan stops them at the next stop at the latest.
class StopReporter {
  const StopReporter(this._ref);

  final Ref _ref;

  /// Throws an [ApiError]. Once the server has answered (success or refusal) the route and the list are read again.
  Future<DriverStopUpdateResponse> report({
    required int routeId,
    required int stopId,
    required StopResult result,
    String? failureReason,
  }) async {
    final api = _ref.read(apiClientProvider).api.getDriverRouteControllerApi();
    try {
      final response = await guard(
        () => api.updateStopStatus(
          routeId: routeId,
          stopId: stopId,
          updateStopStatusRequest: UpdateStopStatusRequest(
            status: UpdateStopStatusRequestStatusEnum.values.byName(result.value),
            failureReason: failureReason,
          ),
        ),
      );
      _reload(routeId);
      return response.data!.data;
    } on ApiError catch (error) {
      if (!sendFailed(error)) _reload(routeId);
      rethrow;
    }
  }

  void _reload(int routeId) => _ref
    ..invalidate(driverRouteProvider(routeId))
    ..invalidate(driverRoutesProvider);
}

final stopReporterProvider = Provider<StopReporter>(StopReporter.new);
