// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_directions_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseDirectionsResponse _$ApiResponseDirectionsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseDirectionsResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseDirectionsResponse(
    data: $checkedConvert(
      'data',
      (v) => DirectionsResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseDirectionsResponseToJson(
  ApiResponseDirectionsResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
