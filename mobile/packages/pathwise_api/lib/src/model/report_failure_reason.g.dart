// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_failure_reason.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReportFailureReason _$ReportFailureReasonFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ReportFailureReason', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['count', 'reason']);
      final val = ReportFailureReason(
        count: $checkedConvert('count', (v) => (v as num).toInt()),
        reason: $checkedConvert('reason', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$ReportFailureReasonToJson(
  ReportFailureReason instance,
) => <String, dynamic>{'count': instance.count, 'reason': instance.reason};
