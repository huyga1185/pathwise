//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'request_password_change_otp_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RequestPasswordChangeOtpRequest {
  /// Returns a new [RequestPasswordChangeOtpRequest] instance.
  RequestPasswordChangeOtpRequest({

    required  this.currentPassword,
  });

  @JsonKey(
    
    name: r'currentPassword',
    required: true,
    includeIfNull: false,
  )


  final String currentPassword;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RequestPasswordChangeOtpRequest &&
      other.currentPassword == currentPassword;

    @override
    int get hashCode =>
        currentPassword.hashCode;

  factory RequestPasswordChangeOtpRequest.fromJson(Map<String, dynamic> json) => _$RequestPasswordChangeOtpRequestFromJson(json);

  Map<String, dynamic> toJson() => _$RequestPasswordChangeOtpRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

