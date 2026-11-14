//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'admin_get_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AdminGetUserResponse {
  /// Returns a new [AdminGetUserResponse] instance.
  AdminGetUserResponse({

    required  this.createdAt,

    required  this.email,

    required  this.id,

    required  this.phoneNumber,

    required  this.role,

    required  this.status,

    required  this.storeId,

    required  this.updatedAt,
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


  final AdminGetUserResponseRoleEnum role;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final AdminGetUserResponseStatusEnum status;



  @JsonKey(
    
    name: r'storeId',
    required: true,
    includeIfNull: true,
  )


  final int? storeId;



  @JsonKey(
    
    name: r'updatedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime updatedAt;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AdminGetUserResponse &&
      other.createdAt == createdAt &&
      other.email == email &&
      other.id == id &&
      other.phoneNumber == phoneNumber &&
      other.role == role &&
      other.status == status &&
      other.storeId == storeId &&
      other.updatedAt == updatedAt;

    @override
    int get hashCode =>
        createdAt.hashCode +
        email.hashCode +
        id.hashCode +
        (phoneNumber == null ? 0 : phoneNumber.hashCode) +
        role.hashCode +
        status.hashCode +
        (storeId == null ? 0 : storeId.hashCode) +
        updatedAt.hashCode;

  factory AdminGetUserResponse.fromJson(Map<String, dynamic> json) => _$AdminGetUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AdminGetUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AdminGetUserResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const AdminGetUserResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum AdminGetUserResponseStatusEnum {
@JsonValue(r'PENDING_VERIFICATION')
PENDING_VERIFICATION(r'PENDING_VERIFICATION'),
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const AdminGetUserResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


