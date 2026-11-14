// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'technical_metrics_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TechnicalMetricsResponse _$TechnicalMetricsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('TechnicalMetricsResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'database',
      'failuresLast24h',
      'http',
      'jobs',
      'jvm',
      'measuredAt',
      'rateLimitedLast24h',
      'startedAt',
      'uptimeSeconds',
    ],
  );
  final val = TechnicalMetricsResponse(
    database: $checkedConvert(
      'database',
      (v) => Database.fromJson(v as Map<String, dynamic>),
    ),
    failuresLast24h: $checkedConvert(
      'failuresLast24h',
      (v) => Failures.fromJson(v as Map<String, dynamic>),
    ),
    http: $checkedConvert(
      'http',
      (v) => Http.fromJson(v as Map<String, dynamic>),
    ),
    jobs: $checkedConvert(
      'jobs',
      (v) => Jobs.fromJson(v as Map<String, dynamic>),
    ),
    jvm: $checkedConvert('jvm', (v) => Jvm.fromJson(v as Map<String, dynamic>)),
    measuredAt: $checkedConvert(
      'measuredAt',
      (v) => DateTime.parse(v as String),
    ),
    rateLimitedLast24h: $checkedConvert(
      'rateLimitedLast24h',
      (v) => (v as num).toInt(),
    ),
    startedAt: $checkedConvert('startedAt', (v) => DateTime.parse(v as String)),
    uptimeSeconds: $checkedConvert('uptimeSeconds', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$TechnicalMetricsResponseToJson(
  TechnicalMetricsResponse instance,
) => <String, dynamic>{
  'database': instance.database.toJson(),
  'failuresLast24h': instance.failuresLast24h.toJson(),
  'http': instance.http.toJson(),
  'jobs': instance.jobs.toJson(),
  'jvm': instance.jvm.toJson(),
  'measuredAt': instance.measuredAt.toIso8601String(),
  'rateLimitedLast24h': instance.rateLimitedLast24h,
  'startedAt': instance.startedAt.toIso8601String(),
  'uptimeSeconds': instance.uptimeSeconds,
};
