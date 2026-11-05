//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/vehicle_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_vehicle_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListVehicleResponse {
  /// Returns a new [ApiResponseListVehicleResponse] instance.
  ApiResponseListVehicleResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<VehicleResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListVehicleResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListVehicleResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListVehicleResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListVehicleResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

