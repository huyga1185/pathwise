// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'location_batch_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LocationBatchRequest _$LocationBatchRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('LocationBatchRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['points']);
  final val = LocationBatchRequest(
    points: $checkedConvert(
      'points',
      (v) => (v as List<dynamic>)
          .map((e) => LocationPointRequest.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
  );
  return val;
});

Map<String, dynamic> _$LocationBatchRequestToJson(
  LocationBatchRequest instance,
) => <String, dynamic>{
  'points': instance.points.map((e) => e.toJson()).toList(),
};
