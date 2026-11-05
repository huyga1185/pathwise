// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'assignment_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AssignmentRequest _$AssignmentRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('AssignmentRequest', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['driverId', 'shiftEnd', 'shiftStart', 'vehicleId'],
      );
      final val = AssignmentRequest(
        driverId: $checkedConvert('driverId', (v) => (v as num).toInt()),
        maxDistanceMeters: $checkedConvert(
          'maxDistanceMeters',
          (v) => (v as num?)?.toInt(),
        ),
        shiftEnd: $checkedConvert('shiftEnd', (v) => v as String),
        shiftStart: $checkedConvert('shiftStart', (v) => v as String),
        vehicleId: $checkedConvert('vehicleId', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$AssignmentRequestToJson(AssignmentRequest instance) =>
    <String, dynamic>{
      'driverId': instance.driverId,
      'maxDistanceMeters': ?instance.maxDistanceMeters,
      'shiftEnd': instance.shiftEnd,
      'shiftStart': instance.shiftStart,
      'vehicleId': instance.vehicleId,
    };
