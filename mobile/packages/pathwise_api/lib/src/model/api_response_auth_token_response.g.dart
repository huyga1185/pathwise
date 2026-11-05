// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_auth_token_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseAuthTokenResponse _$ApiResponseAuthTokenResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseAuthTokenResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseAuthTokenResponse(
    data: $checkedConvert(
      'data',
      (v) => AuthTokenResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseAuthTokenResponseToJson(
  ApiResponseAuthTokenResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
