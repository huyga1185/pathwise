// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'resend_verification_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ResendVerificationRequest _$ResendVerificationRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ResendVerificationRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['email']);
  final val = ResendVerificationRequest(
    email: $checkedConvert('email', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$ResendVerificationRequestToJson(
  ResendVerificationRequest instance,
) => <String, dynamic>{'email': instance.email};
