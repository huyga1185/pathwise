// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'place_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlaceResponse _$PlaceResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlaceResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['displayName', 'lat', 'lon']);
      final val = PlaceResponse(
        displayName: $checkedConvert('displayName', (v) => v as String),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$PlaceResponseToJson(PlaceResponse instance) =>
    <String, dynamic>{
      'displayName': instance.displayName,
      'lat': instance.lat,
      'lon': instance.lon,
    };
