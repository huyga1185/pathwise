// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'member_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MemberResponse _$MemberResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('MemberResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'canDrive',
          'email',
          'fullName',
          'id',
          'phoneNumber',
          'role',
          'status',
        ],
      );
      final val = MemberResponse(
        canDrive: $checkedConvert('canDrive', (v) => v as bool),
        email: $checkedConvert('email', (v) => v as String),
        fullName: $checkedConvert('fullName', (v) => v as String?),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        phoneNumber: $checkedConvert('phoneNumber', (v) => v as String?),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$MemberResponseRoleEnumEnumMap, v),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$MemberResponseStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$MemberResponseToJson(MemberResponse instance) =>
    <String, dynamic>{
      'canDrive': instance.canDrive,
      'email': instance.email,
      'fullName': instance.fullName,
      'id': instance.id,
      'phoneNumber': instance.phoneNumber,
      'role': _$MemberResponseRoleEnumEnumMap[instance.role]!,
      'status': _$MemberResponseStatusEnumEnumMap[instance.status]!,
    };

const _$MemberResponseRoleEnumEnumMap = {
  MemberResponseRoleEnum.ADMIN: 'ADMIN',
  MemberResponseRoleEnum.OWNER: 'OWNER',
  MemberResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  MemberResponseRoleEnum.DRIVER: 'DRIVER',
};

const _$MemberResponseStatusEnumEnumMap = {
  MemberResponseStatusEnum.PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  MemberResponseStatusEnum.ACTIVE: 'ACTIVE',
  MemberResponseStatusEnum.DISABLED: 'DISABLED',
};
