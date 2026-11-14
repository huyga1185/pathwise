//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/business_metrics_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_business_metrics_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseBusinessMetricsResponse {
  /// Returns a new [ApiResponseBusinessMetricsResponse] instance.
  ApiResponseBusinessMetricsResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final BusinessMetricsResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseBusinessMetricsResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseBusinessMetricsResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseBusinessMetricsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseBusinessMetricsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

