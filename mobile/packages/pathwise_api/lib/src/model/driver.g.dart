// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Driver _$DriverFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Driver', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['fullName', 'id', 'phoneNumber', 'role'],
      );
      final val = Driver(
        fullName: $checkedConvert('fullName', (v) => v as String?),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        phoneNumber: $checkedConvert('phoneNumber', (v) => v as String?),
        role: $checkedConvert(
          'role',
          (v) => $enumDecode(_$DriverRoleEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$DriverToJson(Driver instance) => <String, dynamic>{
  'fullName': instance.fullName,
  'id': instance.id,
  'phoneNumber': instance.phoneNumber,
  'role': _$DriverRoleEnumEnumMap[instance.role]!,
};

const _$DriverRoleEnumEnumMap = {
  DriverRoleEnum.ADMIN: 'ADMIN',
  DriverRoleEnum.OWNER: 'OWNER',
  DriverRoleEnum.DISPATCHER: 'DISPATCHER',
  DriverRoleEnum.DRIVER: 'DRIVER',
};
