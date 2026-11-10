//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_location_sharing_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateLocationSharingRequest {
  /// Returns a new [UpdateLocationSharingRequest] instance.
  UpdateLocationSharingRequest({

    required  this.routeId,

    required  this.state,
  });

  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



  @JsonKey(
    
    name: r'state',
    required: true,
    includeIfNull: false,
  )


  final UpdateLocationSharingRequestStateEnum state;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateLocationSharingRequest &&
      other.routeId == routeId &&
      other.state == state;

    @override
    int get hashCode =>
        routeId.hashCode +
        state.hashCode;

  factory UpdateLocationSharingRequest.fromJson(Map<String, dynamic> json) => _$UpdateLocationSharingRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateLocationSharingRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum UpdateLocationSharingRequestStateEnum {
@JsonValue(r'SHARING')
SHARING(r'SHARING'),
@JsonValue(r'DECLINED')
DECLINED(r'DECLINED'),
@JsonValue(r'NO_PERMISSION')
NO_PERMISSION(r'NO_PERMISSION'),
@JsonValue(r'GPS_OFF')
GPS_OFF(r'GPS_OFF');

const UpdateLocationSharingRequestStateEnum(this.value);

final String value;

@override
String toString() => value;
}


