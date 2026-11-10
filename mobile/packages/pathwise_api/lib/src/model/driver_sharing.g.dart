// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_sharing.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverSharing _$DriverSharingFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DriverSharing', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['state', 'updatedAt']);
      final val = DriverSharing(
        state: $checkedConvert(
          'state',
          (v) => $enumDecode(_$DriverSharingStateEnumEnumMap, v),
        ),
        updatedAt: $checkedConvert(
          'updatedAt',
          (v) => DateTime.parse(v as String),
        ),
      );
      return val;
    });

Map<String, dynamic> _$DriverSharingToJson(DriverSharing instance) =>
    <String, dynamic>{
      'state': _$DriverSharingStateEnumEnumMap[instance.state]!,
      'updatedAt': instance.updatedAt.toIso8601String(),
    };

const _$DriverSharingStateEnumEnumMap = {
  DriverSharingStateEnum.SHARING: 'SHARING',
  DriverSharingStateEnum.DECLINED: 'DECLINED',
  DriverSharingStateEnum.NO_PERMISSION: 'NO_PERMISSION',
  DriverSharingStateEnum.GPS_OFF: 'GPS_OFF',
};
