// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_vehicle_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListVehicleResponse _$ApiResponseListVehicleResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListVehicleResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListVehicleResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map((e) => VehicleResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListVehicleResponseToJson(
  ApiResponseListVehicleResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
