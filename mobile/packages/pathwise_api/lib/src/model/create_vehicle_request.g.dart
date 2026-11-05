// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'create_vehicle_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CreateVehicleRequest _$CreateVehicleRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('CreateVehicleRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['capacity', 'capacityUnit', 'name']);
  final val = CreateVehicleRequest(
    capacity: $checkedConvert('capacity', (v) => (v as num).toInt()),
    capacityUnit: $checkedConvert(
      'capacityUnit',
      (v) => $enumDecode(_$CreateVehicleRequestCapacityUnitEnumEnumMap, v),
    ),
    name: $checkedConvert('name', (v) => v as String),
    plateNumber: $checkedConvert('plateNumber', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$CreateVehicleRequestToJson(
  CreateVehicleRequest instance,
) => <String, dynamic>{
  'capacity': instance.capacity,
  'capacityUnit':
      _$CreateVehicleRequestCapacityUnitEnumEnumMap[instance.capacityUnit]!,
  'name': instance.name,
  'plateNumber': ?instance.plateNumber,
};

const _$CreateVehicleRequestCapacityUnitEnumEnumMap = {
  CreateVehicleRequestCapacityUnitEnum.KG: 'KG',
  CreateVehicleRequestCapacityUnitEnum.BOX: 'BOX',
  CreateVehicleRequestCapacityUnitEnum.PACKAGE: 'PACKAGE',
  CreateVehicleRequestCapacityUnitEnum.ORDER: 'ORDER',
};
