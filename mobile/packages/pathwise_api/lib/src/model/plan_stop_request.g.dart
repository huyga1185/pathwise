// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_stop_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanStopRequest _$PlanStopRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanStopRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['label']);
      final val = PlanStopRequest(
        address: $checkedConvert('address', (v) => v as String?),
        demands: $checkedConvert(
          'demands',
          (v) => (v as Map<String, dynamic>?)?.map(
            (k, e) => MapEntry(k, (e as num).toInt()),
          ),
        ),
        id: $checkedConvert('id', (v) => (v as num?)?.toInt()),
        label: $checkedConvert('label', (v) => v as String),
        lat: $checkedConvert('lat', (v) => (v as num?)?.toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num?)?.toDouble()),
        note: $checkedConvert('note', (v) => v as String?),
        recipientName: $checkedConvert('recipientName', (v) => v as String?),
        recipientPhone: $checkedConvert('recipientPhone', (v) => v as String?),
        serviceTimeSeconds: $checkedConvert(
          'serviceTimeSeconds',
          (v) => (v as num?)?.toInt(),
        ),
        timeWindow: $checkedConvert(
          'timeWindow',
          (v) => v == null
              ? null
              : TimeWindowRequest.fromJson(v as Map<String, dynamic>),
        ),
      );
      return val;
    });

Map<String, dynamic> _$PlanStopRequestToJson(PlanStopRequest instance) =>
    <String, dynamic>{
      'address': ?instance.address,
      'demands': ?instance.demands,
      'id': ?instance.id,
      'label': instance.label,
      'lat': ?instance.lat,
      'lon': ?instance.lon,
      'note': ?instance.note,
      'recipientName': ?instance.recipientName,
      'recipientPhone': ?instance.recipientPhone,
      'serviceTimeSeconds': ?instance.serviceTimeSeconds,
      'timeWindow': ?instance.timeWindow?.toJson(),
    };
