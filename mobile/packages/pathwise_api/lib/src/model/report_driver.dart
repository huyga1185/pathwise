//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'report_driver.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReportDriver {
  /// Returns a new [ReportDriver] instance.
  ReportDriver({

    required  this.delivered,

    required  this.driverId,

    required  this.failed,

    required  this.firstResultAt,

    required  this.fullName,

    required  this.lastResultAt,

    required  this.pending,

    required  this.plannedDistanceMeters,

    required  this.routes,

    required  this.skipped,
  });

  @JsonKey(
    
    name: r'delivered',
    required: true,
    includeIfNull: false,
  )


  final int delivered;



  @JsonKey(
    
    name: r'driverId',
    required: true,
    includeIfNull: false,
  )


  final int driverId;



  @JsonKey(
    
    name: r'failed',
    required: true,
    includeIfNull: false,
  )


  final int failed;



  @JsonKey(
    
    name: r'firstResultAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? firstResultAt;



  @JsonKey(
    
    name: r'fullName',
    required: true,
    includeIfNull: false,
  )


  final String fullName;



  @JsonKey(
    
    name: r'lastResultAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? lastResultAt;



  @JsonKey(
    
    name: r'pending',
    required: true,
    includeIfNull: false,
  )


  final int pending;



  @JsonKey(
    
    name: r'plannedDistanceMeters',
    required: true,
    includeIfNull: false,
  )


  final int plannedDistanceMeters;



  @JsonKey(
    
    name: r'routes',
    required: true,
    includeIfNull: false,
  )


  final int routes;



  @JsonKey(
    
    name: r'skipped',
    required: true,
    includeIfNull: false,
  )


  final int skipped;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReportDriver &&
      other.delivered == delivered &&
      other.driverId == driverId &&
      other.failed == failed &&
      other.firstResultAt == firstResultAt &&
      other.fullName == fullName &&
      other.lastResultAt == lastResultAt &&
      other.pending == pending &&
      other.plannedDistanceMeters == plannedDistanceMeters &&
      other.routes == routes &&
      other.skipped == skipped;

    @override
    int get hashCode =>
        delivered.hashCode +
        driverId.hashCode +
        failed.hashCode +
        (firstResultAt == null ? 0 : firstResultAt.hashCode) +
        fullName.hashCode +
        (lastResultAt == null ? 0 : lastResultAt.hashCode) +
        pending.hashCode +
        plannedDistanceMeters.hashCode +
        routes.hashCode +
        skipped.hashCode;

  factory ReportDriver.fromJson(Map<String, dynamic> json) => _$ReportDriverFromJson(json);

  Map<String, dynamic> toJson() => _$ReportDriverToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

