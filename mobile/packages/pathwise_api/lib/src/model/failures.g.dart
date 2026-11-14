// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'failures.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Failures _$FailuresFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Failures', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['email', 'geocoding', 'job', 'routing'],
      );
      final val = Failures(
        email: $checkedConvert('email', (v) => (v as num).toInt()),
        geocoding: $checkedConvert('geocoding', (v) => (v as num).toInt()),
        job: $checkedConvert('job', (v) => (v as num).toInt()),
        routing: $checkedConvert('routing', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$FailuresToJson(Failures instance) => <String, dynamic>{
  'email': instance.email,
  'geocoding': instance.geocoding,
  'job': instance.job,
  'routing': instance.routing,
};
