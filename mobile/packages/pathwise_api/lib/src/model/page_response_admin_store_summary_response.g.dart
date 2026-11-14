// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'page_response_admin_store_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PageResponseAdminStoreSummaryResponse
_$PageResponseAdminStoreSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('PageResponseAdminStoreSummaryResponse', json, (
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
  final val = PageResponseAdminStoreSummaryResponse(
    items: $checkedConvert(
      'items',
      (v) => (v as List<dynamic>)
          .map(
            (e) =>
                AdminStoreSummaryResponse.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
    ),
    page: $checkedConvert('page', (v) => (v as num).toInt()),
    size: $checkedConvert('size', (v) => (v as num).toInt()),
    totalElements: $checkedConvert('totalElements', (v) => (v as num).toInt()),
    totalPages: $checkedConvert('totalPages', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$PageResponseAdminStoreSummaryResponseToJson(
  PageResponseAdminStoreSummaryResponse instance,
) => <String, dynamic>{
  'items': instance.items.map((e) => e.toJson()).toList(),
  'page': instance.page,
  'size': instance.size,
  'totalElements': instance.totalElements,
  'totalPages': instance.totalPages,
};
