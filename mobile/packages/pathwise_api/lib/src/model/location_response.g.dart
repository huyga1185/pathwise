// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'location_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LocationResponse _$LocationResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('LocationResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['address', 'lat', 'lon']);
      final val = LocationResponse(
        address: $checkedConvert('address', (v) => v as String?),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$LocationResponseToJson(LocationResponse instance) =>
    <String, dynamic>{
      'address': instance.address,
      'lat': instance.lat,
      'lon': instance.lon,
    };
