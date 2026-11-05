// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_invitation_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListInvitationResponse _$ApiResponseListInvitationResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListInvitationResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListInvitationResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map((e) => InvitationResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListInvitationResponseToJson(
  ApiResponseListInvitationResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
