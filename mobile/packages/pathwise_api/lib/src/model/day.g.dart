// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'day.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Day _$DayFromJson(Map<String, dynamic> json) => $checkedCreate('Day', json, (
  $checkedConvert,
) {
  $checkKeys(
    json,
    requiredKeys: const [
      'date',
      'newStores',
      'optimizations',
      'optimizationsFailed',
      'plansCompleted',
      'plansCreated',
      'plansPublished',
      'stopsDelivered',
      'stopsFailed',
      'stopsSkipped',
    ],
  );
  final val = Day(
    date: $checkedConvert('date', (v) => v as String),
    newStores: $checkedConvert('newStores', (v) => (v as num).toInt()),
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
    stopsDelivered: $checkedConvert(
      'stopsDelivered',
      (v) => (v as num).toInt(),
    ),
    stopsFailed: $checkedConvert('stopsFailed', (v) => (v as num).toInt()),
    stopsSkipped: $checkedConvert('stopsSkipped', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$DayToJson(Day instance) => <String, dynamic>{
  'date': instance.date,
  'newStores': instance.newStores,
  'optimizations': instance.optimizations,
  'optimizationsFailed': instance.optimizationsFailed,
  'plansCompleted': instance.plansCompleted,
  'plansCreated': instance.plansCreated,
  'plansPublished': instance.plansPublished,
  'stopsDelivered': instance.stopsDelivered,
  'stopsFailed': instance.stopsFailed,
  'stopsSkipped': instance.stopsSkipped,
};
