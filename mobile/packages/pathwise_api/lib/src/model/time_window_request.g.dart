// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'time_window_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TimeWindowRequest _$TimeWindowRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('TimeWindowRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['end', 'start']);
      final val = TimeWindowRequest(
        end: $checkedConvert('end', (v) => v as String),
        start: $checkedConvert('start', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$TimeWindowRequestToJson(TimeWindowRequest instance) =>
    <String, dynamic>{'end': instance.end, 'start': instance.start};
