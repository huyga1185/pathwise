//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'resend_verification_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResendVerificationRequest {
  /// Returns a new [ResendVerificationRequest] instance.
  ResendVerificationRequest({

    required  this.email,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResendVerificationRequest &&
      other.email == email;

    @override
    int get hashCode =>
        email.hashCode;

  factory ResendVerificationRequest.fromJson(Map<String, dynamic> json) => _$ResendVerificationRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ResendVerificationRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

