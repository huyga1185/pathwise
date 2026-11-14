// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_day.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReportDay _$ReportDayFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ReportDay', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'date',
          'delivered',
          'failed',
          'pending',
          'plans',
          'skipped',
        ],
      );
      final val = ReportDay(
        date: $checkedConvert('date', (v) => v as String),
        delivered: $checkedConvert('delivered', (v) => (v as num).toInt()),
        failed: $checkedConvert('failed', (v) => (v as num).toInt()),
        pending: $checkedConvert('pending', (v) => (v as num).toInt()),
        plans: $checkedConvert('plans', (v) => (v as num).toInt()),
        skipped: $checkedConvert('skipped', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$ReportDayToJson(ReportDay instance) => <String, dynamic>{
  'date': instance.date,
  'delivered': instance.delivered,
  'failed': instance.failed,
  'pending': instance.pending,
  'plans': instance.plans,
  'skipped': instance.skipped,
};
