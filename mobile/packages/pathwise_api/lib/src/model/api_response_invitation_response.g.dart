// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_invitation_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseInvitationResponse _$ApiResponseInvitationResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseInvitationResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseInvitationResponse(
    data: $checkedConvert(
      'data',
      (v) => InvitationResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseInvitationResponseToJson(
  ApiResponseInvitationResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
