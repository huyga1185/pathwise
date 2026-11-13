import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../platform/location.dart';
import '../../shared/i18n/vi.dart';
import '../auth/auth.dart';
import 'routes_data.dart';

/// What the driver picked in the account menu (P-54): asked at each new route (the default), always or never.
enum SharingChoice {
  ask('ASK'),
  always('ALWAYS'),
  never('NEVER');

  const SharingChoice(this.value);

  final String value;
}

const choiceKey = 'pw-location-sharing';

/// The answer for the last route asked about ("routeId:1" or "routeId:0"), so opening it again does not ask again.
const routeAnswerKey = 'pw-location-route';

/// The choice saved on this phone; never sent to the server (P-53).
class SharingChoiceController extends AsyncNotifier<SharingChoice> {
  @override
  Future<SharingChoice> build() async {
    final saved = await ref.read(preferencesProvider).read(choiceKey);
    return SharingChoice.values.where((choice) => choice.value == saved).firstOrNull ?? SharingChoice.ask;
  }

  Future<void> choose(SharingChoice choice) async {
    state = AsyncData(choice);
    await ref.read(preferencesProvider).write(choiceKey, choice == SharingChoice.ask ? null : choice.value);
  }
}

final sharingChoiceProvider = AsyncNotifierProvider<SharingChoiceController, SharingChoice>(
  SharingChoiceController.new,
);

enum SharingPhase {
  /// No route is being shared.
  off,

  /// The route waits for the driver's answer (the dialog).
  asking,
  sharing,

  /// The driver chose not to share this route.
  declined,

  /// The phone does not let the app use the location; [SharingStatus.canAsk] says whether asking again shows the
  /// system dialog or only the settings can change it.
  noPermission,
  gpsOff,
}

class SharingStatus {
  const SharingStatus(this.phase, {this.routeId, this.canAsk = false});

  const SharingStatus.off() : this(SharingPhase.off);

  final SharingPhase phase;
  final int? routeId;
  final bool canAsk;

  bool isFor(int routeId) => this.routeId == routeId && phase != SharingPhase.off;
}

/// Sends the driver's position while a route is being delivered (FR-D04, P-54), one route at a time: from opening the
/// detail of a route not finished in a running plan, with the driver's consent, until the route is done, the plan
/// is no longer running, the driver stops sharing or signs out. Positions are batched (D-26) and sent every
/// [sendEvery]; while offline they wait in memory (at most [maxPoints], the oldest dropped). Each change of state is
/// reported to the server so the dispatcher knows why there is no position (P-53).
class LocationSharingController extends Notifier<SharingStatus> {
  static const sendEvery = Duration(seconds: 30);
  static const maxPoints = 100;

  final _buffer = <LocationFix>[];
  StreamSubscription<LocationFix>? _fixes;
  Timer? _timer;
  bool _sending = false;
  bool _sentOnce = false;
  String _planName = '';

  @override
  SharingStatus build() {
    ref.listen(sessionProvider, (previous, next) {
      if (next is! SessionSignedIn) stop();
    });
    ref.onDispose(_stopWatching);
    return const SharingStatus.off();
  }

  LocationSource get _source => ref.read(locationSourceProvider);

  /// The detail of [route] was opened: share, ask or not, depending on the answer for this route or the choice.
  /// Opening the route being handled again reports its state again (the server keeps it for 12 hours).
  Future<void> openRoute(DriverRouteDetailResponse route) async {
    if (route.status == DriverRouteDetailResponseStatusEnum.COMPLETED) return routeEnded(route.routeId);
    _planName = route.planName;
    if (state.isFor(route.routeId)) {
      if (state.phase != SharingPhase.asking) unawaited(_report(state.phase));
      return;
    }
    _stopWatching();
    final answer = await _savedAnswer(route.routeId);
    if (answer != null) return answer ? _start(route.routeId) : _decline(route.routeId);
    switch (await ref.read(sharingChoiceProvider.future)) {
      case SharingChoice.always:
        await _start(route.routeId);
      case SharingChoice.never:
        await _decline(route.routeId);
      case SharingChoice.ask:
        state = SharingStatus(SharingPhase.asking, routeId: route.routeId);
    }
  }

  /// The driver answered the dialog; [remember] saves it as the choice of the account menu.
  Future<void> answer({required bool share, required bool remember}) async {
    final routeId = state.routeId;
    if (routeId == null) return;
    if (remember) {
      await ref.read(sharingChoiceProvider.notifier).choose(share ? SharingChoice.always : SharingChoice.never);
    }
    await _saveAnswer(routeId, share);
    if (share) {
      await _start(routeId);
    } else {
      await _decline(routeId);
    }
  }

  /// "Bật chia sẻ vị trí": share this route after all (asks for the permission when it still can).
  Future<void> enable() async {
    final routeId = state.routeId;
    if (routeId == null) return;
    await _saveAnswer(routeId, true);
    await _start(routeId);
  }

  /// "Mở cài đặt" / "Bật định vị": the phone's settings; [recheck] when the app comes back.
  Future<void> openSettings() async {
    if (state.phase == SharingPhase.gpsOff) {
      await _source.openLocationSettings();
    } else {
      await _source.openAppSettings();
    }
  }

  /// The app came back to the front: the driver may have changed the permission or turned the GPS on.
  Future<void> recheck() async {
    final routeId = state.routeId;
    if (routeId == null) return;
    if (state.phase == SharingPhase.noPermission || state.phase == SharingPhase.gpsOff) {
      await _start(routeId, ask: false);
    }
  }

