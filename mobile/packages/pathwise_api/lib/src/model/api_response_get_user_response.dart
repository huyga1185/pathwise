//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/get_user_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_get_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseGetUserResponse {
  /// Returns a new [ApiResponseGetUserResponse] instance.
  ApiResponseGetUserResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final GetUserResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseGetUserResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseGetUserResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseGetUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseGetUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

