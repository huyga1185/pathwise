//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'location_point_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LocationPointRequest {
  /// Returns a new [LocationPointRequest] instance.
  LocationPointRequest({

     this.accuracyMeters,

     this.headingDegrees,

    required  this.lat,

    required  this.lon,

    required  this.recordedAt,

     this.speedMps,
  });

          // minimum: 0
  @JsonKey(
    
    name: r'accuracyMeters',
    required: false,
    includeIfNull: false,
  )


  final double? accuracyMeters;



          // minimum: 0
  @JsonKey(
    
    name: r'headingDegrees',
    required: false,
    includeIfNull: false,
  )


  final double? headingDegrees;



          // minimum: -90
          // maximum: 90
  @JsonKey(
    
    name: r'lat',
    required: true,
    includeIfNull: false,
  )


  final double lat;



          // minimum: -180
          // maximum: 180
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



          // minimum: 0
  @JsonKey(
    
    name: r'speedMps',
    required: false,
    includeIfNull: false,
  )


  final double? speedMps;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LocationPointRequest &&
      other.accuracyMeters == accuracyMeters &&
      other.headingDegrees == headingDegrees &&
      other.lat == lat &&
      other.lon == lon &&
      other.recordedAt == recordedAt &&
      other.speedMps == speedMps;

    @override
    int get hashCode =>
        accuracyMeters.hashCode +
        headingDegrees.hashCode +
        lat.hashCode +
        lon.hashCode +
        recordedAt.hashCode +
        speedMps.hashCode;

  factory LocationPointRequest.fromJson(Map<String, dynamic> json) => _$LocationPointRequestFromJson(json);

  Map<String, dynamic> toJson() => _$LocationPointRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

