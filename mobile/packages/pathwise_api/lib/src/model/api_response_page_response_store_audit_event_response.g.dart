// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_page_response_store_audit_event_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePageResponseStoreAuditEventResponse
_$ApiResponsePageResponseStoreAuditEventResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePageResponseStoreAuditEventResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePageResponseStoreAuditEventResponse(
    data: $checkedConvert(
      'data',
      (v) => PageResponseStoreAuditEventResponse.fromJson(
        v as Map<String, dynamic>,
      ),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePageResponseStoreAuditEventResponseToJson(
  ApiResponsePageResponseStoreAuditEventResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
