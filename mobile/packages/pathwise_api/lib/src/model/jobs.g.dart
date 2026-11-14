// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'jobs.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Jobs _$JobsFromJson(Map<String, dynamic> json) => $checkedCreate('Jobs', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['failedLast24h', 'unfinished']);
  final val = Jobs(
    failedLast24h: $checkedConvert('failedLast24h', (v) => (v as num).toInt()),
    unfinished: $checkedConvert('unfinished', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$JobsToJson(Jobs instance) => <String, dynamic>{
  'failedLast24h': instance.failedLast24h,
  'unfinished': instance.unfinished,
};
