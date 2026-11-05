//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/plan_route_response.dart';
import 'package:pathwise_api/src/model/job_response.dart';
import 'package:pathwise_api/src/model/geocoding_summary_response.dart';
import 'package:pathwise_api/src/model/location_response.dart';
import 'package:pathwise_api/src/model/plan_stop_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_detail_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanDetailResponse {
  /// Returns a new [PlanDetailResponse] instance.
  PlanDetailResponse({

    required  this.activeJob,

    required  this.depot,

    required  this.geocodingSummary,

    required  this.id,

    required  this.name,

    required  this.optimizedAt,

    required  this.planDate,

    required  this.processingStatus,

    required  this.routes,

    required  this.status,

    required  this.timezone,

    required  this.totalDistanceMeters,

    required  this.totalDurationSeconds,

    required  this.unassignedStops,

    required  this.version,
  });

  @JsonKey(
    
    name: r'activeJob',
    required: true,
    includeIfNull: true,
  )


  final JobResponse? activeJob;



  @JsonKey(
    
    name: r'depot',
    required: true,
    includeIfNull: false,
  )


  final LocationResponse depot;



  @JsonKey(
    
    name: r'geocodingSummary',
    required: true,
    includeIfNull: false,
  )


  final GeocodingSummaryResponse geocodingSummary;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'optimizedAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? optimizedAt;



  @JsonKey(
    
    name: r'planDate',
    required: true,
    includeIfNull: false,
  )


  final DateTime planDate;



  @JsonKey(
    
    name: r'processingStatus',
    required: true,
    includeIfNull: false,
  )


  final PlanDetailResponseProcessingStatusEnum processingStatus;



  @JsonKey(
    
    name: r'routes',
    required: true,
    includeIfNull: false,
  )


  final List<PlanRouteResponse> routes;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final PlanDetailResponseStatusEnum status;



  @JsonKey(
    
    name: r'timezone',
    required: true,
    includeIfNull: false,
  )


  final String timezone;



  @JsonKey(
    
    name: r'totalDistanceMeters',
    required: true,
    includeIfNull: true,
  )


  final int? totalDistanceMeters;



  @JsonKey(
    
    name: r'totalDurationSeconds',
    required: true,
    includeIfNull: true,
  )


  final int? totalDurationSeconds;



  @JsonKey(
    
    name: r'unassignedStops',
    required: true,
    includeIfNull: false,
  )


  final List<PlanStopResponse> unassignedStops;



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanDetailResponse &&
      other.activeJob == activeJob &&
      other.depot == depot &&
      other.geocodingSummary == geocodingSummary &&
      other.id == id &&
      other.name == name &&
      other.optimizedAt == optimizedAt &&
      other.planDate == planDate &&
      other.processingStatus == processingStatus &&
      other.routes == routes &&
      other.status == status &&
      other.timezone == timezone &&
      other.totalDistanceMeters == totalDistanceMeters &&
      other.totalDurationSeconds == totalDurationSeconds &&
      other.unassignedStops == unassignedStops &&
      other.version == version;

    @override
    int get hashCode =>
        (activeJob == null ? 0 : activeJob.hashCode) +
        depot.hashCode +
        geocodingSummary.hashCode +
        id.hashCode +
        name.hashCode +
        (optimizedAt == null ? 0 : optimizedAt.hashCode) +
        planDate.hashCode +
        processingStatus.hashCode +
        routes.hashCode +
        status.hashCode +
        timezone.hashCode +
        (totalDistanceMeters == null ? 0 : totalDistanceMeters.hashCode) +
        (totalDurationSeconds == null ? 0 : totalDurationSeconds.hashCode) +
        unassignedStops.hashCode +
        version.hashCode;

  factory PlanDetailResponse.fromJson(Map<String, dynamic> json) => _$PlanDetailResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanDetailResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum PlanDetailResponseProcessingStatusEnum {
@JsonValue(r'IDLE')
IDLE(r'IDLE'),
@JsonValue(r'GEOCODING')
GEOCODING(r'GEOCODING'),
@JsonValue(r'OPTIMIZING')
OPTIMIZING(r'OPTIMIZING');

const PlanDetailResponseProcessingStatusEnum(this.value);

final String value;

@override
String toString() => value;
}



enum PlanDetailResponseStatusEnum {
@JsonValue(r'DRAFT')
DRAFT(r'DRAFT'),
@JsonValue(r'PUBLISHED')
PUBLISHED(r'PUBLISHED'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED'),
@JsonValue(r'CANCELLED')
CANCELLED(r'CANCELLED');

const PlanDetailResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


