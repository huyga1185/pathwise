//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'place_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlaceResponse {
  /// Returns a new [PlaceResponse] instance.
  PlaceResponse({

    required  this.displayName,

    required  this.lat,

    required  this.lon,
  });

  @JsonKey(
    
    name: r'displayName',
    required: true,
    includeIfNull: false,
  )


  final String displayName;



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
    bool operator ==(Object other) => identical(this, other) || other is PlaceResponse &&
      other.displayName == displayName &&
      other.lat == lat &&
      other.lon == lon;

    @override
    int get hashCode =>
        displayName.hashCode +
        lat.hashCode +
        lon.hashCode;

  factory PlaceResponse.fromJson(Map<String, dynamic> json) => _$PlaceResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlaceResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

