// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_page_response_audit_event_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePageResponseAuditEventResponse
_$ApiResponsePageResponseAuditEventResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePageResponseAuditEventResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePageResponseAuditEventResponse(
    data: $checkedConvert(
      'data',
      (v) => PageResponseAuditEventResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePageResponseAuditEventResponseToJson(
  ApiResponsePageResponseAuditEventResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
