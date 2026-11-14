// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'audit_event_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AuditEventResponse _$AuditEventResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('AuditEventResponse', json, ($checkedConvert) {
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
          'storeId',
          'targetId',
          'targetType',
        ],
      );
      final val = AuditEventResponse(
        action: $checkedConvert(
          'action',
          (v) => $enumDecode(_$AuditEventResponseActionEnumEnumMap, v),
        ),
        actorEmail: $checkedConvert('actorEmail', (v) => v as String?),
        actorId: $checkedConvert('actorId', (v) => (v as num?)?.toInt()),
        actorRole: $checkedConvert(
          'actorRole',
          (v) =>
              $enumDecodeNullable(_$AuditEventResponseActorRoleEnumEnumMap, v),
        ),
        details: $checkedConvert(
          'details',
          (v) => (v as Map<String, dynamic>).map(
            (k, e) => MapEntry(k, e as Object),
          ),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        ip: $checkedConvert('ip', (v) => v as String?),
        occurredAt: $checkedConvert(
          'occurredAt',
          (v) => DateTime.parse(v as String),
        ),
        requestId: $checkedConvert('requestId', (v) => v as String?),
        storeId: $checkedConvert('storeId', (v) => (v as num?)?.toInt()),
        targetId: $checkedConvert('targetId', (v) => (v as num?)?.toInt()),
        targetType: $checkedConvert(
          'targetType',
          (v) => $enumDecode(_$AuditEventResponseTargetTypeEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$AuditEventResponseToJson(
  AuditEventResponse instance,
) => <String, dynamic>{
  'action': _$AuditEventResponseActionEnumEnumMap[instance.action]!,
  'actorEmail': instance.actorEmail,
  'actorId': instance.actorId,
  'actorRole': _$AuditEventResponseActorRoleEnumEnumMap[instance.actorRole],
  'details': instance.details,
  'id': instance.id,
  'ip': instance.ip,
  'occurredAt': instance.occurredAt.toIso8601String(),
  'requestId': instance.requestId,
  'storeId': instance.storeId,
  'targetId': instance.targetId,
  'targetType': _$AuditEventResponseTargetTypeEnumEnumMap[instance.targetType]!,
};

const _$AuditEventResponseActionEnumEnumMap = {
  AuditEventResponseActionEnum.MEMBER_UPDATED: 'MEMBER_UPDATED',
  AuditEventResponseActionEnum.INVITATION_REVOKED: 'INVITATION_REVOKED',
  AuditEventResponseActionEnum.PLAN_PUBLISHED: 'PLAN_PUBLISHED',
  AuditEventResponseActionEnum.PLAN_CANCELLED: 'PLAN_CANCELLED',
  AuditEventResponseActionEnum.PLAN_ROUTES_EDITED: 'PLAN_ROUTES_EDITED',
  AuditEventResponseActionEnum.STORE_VIEWED: 'STORE_VIEWED',
  AuditEventResponseActionEnum.STORE_LOCKED: 'STORE_LOCKED',
  AuditEventResponseActionEnum.STORE_UNLOCKED: 'STORE_UNLOCKED',
  AuditEventResponseActionEnum.USER_DISABLED: 'USER_DISABLED',
  AuditEventResponseActionEnum.USER_ENABLED: 'USER_ENABLED',
  AuditEventResponseActionEnum.USER_SESSIONS_REVOKED: 'USER_SESSIONS_REVOKED',
  AuditEventResponseActionEnum.VERIFICATION_EMAIL_SENT:
      'VERIFICATION_EMAIL_SENT',
  AuditEventResponseActionEnum.PASSWORD_RESET_LINK_SENT:
      'PASSWORD_RESET_LINK_SENT',
  AuditEventResponseActionEnum.STORE_OWNER_TRANSFERRED:
      'STORE_OWNER_TRANSFERRED',
};

const _$AuditEventResponseActorRoleEnumEnumMap = {
  AuditEventResponseActorRoleEnum.ADMIN: 'ADMIN',
  AuditEventResponseActorRoleEnum.OWNER: 'OWNER',
  AuditEventResponseActorRoleEnum.DISPATCHER: 'DISPATCHER',
  AuditEventResponseActorRoleEnum.DRIVER: 'DRIVER',
};

const _$AuditEventResponseTargetTypeEnumEnumMap = {
  AuditEventResponseTargetTypeEnum.STORE: 'STORE',
  AuditEventResponseTargetTypeEnum.USER: 'USER',
  AuditEventResponseTargetTypeEnum.INVITATION: 'INVITATION',
  AuditEventResponseTargetTypeEnum.PLAN: 'PLAN',
};
