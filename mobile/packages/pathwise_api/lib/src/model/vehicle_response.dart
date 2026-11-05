//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'vehicle_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class VehicleResponse {
  /// Returns a new [VehicleResponse] instance.
  VehicleResponse({

    required  this.active,

    required  this.capacity,

    required  this.capacityUnit,

    required  this.id,

    required  this.name,

    required  this.plateNumber,
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


  final VehicleResponseCapacityUnitEnum capacityUnit;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'plateNumber',
    required: true,
    includeIfNull: true,
  )


  final String? plateNumber;





    @override
    bool operator ==(Object other) => identical(this, other) || other is VehicleResponse &&
      other.active == active &&
      other.capacity == capacity &&
      other.capacityUnit == capacityUnit &&
      other.id == id &&
      other.name == name &&
      other.plateNumber == plateNumber;

    @override
    int get hashCode =>
        active.hashCode +
        capacity.hashCode +
        capacityUnit.hashCode +
        id.hashCode +
        name.hashCode +
        (plateNumber == null ? 0 : plateNumber.hashCode);

  factory VehicleResponse.fromJson(Map<String, dynamic> json) => _$VehicleResponseFromJson(json);

  Map<String, dynamic> toJson() => _$VehicleResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum VehicleResponseCapacityUnitEnum {
@JsonValue(r'KG')
KG(r'KG'),
@JsonValue(r'BOX')
BOX(r'BOX'),
@JsonValue(r'PACKAGE')
PACKAGE(r'PACKAGE'),
@JsonValue(r'ORDER')
ORDER(r'ORDER');

const VehicleResponseCapacityUnitEnum(this.value);

final String value;

@override
String toString() => value;
}


