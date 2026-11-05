// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_member_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseMemberResponse _$ApiResponseMemberResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseMemberResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseMemberResponse(
    data: $checkedConvert(
      'data',
      (v) => MemberResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseMemberResponseToJson(
  ApiResponseMemberResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
