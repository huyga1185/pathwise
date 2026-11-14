//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/store_report_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_store_report_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseStoreReportResponse {
  /// Returns a new [ApiResponseStoreReportResponse] instance.
  ApiResponseStoreReportResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final StoreReportResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseStoreReportResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseStoreReportResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseStoreReportResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseStoreReportResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

