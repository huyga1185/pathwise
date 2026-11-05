//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'create_vehicle_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class CreateVehicleRequest {
  /// Returns a new [CreateVehicleRequest] instance.
  CreateVehicleRequest({

    required  this.capacity,

    required  this.capacityUnit,

    required  this.name,

     this.plateNumber,
  });

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


  final CreateVehicleRequestCapacityUnitEnum capacityUnit;



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
    bool operator ==(Object other) => identical(this, other) || other is CreateVehicleRequest &&
      other.capacity == capacity &&
      other.capacityUnit == capacityUnit &&
      other.name == name &&
      other.plateNumber == plateNumber;

    @override
    int get hashCode =>
        capacity.hashCode +
        capacityUnit.hashCode +
        name.hashCode +
        plateNumber.hashCode;

  factory CreateVehicleRequest.fromJson(Map<String, dynamic> json) => _$CreateVehicleRequestFromJson(json);

  Map<String, dynamic> toJson() => _$CreateVehicleRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum CreateVehicleRequestCapacityUnitEnum {
@JsonValue(r'KG')
KG(r'KG'),
@JsonValue(r'BOX')
BOX(r'BOX'),
@JsonValue(r'PACKAGE')
PACKAGE(r'PACKAGE'),
@JsonValue(r'ORDER')
ORDER(r'ORDER');

const CreateVehicleRequestCapacityUnitEnum(this.value);

final String value;

@override
String toString() => value;
}


