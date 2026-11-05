// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'vehicle_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

VehicleResponse _$VehicleResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('VehicleResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'active',
          'capacity',
          'capacityUnit',
          'id',
          'name',
          'plateNumber',
        ],
      );
      final val = VehicleResponse(
        active: $checkedConvert('active', (v) => v as bool),
        capacity: $checkedConvert('capacity', (v) => (v as num).toInt()),
        capacityUnit: $checkedConvert(
          'capacityUnit',
          (v) => $enumDecode(_$VehicleResponseCapacityUnitEnumEnumMap, v),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        name: $checkedConvert('name', (v) => v as String),
        plateNumber: $checkedConvert('plateNumber', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$VehicleResponseToJson(VehicleResponse instance) =>
    <String, dynamic>{
      'active': instance.active,
      'capacity': instance.capacity,
      'capacityUnit':
          _$VehicleResponseCapacityUnitEnumEnumMap[instance.capacityUnit]!,
      'id': instance.id,
      'name': instance.name,
      'plateNumber': instance.plateNumber,
    };

const _$VehicleResponseCapacityUnitEnumEnumMap = {
  VehicleResponseCapacityUnitEnum.KG: 'KG',
  VehicleResponseCapacityUnitEnum.BOX: 'BOX',
  VehicleResponseCapacityUnitEnum.PACKAGE: 'PACKAGE',
  VehicleResponseCapacityUnitEnum.ORDER: 'ORDER',
};
