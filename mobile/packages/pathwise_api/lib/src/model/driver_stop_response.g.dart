// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_stop_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverStopResponse _$DriverStopResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DriverStopResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'address',
          'failureReason',
          'id',
          'label',
          'lat',
          'lon',
          'note',
          'plannedArrival',
          'recipientName',
          'recipientPhone',
          'sequence',
          'status',
          'timeWindow',
        ],
      );
      final val = DriverStopResponse(
        address: $checkedConvert('address', (v) => v as String?),
        failureReason: $checkedConvert('failureReason', (v) => v as String?),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        label: $checkedConvert('label', (v) => v as String),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
        note: $checkedConvert('note', (v) => v as String?),
        plannedArrival: $checkedConvert(
          'plannedArrival',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        recipientName: $checkedConvert('recipientName', (v) => v as String?),
        recipientPhone: $checkedConvert('recipientPhone', (v) => v as String?),
        sequence: $checkedConvert('sequence', (v) => (v as num).toInt()),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$DriverStopResponseStatusEnumEnumMap, v),
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

Map<String, dynamic> _$DriverStopResponseToJson(DriverStopResponse instance) =>
    <String, dynamic>{
      'address': instance.address,
      'failureReason': instance.failureReason,
      'id': instance.id,
      'label': instance.label,
      'lat': instance.lat,
      'lon': instance.lon,
      'note': instance.note,
      'plannedArrival': instance.plannedArrival?.toIso8601String(),
      'recipientName': instance.recipientName,
      'recipientPhone': instance.recipientPhone,
      'sequence': instance.sequence,
      'status': _$DriverStopResponseStatusEnumEnumMap[instance.status]!,
      'timeWindow': instance.timeWindow?.toJson(),
    };

const _$DriverStopResponseStatusEnumEnumMap = {
  DriverStopResponseStatusEnum.PENDING: 'PENDING',
  DriverStopResponseStatusEnum.DELIVERED: 'DELIVERED',
  DriverStopResponseStatusEnum.FAILED: 'FAILED',
  DriverStopResponseStatusEnum.SKIPPED: 'SKIPPED',
};
