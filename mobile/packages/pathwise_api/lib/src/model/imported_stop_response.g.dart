// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'imported_stop_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ImportedStopResponse _$ImportedStopResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ImportedStopResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'address',
      'demands',
      'label',
      'lat',
      'lon',
      'note',
      'recipientName',
      'recipientPhone',
      'serviceTimeSeconds',
      'timeWindow',
    ],
  );
  final val = ImportedStopResponse(
    address: $checkedConvert('address', (v) => v as String?),
    demands: $checkedConvert('demands', (v) => Map<String, int>.from(v as Map)),
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
          : TimeWindowResponse.fromJson(v as Map<String, dynamic>),
    ),
  );
  return val;
});

Map<String, dynamic> _$ImportedStopResponseToJson(
  ImportedStopResponse instance,
) => <String, dynamic>{
  'address': instance.address,
  'demands': instance.demands,
  'label': instance.label,
  'lat': instance.lat,
  'lon': instance.lon,
  'note': instance.note,
  'recipientName': instance.recipientName,
  'recipientPhone': instance.recipientPhone,
  'serviceTimeSeconds': instance.serviceTimeSeconds,
  'timeWindow': instance.timeWindow?.toJson(),
};
