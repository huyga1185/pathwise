// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'admin_get_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AdminGetUserResponse _$AdminGetUserResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('AdminGetUserResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'createdAt',
      'email',
      'id',
      'phoneNumber',
      'role',
      'status',
      'storeId',
      'updatedAt',
    ],
  );
  final val = AdminGetUserResponse(
    createdAt: $checkedConvert('createdAt', (v) => DateTime.parse(v as String)),
    email: $checkedConvert('email', (v) => v as String),
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    phoneNumber: $checkedConvert('phoneNumber', (v) => v as String?),
    role: $checkedConvert(
      'role',
      (v) => $enumDecode(_$AdminGetUserResponseRoleEnumEnumMap, v),
    ),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$AdminGetUserResponseStatusEnumEnumMap, v),
    ),
    storeId: $checkedConvert('storeId', (v) => (v as num?)?.toInt()),
    updatedAt: $checkedConvert('updatedAt', (v) => DateTime.parse(v as String)),
  );
  return val;
});

Map<String, dynamic> _$AdminGetUserResponseToJson(
  AdminGetUserResponse instance,
) => <String, dynamic>{
  'createdAt': instance.createdAt.toIso8601String(),
  'email': instance.email,
  'id': instance.id,
  'phoneNumber': instance.phoneNumber,
  'role': _$AdminGetUserResponseRoleEnumEnumMap[instance.role]!,
  'status': _$AdminGetUserResponseStatusEnumEnumMap[instance.status]!,
  'storeId': instance.storeId,
  'updatedAt': instance.updatedAt.toIso8601String(),
};

const _$AdminGetUserResponseRoleEnumEnumMap = {
  AdminGetUserResponseRoleEnum.ADMIN: 'ADMIN',
  AdminGetUserResponseRoleEnum.OWNER: 'OWNER',
  AdminGetUserResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  AdminGetUserResponseRoleEnum.DRIVER: 'DRIVER',
};

const _$AdminGetUserResponseStatusEnumEnumMap = {
  AdminGetUserResponseStatusEnum.PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  AdminGetUserResponseStatusEnum.ACTIVE: 'ACTIVE',
  AdminGetUserResponseStatusEnum.DISABLED: 'DISABLED',
};
