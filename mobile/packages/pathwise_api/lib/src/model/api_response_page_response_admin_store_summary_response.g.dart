// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_page_response_admin_store_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePageResponseAdminStoreSummaryResponse
_$ApiResponsePageResponseAdminStoreSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePageResponseAdminStoreSummaryResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePageResponseAdminStoreSummaryResponse(
    data: $checkedConvert(
      'data',
      (v) => PageResponseAdminStoreSummaryResponse.fromJson(
        v as Map<String, dynamic>,
      ),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePageResponseAdminStoreSummaryResponseToJson(
  ApiResponsePageResponseAdminStoreSummaryResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
