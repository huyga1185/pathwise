// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_period_stats_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StorePeriodStatsResponse _$StorePeriodStatsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('StorePeriodStatsResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'deliveredRate',
      'plansCompleted',
      'plansCreated',
      'plansPublished',
      'since',
      'stopsDelivered',
      'stopsFailed',
      'stopsSkipped',
    ],
  );
  final val = StorePeriodStatsResponse(
    deliveredRate: $checkedConvert(
      'deliveredRate',
      (v) => (v as num?)?.toDouble(),
    ),
    plansCompleted: $checkedConvert(
      'plansCompleted',
      (v) => (v as num).toInt(),
    ),
    plansCreated: $checkedConvert('plansCreated', (v) => (v as num).toInt()),
    plansPublished: $checkedConvert(
      'plansPublished',
      (v) => (v as num).toInt(),
    ),
    since: $checkedConvert('since', (v) => DateTime.parse(v as String)),
    stopsDelivered: $checkedConvert(
      'stopsDelivered',
      (v) => (v as num).toInt(),
    ),
    stopsFailed: $checkedConvert('stopsFailed', (v) => (v as num).toInt()),
    stopsSkipped: $checkedConvert('stopsSkipped', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$StorePeriodStatsResponseToJson(
  StorePeriodStatsResponse instance,
) => <String, dynamic>{
  'deliveredRate': instance.deliveredRate,
  'plansCompleted': instance.plansCompleted,
  'plansCreated': instance.plansCreated,
  'plansPublished': instance.plansPublished,
  'since': instance.since.toIso8601String(),
  'stopsDelivered': instance.stopsDelivered,
  'stopsFailed': instance.stopsFailed,
  'stopsSkipped': instance.stopsSkipped,
};
