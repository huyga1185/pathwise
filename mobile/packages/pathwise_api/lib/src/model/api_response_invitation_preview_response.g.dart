// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_invitation_preview_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseInvitationPreviewResponse
_$ApiResponseInvitationPreviewResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseInvitationPreviewResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseInvitationPreviewResponse(
        data: $checkedConvert(
          'data',
          (v) => InvitationPreviewResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseInvitationPreviewResponseToJson(
  ApiResponseInvitationPreviewResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
