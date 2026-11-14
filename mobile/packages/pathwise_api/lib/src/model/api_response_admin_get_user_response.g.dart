// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_admin_get_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseAdminGetUserResponse _$ApiResponseAdminGetUserResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseAdminGetUserResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseAdminGetUserResponse(
    data: $checkedConvert(
      'data',
      (v) => AdminGetUserResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseAdminGetUserResponseToJson(
  ApiResponseAdminGetUserResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
