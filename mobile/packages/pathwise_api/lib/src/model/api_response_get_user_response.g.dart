// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_get_user_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseGetUserResponse _$ApiResponseGetUserResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseGetUserResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseGetUserResponse(
    data: $checkedConvert(
      'data',
      (v) => GetUserResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseGetUserResponseToJson(
  ApiResponseGetUserResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
