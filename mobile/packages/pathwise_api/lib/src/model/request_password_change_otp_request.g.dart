// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'request_password_change_otp_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RequestPasswordChangeOtpRequest _$RequestPasswordChangeOtpRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('RequestPasswordChangeOtpRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['currentPassword']);
  final val = RequestPasswordChangeOtpRequest(
    currentPassword: $checkedConvert('currentPassword', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$RequestPasswordChangeOtpRequestToJson(
  RequestPasswordChangeOtpRequest instance,
) => <String, dynamic>{'currentPassword': instance.currentPassword};
