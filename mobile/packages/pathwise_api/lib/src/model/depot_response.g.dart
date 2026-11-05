// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'depot_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DepotResponse _$DepotResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DepotResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['address', 'lat', 'lon']);
      final val = DepotResponse(
        address: $checkedConvert('address', (v) => v as String?),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$DepotResponseToJson(DepotResponse instance) =>
    <String, dynamic>{
      'address': instance.address,
      'lat': instance.lat,
      'lon': instance.lon,
    };
