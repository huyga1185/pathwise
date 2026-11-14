// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'now.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Now _$NowFromJson(Map<String, dynamic> json) => $checkedCreate('Now', json, (
  $checkedConvert,
) {
  $checkKeys(
    json,
    requiredKeys: const [
      'activeStores30d',
      'activeStores7d',
      'admins',
      'dispatchers',
      'drivers',
      'lockedStores',
      'owners',
      'stores',
    ],
  );
  final val = Now(
    activeStores30d: $checkedConvert(
      'activeStores30d',
      (v) => (v as num).toInt(),
    ),
    activeStores7d: $checkedConvert(
      'activeStores7d',
      (v) => (v as num).toInt(),
    ),
    admins: $checkedConvert('admins', (v) => (v as num).toInt()),
    dispatchers: $checkedConvert('dispatchers', (v) => (v as num).toInt()),
    drivers: $checkedConvert('drivers', (v) => (v as num).toInt()),
    lockedStores: $checkedConvert('lockedStores', (v) => (v as num).toInt()),
    owners: $checkedConvert('owners', (v) => (v as num).toInt()),
    stores: $checkedConvert('stores', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$NowToJson(Now instance) => <String, dynamic>{
  'activeStores30d': instance.activeStores30d,
  'activeStores7d': instance.activeStores7d,
  'admins': instance.admins,
  'dispatchers': instance.dispatchers,
  'drivers': instance.drivers,
  'lockedStores': instance.lockedStores,
  'owners': instance.owners,
  'stores': instance.stores,
};
