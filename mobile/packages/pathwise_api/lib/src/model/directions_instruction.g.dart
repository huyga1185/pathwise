// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'directions_instruction.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DirectionsInstruction _$DirectionsInstructionFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DirectionsInstruction', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'distanceMeters',
      'durationSeconds',
      'exitNumber',
      'location',
      'maneuver',
      'roadName',
    ],
  );
  final val = DirectionsInstruction(
    distanceMeters: $checkedConvert(
      'distanceMeters',
      (v) => (v as num).toInt(),
    ),
    durationSeconds: $checkedConvert(
      'durationSeconds',
      (v) => (v as num).toInt(),
    ),
    exitNumber: $checkedConvert('exitNumber', (v) => (v as num?)?.toInt()),
    location: $checkedConvert(
      'location',
      (v) => DirectionsPoint.fromJson(v as Map<String, dynamic>),
    ),
    maneuver: $checkedConvert(
      'maneuver',
      (v) => $enumDecode(_$DirectionsInstructionManeuverEnumEnumMap, v),
    ),
    roadName: $checkedConvert('roadName', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$DirectionsInstructionToJson(
  DirectionsInstruction instance,
) => <String, dynamic>{
  'distanceMeters': instance.distanceMeters,
  'durationSeconds': instance.durationSeconds,
  'exitNumber': instance.exitNumber,
  'location': instance.location.toJson(),
  'maneuver': _$DirectionsInstructionManeuverEnumEnumMap[instance.maneuver]!,
  'roadName': instance.roadName,
};

const _$DirectionsInstructionManeuverEnumEnumMap = {
  DirectionsInstructionManeuverEnum.DEPART: 'DEPART',
  DirectionsInstructionManeuverEnum.ARRIVE: 'ARRIVE',
  DirectionsInstructionManeuverEnum.STRAIGHT: 'STRAIGHT',
  DirectionsInstructionManeuverEnum.SLIGHT_LEFT: 'SLIGHT_LEFT',
  DirectionsInstructionManeuverEnum.SLIGHT_RIGHT: 'SLIGHT_RIGHT',
  DirectionsInstructionManeuverEnum.LEFT: 'LEFT',
  DirectionsInstructionManeuverEnum.RIGHT: 'RIGHT',
  DirectionsInstructionManeuverEnum.SHARP_LEFT: 'SHARP_LEFT',
  DirectionsInstructionManeuverEnum.SHARP_RIGHT: 'SHARP_RIGHT',
  DirectionsInstructionManeuverEnum.UTURN: 'UTURN',
  DirectionsInstructionManeuverEnum.KEEP_LEFT: 'KEEP_LEFT',
  DirectionsInstructionManeuverEnum.KEEP_RIGHT: 'KEEP_RIGHT',
  DirectionsInstructionManeuverEnum.ROUNDABOUT: 'ROUNDABOUT',
};
