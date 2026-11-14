//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'directions_destination.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DirectionsDestination {
  /// Returns a new [DirectionsDestination] instance.
  DirectionsDestination({

    required  this.lat,

    required  this.lon,

    required  this.stopId,
  });

  @JsonKey(
    
    name: r'lat',
    required: true,
    includeIfNull: false,
  )


  final double lat;



  @JsonKey(
    
    name: r'lon',
    required: true,
    includeIfNull: false,
  )


  final double lon;



  @JsonKey(
    
    name: r'stopId',
    required: true,
    includeIfNull: true,
  )


  final int? stopId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DirectionsDestination &&
      other.lat == lat &&
      other.lon == lon &&
      other.stopId == stopId;

    @override
    int get hashCode =>
        lat.hashCode +
        lon.hashCode +
        (stopId == null ? 0 : stopId.hashCode);

  factory DirectionsDestination.fromJson(Map<String, dynamic> json) => _$DirectionsDestinationFromJson(json);

  Map<String, dynamic> toJson() => _$DirectionsDestinationToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

