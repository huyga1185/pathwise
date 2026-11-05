// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_driver_route_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListDriverRouteSummaryResponse
_$ApiResponseListDriverRouteSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListDriverRouteSummaryResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListDriverRouteSummaryResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map(
            (e) =>
                DriverRouteSummaryResponse.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListDriverRouteSummaryResponseToJson(
  ApiResponseListDriverRouteSummaryResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
