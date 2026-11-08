// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'create_invitation_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CreateInvitationRequest _$CreateInvitationRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('CreateInvitationRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['email', 'role']);
  final val = CreateInvitationRequest(
    email: $checkedConvert('email', (v) => v as String),
    role: $checkedConvert(
      'role',
      (v) => $enumDecode(_$CreateInvitationRequestRoleEnumEnumMap, v),
    ),
  );
  return val;
});

Map<String, dynamic> _$CreateInvitationRequestToJson(
  CreateInvitationRequest instance,
) => <String, dynamic>{
  'email': instance.email,
  'role': _$CreateInvitationRequestRoleEnumEnumMap[instance.role]!,
};

const _$CreateInvitationRequestRoleEnumEnumMap = {
  CreateInvitationRequestRoleEnum.DISPATCHER: 'DISPATCHER',
  CreateInvitationRequestRoleEnum.DRIVER: 'DRIVER',
};
