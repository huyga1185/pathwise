//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'depot_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DepotRequest {
  /// Returns a new [DepotRequest] instance.
  DepotRequest({

     this.address,

    required  this.lat,

    required  this.lon,
  });

  @JsonKey(
    
    name: r'address',
    required: false,
    includeIfNull: false,
  )


  final String? address;



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





    @override
    bool operator ==(Object other) => identical(this, other) || other is DepotRequest &&
      other.address == address &&
      other.lat == lat &&
      other.lon == lon;

    @override
    int get hashCode =>
        address.hashCode +
        lat.hashCode +
        lon.hashCode;

  factory DepotRequest.fromJson(Map<String, dynamic> json) => _$DepotRequestFromJson(json);

  Map<String, dynamic> toJson() => _$DepotRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

