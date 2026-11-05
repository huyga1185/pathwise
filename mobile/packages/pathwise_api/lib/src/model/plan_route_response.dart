//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/location_response.dart';
import 'package:pathwise_api/src/model/plan_stop_response.dart';
import 'package:pathwise_api/src/model/vehicle.dart';
import 'package:pathwise_api/src/model/driver.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_route_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanRouteResponse {
  /// Returns a new [PlanRouteResponse] instance.
  PlanRouteResponse({

    required  this.driver,

    required  this.end,

    required  this.id,

    required  this.load,

    required  this.maxDistanceMeters,

    required  this.plannedDistanceMeters,

    required  this.plannedDurationSeconds,

    required  this.plannedEnd,

    required  this.plannedStart,

    required  this.shiftEnd,

    required  this.shiftStart,

    required  this.start,

    required  this.status,

    required  this.stops,

    required  this.vehicle,
  });

  @JsonKey(
    
    name: r'driver',
    required: true,
    includeIfNull: false,
  )


  final Driver driver;



  @JsonKey(
    
    name: r'end',
    required: true,
    includeIfNull: false,
  )


  final LocationResponse end;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'load',
    required: true,
    includeIfNull: true,
  )


  final int? load;



  @JsonKey(
    
    name: r'maxDistanceMeters',
    required: true,
    includeIfNull: true,
  )


  final int? maxDistanceMeters;



  @JsonKey(
    
    name: r'plannedDistanceMeters',
    required: true,
    includeIfNull: true,
  )


  final int? plannedDistanceMeters;



  @JsonKey(
    
    name: r'plannedDurationSeconds',
    required: true,
    includeIfNull: true,
  )


  final int? plannedDurationSeconds;



  @JsonKey(
    
    name: r'plannedEnd',
    required: true,
    includeIfNull: true,
  )


  final DateTime? plannedEnd;



  @JsonKey(
    
    name: r'plannedStart',
    required: true,
    includeIfNull: true,
  )


  final DateTime? plannedStart;



  @JsonKey(
    
    name: r'shiftEnd',
    required: true,
    includeIfNull: false,
  )


  final String shiftEnd;



  @JsonKey(
    
    name: r'shiftStart',
    required: true,
    includeIfNull: false,
  )


  final String shiftStart;



  @JsonKey(
    
    name: r'start',
    required: true,
    includeIfNull: false,
  )


  final LocationResponse start;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final PlanRouteResponseStatusEnum status;



  @JsonKey(
    
    name: r'stops',
    required: true,
    includeIfNull: false,
  )


  final List<PlanStopResponse> stops;



  @JsonKey(
    
    name: r'vehicle',
    required: true,
    includeIfNull: false,
  )


  final Vehicle vehicle;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanRouteResponse &&
      other.driver == driver &&
      other.end == end &&
      other.id == id &&
      other.load == load &&
      other.maxDistanceMeters == maxDistanceMeters &&
      other.plannedDistanceMeters == plannedDistanceMeters &&
      other.plannedDurationSeconds == plannedDurationSeconds &&
      other.plannedEnd == plannedEnd &&
      other.plannedStart == plannedStart &&
      other.shiftEnd == shiftEnd &&
      other.shiftStart == shiftStart &&
      other.start == start &&
      other.status == status &&
      other.stops == stops &&
      other.vehicle == vehicle;

    @override
    int get hashCode =>
        driver.hashCode +
        end.hashCode +
        id.hashCode +
        (load == null ? 0 : load.hashCode) +
        (maxDistanceMeters == null ? 0 : maxDistanceMeters.hashCode) +
        (plannedDistanceMeters == null ? 0 : plannedDistanceMeters.hashCode) +
        (plannedDurationSeconds == null ? 0 : plannedDurationSeconds.hashCode) +
        (plannedEnd == null ? 0 : plannedEnd.hashCode) +
        (plannedStart == null ? 0 : plannedStart.hashCode) +
        shiftEnd.hashCode +
        shiftStart.hashCode +
        start.hashCode +
        status.hashCode +
        stops.hashCode +
        vehicle.hashCode;

  factory PlanRouteResponse.fromJson(Map<String, dynamic> json) => _$PlanRouteResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanRouteResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum PlanRouteResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED');

const PlanRouteResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


