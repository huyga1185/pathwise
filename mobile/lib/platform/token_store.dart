import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// TokenStore port (P-46, ARCHITECTURE §2.1, §4): the access token lives in memory only, the refresh token in secure
/// storage (Android Keystore, iOS Keychain). Never logged.
abstract interface class TokenStore {
  String? get accessToken;
  set accessToken(String? token);
  Future<String?> readRefreshToken();
  Future<void> writeRefreshToken(String? token);
}

class SecureTokenStore implements TokenStore {
  SecureTokenStore([FlutterSecureStorage? storage]) : _storage = storage ?? const FlutterSecureStorage();

  static const _refreshKey = 'pathwise.refreshToken';
  final FlutterSecureStorage _storage;

  @override
  String? accessToken;

  @override
  Future<String?> readRefreshToken() => _storage.read(key: _refreshKey);

  @override
  Future<void> writeRefreshToken(String? token) =>
      token == null ? _storage.delete(key: _refreshKey) : _storage.write(key: _refreshKey, value: token);
}

/// Tests and previews: nothing leaves memory.
class MemoryTokenStore implements TokenStore {
  MemoryTokenStore({this.accessToken, String? refreshToken}) : _refresh = refreshToken;

  String? _refresh;

  @override
  String? accessToken;

  String? get refreshToken => _refresh;

  @override
  Future<String?> readRefreshToken() async => _refresh;

  @override
  Future<void> writeRefreshToken(String? token) async => _refresh = token;
}
