// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'vehicle.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Vehicle _$VehicleFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Vehicle', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['capacity', 'capacityUnit', 'id', 'name'],
      );
      final val = Vehicle(
        capacity: $checkedConvert('capacity', (v) => (v as num).toInt()),
        capacityUnit: $checkedConvert(
          'capacityUnit',
          (v) => $enumDecode(_$VehicleCapacityUnitEnumEnumMap, v),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        name: $checkedConvert('name', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$VehicleToJson(Vehicle instance) => <String, dynamic>{
  'capacity': instance.capacity,
  'capacityUnit': _$VehicleCapacityUnitEnumEnumMap[instance.capacityUnit]!,
  'id': instance.id,
  'name': instance.name,
};

const _$VehicleCapacityUnitEnumEnumMap = {
  VehicleCapacityUnitEnum.KG: 'KG',
  VehicleCapacityUnitEnum.BOX: 'BOX',
  VehicleCapacityUnitEnum.PACKAGE: 'PACKAGE',
  VehicleCapacityUnitEnum.ORDER: 'ORDER',
};
