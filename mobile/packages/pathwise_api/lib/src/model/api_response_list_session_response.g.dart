// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_session_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListSessionResponse _$ApiResponseListSessionResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListSessionResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListSessionResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map((e) => SessionResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListSessionResponseToJson(
  ApiResponseListSessionResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
