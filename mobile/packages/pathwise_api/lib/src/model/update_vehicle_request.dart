//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_vehicle_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateVehicleRequest {
  /// Returns a new [UpdateVehicleRequest] instance.
  UpdateVehicleRequest({

    required  this.active,

    required  this.capacity,

    required  this.capacityUnit,

    required  this.name,

     this.plateNumber,
  });

  @JsonKey(
    
    name: r'active',
    required: true,
    includeIfNull: false,
  )


  final bool active;



  @JsonKey(
    
    name: r'capacity',
    required: true,
    includeIfNull: false,
  )


  final int capacity;



  @JsonKey(
    
    name: r'capacityUnit',
    required: true,
    includeIfNull: false,
  )


  final UpdateVehicleRequestCapacityUnitEnum capacityUnit;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'plateNumber',
    required: false,
    includeIfNull: false,
  )


  final String? plateNumber;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateVehicleRequest &&
      other.active == active &&
      other.capacity == capacity &&
      other.capacityUnit == capacityUnit &&
      other.name == name &&
      other.plateNumber == plateNumber;

    @override
    int get hashCode =>
        active.hashCode +
        capacity.hashCode +
        capacityUnit.hashCode +
        name.hashCode +
        plateNumber.hashCode;

  factory UpdateVehicleRequest.fromJson(Map<String, dynamic> json) => _$UpdateVehicleRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateVehicleRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum UpdateVehicleRequestCapacityUnitEnum {
@JsonValue(r'KG')
KG(r'KG'),
@JsonValue(r'BOX')
BOX(r'BOX'),
@JsonValue(r'PACKAGE')
PACKAGE(r'PACKAGE'),
@JsonValue(r'ORDER')
ORDER(r'ORDER');

const UpdateVehicleRequestCapacityUnitEnum(this.value);

final String value;

@override
String toString() => value;
}


