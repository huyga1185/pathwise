//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'create_invitation_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class CreateInvitationRequest {
  /// Returns a new [CreateInvitationRequest] instance.
  CreateInvitationRequest({

    required  this.email,

    required  this.role,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'role',
    required: true,
    includeIfNull: false,
  )


  final CreateInvitationRequestRoleEnum role;





    @override
    bool operator ==(Object other) => identical(this, other) || other is CreateInvitationRequest &&
      other.email == email &&
      other.role == role;

    @override
    int get hashCode =>
        email.hashCode +
        role.hashCode;

  factory CreateInvitationRequest.fromJson(Map<String, dynamic> json) => _$CreateInvitationRequestFromJson(json);

  Map<String, dynamic> toJson() => _$CreateInvitationRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum CreateInvitationRequestRoleEnum {
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const CreateInvitationRequestRoleEnum(this.value);

final String value;

@override
String toString() => value;
}


