// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'page_response_store_audit_event_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PageResponseStoreAuditEventResponse
_$PageResponseStoreAuditEventResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PageResponseStoreAuditEventResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(
        json,
        requiredKeys: const [
          'items',
          'page',
          'size',
          'totalElements',
          'totalPages',
        ],
      );
      final val = PageResponseStoreAuditEventResponse(
        items: $checkedConvert(
          'items',
          (v) => (v as List<dynamic>)
              .map(
                (e) =>
                    StoreAuditEventResponse.fromJson(e as Map<String, dynamic>),
              )
              .toList(),
        ),
        page: $checkedConvert('page', (v) => (v as num).toInt()),
        size: $checkedConvert('size', (v) => (v as num).toInt()),
        totalElements: $checkedConvert(
          'totalElements',
          (v) => (v as num).toInt(),
        ),
        totalPages: $checkedConvert('totalPages', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$PageResponseStoreAuditEventResponseToJson(
  PageResponseStoreAuditEventResponse instance,
) => <String, dynamic>{
  'items': instance.items.map((e) => e.toJson()).toList(),
  'page': instance.page,
  'size': instance.size,
  'totalElements': instance.totalElements,
  'totalPages': instance.totalPages,
};
