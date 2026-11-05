// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_member_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateMemberRequest _$UpdateMemberRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('UpdateMemberRequest', json, ($checkedConvert) {
      final val = UpdateMemberRequest(
        canDrive: $checkedConvert('canDrive', (v) => v as bool?),
        role: $checkedConvert(
          'role',
          (v) => $enumDecodeNullable(_$UpdateMemberRequestRoleEnumEnumMap, v),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecodeNullable(_$UpdateMemberRequestStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$UpdateMemberRequestToJson(
  UpdateMemberRequest instance,
) => <String, dynamic>{
  'canDrive': ?instance.canDrive,
  'role': ?_$UpdateMemberRequestRoleEnumEnumMap[instance.role],
  'status': ?_$UpdateMemberRequestStatusEnumEnumMap[instance.status],
};

const _$UpdateMemberRequestRoleEnumEnumMap = {
  UpdateMemberRequestRoleEnum.DISPATCHER: 'DISPATCHER',
  UpdateMemberRequestRoleEnum.DRIVER: 'DRIVER',
};

const _$UpdateMemberRequestStatusEnumEnumMap = {
  UpdateMemberRequestStatusEnum.ACTIVE: 'ACTIVE',
  UpdateMemberRequestStatusEnum.DISABLED: 'DISABLED',
};
