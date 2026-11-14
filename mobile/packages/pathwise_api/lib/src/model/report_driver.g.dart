// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_driver.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReportDriver _$ReportDriverFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ReportDriver', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'delivered',
          'driverId',
          'failed',
          'firstResultAt',
          'fullName',
          'lastResultAt',
          'pending',
          'plannedDistanceMeters',
          'routes',
          'skipped',
        ],
      );
      final val = ReportDriver(
        delivered: $checkedConvert('delivered', (v) => (v as num).toInt()),
        driverId: $checkedConvert('driverId', (v) => (v as num).toInt()),
        failed: $checkedConvert('failed', (v) => (v as num).toInt()),
        firstResultAt: $checkedConvert(
          'firstResultAt',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        fullName: $checkedConvert('fullName', (v) => v as String),
        lastResultAt: $checkedConvert(
          'lastResultAt',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        pending: $checkedConvert('pending', (v) => (v as num).toInt()),
        plannedDistanceMeters: $checkedConvert(
          'plannedDistanceMeters',
          (v) => (v as num).toInt(),
        ),
        routes: $checkedConvert('routes', (v) => (v as num).toInt()),
        skipped: $checkedConvert('skipped', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$ReportDriverToJson(ReportDriver instance) =>
    <String, dynamic>{
      'delivered': instance.delivered,
      'driverId': instance.driverId,
      'failed': instance.failed,
      'firstResultAt': instance.firstResultAt?.toIso8601String(),
      'fullName': instance.fullName,
      'lastResultAt': instance.lastResultAt?.toIso8601String(),
      'pending': instance.pending,
      'plannedDistanceMeters': instance.plannedDistanceMeters,
      'routes': instance.routes,
      'skipped': instance.skipped,
    };
