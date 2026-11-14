//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'directions_point.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DirectionsPoint {
  /// Returns a new [DirectionsPoint] instance.
  DirectionsPoint({

    required  this.lat,

    required  this.lon,
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





    @override
    bool operator ==(Object other) => identical(this, other) || other is DirectionsPoint &&
      other.lat == lat &&
      other.lon == lon;

    @override
    int get hashCode =>
        lat.hashCode +
        lon.hashCode;

  factory DirectionsPoint.fromJson(Map<String, dynamic> json) => _$DirectionsPointFromJson(json);

  Map<String, dynamic> toJson() => _$DirectionsPointToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

