// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reset_token_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ResetTokenResponse _$ResetTokenResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ResetTokenResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['expiresAt', 'resetToken']);
      final val = ResetTokenResponse(
        expiresAt: $checkedConvert(
          'expiresAt',
          (v) => DateTime.parse(v as String),
        ),
        resetToken: $checkedConvert('resetToken', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$ResetTokenResponseToJson(ResetTokenResponse instance) =>
    <String, dynamic>{
      'expiresAt': instance.expiresAt.toIso8601String(),
      'resetToken': instance.resetToken,
    };
