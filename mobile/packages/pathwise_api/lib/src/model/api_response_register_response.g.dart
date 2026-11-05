// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_register_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseRegisterResponse _$ApiResponseRegisterResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseRegisterResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseRegisterResponse(
    data: $checkedConvert(
      'data',
      (v) => RegisterResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseRegisterResponseToJson(
  ApiResponseRegisterResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
