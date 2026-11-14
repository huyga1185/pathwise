// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reset_link_otp_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ResetLinkOtpRequest _$ResetLinkOtpRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ResetLinkOtpRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['token']);
      final val = ResetLinkOtpRequest(
        token: $checkedConvert('token', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$ResetLinkOtpRequestToJson(
  ResetLinkOtpRequest instance,
) => <String, dynamic>{'token': instance.token};
