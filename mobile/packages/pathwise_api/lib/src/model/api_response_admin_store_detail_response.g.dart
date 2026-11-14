// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_admin_store_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseAdminStoreDetailResponse
_$ApiResponseAdminStoreDetailResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseAdminStoreDetailResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseAdminStoreDetailResponse(
        data: $checkedConvert(
          'data',
          (v) => AdminStoreDetailResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseAdminStoreDetailResponseToJson(
  ApiResponseAdminStoreDetailResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
