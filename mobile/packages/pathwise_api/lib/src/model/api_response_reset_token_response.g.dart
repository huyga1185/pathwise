// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_reset_token_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseResetTokenResponse _$ApiResponseResetTokenResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseResetTokenResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseResetTokenResponse(
    data: $checkedConvert(
      'data',
      (v) => ResetTokenResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseResetTokenResponseToJson(
  ApiResponseResetTokenResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
