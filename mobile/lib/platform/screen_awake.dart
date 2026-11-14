import 'package:wakelock_plus/wakelock_plus.dart';

/// Keeps the screen on while turn-by-turn directions are open (M05, P-57); only this file knows `wakelock_plus`.
/// Tests use a fake.
abstract interface class ScreenAwake {
  Future<void> keepOn();

  Future<void> release();
}

class WakelockScreenAwake implements ScreenAwake {
  const WakelockScreenAwake();

  @override
  Future<void> keepOn() => WakelockPlus.enable();

  @override
  Future<void> release() => WakelockPlus.disable();
}
