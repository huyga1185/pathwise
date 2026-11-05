// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'change_password_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ChangePasswordRequest _$ChangePasswordRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ChangePasswordRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['newPassword', 'otp']);
  final val = ChangePasswordRequest(
    newPassword: $checkedConvert('newPassword', (v) => v as String),
    otp: $checkedConvert('otp', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$ChangePasswordRequestToJson(
  ChangePasswordRequest instance,
) => <String, dynamic>{
  'newPassword': instance.newPassword,
  'otp': instance.otp,
};
