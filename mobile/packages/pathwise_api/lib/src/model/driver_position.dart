//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'driver_position.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverPosition {
  /// Returns a new [DriverPosition] instance.
  DriverPosition({

    required  this.accuracyMeters,

    required  this.headingDegrees,

    required  this.lat,

    required  this.lon,

    required  this.recordedAt,

    required  this.speedMps,
  });

  @JsonKey(
    
    name: r'accuracyMeters',
    required: true,
    includeIfNull: true,
  )


  final double? accuracyMeters;



  @JsonKey(
    
    name: r'headingDegrees',
    required: true,
    includeIfNull: true,
  )


  final double? headingDegrees;



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
    
    name: r'recordedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime recordedAt;



  @JsonKey(
    
    name: r'speedMps',
    required: true,
    includeIfNull: true,
  )


  final double? speedMps;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverPosition &&
      other.accuracyMeters == accuracyMeters &&
      other.headingDegrees == headingDegrees &&
      other.lat == lat &&
      other.lon == lon &&
      other.recordedAt == recordedAt &&
      other.speedMps == speedMps;

    @override
    int get hashCode =>
        (accuracyMeters == null ? 0 : accuracyMeters.hashCode) +
        (headingDegrees == null ? 0 : headingDegrees.hashCode) +
        lat.hashCode +
        lon.hashCode +
        recordedAt.hashCode +
        (speedMps == null ? 0 : speedMps.hashCode);

  factory DriverPosition.fromJson(Map<String, dynamic> json) => _$DriverPositionFromJson(json);

  Map<String, dynamic> toJson() => _$DriverPositionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

