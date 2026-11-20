import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../auth/auth.dart';

/// The signed-in person as the server has it now (`GET /users/me`), read each time "Thông tin của tôi" opens.
final myProfileProvider = FutureProvider.autoDispose<GetUserResponse>((ref) async {
  final response = await guard(() => ref.watch(apiClientProvider).api.getUserControllerApi().getUserProfile());
  return response.data!.data;
}, retry: (retryCount, error) => null);

/// Saves the name and phone number (`PATCH /users/me`, M06).
class ProfileUpdater {
  const ProfileUpdater(this._ref);

  final Ref _ref;

  /// A merge patch (API_SPEC §1): an emptied field is sent as `null` to clear it, as on the web's profile page ("" is
  /// not a phone number, F-43). Sent as a map: the generated request leaves `null` out, which would keep the old value.
  /// Throws an [ApiError]; on success the session takes the new name.
  Future<GetUserResponse> save({required String fullName, required String phoneNumber}) async {
    final client = _ref.read(apiClientProvider);
    final name = fullName.trim();
    final phone = phoneNumber.trim();
    final response = await guard(
      () => client.dio.patch<Map<String, dynamic>>(
        '/api/v1/users/me',
        data: {'fullName': name.isEmpty ? null : name, 'phoneNumber': phone.isEmpty ? null : phone},
      ),
    );
    final user = GetUserResponse.fromJson(Map<String, dynamic>.from(response.data!['data'] as Map));
    _ref.read(sessionProvider.notifier).renamed(user.fullName);
    return user;
  }
}

final profileUpdaterProvider = Provider<ProfileUpdater>(ProfileUpdater.new);
