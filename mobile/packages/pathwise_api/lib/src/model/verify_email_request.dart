//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'verify_email_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class VerifyEmailRequest {
  /// Returns a new [VerifyEmailRequest] instance.
  VerifyEmailRequest({

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
    bool operator ==(Object other) => identical(this, other) || other is VerifyEmailRequest &&
      other.email == email &&
      other.otp == otp;

    @override
    int get hashCode =>
        email.hashCode +
        otp.hashCode;

  factory VerifyEmailRequest.fromJson(Map<String, dynamic> json) => _$VerifyEmailRequestFromJson(json);

  Map<String, dynamic> toJson() => _$VerifyEmailRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

