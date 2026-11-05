//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/auth_user_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'auth_token_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AuthTokenResponse {
  /// Returns a new [AuthTokenResponse] instance.
  AuthTokenResponse({

    required  this.accessToken,

    required  this.accessTokenExpiresAt,

     this.refreshToken,

    required  this.refreshTokenExpiresAt,

     this.user,
  });

  @JsonKey(
    
    name: r'accessToken',
    required: true,
    includeIfNull: false,
  )


  final String accessToken;



  @JsonKey(
    
    name: r'accessTokenExpiresAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime accessTokenExpiresAt;



  @JsonKey(
    
    name: r'refreshToken',
    required: false,
    includeIfNull: false,
  )


  final String? refreshToken;



  @JsonKey(
    
    name: r'refreshTokenExpiresAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime refreshTokenExpiresAt;



  @JsonKey(
    
    name: r'user',
    required: false,
    includeIfNull: false,
  )


  final AuthUserResponse? user;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AuthTokenResponse &&
      other.accessToken == accessToken &&
      other.accessTokenExpiresAt == accessTokenExpiresAt &&
      other.refreshToken == refreshToken &&
      other.refreshTokenExpiresAt == refreshTokenExpiresAt &&
      other.user == user;

    @override
    int get hashCode =>
        accessToken.hashCode +
        accessTokenExpiresAt.hashCode +
        refreshToken.hashCode +
        refreshTokenExpiresAt.hashCode +
        user.hashCode;

  factory AuthTokenResponse.fromJson(Map<String, dynamic> json) => _$AuthTokenResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AuthTokenResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

