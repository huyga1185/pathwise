//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/vehicle_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_vehicle_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseVehicleResponse {
  /// Returns a new [ApiResponseVehicleResponse] instance.
  ApiResponseVehicleResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final VehicleResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseVehicleResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseVehicleResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseVehicleResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseVehicleResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

