import 'dart:async';

import 'package:pathwise_driver/platform/location.dart';

/// The phone's location in memory: [access] is what `check` answers, [afterRequest] what the system dialog leads to;
/// [emit] sends a position, [fail] stops the stream like the phone would.
class FakeLocationSource implements LocationSource {
  FakeLocationSource({this.access = LocationAccess.granted, LocationAccess? afterRequest})
    : afterRequest = afterRequest ?? access;

  LocationAccess access;
  LocationAccess afterRequest;
  int requests = 0;
  int settingsOpened = 0;
  int locationSettingsOpened = 0;
  StreamController<LocationFix>? _fixes;
  String? notificationText;

  bool get watching => _fixes != null && !_fixes!.isClosed;

  @override
  Future<LocationAccess> check() async => access;

  @override
  Future<LocationAccess> request() async {
    requests++;
    access = afterRequest;
    return access;
  }

  @override
  Stream<LocationFix> watch({required String title, required String text}) {
    notificationText = text;
    final controller = StreamController<LocationFix>();
    controller.onCancel = () => _fixes = null;
    _fixes = controller;
    return controller.stream;
  }

  void emit(LocationFix fix) => _fixes!.add(fix);

  void fail(LocationAccess access) {
    this.access = access;
    _fixes!.addError(LocationUnavailable(access));
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
