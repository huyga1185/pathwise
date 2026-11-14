//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'admin_member_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AdminMemberResponse {
  /// Returns a new [AdminMemberResponse] instance.
  AdminMemberResponse({

    required  this.createdAt,

    required  this.email,

    required  this.id,

    required  this.role,

    required  this.status,
  });

  @JsonKey(
    
    name: r'createdAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime createdAt;



  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



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


  final AdminMemberResponseRoleEnum role;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final AdminMemberResponseStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AdminMemberResponse &&
      other.createdAt == createdAt &&
      other.email == email &&
      other.id == id &&
      other.role == role &&
      other.status == status;

    @override
    int get hashCode =>
        createdAt.hashCode +
        email.hashCode +
        id.hashCode +
        role.hashCode +
        status.hashCode;

  factory AdminMemberResponse.fromJson(Map<String, dynamic> json) => _$AdminMemberResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AdminMemberResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AdminMemberResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const AdminMemberResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum AdminMemberResponseStatusEnum {
@JsonValue(r'PENDING_VERIFICATION')
PENDING_VERIFICATION(r'PENDING_VERIFICATION'),
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const AdminMemberResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


