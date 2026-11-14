// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'group.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Group _$GroupFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Group', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['group', 'p95Millis', 'requestsLastHour'],
      );
      final val = Group(
        group: $checkedConvert('group', (v) => v as String),
        p95Millis: $checkedConvert('p95Millis', (v) => (v as num?)?.toDouble()),
        requestsLastHour: $checkedConvert(
          'requestsLastHour',
          (v) => (v as num).toInt(),
        ),
      );
      return val;
    });

Map<String, dynamic> _$GroupToJson(Group instance) => <String, dynamic>{
  'group': instance.group,
  'p95Millis': instance.p95Millis,
  'requestsLastHour': instance.requestsLastHour,
};
