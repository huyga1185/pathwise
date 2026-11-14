// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'admin_store_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AdminStoreSummaryResponse _$AdminStoreSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('AdminStoreSummaryResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'createdAt',
      'id',
      'lastActivityAt',
      'memberCounts',
      'name',
      'ownerEmail',
      'plans',
      'status',
      'vehicles',
    ],
  );
  final val = AdminStoreSummaryResponse(
    createdAt: $checkedConvert('createdAt', (v) => DateTime.parse(v as String)),
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    lastActivityAt: $checkedConvert(
      'lastActivityAt',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    memberCounts: $checkedConvert(
      'memberCounts',
      (v) => MemberCountsResponse.fromJson(v as Map<String, dynamic>),
    ),
    name: $checkedConvert('name', (v) => v as String),
    ownerEmail: $checkedConvert('ownerEmail', (v) => v as String?),
    plans: $checkedConvert('plans', (v) => (v as num).toInt()),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$AdminStoreSummaryResponseStatusEnumEnumMap, v),
    ),
    vehicles: $checkedConvert('vehicles', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$AdminStoreSummaryResponseToJson(
  AdminStoreSummaryResponse instance,
) => <String, dynamic>{
  'createdAt': instance.createdAt.toIso8601String(),
  'id': instance.id,
  'lastActivityAt': instance.lastActivityAt?.toIso8601String(),
  'memberCounts': instance.memberCounts.toJson(),
  'name': instance.name,
  'ownerEmail': instance.ownerEmail,
  'plans': instance.plans,
  'status': _$AdminStoreSummaryResponseStatusEnumEnumMap[instance.status]!,
  'vehicles': instance.vehicles,
};

const _$AdminStoreSummaryResponseStatusEnumEnumMap = {
  AdminStoreSummaryResponseStatusEnum.ACTIVE: 'ACTIVE',
  AdminStoreSummaryResponseStatusEnum.READ_ONLY: 'READ_ONLY',
};
