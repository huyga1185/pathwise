// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'patch_stop_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PatchStopRequest _$PatchStopRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PatchStopRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['version']);
      final val = PatchStopRequest(
        address: $checkedConvert('address', (v) => v as String?),
        demands: $checkedConvert(
          'demands',
          (v) => (v as Map<String, dynamic>?)?.map(
            (k, e) => MapEntry(k, (e as num).toInt()),
          ),
        ),
        label: $checkedConvert('label', (v) => v as String?),
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
        version: $checkedConvert('version', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$PatchStopRequestToJson(PatchStopRequest instance) =>
    <String, dynamic>{
      'address': ?instance.address,
      'demands': ?instance.demands,
      'label': ?instance.label,
      'lat': ?instance.lat,
      'lon': ?instance.lon,
      'note': ?instance.note,
      'recipientName': ?instance.recipientName,
      'recipientPhone': ?instance.recipientPhone,
      'serviceTimeSeconds': ?instance.serviceTimeSeconds,
      'timeWindow': ?instance.timeWindow?.toJson(),
      'version': instance.version,
    };
