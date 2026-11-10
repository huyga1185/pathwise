//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/driver_position.dart';
import 'package:pathwise_api/src/model/driver_sharing.dart';
import 'package:json_annotation/json_annotation.dart';

part 'driver_location_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverLocationResponse {
  /// Returns a new [DriverLocationResponse] instance.
  DriverLocationResponse({

    required  this.driverId,

    required  this.location,

    required  this.routeId,

    required  this.sharing,
  });

  @JsonKey(
    
    name: r'driverId',
    required: true,
    includeIfNull: false,
  )


  final int driverId;



  @JsonKey(
    
    name: r'location',
    required: true,
    includeIfNull: true,
  )


  final DriverPosition? location;



  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



  @JsonKey(
    
    name: r'sharing',
    required: true,
    includeIfNull: true,
  )


  final DriverSharing? sharing;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverLocationResponse &&
      other.driverId == driverId &&
      other.location == location &&
      other.routeId == routeId &&
      other.sharing == sharing;

    @override
    int get hashCode =>
        driverId.hashCode +
        (location == null ? 0 : location.hashCode) +
        routeId.hashCode +
        (sharing == null ? 0 : sharing.hashCode);

  factory DriverLocationResponse.fromJson(Map<String, dynamic> json) => _$DriverLocationResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DriverLocationResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

