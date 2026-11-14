// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'admin_member_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AdminMemberResponse _$AdminMemberResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('AdminMemberResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['createdAt', 'email', 'id', 'role', 'status'],
      );
      final val = AdminMemberResponse(
        createdAt: $checkedConvert(
          'createdAt',
          (v) => DateTime.parse(v as String),
        ),
        email: $checkedConvert('email', (v) => v as String),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$AdminMemberResponseRoleEnumEnumMap, v),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$AdminMemberResponseStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$AdminMemberResponseToJson(
  AdminMemberResponse instance,
) => <String, dynamic>{
  'createdAt': instance.createdAt.toIso8601String(),
  'email': instance.email,
  'id': instance.id,
  'role': _$AdminMemberResponseRoleEnumEnumMap[instance.role]!,
  'status': _$AdminMemberResponseStatusEnumEnumMap[instance.status]!,
};

const _$AdminMemberResponseRoleEnumEnumMap = {
  AdminMemberResponseRoleEnum.ADMIN: 'ADMIN',
  AdminMemberResponseRoleEnum.OWNER: 'OWNER',
  AdminMemberResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  AdminMemberResponseRoleEnum.DRIVER: 'DRIVER',
};

const _$AdminMemberResponseStatusEnumEnumMap = {
  AdminMemberResponseStatusEnum.PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  AdminMemberResponseStatusEnum.ACTIVE: 'ACTIVE',
  AdminMemberResponseStatusEnum.DISABLED: 'DISABLED',
};
