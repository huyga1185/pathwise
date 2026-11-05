// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_driver_route_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseDriverRouteDetailResponse
_$ApiResponseDriverRouteDetailResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseDriverRouteDetailResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseDriverRouteDetailResponse(
        data: $checkedConvert(
          'data',
          (v) => DriverRouteDetailResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseDriverRouteDetailResponseToJson(
  ApiResponseDriverRouteDetailResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
