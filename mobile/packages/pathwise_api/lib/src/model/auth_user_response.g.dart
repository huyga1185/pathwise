// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AuthUserResponse _$AuthUserResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('AuthUserResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['canDrive', 'email', 'id', 'role']);
      final val = AuthUserResponse(
        canDrive: $checkedConvert('canDrive', (v) => v as bool),
        email: $checkedConvert('email', (v) => v as String),
        fullName: $checkedConvert('fullName', (v) => v as String?),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$AuthUserResponseRoleEnumEnumMap, v),
        ),
        storeId: $checkedConvert('storeId', (v) => (v as num?)?.toInt()),
      );
      return val;
    });

Map<String, dynamic> _$AuthUserResponseToJson(AuthUserResponse instance) =>
    <String, dynamic>{
      'canDrive': instance.canDrive,
      'email': instance.email,
      'fullName': ?instance.fullName,
      'id': instance.id,
      'role': _$AuthUserResponseRoleEnumEnumMap[instance.role]!,
      'storeId': ?instance.storeId,
    };

const _$AuthUserResponseRoleEnumEnumMap = {
  AuthUserResponseRoleEnum.ADMIN: 'ADMIN',
  AuthUserResponseRoleEnum.OWNER: 'OWNER',
  AuthUserResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  AuthUserResponseRoleEnum.DRIVER: 'DRIVER',
};
