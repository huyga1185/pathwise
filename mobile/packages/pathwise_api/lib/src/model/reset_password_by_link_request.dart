//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'reset_password_by_link_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResetPasswordByLinkRequest {
  /// Returns a new [ResetPasswordByLinkRequest] instance.
  ResetPasswordByLinkRequest({

    required  this.newPassword,

    required  this.otp,

    required  this.token,
  });

  @JsonKey(
    
    name: r'newPassword',
    required: true,
    includeIfNull: false,
  )


  final String newPassword;



  @JsonKey(
    
    name: r'otp',
    required: true,
    includeIfNull: false,
  )


  final String otp;



  @JsonKey(
    
    name: r'token',
    required: true,
    includeIfNull: false,
  )


  final String token;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResetPasswordByLinkRequest &&
      other.newPassword == newPassword &&
      other.otp == otp &&
      other.token == token;

    @override
    int get hashCode =>
        newPassword.hashCode +
        otp.hashCode +
        token.hashCode;

  factory ResetPasswordByLinkRequest.fromJson(Map<String, dynamic> json) => _$ResetPasswordByLinkRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ResetPasswordByLinkRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

