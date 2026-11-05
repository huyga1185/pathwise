//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'verify_reset_otp_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class VerifyResetOtpRequest {
  /// Returns a new [VerifyResetOtpRequest] instance.
  VerifyResetOtpRequest({

    required  this.email,

    required  this.otp,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'otp',
    required: true,
    includeIfNull: false,
  )


  final String otp;





    @override
    bool operator ==(Object other) => identical(this, other) || other is VerifyResetOtpRequest &&
      other.email == email &&
      other.otp == otp;

    @override
    int get hashCode =>
        email.hashCode +
        otp.hashCode;

  factory VerifyResetOtpRequest.fromJson(Map<String, dynamic> json) => _$VerifyResetOtpRequestFromJson(json);

  Map<String, dynamic> toJson() => _$VerifyResetOtpRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

