//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'assignment_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AssignmentRequest {
  /// Returns a new [AssignmentRequest] instance.
  AssignmentRequest({

    required  this.driverId,

     this.maxDistanceMeters,

    required  this.shiftEnd,

    required  this.shiftStart,

    required  this.vehicleId,
  });

  @JsonKey(
    
    name: r'driverId',
    required: true,
    includeIfNull: false,
  )


  final int driverId;



  @JsonKey(
    
    name: r'maxDistanceMeters',
    required: false,
    includeIfNull: false,
  )


  final int? maxDistanceMeters;



  @JsonKey(
    
    name: r'shiftEnd',
    required: true,
    includeIfNull: false,
  )


  final String shiftEnd;



  @JsonKey(
    
    name: r'shiftStart',
    required: true,
    includeIfNull: false,
  )


  final String shiftStart;



  @JsonKey(
    
    name: r'vehicleId',
    required: true,
    includeIfNull: false,
  )


  final int vehicleId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AssignmentRequest &&
      other.driverId == driverId &&
      other.maxDistanceMeters == maxDistanceMeters &&
      other.shiftEnd == shiftEnd &&
      other.shiftStart == shiftStart &&
      other.vehicleId == vehicleId;

    @override
    int get hashCode =>
        driverId.hashCode +
        maxDistanceMeters.hashCode +
        shiftEnd.hashCode +
        shiftStart.hashCode +
        vehicleId.hashCode;

  factory AssignmentRequest.fromJson(Map<String, dynamic> json) => _$AssignmentRequestFromJson(json);

  Map<String, dynamic> toJson() => _$AssignmentRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

