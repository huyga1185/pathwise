//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'accept_invitation_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AcceptInvitationRequest {
  /// Returns a new [AcceptInvitationRequest] instance.
  AcceptInvitationRequest({

    required  this.code,

    required  this.fullName,

    required  this.password,

     this.phoneNumber,
  });

  @JsonKey(
    
    name: r'code',
    required: true,
    includeIfNull: false,
  )


  final String code;



  @JsonKey(
    
    name: r'fullName',
    required: true,
    includeIfNull: false,
  )


  final String fullName;



  @JsonKey(
    
    name: r'password',
    required: true,
    includeIfNull: false,
  )


  final String password;



  @JsonKey(
    
    name: r'phoneNumber',
    required: false,
    includeIfNull: false,
  )


  final String? phoneNumber;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AcceptInvitationRequest &&
      other.code == code &&
      other.fullName == fullName &&
      other.password == password &&
      other.phoneNumber == phoneNumber;

    @override
    int get hashCode =>
        code.hashCode +
        fullName.hashCode +
        password.hashCode +
        phoneNumber.hashCode;

  factory AcceptInvitationRequest.fromJson(Map<String, dynamic> json) => _$AcceptInvitationRequestFromJson(json);

  Map<String, dynamic> toJson() => _$AcceptInvitationRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

