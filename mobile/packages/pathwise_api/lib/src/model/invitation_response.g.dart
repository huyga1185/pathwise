// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'invitation_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

InvitationResponse _$InvitationResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('InvitationResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['email', 'expiresAt', 'id', 'role', 'status'],
      );
      final val = InvitationResponse(
        email: $checkedConvert('email', (v) => v as String),
        expiresAt: $checkedConvert(
          'expiresAt',
          (v) => DateTime.parse(v as String),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$InvitationResponseRoleEnumEnumMap, v),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$InvitationResponseStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$InvitationResponseToJson(InvitationResponse instance) =>
    <String, dynamic>{
      'email': instance.email,
      'expiresAt': instance.expiresAt.toIso8601String(),
      'id': instance.id,
      'role': _$InvitationResponseRoleEnumEnumMap[instance.role]!,
      'status': _$InvitationResponseStatusEnumEnumMap[instance.status]!,
    };

const _$InvitationResponseRoleEnumEnumMap = {
  InvitationResponseRoleEnum.ADMIN: 'ADMIN',
  InvitationResponseRoleEnum.OWNER: 'OWNER',
  InvitationResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  InvitationResponseRoleEnum.DRIVER: 'DRIVER',
};

const _$InvitationResponseStatusEnumEnumMap = {
  InvitationResponseStatusEnum.PENDING: 'PENDING',
  InvitationResponseStatusEnum.ACCEPTED: 'ACCEPTED',
  InvitationResponseStatusEnum.REVOKED: 'REVOKED',
  InvitationResponseStatusEnum.EXPIRED: 'EXPIRED',
};
