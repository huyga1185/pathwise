// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_plan_counts.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReportPlanCounts _$ReportPlanCountsFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ReportPlanCounts', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'cancelled',
          'completed',
          'draft',
          'inProgress',
          'published',
          'total',
        ],
      );
      final val = ReportPlanCounts(
        cancelled: $checkedConvert('cancelled', (v) => (v as num).toInt()),
        completed: $checkedConvert('completed', (v) => (v as num).toInt()),
        draft: $checkedConvert('draft', (v) => (v as num).toInt()),
        inProgress: $checkedConvert('inProgress', (v) => (v as num).toInt()),
        published: $checkedConvert('published', (v) => (v as num).toInt()),
        total: $checkedConvert('total', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$ReportPlanCountsToJson(ReportPlanCounts instance) =>
    <String, dynamic>{
      'cancelled': instance.cancelled,
      'completed': instance.completed,
      'draft': instance.draft,
      'inProgress': instance.inProgress,
      'published': instance.published,
      'total': instance.total,
    };
