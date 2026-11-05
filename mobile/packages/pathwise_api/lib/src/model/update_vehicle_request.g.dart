// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_vehicle_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateVehicleRequest _$UpdateVehicleRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('UpdateVehicleRequest', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['active', 'capacity', 'capacityUnit', 'name'],
  );
  final val = UpdateVehicleRequest(
    active: $checkedConvert('active', (v) => v as bool),
    capacity: $checkedConvert('capacity', (v) => (v as num).toInt()),
    capacityUnit: $checkedConvert(
      'capacityUnit',
      (v) => $enumDecode(_$UpdateVehicleRequestCapacityUnitEnumEnumMap, v),
    ),
    name: $checkedConvert('name', (v) => v as String),
    plateNumber: $checkedConvert('plateNumber', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$UpdateVehicleRequestToJson(
  UpdateVehicleRequest instance,
) => <String, dynamic>{
  'active': instance.active,
  'capacity': instance.capacity,
  'capacityUnit':
      _$UpdateVehicleRequestCapacityUnitEnumEnumMap[instance.capacityUnit]!,
  'name': instance.name,
  'plateNumber': ?instance.plateNumber,
};

const _$UpdateVehicleRequestCapacityUnitEnumEnumMap = {
  UpdateVehicleRequestCapacityUnitEnum.KG: 'KG',
  UpdateVehicleRequestCapacityUnitEnum.BOX: 'BOX',
  UpdateVehicleRequestCapacityUnitEnum.PACKAGE: 'PACKAGE',
  UpdateVehicleRequestCapacityUnitEnum.ORDER: 'ORDER',
};
