//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'plan_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanSummaryResponse {
  /// Returns a new [PlanSummaryResponse] instance.
  PlanSummaryResponse({

    required  this.createdAt,

    required  this.id,

    required  this.name,

    required  this.optimizedAt,

    required  this.planDate,

    required  this.processingStatus,

    required  this.routeCount,

    required  this.status,

    required  this.stopCount,

    required  this.totalDistanceMeters,

    required  this.totalDurationSeconds,

    required  this.version,
  });

  @JsonKey(
    
    name: r'createdAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime createdAt;



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


  final String planDate;



  @JsonKey(
    
    name: r'processingStatus',
    required: true,
    includeIfNull: false,
  )


  final PlanSummaryResponseProcessingStatusEnum processingStatus;



  @JsonKey(
    
    name: r'routeCount',
    required: true,
    includeIfNull: false,
  )


  final int routeCount;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final PlanSummaryResponseStatusEnum status;



  @JsonKey(
    
    name: r'stopCount',
    required: true,
    includeIfNull: false,
  )


  final int stopCount;



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
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanSummaryResponse &&
      other.createdAt == createdAt &&
      other.id == id &&
      other.name == name &&
      other.optimizedAt == optimizedAt &&
      other.planDate == planDate &&
      other.processingStatus == processingStatus &&
      other.routeCount == routeCount &&
      other.status == status &&
      other.stopCount == stopCount &&
      other.totalDistanceMeters == totalDistanceMeters &&
      other.totalDurationSeconds == totalDurationSeconds &&
      other.version == version;

    @override
    int get hashCode =>
        createdAt.hashCode +
        id.hashCode +
        name.hashCode +
        (optimizedAt == null ? 0 : optimizedAt.hashCode) +
        planDate.hashCode +
        processingStatus.hashCode +
        routeCount.hashCode +
        status.hashCode +
        stopCount.hashCode +
        (totalDistanceMeters == null ? 0 : totalDistanceMeters.hashCode) +
        (totalDurationSeconds == null ? 0 : totalDurationSeconds.hashCode) +
        version.hashCode;

  factory PlanSummaryResponse.fromJson(Map<String, dynamic> json) => _$PlanSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum PlanSummaryResponseProcessingStatusEnum {
@JsonValue(r'IDLE')
IDLE(r'IDLE'),
@JsonValue(r'GEOCODING')
GEOCODING(r'GEOCODING'),
@JsonValue(r'OPTIMIZING')
OPTIMIZING(r'OPTIMIZING');

const PlanSummaryResponseProcessingStatusEnum(this.value);

final String value;

@override
String toString() => value;
}



enum PlanSummaryResponseStatusEnum {
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

const PlanSummaryResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


