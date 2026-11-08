//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'member_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class MemberResponse {
  /// Returns a new [MemberResponse] instance.
  MemberResponse({

    required  this.canDrive,

    required  this.email,

    required  this.fullName,

    required  this.id,

    required  this.phoneNumber,

    required  this.role,

    required  this.status,
  });

  @JsonKey(
    
    name: r'canDrive',
    required: true,
    includeIfNull: false,
  )


  final bool canDrive;



  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'fullName',
    required: true,
    includeIfNull: true,
  )


  final String? fullName;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'phoneNumber',
    required: true,
    includeIfNull: true,
  )


  final String? phoneNumber;



  @JsonKey(
    
    name: r'role',
    required: true,
    includeIfNull: false,
  )


  final MemberResponseRoleEnum role;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final MemberResponseStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is MemberResponse &&
      other.canDrive == canDrive &&
      other.email == email &&
      other.fullName == fullName &&
      other.id == id &&
      other.phoneNumber == phoneNumber &&
      other.role == role &&
      other.status == status;

    @override
    int get hashCode =>
        canDrive.hashCode +
        email.hashCode +
        (fullName == null ? 0 : fullName.hashCode) +
        id.hashCode +
        (phoneNumber == null ? 0 : phoneNumber.hashCode) +
        role.hashCode +
        status.hashCode;

  factory MemberResponse.fromJson(Map<String, dynamic> json) => _$MemberResponseFromJson(json);

  Map<String, dynamic> toJson() => _$MemberResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum MemberResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const MemberResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum MemberResponseStatusEnum {
@JsonValue(r'PENDING_VERIFICATION')
PENDING_VERIFICATION(r'PENDING_VERIFICATION'),
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const MemberResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


