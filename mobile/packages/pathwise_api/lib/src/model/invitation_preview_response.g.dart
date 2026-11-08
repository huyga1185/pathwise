// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'invitation_preview_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

InvitationPreviewResponse _$InvitationPreviewResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('InvitationPreviewResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['email', 'expiresAt', 'role', 'storeName'],
  );
  final val = InvitationPreviewResponse(
    email: $checkedConvert('email', (v) => v as String),
    expiresAt: $checkedConvert('expiresAt', (v) => DateTime.parse(v as String)),
    role: $checkedConvert(
      'role',
      (v) => $enumDecode(_$InvitationPreviewResponseRoleEnumEnumMap, v),
    ),
    storeName: $checkedConvert('storeName', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$InvitationPreviewResponseToJson(
  InvitationPreviewResponse instance,
) => <String, dynamic>{
  'email': instance.email,
  'expiresAt': instance.expiresAt.toIso8601String(),
  'role': _$InvitationPreviewResponseRoleEnumEnumMap[instance.role]!,
  'storeName': instance.storeName,
};

const _$InvitationPreviewResponseRoleEnumEnumMap = {
  InvitationPreviewResponseRoleEnum.ADMIN: 'ADMIN',
  InvitationPreviewResponseRoleEnum.OWNER: 'OWNER',
  InvitationPreviewResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  InvitationPreviewResponseRoleEnum.DRIVER: 'DRIVER',
};
