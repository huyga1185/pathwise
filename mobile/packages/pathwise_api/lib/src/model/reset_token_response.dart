//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'reset_token_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ResetTokenResponse {
  /// Returns a new [ResetTokenResponse] instance.
  ResetTokenResponse({

    required  this.expiresAt,

    required  this.resetToken,
  });

  @JsonKey(
    
    name: r'expiresAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiresAt;



  @JsonKey(
    
    name: r'resetToken',
    required: true,
    includeIfNull: false,
  )


  final String resetToken;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ResetTokenResponse &&
      other.expiresAt == expiresAt &&
      other.resetToken == resetToken;

    @override
    int get hashCode =>
        expiresAt.hashCode +
        resetToken.hashCode;

  factory ResetTokenResponse.fromJson(Map<String, dynamic> json) => _$ResetTokenResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ResetTokenResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

