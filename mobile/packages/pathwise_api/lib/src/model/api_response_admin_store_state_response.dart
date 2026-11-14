//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/admin_store_state_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_admin_store_state_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseAdminStoreStateResponse {
  /// Returns a new [ApiResponseAdminStoreStateResponse] instance.
  ApiResponseAdminStoreStateResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final AdminStoreStateResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseAdminStoreStateResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseAdminStoreStateResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseAdminStoreStateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseAdminStoreStateResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

