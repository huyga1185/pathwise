// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_admin_store_state_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseAdminStoreStateResponse _$ApiResponseAdminStoreStateResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseAdminStoreStateResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseAdminStoreStateResponse(
    data: $checkedConvert(
      'data',
      (v) => AdminStoreStateResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseAdminStoreStateResponseToJson(
  ApiResponseAdminStoreStateResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
