import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../platform/preferences.dart';
import 'providers.dart';

const _key = 'pw-theme';

/// The theme saved on this device, read before the first frame (main.dart) so the app never flashes the other one.
Future<ThemeMode> savedThemeMode(Preferences preferences) async {
  try {
    return switch (await preferences.read(_key)) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
  } on Object {
    return ThemeMode.system;
  }
}

/// What [savedThemeMode] found at start; the system's theme unless main.dart overrides it.
final initialThemeModeProvider = Provider<ThemeMode>((ref) => ThemeMode.system);

/// Light/dark theme (DESIGN_SYSTEM "Theme sáng và tối", ARCHITECTURE §8): follows the system until the driver picks
/// one, and remembers the pick on this device, like the web app (`pw-theme`).
class ThemeModeController extends Notifier<ThemeMode> {
  @override
  ThemeMode build() => ref.read(initialThemeModeProvider);

  Future<void> choose(ThemeMode mode) async {
    state = mode;
    await ref.read(preferencesProvider).write(_key, mode == ThemeMode.system ? null : mode.name);
  }
}

final themeModeProvider = NotifierProvider<ThemeModeController, ThemeMode>(ThemeModeController.new);
