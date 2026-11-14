//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'audit_event_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AuditEventResponse {
  /// Returns a new [AuditEventResponse] instance.
  AuditEventResponse({

    required  this.action,

    required  this.actorEmail,

    required  this.actorId,

    required  this.actorRole,

    required  this.details,

    required  this.id,

    required  this.ip,

    required  this.occurredAt,

    required  this.requestId,

    required  this.storeId,

    required  this.targetId,

    required  this.targetType,
  });

  @JsonKey(
    
    name: r'action',
    required: true,
    includeIfNull: false,
  )


  final AuditEventResponseActionEnum action;



  @JsonKey(
    
    name: r'actorEmail',
    required: true,
    includeIfNull: true,
  )


  final String? actorEmail;



  @JsonKey(
    
    name: r'actorId',
    required: true,
    includeIfNull: true,
  )


  final int? actorId;



  @JsonKey(
    
    name: r'actorRole',
    required: true,
    includeIfNull: true,
  )


  final AuditEventResponseActorRoleEnum? actorRole;



  @JsonKey(
    
    name: r'details',
    required: true,
    includeIfNull: false,
  )


  final Map<String, Object> details;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'ip',
    required: true,
    includeIfNull: true,
  )


  final String? ip;



  @JsonKey(
    
    name: r'occurredAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime occurredAt;



  @JsonKey(
    
    name: r'requestId',
    required: true,
    includeIfNull: true,
  )


  final String? requestId;



  @JsonKey(
    
    name: r'storeId',
    required: true,
    includeIfNull: true,
  )


  final int? storeId;



  @JsonKey(
    
    name: r'targetId',
    required: true,
    includeIfNull: true,
  )


  final int? targetId;



  @JsonKey(
    
    name: r'targetType',
    required: true,
    includeIfNull: false,
  )


  final AuditEventResponseTargetTypeEnum targetType;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AuditEventResponse &&
      other.action == action &&
      other.actorEmail == actorEmail &&
      other.actorId == actorId &&
      other.actorRole == actorRole &&
      other.details == details &&
      other.id == id &&
      other.ip == ip &&
      other.occurredAt == occurredAt &&
      other.requestId == requestId &&
      other.storeId == storeId &&
      other.targetId == targetId &&
      other.targetType == targetType;

    @override
    int get hashCode =>
        action.hashCode +
        (actorEmail == null ? 0 : actorEmail.hashCode) +
        (actorId == null ? 0 : actorId.hashCode) +
        (actorRole == null ? 0 : actorRole.hashCode) +
        details.hashCode +
        id.hashCode +
        (ip == null ? 0 : ip.hashCode) +
        occurredAt.hashCode +
        (requestId == null ? 0 : requestId.hashCode) +
        (storeId == null ? 0 : storeId.hashCode) +
        (targetId == null ? 0 : targetId.hashCode) +
        targetType.hashCode;

  factory AuditEventResponse.fromJson(Map<String, dynamic> json) => _$AuditEventResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AuditEventResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AuditEventResponseActionEnum {
@JsonValue(r'MEMBER_UPDATED')
MEMBER_UPDATED(r'MEMBER_UPDATED'),
@JsonValue(r'INVITATION_REVOKED')
INVITATION_REVOKED(r'INVITATION_REVOKED'),
@JsonValue(r'PLAN_PUBLISHED')
PLAN_PUBLISHED(r'PLAN_PUBLISHED'),
@JsonValue(r'PLAN_CANCELLED')
PLAN_CANCELLED(r'PLAN_CANCELLED'),
@JsonValue(r'PLAN_ROUTES_EDITED')
PLAN_ROUTES_EDITED(r'PLAN_ROUTES_EDITED'),
@JsonValue(r'STORE_VIEWED')
STORE_VIEWED(r'STORE_VIEWED');

const AuditEventResponseActionEnum(this.value);

final String value;

@override
String toString() => value;
}



enum AuditEventResponseActorRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const AuditEventResponseActorRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum AuditEventResponseTargetTypeEnum {
@JsonValue(r'STORE')
STORE(r'STORE'),
@JsonValue(r'USER')
USER(r'USER'),
@JsonValue(r'INVITATION')
INVITATION(r'INVITATION'),
@JsonValue(r'PLAN')
PLAN(r'PLAN');

const AuditEventResponseTargetTypeEnum(this.value);

final String value;

@override
String toString() => value;
}


