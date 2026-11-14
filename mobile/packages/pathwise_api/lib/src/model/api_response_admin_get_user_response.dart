//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/admin_get_user_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_admin_get_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseAdminGetUserResponse {
  /// Returns a new [ApiResponseAdminGetUserResponse] instance.
  ApiResponseAdminGetUserResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final AdminGetUserResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseAdminGetUserResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseAdminGetUserResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseAdminGetUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseAdminGetUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

