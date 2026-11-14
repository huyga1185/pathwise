// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'totals.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Totals _$TotalsFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('Totals', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'deliveredRate',
      'failedRate',
      'newStores',
      'optimizationFailureRate',
      'optimizations',
      'optimizationsFailed',
      'plansCompleted',
      'plansCreated',
      'plansPublished',
      'skippedRate',
      'stopsDelivered',
      'stopsFailed',
      'stopsSkipped',
    ],
  );
  final val = Totals(
    deliveredRate: $checkedConvert(
      'deliveredRate',
      (v) => (v as num?)?.toDouble(),
    ),
    failedRate: $checkedConvert('failedRate', (v) => (v as num?)?.toDouble()),
    newStores: $checkedConvert('newStores', (v) => (v as num).toInt()),
    optimizationFailureRate: $checkedConvert(
      'optimizationFailureRate',
      (v) => (v as num?)?.toDouble(),
    ),
    optimizations: $checkedConvert('optimizations', (v) => (v as num).toInt()),
    optimizationsFailed: $checkedConvert(
      'optimizationsFailed',
      (v) => (v as num).toInt(),
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
    skippedRate: $checkedConvert('skippedRate', (v) => (v as num?)?.toDouble()),
    stopsDelivered: $checkedConvert(
      'stopsDelivered',
      (v) => (v as num).toInt(),
    ),
    stopsFailed: $checkedConvert('stopsFailed', (v) => (v as num).toInt()),
    stopsSkipped: $checkedConvert('stopsSkipped', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$TotalsToJson(Totals instance) => <String, dynamic>{
  'deliveredRate': instance.deliveredRate,
  'failedRate': instance.failedRate,
  'newStores': instance.newStores,
  'optimizationFailureRate': instance.optimizationFailureRate,
  'optimizations': instance.optimizations,
  'optimizationsFailed': instance.optimizationsFailed,
  'plansCompleted': instance.plansCompleted,
  'plansCreated': instance.plansCreated,
  'plansPublished': instance.plansPublished,
  'skippedRate': instance.skippedRate,
  'stopsDelivered': instance.stopsDelivered,
  'stopsFailed': instance.stopsFailed,
  'stopsSkipped': instance.stopsSkipped,
};
