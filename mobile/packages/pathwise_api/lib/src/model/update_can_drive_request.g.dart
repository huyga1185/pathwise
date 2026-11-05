// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_can_drive_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateCanDriveRequest _$UpdateCanDriveRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('UpdateCanDriveRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['canDrive']);
  final val = UpdateCanDriveRequest(
    canDrive: $checkedConvert('canDrive', (v) => v as bool),
  );
  return val;
});

Map<String, dynamic> _$UpdateCanDriveRequestToJson(
  UpdateCanDriveRequest instance,
) => <String, dynamic>{'canDrive': instance.canDrive};
