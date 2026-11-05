// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reset_password_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ResetPasswordRequest _$ResetPasswordRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ResetPasswordRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['newPassword', 'resetToken']);
  final val = ResetPasswordRequest(
    newPassword: $checkedConvert('newPassword', (v) => v as String),
    resetToken: $checkedConvert('resetToken', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$ResetPasswordRequestToJson(
  ResetPasswordRequest instance,
) => <String, dynamic>{
  'newPassword': instance.newPassword,
  'resetToken': instance.resetToken,
};
