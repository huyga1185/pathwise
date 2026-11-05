//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'reset_password_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResetPasswordRequest {
  /// Returns a new [ResetPasswordRequest] instance.
  ResetPasswordRequest({

    required  this.newPassword,

    required  this.resetToken,
  });

  @JsonKey(
    
    name: r'newPassword',
    required: true,
    includeIfNull: false,
  )


  final String newPassword;



  @JsonKey(
    
    name: r'resetToken',
    required: true,
    includeIfNull: false,
  )


  final String resetToken;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResetPasswordRequest &&
      other.newPassword == newPassword &&
      other.resetToken == resetToken;

    @override
    int get hashCode =>
        newPassword.hashCode +
        resetToken.hashCode;

  factory ResetPasswordRequest.fromJson(Map<String, dynamic> json) => _$ResetPasswordRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ResetPasswordRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

