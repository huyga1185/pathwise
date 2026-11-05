//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'vehicle.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Vehicle {
  /// Returns a new [Vehicle] instance.
  Vehicle({

    required  this.capacity,

    required  this.capacityUnit,

    required  this.id,

    required  this.name,
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


  final VehicleCapacityUnitEnum capacityUnit;



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





    @override
    bool operator ==(Object other) => identical(this, other) || other is Vehicle &&
      other.capacity == capacity &&
      other.capacityUnit == capacityUnit &&
      other.id == id &&
      other.name == name;

    @override
    int get hashCode =>
        capacity.hashCode +
        capacityUnit.hashCode +
        id.hashCode +
        name.hashCode;

  factory Vehicle.fromJson(Map<String, dynamic> json) => _$VehicleFromJson(json);

  Map<String, dynamic> toJson() => _$VehicleToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum VehicleCapacityUnitEnum {
@JsonValue(r'KG')
KG(r'KG'),
@JsonValue(r'BOX')
BOX(r'BOX'),
@JsonValue(r'PACKAGE')
PACKAGE(r'PACKAGE'),
@JsonValue(r'ORDER')
ORDER(r'ORDER');

const VehicleCapacityUnitEnum(this.value);

final String value;

@override
String toString() => value;
}


