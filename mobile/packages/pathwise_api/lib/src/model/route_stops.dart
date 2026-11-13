//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'route_stops.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RouteStops {
  /// Returns a new [RouteStops] instance.
  RouteStops({

    required  this.routeId,

    required  this.stopIds,
  });

  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



  @JsonKey(
    
    name: r'stopIds',
    required: true,
    includeIfNull: false,
  )


  final List<int> stopIds;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RouteStops &&
      other.routeId == routeId &&
      other.stopIds == stopIds;

    @override
    int get hashCode =>
        routeId.hashCode +
        stopIds.hashCode;

  factory RouteStops.fromJson(Map<String, dynamic> json) => _$RouteStopsFromJson(json);

  Map<String, dynamic> toJson() => _$RouteStopsToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

