// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_report_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StoreReportResponse _$StoreReportResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('StoreReportResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'days',
          'drivers',
          'failureReasons',
          'from',
          'plannedDistanceMeters',
          'plans',
          'stops',
          'to',
        ],
      );
      final val = StoreReportResponse(
        days: $checkedConvert(
          'days',
          (v) => (v as List<dynamic>)
              .map((e) => ReportDay.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        drivers: $checkedConvert(
          'drivers',
          (v) => (v as List<dynamic>)
              .map((e) => ReportDriver.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        failureReasons: $checkedConvert(
          'failureReasons',
          (v) => (v as List<dynamic>)
              .map(
                (e) => ReportFailureReason.fromJson(e as Map<String, dynamic>),
              )
              .toList(),
        ),
        from: $checkedConvert('from', (v) => v as String),
        plannedDistanceMeters: $checkedConvert(
          'plannedDistanceMeters',
          (v) => (v as num).toInt(),
        ),
        plans: $checkedConvert(
          'plans',
          (v) => ReportPlanCounts.fromJson(v as Map<String, dynamic>),
        ),
        stops: $checkedConvert(
          'stops',
          (v) => ReportStopCounts.fromJson(v as Map<String, dynamic>),
        ),
        to: $checkedConvert('to', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$StoreReportResponseToJson(
  StoreReportResponse instance,
) => <String, dynamic>{
  'days': instance.days.map((e) => e.toJson()).toList(),
  'drivers': instance.drivers.map((e) => e.toJson()).toList(),
  'failureReasons': instance.failureReasons.map((e) => e.toJson()).toList(),
  'from': instance.from,
  'plannedDistanceMeters': instance.plannedDistanceMeters,
  'plans': instance.plans.toJson(),
  'stops': instance.stops.toJson(),
  'to': instance.to,
};
