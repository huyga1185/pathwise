//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'change_password_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ChangePasswordRequest {
  /// Returns a new [ChangePasswordRequest] instance.
  ChangePasswordRequest({

    required  this.newPassword,

    required  this.otp,
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





    @override
    bool operator ==(Object other) => identical(this, other) || other is ChangePasswordRequest &&
      other.newPassword == newPassword &&
      other.otp == otp;

    @override
    int get hashCode =>
        newPassword.hashCode +
        otp.hashCode;

  factory ChangePasswordRequest.fromJson(Map<String, dynamic> json) => _$ChangePasswordRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ChangePasswordRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

