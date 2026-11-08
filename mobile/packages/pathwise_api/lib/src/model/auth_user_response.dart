//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'auth_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AuthUserResponse {
  /// Returns a new [AuthUserResponse] instance.
  AuthUserResponse({

    required  this.canDrive,

    required  this.email,

     this.fullName,

    required  this.id,

    required  this.role,

     this.storeId,
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
    required: false,
    includeIfNull: false,
  )


  final String? fullName;



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


  final AuthUserResponseRoleEnum role;



  @JsonKey(
    
    name: r'storeId',
    required: false,
    includeIfNull: false,
  )


  final int? storeId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AuthUserResponse &&
      other.canDrive == canDrive &&
      other.email == email &&
      other.fullName == fullName &&
      other.id == id &&
      other.role == role &&
      other.storeId == storeId;

    @override
    int get hashCode =>
        canDrive.hashCode +
        email.hashCode +
        fullName.hashCode +
        id.hashCode +
        role.hashCode +
        storeId.hashCode;

  factory AuthUserResponse.fromJson(Map<String, dynamic> json) => _$AuthUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AuthUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AuthUserResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const AuthUserResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}


