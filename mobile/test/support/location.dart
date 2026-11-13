import 'dart:async';

import 'package:pathwise_driver/platform/location.dart';

/// The phone's location in memory: [access] is what `check` answers, [afterRequest] what the system dialog leads to;
/// [emit] sends a position to every watcher, [fail] stops the streams like the phone would.
class FakeLocationSource implements LocationSource {
  FakeLocationSource({this.access = LocationAccess.granted, LocationAccess? afterRequest})
    : afterRequest = afterRequest ?? access;

  LocationAccess access;
  LocationAccess afterRequest;
  int requests = 0;
  int settingsOpened = 0;
  int locationSettingsOpened = 0;
  final _watchers = <({StreamController<LocationFix> controller, bool precise})>[];
  String? notificationText;

  /// Someone listens to positions.
  bool get watching => _watchers.isNotEmpty;

  /// Someone listens with the turn-by-turn settings (about one position a second).
  bool get watchingPrecisely => _watchers.any((watcher) => watcher.precise);

  @override
  Future<LocationAccess> check() async => access;

  @override
  Future<LocationAccess> request() async {
    requests++;
    access = afterRequest;
    return access;
  }

  @override
  Stream<LocationFix> watch({required String title, required String text, bool precise = false}) {
    if (!precise) notificationText = text;
    final controller = StreamController<LocationFix>();
    final watcher = (controller: controller, precise: precise);
    controller.onListen = () {
      _watchers.add(watcher);
    };
    controller.onCancel = () {
      _watchers.remove(watcher);
    };
    return controller.stream;
  }

  void emit(LocationFix fix) {
    for (final watcher in List.of(_watchers)) {
      watcher.controller.add(fix);
    }
  }

  void fail(LocationAccess access) {
    this.access = access;
    for (final watcher in List.of(_watchers)) {
      watcher.controller.addError(LocationUnavailable(access));
    }
  }

  @override
  Future<bool> openAppSettings() async {
    settingsOpened++;
    return true;
  }

  @override
  Future<bool> openLocationSettings() async {
    locationSettingsOpened++;
    return true;
  }
}
