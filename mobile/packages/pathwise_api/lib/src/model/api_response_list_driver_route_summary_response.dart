//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/driver_route_summary_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_driver_route_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListDriverRouteSummaryResponse {
  /// Returns a new [ApiResponseListDriverRouteSummaryResponse] instance.
  ApiResponseListDriverRouteSummaryResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<DriverRouteSummaryResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListDriverRouteSummaryResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListDriverRouteSummaryResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListDriverRouteSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListDriverRouteSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

