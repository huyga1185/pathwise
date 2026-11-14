// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'member_counts_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MemberCountsResponse _$MemberCountsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('MemberCountsResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['dispatchers', 'drivers', 'owners']);
  final val = MemberCountsResponse(
    dispatchers: $checkedConvert('dispatchers', (v) => (v as num).toInt()),
    drivers: $checkedConvert('drivers', (v) => (v as num).toInt()),
    owners: $checkedConvert('owners', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$MemberCountsResponseToJson(
  MemberCountsResponse instance,
) => <String, dynamic>{
  'dispatchers': instance.dispatchers,
  'drivers': instance.drivers,
  'owners': instance.owners,
};
