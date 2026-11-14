//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'reset_link_otp_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResetLinkOtpRequest {
  /// Returns a new [ResetLinkOtpRequest] instance.
  ResetLinkOtpRequest({

    required  this.token,
  });

  @JsonKey(
    
    name: r'token',
    required: true,
    includeIfNull: false,
  )


  final String token;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResetLinkOtpRequest &&
      other.token == token;

    @override
    int get hashCode =>
        token.hashCode;

  factory ResetLinkOtpRequest.fromJson(Map<String, dynamic> json) => _$ResetLinkOtpRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ResetLinkOtpRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

