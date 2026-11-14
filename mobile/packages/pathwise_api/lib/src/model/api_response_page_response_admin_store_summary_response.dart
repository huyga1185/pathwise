//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/page_response_admin_store_summary_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_page_response_admin_store_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePageResponseAdminStoreSummaryResponse {
  /// Returns a new [ApiResponsePageResponseAdminStoreSummaryResponse] instance.
  ApiResponsePageResponseAdminStoreSummaryResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PageResponseAdminStoreSummaryResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePageResponseAdminStoreSummaryResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePageResponseAdminStoreSummaryResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePageResponseAdminStoreSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePageResponseAdminStoreSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

