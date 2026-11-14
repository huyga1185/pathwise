// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_stop_counts.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReportStopCounts _$ReportStopCountsFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ReportStopCounts', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'delivered',
          'deliveredRate',
          'failed',
          'pending',
          'skipped',
          'total',
        ],
      );
      final val = ReportStopCounts(
        delivered: $checkedConvert('delivered', (v) => (v as num).toInt()),
        deliveredRate: $checkedConvert(
          'deliveredRate',
          (v) => (v as num?)?.toDouble(),
        ),
        failed: $checkedConvert('failed', (v) => (v as num).toInt()),
        pending: $checkedConvert('pending', (v) => (v as num).toInt()),
        skipped: $checkedConvert('skipped', (v) => (v as num).toInt()),
        total: $checkedConvert('total', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$ReportStopCountsToJson(ReportStopCounts instance) =>
    <String, dynamic>{
      'delivered': instance.delivered,
      'deliveredRate': instance.deliveredRate,
      'failed': instance.failed,
      'pending': instance.pending,
      'skipped': instance.skipped,
      'total': instance.total,
    };
