//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'driver_sharing.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverSharing {
  /// Returns a new [DriverSharing] instance.
  DriverSharing({

    required  this.state,

    required  this.updatedAt,
  });

  @JsonKey(
    
    name: r'state',
    required: true,
    includeIfNull: false,
  )


  final DriverSharingStateEnum state;



  @JsonKey(
    
    name: r'updatedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime updatedAt;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverSharing &&
      other.state == state &&
      other.updatedAt == updatedAt;

    @override
    int get hashCode =>
        state.hashCode +
        updatedAt.hashCode;

  factory DriverSharing.fromJson(Map<String, dynamic> json) => _$DriverSharingFromJson(json);

  Map<String, dynamic> toJson() => _$DriverSharingToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverSharingStateEnum {
@JsonValue(r'SHARING')
SHARING(r'SHARING'),
@JsonValue(r'DECLINED')
DECLINED(r'DECLINED'),
@JsonValue(r'NO_PERMISSION')
NO_PERMISSION(r'NO_PERMISSION'),
@JsonValue(r'GPS_OFF')
GPS_OFF(r'GPS_OFF');

const DriverSharingStateEnum(this.value);

final String value;

@override
String toString() => value;
}


