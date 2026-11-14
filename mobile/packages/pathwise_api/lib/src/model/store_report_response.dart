//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/report_day.dart';
import 'package:pathwise_api/src/model/report_driver.dart';
import 'package:pathwise_api/src/model/report_failure_reason.dart';
import 'package:pathwise_api/src/model/report_stop_counts.dart';
import 'package:pathwise_api/src/model/report_plan_counts.dart';
import 'package:json_annotation/json_annotation.dart';

part 'store_report_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class StoreReportResponse {
  /// Returns a new [StoreReportResponse] instance.
  StoreReportResponse({

    required  this.days,

    required  this.drivers,

    required  this.failureReasons,

    required  this.from,

    required  this.plannedDistanceMeters,

    required  this.plans,

    required  this.stops,

    required  this.to,
  });

  @JsonKey(
    
    name: r'days',
    required: true,
    includeIfNull: false,
  )


  final List<ReportDay> days;



  @JsonKey(
    
    name: r'drivers',
    required: true,
    includeIfNull: false,
  )


  final List<ReportDriver> drivers;



  @JsonKey(
    
    name: r'failureReasons',
    required: true,
    includeIfNull: false,
  )


  final List<ReportFailureReason> failureReasons;



  @JsonKey(
    
    name: r'from',
    required: true,
    includeIfNull: false,
  )


  final String from;



  @JsonKey(
    
    name: r'plannedDistanceMeters',
    required: true,
    includeIfNull: false,
  )


  final int plannedDistanceMeters;



  @JsonKey(
    
    name: r'plans',
    required: true,
    includeIfNull: false,
  )


  final ReportPlanCounts plans;



  @JsonKey(
    
    name: r'stops',
    required: true,
    includeIfNull: false,
  )


  final ReportStopCounts stops;



  @JsonKey(
    
    name: r'to',
    required: true,
    includeIfNull: false,
  )


  final String to;





    @override
    bool operator ==(Object other) => identical(this, other) || other is StoreReportResponse &&
      other.days == days &&
      other.drivers == drivers &&
      other.failureReasons == failureReasons &&
      other.from == from &&
      other.plannedDistanceMeters == plannedDistanceMeters &&
      other.plans == plans &&
      other.stops == stops &&
      other.to == to;

    @override
    int get hashCode =>
        days.hashCode +
        drivers.hashCode +
        failureReasons.hashCode +
        from.hashCode +
        plannedDistanceMeters.hashCode +
        plans.hashCode +
        stops.hashCode +
        to.hashCode;

  factory StoreReportResponse.fromJson(Map<String, dynamic> json) => _$StoreReportResponseFromJson(json);

  Map<String, dynamic> toJson() => _$StoreReportResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

