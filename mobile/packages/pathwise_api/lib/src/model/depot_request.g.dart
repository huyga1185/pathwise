// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'depot_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DepotRequest _$DepotRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DepotRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['lat', 'lon']);
      final val = DepotRequest(
        address: $checkedConvert('address', (v) => v as String?),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$DepotRequestToJson(DepotRequest instance) =>
    <String, dynamic>{
      'address': ?instance.address,
      'lat': instance.lat,
      'lon': instance.lon,
    };
