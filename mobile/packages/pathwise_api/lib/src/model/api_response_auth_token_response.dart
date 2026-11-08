//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/auth_token_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_auth_token_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseAuthTokenResponse {
  /// Returns a new [ApiResponseAuthTokenResponse] instance.
  ApiResponseAuthTokenResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final AuthTokenResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseAuthTokenResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseAuthTokenResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseAuthTokenResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseAuthTokenResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

