// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reset_password_by_link_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ResetPasswordByLinkRequest _$ResetPasswordByLinkRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ResetPasswordByLinkRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['newPassword', 'otp', 'token']);
  final val = ResetPasswordByLinkRequest(
    newPassword: $checkedConvert('newPassword', (v) => v as String),
    otp: $checkedConvert('otp', (v) => v as String),
    token: $checkedConvert('token', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$ResetPasswordByLinkRequestToJson(
  ResetPasswordByLinkRequest instance,
) => <String, dynamic>{
  'newPassword': instance.newPassword,
  'otp': instance.otp,
  'token': instance.token,
};
