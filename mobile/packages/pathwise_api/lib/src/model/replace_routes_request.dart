//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/route_stops.dart';
import 'package:json_annotation/json_annotation.dart';

part 'replace_routes_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReplaceRoutesRequest {
  /// Returns a new [ReplaceRoutesRequest] instance.
  ReplaceRoutesRequest({

    required  this.routes,

    required  this.unassignedStopIds,

    required  this.version,
  });

  @JsonKey(
    
    name: r'routes',
    required: true,
    includeIfNull: false,
  )


  final List<RouteStops> routes;



  @JsonKey(
    
    name: r'unassignedStopIds',
    required: true,
    includeIfNull: false,
  )


  final List<int> unassignedStopIds;



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReplaceRoutesRequest &&
      other.routes == routes &&
      other.unassignedStopIds == unassignedStopIds &&
      other.version == version;

    @override
    int get hashCode =>
        routes.hashCode +
        unassignedStopIds.hashCode +
        version.hashCode;

  factory ReplaceRoutesRequest.fromJson(Map<String, dynamic> json) => _$ReplaceRoutesRequestFromJson(json);

  Map<String, dynamic> toJson() => _$ReplaceRoutesRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

