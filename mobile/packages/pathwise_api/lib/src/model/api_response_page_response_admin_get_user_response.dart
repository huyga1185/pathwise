//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/page_response_admin_get_user_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_page_response_admin_get_user_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePageResponseAdminGetUserResponse {
  /// Returns a new [ApiResponsePageResponseAdminGetUserResponse] instance.
  ApiResponsePageResponseAdminGetUserResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PageResponseAdminGetUserResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePageResponseAdminGetUserResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePageResponseAdminGetUserResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePageResponseAdminGetUserResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePageResponseAdminGetUserResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

