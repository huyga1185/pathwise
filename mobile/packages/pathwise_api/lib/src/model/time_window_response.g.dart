// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'time_window_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TimeWindowResponse _$TimeWindowResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('TimeWindowResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['end', 'start']);
      final val = TimeWindowResponse(
        end: $checkedConvert('end', (v) => v as String),
        start: $checkedConvert('start', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$TimeWindowResponseToJson(TimeWindowResponse instance) =>
    <String, dynamic>{'end': instance.end, 'start': instance.start};
