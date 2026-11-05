//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'depot_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DepotResponse {
  /// Returns a new [DepotResponse] instance.
  DepotResponse({

    required  this.address,

    required  this.lat,

    required  this.lon,
  });

  @JsonKey(
    
    name: r'address',
    required: true,
    includeIfNull: true,
  )


  final String? address;



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
    bool operator ==(Object other) => identical(this, other) || other is DepotResponse &&
      other.address == address &&
      other.lat == lat &&
      other.lon == lon;

    @override
    int get hashCode =>
        (address == null ? 0 : address.hashCode) +
        lat.hashCode +
        lon.hashCode;

  factory DepotResponse.fromJson(Map<String, dynamic> json) => _$DepotResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DepotResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

