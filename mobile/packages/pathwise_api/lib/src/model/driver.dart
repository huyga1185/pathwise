//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'driver.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Driver {
  /// Returns a new [Driver] instance.
  Driver({

    required  this.fullName,

    required  this.id,

    required  this.role,
  });

  @JsonKey(
    
    name: r'fullName',
    required: true,
    includeIfNull: true,
  )


  final String? fullName;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'role',
    required: true,
    includeIfNull: false,
  )


  final DriverRoleEnum role;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Driver &&
      other.fullName == fullName &&
      other.id == id &&
      other.role == role;

    @override
    int get hashCode =>
        (fullName == null ? 0 : fullName.hashCode) +
        id.hashCode +
        role.hashCode;

  factory Driver.fromJson(Map<String, dynamic> json) => _$DriverFromJson(json);

  Map<String, dynamic> toJson() => _$DriverToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverRoleEnum {
@JsonValue(r'ADMIN')
ADMIN(r'ADMIN'),
@JsonValue(r'OWNER')
OWNER(r'OWNER'),
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const DriverRoleEnum(this.value);

final String value;

@override
String toString() => value;
}


