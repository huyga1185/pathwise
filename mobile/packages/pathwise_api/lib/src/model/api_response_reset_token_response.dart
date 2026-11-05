//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/reset_token_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_reset_token_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseResetTokenResponse {
  /// Returns a new [ApiResponseResetTokenResponse] instance.
  ApiResponseResetTokenResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final ResetTokenResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseResetTokenResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseResetTokenResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseResetTokenResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseResetTokenResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

