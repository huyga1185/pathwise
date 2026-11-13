//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/directions_instruction.dart';
import 'package:pathwise_api/src/model/directions_destination.dart';
import 'package:json_annotation/json_annotation.dart';

part 'directions_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DirectionsResponse {
  /// Returns a new [DirectionsResponse] instance.
  DirectionsResponse({

    required  this.destination,

    required  this.distanceMeters,

    required  this.durationSeconds,

    required  this.polyline,

    required  this.steps,
  });

  @JsonKey(
    
    name: r'destination',
    required: true,
    includeIfNull: false,
  )


  final DirectionsDestination destination;



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
    
    name: r'polyline',
    required: true,
    includeIfNull: false,
  )


  final String polyline;



  @JsonKey(
    
    name: r'steps',
    required: true,
    includeIfNull: false,
  )


  final List<DirectionsInstruction> steps;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DirectionsResponse &&
      other.destination == destination &&
      other.distanceMeters == distanceMeters &&
      other.durationSeconds == durationSeconds &&
      other.polyline == polyline &&
      other.steps == steps;

    @override
    int get hashCode =>
        destination.hashCode +
        distanceMeters.hashCode +
        durationSeconds.hashCode +
        polyline.hashCode +
        steps.hashCode;

  factory DirectionsResponse.fromJson(Map<String, dynamic> json) => _$DirectionsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DirectionsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

