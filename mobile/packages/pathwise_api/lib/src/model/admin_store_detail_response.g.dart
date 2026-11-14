// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'admin_store_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AdminStoreDetailResponse _$AdminStoreDetailResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('AdminStoreDetailResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'createdAt',
      'id',
      'last30Days',
      'lastActivityAt',
      'lockReason',
      'lockedAt',
      'memberCounts',
      'members',
      'name',
      'ownerEmail',
      'plans',
      'status',
      'timezone',
      'vehicles',
    ],
  );
  final val = AdminStoreDetailResponse(
    createdAt: $checkedConvert('createdAt', (v) => DateTime.parse(v as String)),
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    last30Days: $checkedConvert(
      'last30Days',
      (v) => StorePeriodStatsResponse.fromJson(v as Map<String, dynamic>),
    ),
    lastActivityAt: $checkedConvert(
      'lastActivityAt',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    lockReason: $checkedConvert('lockReason', (v) => v as String?),
    lockedAt: $checkedConvert(
      'lockedAt',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    memberCounts: $checkedConvert(
      'memberCounts',
      (v) => MemberCountsResponse.fromJson(v as Map<String, dynamic>),
    ),
    members: $checkedConvert(
      'members',
      (v) => (v as List<dynamic>)
          .map((e) => AdminMemberResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    name: $checkedConvert('name', (v) => v as String),
    ownerEmail: $checkedConvert('ownerEmail', (v) => v as String?),
    plans: $checkedConvert('plans', (v) => (v as num).toInt()),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$AdminStoreDetailResponseStatusEnumEnumMap, v),
    ),
    timezone: $checkedConvert('timezone', (v) => v as String),
    vehicles: $checkedConvert('vehicles', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$AdminStoreDetailResponseToJson(
  AdminStoreDetailResponse instance,
) => <String, dynamic>{
  'createdAt': instance.createdAt.toIso8601String(),
  'id': instance.id,
  'last30Days': instance.last30Days.toJson(),
  'lastActivityAt': instance.lastActivityAt?.toIso8601String(),
  'lockReason': instance.lockReason,
  'lockedAt': instance.lockedAt?.toIso8601String(),
  'memberCounts': instance.memberCounts.toJson(),
  'members': instance.members.map((e) => e.toJson()).toList(),
  'name': instance.name,
  'ownerEmail': instance.ownerEmail,
  'plans': instance.plans,
  'status': _$AdminStoreDetailResponseStatusEnumEnumMap[instance.status]!,
  'timezone': instance.timezone,
  'vehicles': instance.vehicles,
};

const _$AdminStoreDetailResponseStatusEnumEnumMap = {
  AdminStoreDetailResponseStatusEnum.ACTIVE: 'ACTIVE',
  AdminStoreDetailResponseStatusEnum.READ_ONLY: 'READ_ONLY',
};
