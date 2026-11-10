// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_driver_location_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListDriverLocationResponse
_$ApiResponseListDriverLocationResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseListDriverLocationResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseListDriverLocationResponse(
        data: $checkedConvert(
          'data',
          (v) => (v as List<dynamic>)
              .map(
                (e) =>
                    DriverLocationResponse.fromJson(e as Map<String, dynamic>),
              )
              .toList(),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseListDriverLocationResponseToJson(
  ApiResponseListDriverLocationResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
