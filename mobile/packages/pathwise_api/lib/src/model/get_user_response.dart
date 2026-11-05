//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'get_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class GetUserResponse {
  /// Returns a new [GetUserResponse] instance.
  GetUserResponse({

    required  this.canDrive,

    required  this.email,

    required  this.fullName,

    required  this.id,

    required  this.phoneNumber,

    required  this.role,

    required  this.status,

    required  this.storeId,
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


  final GetUserResponseRoleEnum role;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final GetUserResponseStatusEnum status;



  @JsonKey(
    
    name: r'storeId',
    required: true,
    includeIfNull: true,
  )


  final int? storeId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is GetUserResponse &&
      other.canDrive == canDrive &&
      other.email == email &&
      other.fullName == fullName &&
      other.id == id &&
      other.phoneNumber == phoneNumber &&
      other.role == role &&
      other.status == status &&
      other.storeId == storeId;

    @override
    int get hashCode =>
        canDrive.hashCode +
        email.hashCode +
        (fullName == null ? 0 : fullName.hashCode) +
        id.hashCode +
        (phoneNumber == null ? 0 : phoneNumber.hashCode) +
        role.hashCode +
        status.hashCode +
        (storeId == null ? 0 : storeId.hashCode);

  factory GetUserResponse.fromJson(Map<String, dynamic> json) => _$GetUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$GetUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum GetUserResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const GetUserResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum GetUserResponseStatusEnum {
@JsonValue(r'PENDING_VERIFICATION')
PENDING_VERIFICATION(r'PENDING_VERIFICATION'),
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const GetUserResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


