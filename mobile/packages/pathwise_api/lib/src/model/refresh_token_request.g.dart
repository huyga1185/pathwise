// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'refresh_token_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RefreshTokenRequest _$RefreshTokenRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('RefreshTokenRequest', json, ($checkedConvert) {
      final val = RefreshTokenRequest(
        refreshToken: $checkedConvert('refreshToken', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$RefreshTokenRequestToJson(
  RefreshTokenRequest instance,
) => <String, dynamic>{'refreshToken': ?instance.refreshToken};
