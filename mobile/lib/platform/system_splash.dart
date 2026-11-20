import 'package:flutter/services.dart';

/// The Android system splash screen, kept up by `MainActivity` until the session is known (D2): the app has no splash
/// of its own. Tests use a fake.
abstract interface class SystemSplash {
  /// Lets the system splash go; safe to call more than once.
  Future<void> dismiss();
}

class AndroidSystemSplash implements SystemSplash {
  const AndroidSystemSplash();

  static const _channel = MethodChannel('pathwise/splash');

  @override
  Future<void> dismiss() async {
    try {
      await _channel.invokeMethod<void>('dismiss');
    } on MissingPluginException {
      // No Android side (widget tests, other platforms): nothing to dismiss.
    }
  }
}
