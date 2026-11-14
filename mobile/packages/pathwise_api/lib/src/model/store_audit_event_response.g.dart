// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_audit_event_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StoreAuditEventResponse _$StoreAuditEventResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('StoreAuditEventResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'action',
      'actorEmail',
      'actorId',
      'actorRole',
      'details',
      'id',
      'ip',
      'occurredAt',
      'requestId',
      'targetId',
      'targetType',
    ],
  );
  final val = StoreAuditEventResponse(
    action: $checkedConvert(
      'action',
      (v) => $enumDecode(_$StoreAuditEventResponseActionEnumEnumMap, v),
    ),
    actorEmail: $checkedConvert('actorEmail', (v) => v as String?),
    actorId: $checkedConvert('actorId', (v) => (v as num?)?.toInt()),
    actorRole: $checkedConvert(
      'actorRole',
      (v) =>
          $enumDecodeNullable(_$StoreAuditEventResponseActorRoleEnumEnumMap, v),
    ),
    details: $checkedConvert(
      'details',
      (v) =>
          (v as Map<String, dynamic>).map((k, e) => MapEntry(k, e as Object)),
    ),
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    ip: $checkedConvert('ip', (v) => v as String?),
    occurredAt: $checkedConvert(
      'occurredAt',
      (v) => DateTime.parse(v as String),
    ),
    requestId: $checkedConvert('requestId', (v) => v as String?),
    targetId: $checkedConvert('targetId', (v) => (v as num?)?.toInt()),
    targetType: $checkedConvert(
      'targetType',
      (v) => $enumDecode(_$StoreAuditEventResponseTargetTypeEnumEnumMap, v),
    ),
  );
  return val;
});

Map<String, dynamic> _$StoreAuditEventResponseToJson(
  StoreAuditEventResponse instance,
) => <String, dynamic>{
  'action': _$StoreAuditEventResponseActionEnumEnumMap[instance.action]!,
  'actorEmail': instance.actorEmail,
  'actorId': instance.actorId,
  'actorRole':
      _$StoreAuditEventResponseActorRoleEnumEnumMap[instance.actorRole],
  'details': instance.details,
  'id': instance.id,
  'ip': instance.ip,
  'occurredAt': instance.occurredAt.toIso8601String(),
  'requestId': instance.requestId,
  'targetId': instance.targetId,
  'targetType':
      _$StoreAuditEventResponseTargetTypeEnumEnumMap[instance.targetType]!,
};

const _$StoreAuditEventResponseActionEnumEnumMap = {
  StoreAuditEventResponseActionEnum.MEMBER_UPDATED: 'MEMBER_UPDATED',
  StoreAuditEventResponseActionEnum.INVITATION_REVOKED: 'INVITATION_REVOKED',
  StoreAuditEventResponseActionEnum.PLAN_PUBLISHED: 'PLAN_PUBLISHED',
  StoreAuditEventResponseActionEnum.PLAN_CANCELLED: 'PLAN_CANCELLED',
  StoreAuditEventResponseActionEnum.PLAN_ROUTES_EDITED: 'PLAN_ROUTES_EDITED',
  StoreAuditEventResponseActionEnum.STORE_VIEWED: 'STORE_VIEWED',
  StoreAuditEventResponseActionEnum.STORE_LOCKED: 'STORE_LOCKED',
  StoreAuditEventResponseActionEnum.STORE_UNLOCKED: 'STORE_UNLOCKED',
  StoreAuditEventResponseActionEnum.USER_DISABLED: 'USER_DISABLED',
  StoreAuditEventResponseActionEnum.USER_ENABLED: 'USER_ENABLED',
  StoreAuditEventResponseActionEnum.USER_SESSIONS_REVOKED:
      'USER_SESSIONS_REVOKED',
};

const _$StoreAuditEventResponseActorRoleEnumEnumMap = {
  StoreAuditEventResponseActorRoleEnum.ADMIN: 'ADMIN',
  StoreAuditEventResponseActorRoleEnum.OWNER: 'OWNER',
  StoreAuditEventResponseActorRoleEnum.DISPATCHER: 'DISPATCHER',
  StoreAuditEventResponseActorRoleEnum.DRIVER: 'DRIVER',
};

const _$StoreAuditEventResponseTargetTypeEnumEnumMap = {
  StoreAuditEventResponseTargetTypeEnum.STORE: 'STORE',
  StoreAuditEventResponseTargetTypeEnum.USER: 'USER',
  StoreAuditEventResponseTargetTypeEnum.INVITATION: 'INVITATION',
  StoreAuditEventResponseTargetTypeEnum.PLAN: 'PLAN',
};
