// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'jvm.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Jvm _$JvmFromJson(Map<String, dynamic> json) => $checkedCreate('Jvm', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['heapMaxBytes', 'heapUsedBytes']);
  final val = Jvm(
    heapMaxBytes: $checkedConvert('heapMaxBytes', (v) => (v as num?)?.toInt()),
    heapUsedBytes: $checkedConvert('heapUsedBytes', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$JvmToJson(Jvm instance) => <String, dynamic>{
  'heapMaxBytes': instance.heapMaxBytes,
  'heapUsedBytes': instance.heapUsedBytes,
};
