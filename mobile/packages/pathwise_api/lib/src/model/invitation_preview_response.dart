//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'invitation_preview_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class InvitationPreviewResponse {
  /// Returns a new [InvitationPreviewResponse] instance.
  InvitationPreviewResponse({

    required  this.email,

    required  this.expiresAt,

    required  this.role,

    required  this.storeName,
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
    
    name: r'role',
    required: true,
    includeIfNull: false,
  )


  final InvitationPreviewResponseRoleEnum role;



  @JsonKey(
    
    name: r'storeName',
    required: true,
    includeIfNull: false,
  )


  final String storeName;





    @override
    bool operator ==(Object other) => identical(this, other) || other is InvitationPreviewResponse &&
      other.email == email &&
      other.expiresAt == expiresAt &&
      other.role == role &&
      other.storeName == storeName;

    @override
    int get hashCode =>
        email.hashCode +
        expiresAt.hashCode +
        role.hashCode +
        storeName.hashCode;

  factory InvitationPreviewResponse.fromJson(Map<String, dynamic> json) => _$InvitationPreviewResponseFromJson(json);

  Map<String, dynamic> toJson() => _$InvitationPreviewResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum InvitationPreviewResponseRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const InvitationPreviewResponseRoleEnum(this.value);

final String value;

@override
String toString() => value;
}


