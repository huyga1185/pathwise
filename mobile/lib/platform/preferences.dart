import 'package:shared_preferences/shared_preferences.dart';

/// Preferences port (ARCHITECTURE §2.1, §8): small settings remembered on this device (the theme). Never a token or a
/// secret (those go to TokenStore).
abstract interface class Preferences {
  Future<String?> read(String key);

  /// Writes [value], or removes the key when null.
  Future<void> write(String key, String? value);
}

class SharedPreferencesStore implements Preferences {
  SharedPreferencesStore([SharedPreferencesAsync? prefs]) : _prefs = prefs ?? SharedPreferencesAsync();

  final SharedPreferencesAsync _prefs;

  @override
  Future<String?> read(String key) => _prefs.getString(key);

  @override
  Future<void> write(String key, String? value) => value == null ? _prefs.remove(key) : _prefs.setString(key, value);
}

/// Tests: kept in memory.
class MemoryPreferences implements Preferences {
  MemoryPreferences([Map<String, String>? values]) : values = values ?? {};

  final Map<String, String> values;

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String? value) async => value == null ? values.remove(key) : values[key] = value;
}
