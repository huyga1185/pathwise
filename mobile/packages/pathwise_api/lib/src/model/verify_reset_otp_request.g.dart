// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'verify_reset_otp_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

VerifyResetOtpRequest _$VerifyResetOtpRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('VerifyResetOtpRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['email', 'otp']);
  final val = VerifyResetOtpRequest(
    email: $checkedConvert('email', (v) => v as String),
    otp: $checkedConvert('otp', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$VerifyResetOtpRequestToJson(
  VerifyResetOtpRequest instance,
) => <String, dynamic>{'email': instance.email, 'otp': instance.otp};
