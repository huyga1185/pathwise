//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'invitation_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class InvitationResponse {
  /// Returns a new [InvitationResponse] instance.
  InvitationResponse({

    required  this.email,

    required  this.expiresAt,

    required  this.id,

    required  this.role,

    required  this.status,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'expiresAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiresAt;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'role',
    required: true,
    includeIfNull: false,
  )


  final InvitationResponseRoleEnum role;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final InvitationResponseStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is InvitationResponse &&
      other.email == email &&
      other.expiresAt == expiresAt &&
      other.id == id &&
      other.role == role &&
      other.status == status;

    @override
    int get hashCode =>
        email.hashCode +
        expiresAt.hashCode +
        id.hashCode +
        role.hashCode +
        status.hashCode;

  factory InvitationResponse.fromJson(Map<String, dynamic> json) => _$InvitationResponseFromJson(json);

  Map<String, dynamic> toJson() => _$InvitationResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum InvitationResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const InvitationResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum InvitationResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'ACCEPTED')
ACCEPTED(r'ACCEPTED'),
@JsonValue(r'REVOKED')
REVOKED(r'REVOKED'),
@JsonValue(r'EXPIRED')
EXPIRED(r'EXPIRED');

const InvitationResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


