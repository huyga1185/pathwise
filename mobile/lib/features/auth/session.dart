import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';

/// The signed-in person, from `LoginResponse.user` or `GET /users/me`.
class SessionUser {
  const SessionUser({required this.id, required this.email, this.fullName, required this.role, required this.canDrive});

  final int id;
  final String email;
  final String? fullName;
  final String role;
  final bool canDrive;

  /// Who may use the driver app (P-44, P-46): a DRIVER, or anyone with `canDrive`.
  bool get drives => role == 'DRIVER' || canDrive;
}

sealed class Session {
  const Session();
}

class SessionLoading extends Session {
  const SessionLoading();
}

class SessionAnonymous extends Session {
  const SessionAnonymous({this.expired = false});

  /// True when the session ended by itself (refresh refused): the login screen says so.
  final bool expired;
}

class SessionSignedIn extends Session {
  const SessionSignedIn(this.user);

  final SessionUser user;
}

/// The app's session (P-46, ARCHITECTURE §4): restored from the stored refresh token on start, the access token in
/// memory only, cleared on sign out or when a refresh is refused.
class SessionController extends Notifier<Session> {
  @override
  Session build() {
    final client = ref.watch(apiClientProvider);
    client.onSessionExpired = _expire;
    unawaited(_restore());
    return const SessionLoading();
  }

  Future<void> _restore() async {
    final client = ref.read(apiClientProvider);
    try {
      if (!await client.refresh()) {
        state = const SessionAnonymous();
        return;
      }
      final me = await guard(() => client.api.getUserControllerApi().getUserProfile());
      final user = me.data!.data;
      state = SessionSignedIn(
        SessionUser(
          id: user.id,
          email: user.email,
          fullName: user.fullName,
          role: user.role.value,
          canDrive: user.canDrive,
        ),
      );
    } on Object {
      client.tokens.accessToken = null;
      await client.tokens.writeRefreshToken(null);
      state = const SessionAnonymous();
    }
  }

  /// Signs in as the mobile client (refresh token in the body, kept in secure storage).
  /// Throws an [ApiError] (INVALID_EMAIL_OR_PASSWORD, ACCOUNT_DISABLED, RATE_LIMITED…).
  Future<SessionUser> logIn(String email, String password) async {
    final client = ref.read(apiClientProvider);
    final response = await guard(
      () => client.api.getAuthControllerApi().logIn(
        logInRequest: LogInRequest(email: email, password: password),
        xClientPlatform: 'mobile',
      ),
    );
    final tokens = response.data!.data;
    client.tokens.accessToken = tokens.accessToken;
    await client.tokens.writeRefreshToken(tokens.refreshToken);
    final user = tokens.user!;
    final session = SessionUser(
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      role: user.role.value,
      canDrive: user.canDrive,
    );
    state = SessionSignedIn(session);
    return session;
  }

  /// Ends the session on the server (best effort) and on this device.
  Future<void> logOut() async {
    final client = ref.read(apiClientProvider);
    final refreshToken = await client.tokens.readRefreshToken();
    try {
      if (refreshToken != null) {
        await client.api.getAuthControllerApi().logOut(
          xClientPlatform: 'mobile',
          refreshTokenRequest: RefreshTokenRequest(refreshToken: refreshToken),
        );
      }
    } on Object {
      // The session ends on this device either way.
    }
    client.tokens.accessToken = null;
    await client.tokens.writeRefreshToken(null);
    state = const SessionAnonymous();
  }

  void _expire() {
    final client = ref.read(apiClientProvider);
    client.tokens.accessToken = null;
    unawaited(client.tokens.writeRefreshToken(null));
    state = const SessionAnonymous(expired: true);
  }
}

final sessionProvider = NotifierProvider<SessionController, Session>(SessionController.new);
