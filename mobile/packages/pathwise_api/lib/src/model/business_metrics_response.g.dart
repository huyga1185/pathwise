// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'business_metrics_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

BusinessMetricsResponse _$BusinessMetricsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('BusinessMetricsResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'computedAt',
      'days',
      'from',
      'now',
      'timezone',
      'to',
      'totals',
    ],
  );
  final val = BusinessMetricsResponse(
    computedAt: $checkedConvert(
      'computedAt',
      (v) => DateTime.parse(v as String),
    ),
    days: $checkedConvert(
      'days',
      (v) => (v as List<dynamic>)
          .map((e) => Day.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    from: $checkedConvert('from', (v) => v as String),
    now: $checkedConvert('now', (v) => Now.fromJson(v as Map<String, dynamic>)),
    timezone: $checkedConvert('timezone', (v) => v as String),
    to: $checkedConvert('to', (v) => v as String),
    totals: $checkedConvert(
      'totals',
      (v) => Totals.fromJson(v as Map<String, dynamic>),
    ),
  );
  return val;
});

Map<String, dynamic> _$BusinessMetricsResponseToJson(
  BusinessMetricsResponse instance,
) => <String, dynamic>{
  'computedAt': instance.computedAt.toIso8601String(),
  'days': instance.days.map((e) => e.toJson()).toList(),
  'from': instance.from,
  'now': instance.now.toJson(),
  'timezone': instance.timezone,
  'to': instance.to,
  'totals': instance.totals.toJson(),
};
