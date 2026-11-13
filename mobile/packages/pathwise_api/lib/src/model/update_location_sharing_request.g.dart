// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_location_sharing_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateLocationSharingRequest _$UpdateLocationSharingRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('UpdateLocationSharingRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['routeId', 'state']);
  final val = UpdateLocationSharingRequest(
    routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
    state: $checkedConvert(
      'state',
      (v) => $enumDecode(_$UpdateLocationSharingRequestStateEnumEnumMap, v),
    ),
  );
  return val;
});

Map<String, dynamic> _$UpdateLocationSharingRequestToJson(
  UpdateLocationSharingRequest instance,
) => <String, dynamic>{
  'routeId': instance.routeId,
  'state': _$UpdateLocationSharingRequestStateEnumEnumMap[instance.state]!,
};

const _$UpdateLocationSharingRequestStateEnumEnumMap = {
  UpdateLocationSharingRequestStateEnum.SHARING: 'SHARING',
  UpdateLocationSharingRequestStateEnum.DECLINED: 'DECLINED',
  UpdateLocationSharingRequestStateEnum.NO_PERMISSION: 'NO_PERMISSION',
  UpdateLocationSharingRequestStateEnum.GPS_OFF: 'GPS_OFF',
};
