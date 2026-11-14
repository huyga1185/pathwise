// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'admin_store_state_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AdminStoreStateResponse _$AdminStoreStateResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('AdminStoreStateResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['id', 'lockReason', 'lockedAt', 'status'],
  );
  final val = AdminStoreStateResponse(
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    lockReason: $checkedConvert('lockReason', (v) => v as String?),
    lockedAt: $checkedConvert(
      'lockedAt',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$AdminStoreStateResponseStatusEnumEnumMap, v),
    ),
  );
  return val;
});

Map<String, dynamic> _$AdminStoreStateResponseToJson(
  AdminStoreStateResponse instance,
) => <String, dynamic>{
  'id': instance.id,
  'lockReason': instance.lockReason,
  'lockedAt': instance.lockedAt?.toIso8601String(),
  'status': _$AdminStoreStateResponseStatusEnumEnumMap[instance.status]!,
};

const _$AdminStoreStateResponseStatusEnumEnumMap = {
  AdminStoreStateResponseStatusEnum.ACTIVE: 'ACTIVE',
  AdminStoreStateResponseStatusEnum.READ_ONLY: 'READ_ONLY',
};
