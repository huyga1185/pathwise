//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'driver_route_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverRouteSummaryResponse {
  /// Returns a new [DriverRouteSummaryResponse] instance.
  DriverRouteSummaryResponse({

    required  this.completedCount,

    required  this.planDate,

    required  this.planId,

    required  this.planName,

    required  this.plannedStart,

    required  this.routeId,

    required  this.status,

    required  this.stopCount,

    required  this.timezone,
  });

  @JsonKey(
    
    name: r'completedCount',
    required: true,
    includeIfNull: false,
  )


  final int completedCount;



  @JsonKey(
    
    name: r'planDate',
    required: true,
    includeIfNull: false,
  )


  final DateTime planDate;



  @JsonKey(
    
    name: r'planId',
    required: true,
    includeIfNull: false,
  )


  final int planId;



  @JsonKey(
    
    name: r'planName',
    required: true,
    includeIfNull: false,
  )


  final String planName;



  @JsonKey(
    
    name: r'plannedStart',
    required: true,
    includeIfNull: true,
  )


  final DateTime? plannedStart;



  @JsonKey(
    
    name: r'routeId',
    required: true,
    includeIfNull: false,
  )


  final int routeId;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final DriverRouteSummaryResponseStatusEnum status;



  @JsonKey(
    
    name: r'stopCount',
    required: true,
    includeIfNull: false,
  )


  final int stopCount;



  @JsonKey(
    
    name: r'timezone',
    required: true,
    includeIfNull: false,
  )


  final String timezone;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverRouteSummaryResponse &&
      other.completedCount == completedCount &&
      other.planDate == planDate &&
      other.planId == planId &&
      other.planName == planName &&
      other.plannedStart == plannedStart &&
      other.routeId == routeId &&
      other.status == status &&
      other.stopCount == stopCount &&
      other.timezone == timezone;

    @override
    int get hashCode =>
        completedCount.hashCode +
        planDate.hashCode +
        planId.hashCode +
        planName.hashCode +
        (plannedStart == null ? 0 : plannedStart.hashCode) +
        routeId.hashCode +
        status.hashCode +
        stopCount.hashCode +
        timezone.hashCode;

  factory DriverRouteSummaryResponse.fromJson(Map<String, dynamic> json) => _$DriverRouteSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DriverRouteSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverRouteSummaryResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED');

const DriverRouteSummaryResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


