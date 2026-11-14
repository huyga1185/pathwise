//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/admin_store_detail_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_admin_store_detail_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseAdminStoreDetailResponse {
  /// Returns a new [ApiResponseAdminStoreDetailResponse] instance.
  ApiResponseAdminStoreDetailResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final AdminStoreDetailResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseAdminStoreDetailResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseAdminStoreDetailResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseAdminStoreDetailResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseAdminStoreDetailResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

