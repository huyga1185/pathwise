//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'skipped_assignment_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SkippedAssignmentResponse {
  /// Returns a new [SkippedAssignmentResponse] instance.
  SkippedAssignmentResponse({

    required  this.driverId,

    required  this.reason,

    required  this.vehicleId,
  });

  @JsonKey(
    
    name: r'driverId',
    required: true,
    includeIfNull: false,
  )


  final int driverId;



  @JsonKey(
    
    name: r'reason',
    required: true,
    includeIfNull: false,
  )


  final SkippedAssignmentResponseReasonEnum reason;



  @JsonKey(
    
    name: r'vehicleId',
    required: true,
    includeIfNull: false,
  )


  final int vehicleId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SkippedAssignmentResponse &&
      other.driverId == driverId &&
      other.reason == reason &&
      other.vehicleId == vehicleId;

    @override
    int get hashCode =>
        driverId.hashCode +
        reason.hashCode +
        vehicleId.hashCode;

  factory SkippedAssignmentResponse.fromJson(Map<String, dynamic> json) => _$SkippedAssignmentResponseFromJson(json);

  Map<String, dynamic> toJson() => _$SkippedAssignmentResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SkippedAssignmentResponseReasonEnum {
@JsonValue(r'DRIVER_UNAVAILABLE')
DRIVER_UNAVAILABLE(r'DRIVER_UNAVAILABLE'),
@JsonValue(r'VEHICLE_INACTIVE')
VEHICLE_INACTIVE(r'VEHICLE_INACTIVE');

const SkippedAssignmentResponseReasonEnum(this.value);

final String value;

@override
String toString() => value;
}


