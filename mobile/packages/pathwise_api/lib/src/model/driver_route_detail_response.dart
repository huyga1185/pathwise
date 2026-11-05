//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/driver_stop_response.dart';
import 'package:pathwise_api/src/model/location_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'driver_route_detail_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverRouteDetailResponse {
  /// Returns a new [DriverRouteDetailResponse] instance.
  DriverRouteDetailResponse({

    required  this.end,

    required  this.planDate,

    required  this.planId,

    required  this.planName,

    required  this.routeId,

    required  this.start,

    required  this.status,

    required  this.stops,
  });

  @JsonKey(
    
    name: r'end',
    required: true,
    includeIfNull: false,
  )


  final LocationResponse end;



  @JsonKey(
    
    name: r'planDate',
    required: true,
    includeIfNull: false,
  )


  final DateTime planDate;



  @JsonKey(
    
    name: r'planId',
    required: true,
    includeIfNull: false,
  )


  final int planId;



  @JsonKey(
    
    name: r'planName',
    required: true,
    includeIfNull: false,
  )


  final String planName;



  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



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


  final DriverRouteDetailResponseStatusEnum status;



  @JsonKey(
    
    name: r'stops',
    required: true,
    includeIfNull: false,
  )


  final List<DriverStopResponse> stops;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverRouteDetailResponse &&
      other.end == end &&
      other.planDate == planDate &&
      other.planId == planId &&
      other.planName == planName &&
      other.routeId == routeId &&
      other.start == start &&
      other.status == status &&
      other.stops == stops;

    @override
    int get hashCode =>
        end.hashCode +
        planDate.hashCode +
        planId.hashCode +
        planName.hashCode +
        routeId.hashCode +
        start.hashCode +
        status.hashCode +
        stops.hashCode;

  factory DriverRouteDetailResponse.fromJson(Map<String, dynamic> json) => _$DriverRouteDetailResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DriverRouteDetailResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverRouteDetailResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED');

const DriverRouteDetailResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


