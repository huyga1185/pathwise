//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/directions_point.dart';
import 'package:json_annotation/json_annotation.dart';

part 'directions_instruction.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DirectionsInstruction {
  /// Returns a new [DirectionsInstruction] instance.
  DirectionsInstruction({

    required  this.distanceMeters,

    required  this.durationSeconds,

    required  this.exitNumber,

    required  this.location,

    required  this.maneuver,

    required  this.roadName,
  });

  @JsonKey(
    
    name: r'distanceMeters',
    required: true,
    includeIfNull: false,
  )


  final int distanceMeters;



  @JsonKey(
    
    name: r'durationSeconds',
    required: true,
    includeIfNull: false,
  )


  final int durationSeconds;



  @JsonKey(
    
    name: r'exitNumber',
    required: true,
    includeIfNull: true,
  )


  final int? exitNumber;



  @JsonKey(
    
    name: r'location',
    required: true,
    includeIfNull: false,
  )


  final DirectionsPoint location;



  @JsonKey(
    
    name: r'maneuver',
    required: true,
    includeIfNull: false,
  )


  final DirectionsInstructionManeuverEnum maneuver;



  @JsonKey(
    
    name: r'roadName',
    required: true,
    includeIfNull: true,
  )


  final String? roadName;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DirectionsInstruction &&
      other.distanceMeters == distanceMeters &&
      other.durationSeconds == durationSeconds &&
      other.exitNumber == exitNumber &&
      other.location == location &&
      other.maneuver == maneuver &&
      other.roadName == roadName;

    @override
    int get hashCode =>
        distanceMeters.hashCode +
        durationSeconds.hashCode +
        (exitNumber == null ? 0 : exitNumber.hashCode) +
        location.hashCode +
        maneuver.hashCode +
        (roadName == null ? 0 : roadName.hashCode);

  factory DirectionsInstruction.fromJson(Map<String, dynamic> json) => _$DirectionsInstructionFromJson(json);

  Map<String, dynamic> toJson() => _$DirectionsInstructionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DirectionsInstructionManeuverEnum {
@JsonValue(r'DEPART')
DEPART(r'DEPART'),
@JsonValue(r'ARRIVE')
ARRIVE(r'ARRIVE'),
@JsonValue(r'STRAIGHT')
STRAIGHT(r'STRAIGHT'),
@JsonValue(r'SLIGHT_LEFT')
SLIGHT_LEFT(r'SLIGHT_LEFT'),
@JsonValue(r'SLIGHT_RIGHT')
SLIGHT_RIGHT(r'SLIGHT_RIGHT'),
@JsonValue(r'LEFT')
LEFT(r'LEFT'),
@JsonValue(r'RIGHT')
RIGHT(r'RIGHT'),
@JsonValue(r'SHARP_LEFT')
SHARP_LEFT(r'SHARP_LEFT'),
@JsonValue(r'SHARP_RIGHT')
SHARP_RIGHT(r'SHARP_RIGHT'),
@JsonValue(r'UTURN')
UTURN(r'UTURN'),
@JsonValue(r'KEEP_LEFT')
KEEP_LEFT(r'KEEP_LEFT'),
@JsonValue(r'KEEP_RIGHT')
KEEP_RIGHT(r'KEEP_RIGHT'),
@JsonValue(r'ROUNDABOUT')
ROUNDABOUT(r'ROUNDABOUT');

const DirectionsInstructionManeuverEnum(this.value);

final String value;

@override
String toString() => value;
}


