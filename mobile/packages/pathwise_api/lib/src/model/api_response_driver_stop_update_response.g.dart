// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_driver_stop_update_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseDriverStopUpdateResponse
_$ApiResponseDriverStopUpdateResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseDriverStopUpdateResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseDriverStopUpdateResponse(
        data: $checkedConvert(
          'data',
          (v) => DriverStopUpdateResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseDriverStopUpdateResponseToJson(
  ApiResponseDriverStopUpdateResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
