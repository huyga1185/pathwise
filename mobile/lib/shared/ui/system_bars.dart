import 'package:flutter/services.dart';

/// Status bar icons of [icons] brightness, for content drawn behind the status bar (developer.android.com
/// edge-to-edge: "manually update the system bar icon colors so they contrast with your app's background"). Only the
/// status bar: the navigation bar keeps the system's style.
SystemUiOverlayStyle statusBarFor(Brightness icons) => SystemUiOverlayStyle(
  statusBarColor: const Color(0x00000000),
  statusBarIconBrightness: icons,
  // iOS reads the bar's brightness, the opposite of the icons'.
  statusBarBrightness: icons == Brightness.light ? Brightness.dark : Brightness.light,
);
