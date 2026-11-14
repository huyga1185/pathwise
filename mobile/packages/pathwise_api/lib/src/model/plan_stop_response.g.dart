// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_stop_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanStopResponse _$PlanStopResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanStopResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'address',
          'demands',
          'dropReason',
          'failureReason',
          'geocodeError',
          'geocodeStatus',
          'geocodedDisplayName',
          'id',
          'label',
          'lat',
          'lon',
          'note',
          'plannedArrival',
          'recipientName',
          'recipientPhone',
          'sequence',
          'serviceTimeSeconds',
          'status',
          'timeWindow',
        ],
      );
      final val = PlanStopResponse(
        address: $checkedConvert('address', (v) => v as String?),
        demands: $checkedConvert(
          'demands',
          (v) => Map<String, int>.from(v as Map),
        ),
        dropReason: $checkedConvert(
          'dropReason',
          (v) =>
              $enumDecodeNullable(_$PlanStopResponseDropReasonEnumEnumMap, v),
        ),
        failureReason: $checkedConvert('failureReason', (v) => v as String?),
        geocodeError: $checkedConvert(
          'geocodeError',
          (v) =>
              $enumDecodeNullable(_$PlanStopResponseGeocodeErrorEnumEnumMap, v),
        ),
        geocodeStatus: $checkedConvert(
          'geocodeStatus',
          (v) => $enumDecode(_$PlanStopResponseGeocodeStatusEnumEnumMap, v),
        ),
        geocodedDisplayName: $checkedConvert(
          'geocodedDisplayName',
          (v) => v as String?,
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        label: $checkedConvert('label', (v) => v as String),
        lat: $checkedConvert('lat', (v) => (v as num?)?.toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num?)?.toDouble()),
        note: $checkedConvert('note', (v) => v as String?),
        plannedArrival: $checkedConvert(
          'plannedArrival',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        recipientName: $checkedConvert('recipientName', (v) => v as String?),
        recipientPhone: $checkedConvert('recipientPhone', (v) => v as String?),
        sequence: $checkedConvert('sequence', (v) => (v as num?)?.toInt()),
        serviceTimeSeconds: $checkedConvert(
          'serviceTimeSeconds',
          (v) => (v as num).toInt(),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$PlanStopResponseStatusEnumEnumMap, v),
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

Map<String, dynamic> _$PlanStopResponseToJson(
  PlanStopResponse instance,
) => <String, dynamic>{
  'address': instance.address,
  'demands': instance.demands,
  'dropReason': _$PlanStopResponseDropReasonEnumEnumMap[instance.dropReason],
  'failureReason': instance.failureReason,
  'geocodeError':
      _$PlanStopResponseGeocodeErrorEnumEnumMap[instance.geocodeError],
  'geocodeStatus':
      _$PlanStopResponseGeocodeStatusEnumEnumMap[instance.geocodeStatus]!,
  'geocodedDisplayName': instance.geocodedDisplayName,
  'id': instance.id,
  'label': instance.label,
  'lat': instance.lat,
  'lon': instance.lon,
  'note': instance.note,
  'plannedArrival': instance.plannedArrival?.toIso8601String(),
  'recipientName': instance.recipientName,
  'recipientPhone': instance.recipientPhone,
  'sequence': instance.sequence,
  'serviceTimeSeconds': instance.serviceTimeSeconds,
  'status': _$PlanStopResponseStatusEnumEnumMap[instance.status]!,
  'timeWindow': instance.timeWindow?.toJson(),
};

const _$PlanStopResponseDropReasonEnumEnumMap = {
  PlanStopResponseDropReasonEnum.NOT_OPTIMIZED: 'NOT_OPTIMIZED',
  PlanStopResponseDropReasonEnum.INFEASIBLE: 'INFEASIBLE',
  PlanStopResponseDropReasonEnum.MANUAL: 'MANUAL',
};

const _$PlanStopResponseGeocodeErrorEnumEnumMap = {
  PlanStopResponseGeocodeErrorEnum.NOT_FOUND: 'NOT_FOUND',
  PlanStopResponseGeocodeErrorEnum.PROVIDER_ERROR: 'PROVIDER_ERROR',
};

const _$PlanStopResponseGeocodeStatusEnumEnumMap = {
  PlanStopResponseGeocodeStatusEnum.PROVIDED: 'PROVIDED',
  PlanStopResponseGeocodeStatusEnum.PENDING: 'PENDING',
  PlanStopResponseGeocodeStatusEnum.RESOLVED: 'RESOLVED',
  PlanStopResponseGeocodeStatusEnum.FAILED: 'FAILED',
};

const _$PlanStopResponseStatusEnumEnumMap = {
  PlanStopResponseStatusEnum.PENDING: 'PENDING',
  PlanStopResponseStatusEnum.DELIVERED: 'DELIVERED',
  PlanStopResponseStatusEnum.FAILED: 'FAILED',
  PlanStopResponseStatusEnum.SKIPPED: 'SKIPPED',
};
