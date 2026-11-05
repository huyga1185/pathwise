// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_token_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AuthTokenResponse _$AuthTokenResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('AuthTokenResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'accessToken',
          'accessTokenExpiresAt',
          'refreshTokenExpiresAt',
        ],
      );
      final val = AuthTokenResponse(
        accessToken: $checkedConvert('accessToken', (v) => v as String),
        accessTokenExpiresAt: $checkedConvert(
          'accessTokenExpiresAt',
          (v) => DateTime.parse(v as String),
        ),
        refreshToken: $checkedConvert('refreshToken', (v) => v as String?),
        refreshTokenExpiresAt: $checkedConvert(
          'refreshTokenExpiresAt',
          (v) => DateTime.parse(v as String),
        ),
        user: $checkedConvert(
          'user',
          (v) => v == null
              ? null
              : AuthUserResponse.fromJson(v as Map<String, dynamic>),
        ),
      );
      return val;
    });

Map<String, dynamic> _$AuthTokenResponseToJson(AuthTokenResponse instance) =>
    <String, dynamic>{
      'accessToken': instance.accessToken,
      'accessTokenExpiresAt': instance.accessTokenExpiresAt.toIso8601String(),
      'refreshToken': ?instance.refreshToken,
      'refreshTokenExpiresAt': instance.refreshTokenExpiresAt.toIso8601String(),
      'user': ?instance.user?.toJson(),
    };
