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
  'updatedAt': instance.updatedAt.toIso8601String(),
};

const _$AdminGetUserResponseRoleEnumEnumMap = {
  AdminGetUserResponseRoleEnum.ADMIN: 'ADMIN',
  AdminGetUserResponseRoleEnum.OWNER: 'OWNER',
  AdminGetUserResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  AdminGetUserResponseRoleEnum.DRIVER: 'DRIVER',
};
