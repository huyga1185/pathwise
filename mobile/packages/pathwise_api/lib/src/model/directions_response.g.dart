// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'directions_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DirectionsResponse _$DirectionsResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DirectionsResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'destination',
          'distanceMeters',
          'durationSeconds',
          'polyline',
          'steps',
        ],
      );
      final val = DirectionsResponse(
        destination: $checkedConvert(
          'destination',
          (v) => DirectionsDestination.fromJson(v as Map<String, dynamic>),
        ),
        distanceMeters: $checkedConvert(
          'distanceMeters',
          (v) => (v as num).toInt(),
        ),
        durationSeconds: $checkedConvert(
          'durationSeconds',
          (v) => (v as num).toInt(),
        ),
        polyline: $checkedConvert('polyline', (v) => v as String),
        steps: $checkedConvert(
          'steps',
          (v) => (v as List<dynamic>)
              .map(
                (e) =>
                    DirectionsInstruction.fromJson(e as Map<String, dynamic>),
              )
              .toList(),
        ),
      );
      return val;
    });

Map<String, dynamic> _$DirectionsResponseToJson(DirectionsResponse instance) =>
    <String, dynamic>{
      'destination': instance.destination.toJson(),
      'distanceMeters': instance.distanceMeters,
      'durationSeconds': instance.durationSeconds,
      'polyline': instance.polyline,
      'steps': instance.steps.map((e) => e.toJson()).toList(),
    };
