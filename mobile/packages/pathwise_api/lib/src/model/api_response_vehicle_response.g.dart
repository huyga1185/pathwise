// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_vehicle_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseVehicleResponse _$ApiResponseVehicleResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseVehicleResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseVehicleResponse(
    data: $checkedConvert(
      'data',
      (v) => VehicleResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseVehicleResponseToJson(
  ApiResponseVehicleResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
