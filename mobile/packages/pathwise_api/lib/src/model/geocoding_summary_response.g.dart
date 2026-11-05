// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'geocoding_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

GeocodingSummaryResponse _$GeocodingSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('GeocodingSummaryResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['failed', 'pending', 'provided', 'resolved', 'total'],
  );
  final val = GeocodingSummaryResponse(
    failed: $checkedConvert('failed', (v) => (v as num).toInt()),
    pending: $checkedConvert('pending', (v) => (v as num).toInt()),
    provided: $checkedConvert('provided', (v) => (v as num).toInt()),
    resolved: $checkedConvert('resolved', (v) => (v as num).toInt()),
    total: $checkedConvert('total', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$GeocodingSummaryResponseToJson(
  GeocodingSummaryResponse instance,
) => <String, dynamic>{
  'failed': instance.failed,
  'pending': instance.pending,
  'provided': instance.provided,
  'resolved': instance.resolved,
  'total': instance.total,
};
