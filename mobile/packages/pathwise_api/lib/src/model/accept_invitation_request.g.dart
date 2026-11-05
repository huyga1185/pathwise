// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'accept_invitation_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AcceptInvitationRequest _$AcceptInvitationRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('AcceptInvitationRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['code', 'fullName', 'password']);
  final val = AcceptInvitationRequest(
    code: $checkedConvert('code', (v) => v as String),
    fullName: $checkedConvert('fullName', (v) => v as String),
    password: $checkedConvert('password', (v) => v as String),
    phoneNumber: $checkedConvert('phoneNumber', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$AcceptInvitationRequestToJson(
  AcceptInvitationRequest instance,
) => <String, dynamic>{
  'code': instance.code,
  'fullName': instance.fullName,
  'password': instance.password,
  'phoneNumber': ?instance.phoneNumber,
};
