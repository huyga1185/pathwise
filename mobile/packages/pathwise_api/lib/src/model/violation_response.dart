//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'violation_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ViolationResponse {
  /// Returns a new [ViolationResponse] instance.
  ViolationResponse({

    required  this.code,

    required  this.excess,

    required  this.routeId,

    required  this.stopId,
  });

  @JsonKey(
    
    name: r'code',
    required: true,
    includeIfNull: false,
  )


  final ViolationResponseCodeEnum code;



  @JsonKey(
    
    name: r'excess',
    required: true,
    includeIfNull: false,
  )


  final int excess;



  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



  @JsonKey(
    
    name: r'stopId',
    required: true,
    includeIfNull: true,
  )


  final int? stopId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ViolationResponse &&
      other.code == code &&
      other.excess == excess &&
      other.routeId == routeId &&
      other.stopId == stopId;

    @override
    int get hashCode =>
        code.hashCode +
        excess.hashCode +
        routeId.hashCode +
        (stopId == null ? 0 : stopId.hashCode);

  factory ViolationResponse.fromJson(Map<String, dynamic> json) => _$ViolationResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ViolationResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ViolationResponseCodeEnum {
@JsonValue(r'CAPACITY_EXCEEDED')
CAPACITY_EXCEEDED(r'CAPACITY_EXCEEDED'),
@JsonValue(r'TIME_WINDOW_MISSED')
TIME_WINDOW_MISSED(r'TIME_WINDOW_MISSED'),
@JsonValue(r'SHIFT_EXCEEDED')
SHIFT_EXCEEDED(r'SHIFT_EXCEEDED'),
@JsonValue(r'MAX_DISTANCE_EXCEEDED')
MAX_DISTANCE_EXCEEDED(r'MAX_DISTANCE_EXCEEDED');

const ViolationResponseCodeEnum(this.value);

final String value;

@override
String toString() => value;
}


