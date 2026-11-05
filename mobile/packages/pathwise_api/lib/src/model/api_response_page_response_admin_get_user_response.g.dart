// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_page_response_admin_get_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePageResponseAdminGetUserResponse
_$ApiResponsePageResponseAdminGetUserResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePageResponseAdminGetUserResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePageResponseAdminGetUserResponse(
    data: $checkedConvert(
      'data',
      (v) =>
          PageResponseAdminGetUserResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePageResponseAdminGetUserResponseToJson(
  ApiResponsePageResponseAdminGetUserResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
