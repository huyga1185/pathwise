// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_member_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListMemberResponse _$ApiResponseListMemberResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListMemberResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListMemberResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map((e) => MemberResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListMemberResponseToJson(
  ApiResponseListMemberResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
