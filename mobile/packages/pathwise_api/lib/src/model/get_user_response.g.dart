// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'get_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

GetUserResponse _$GetUserResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('GetUserResponse', json, ($checkedConvert) {
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
          'storeId',
          'storeLockReason',
          'storeStatus',
        ],
      );
      final val = GetUserResponse(
        canDrive: $checkedConvert('canDrive', (v) => v as bool),
        email: $checkedConvert('email', (v) => v as String),
        fullName: $checkedConvert('fullName', (v) => v as String?),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        phoneNumber: $checkedConvert('phoneNumber', (v) => v as String?),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$GetUserResponseRoleEnumEnumMap, v),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$GetUserResponseStatusEnumEnumMap, v),
        ),
        storeId: $checkedConvert('storeId', (v) => (v as num?)?.toInt()),
        storeLockReason: $checkedConvert(
          'storeLockReason',
          (v) => v as String?,
        ),
        storeStatus: $checkedConvert(
          'storeStatus',
          (v) =>
              $enumDecodeNullable(_$GetUserResponseStoreStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$GetUserResponseToJson(
  GetUserResponse instance,
) => <String, dynamic>{
  'canDrive': instance.canDrive,
  'email': instance.email,
  'fullName': instance.fullName,
  'id': instance.id,
  'phoneNumber': instance.phoneNumber,
  'role': _$GetUserResponseRoleEnumEnumMap[instance.role]!,
  'status': _$GetUserResponseStatusEnumEnumMap[instance.status]!,
  'storeId': instance.storeId,
  'storeLockReason': instance.storeLockReason,
  'storeStatus': _$GetUserResponseStoreStatusEnumEnumMap[instance.storeStatus],
};

const _$GetUserResponseRoleEnumEnumMap = {
  GetUserResponseRoleEnum.ADMIN: 'ADMIN',
  GetUserResponseRoleEnum.OWNER: 'OWNER',
  GetUserResponseRoleEnum.DISPATCHER: 'DISPATCHER',
  GetUserResponseRoleEnum.DRIVER: 'DRIVER',
};

const _$GetUserResponseStatusEnumEnumMap = {
  GetUserResponseStatusEnum.PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  GetUserResponseStatusEnum.ACTIVE: 'ACTIVE',
  GetUserResponseStatusEnum.DISABLED: 'DISABLED',
};

const _$GetUserResponseStoreStatusEnumEnumMap = {
  GetUserResponseStoreStatusEnum.ACTIVE: 'ACTIVE',
  GetUserResponseStoreStatusEnum.READ_ONLY: 'READ_ONLY',
};