  /// The route is done, or its plan no longer runs (`PLAN_NOT_ACTIVE`): stop if it is the one shared.
  void routeEnded(int routeId) {
    if (state.routeId == routeId) stop();
  }

  /// Sends what is left, then stops.
  void stop() {
    unawaited(_flush());
    _stopWatching();
    state = const SharingStatus.off();
  }

  Future<void> _start(int routeId, {bool ask = true}) async {
    var access = await _source.check();
    if (access == LocationAccess.denied && ask) access = await _source.request();
    if (state.routeId != null && state.routeId != routeId && state.phase != SharingPhase.off) return;
    switch (access) {
      case LocationAccess.granted:
        state = SharingStatus(SharingPhase.sharing, routeId: routeId);
        _watch();
      case LocationAccess.serviceOff:
        _stopWatching();
        state = SharingStatus(SharingPhase.gpsOff, routeId: routeId);
      case LocationAccess.denied || LocationAccess.deniedForever:
        _stopWatching();
        state = SharingStatus(SharingPhase.noPermission, routeId: routeId, canAsk: access == LocationAccess.denied);
    }
    await _report(state.phase);
  }

  Future<void> _decline(int routeId) async {
    _stopWatching();
    state = SharingStatus(SharingPhase.declined, routeId: routeId);
    await _report(SharingPhase.declined);
  }

  void _watch() {
    if (_fixes != null) return;
    _sentOnce = false;
    _fixes = _source
        .watch(title: Vi.sharingNotificationTitle, text: Vi.sharingNotificationText(_planName))
        .listen(_onFix, onError: _onUnavailable);
    _timer = Timer.periodic(sendEvery, (_) => unawaited(_flush()));
  }

  void _onFix(LocationFix fix) {
    _buffer.add(fix);
    if (_buffer.length > maxPoints) _buffer.removeRange(0, _buffer.length - maxPoints);
    // The first point goes at once, so the dispatcher sees the driver without waiting for the timer.
    if (!_sentOnce) unawaited(_flush());
  }

  void _onUnavailable(Object error) {
    final routeId = state.routeId;
    if (routeId == null) return;
    _stopWatching();
    final access = error is LocationUnavailable ? error.access : LocationAccess.denied;
    state = access == LocationAccess.serviceOff
        ? SharingStatus(SharingPhase.gpsOff, routeId: routeId)
        : SharingStatus(SharingPhase.noPermission, routeId: routeId, canAsk: true);
    unawaited(_report(state.phase));
  }

  void _stopWatching() {
    unawaited(_fixes?.cancel());
    _fixes = null;
    _timer?.cancel();
    _timer = null;
  }

  Future<void> _flush() async {
    if (_sending || _buffer.isEmpty) return;
    _sending = true;
    final batch = List.of(_buffer);
    try {
      await guard(
        () => ref
            .read(apiClientProvider)
            .api
            .getDriverLocationControllerApi()
            .recordLocations(
              locationBatchRequest: LocationBatchRequest(
                points: [
                  for (final fix in batch)
                    LocationPointRequest(
                      lat: fix.lat,
                      lon: fix.lon,
                      accuracyMeters: fix.accuracyMeters,
                      headingDegrees: fix.headingDegrees,
                      speedMps: fix.speedMps,
                      recordedAt: fix.recordedAt,
                    ),
                ],
              ),
            ),
      );
      _sentOnce = true;
      _buffer.removeWhere(batch.contains);
    } on ApiError catch (error) {
      // Offline or server busy: the points wait for the next try. Refused (400…): sending them again would not help.
      if (!sendFailed(error)) _buffer.removeWhere(batch.contains);
    } finally {
      _sending = false;
    }
  }

  /// Best effort: the dispatcher only loses the reason if this fails. A plan no longer running stops the sharing.
  Future<void> _report(SharingPhase phase) async {
    final routeId = state.routeId;
    final value = switch (phase) {
      SharingPhase.sharing => UpdateLocationSharingRequestStateEnum.SHARING,
      SharingPhase.declined => UpdateLocationSharingRequestStateEnum.DECLINED,
      SharingPhase.noPermission => UpdateLocationSharingRequestStateEnum.NO_PERMISSION,
      SharingPhase.gpsOff => UpdateLocationSharingRequestStateEnum.GPS_OFF,
      SharingPhase.off || SharingPhase.asking => null,
    };
    if (routeId == null || value == null) return;
    try {
      await guard(
        () => ref
            .read(apiClientProvider)
            .api
            .getDriverLocationControllerApi()
            .updateSharing(
              updateLocationSharingRequest: UpdateLocationSharingRequest(routeId: routeId, state: value),
            ),
      );
    } on ApiError catch (error) {
      if (error.code == 'PLAN_NOT_ACTIVE') routeEnded(routeId);
    }
  }

  Future<bool?> _savedAnswer(int routeId) async {
    final saved = await ref.read(preferencesProvider).read(routeAnswerKey);
    if (saved?.split(':') case [final id, final share] when id == '$routeId') return share == '1';
    return null;
  }

  Future<void> _saveAnswer(int routeId, bool share) =>
      ref.read(preferencesProvider).write(routeAnswerKey, '$routeId:${share ? 1 : 0}');
}

final locationSharingProvider = NotifierProvider<LocationSharingController, SharingStatus>(
  LocationSharingController.new,
);
