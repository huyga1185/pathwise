//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/driver_location_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_driver_location_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListDriverLocationResponse {
  /// Returns a new [ApiResponseListDriverLocationResponse] instance.
  ApiResponseListDriverLocationResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<DriverLocationResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListDriverLocationResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListDriverLocationResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListDriverLocationResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListDriverLocationResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

