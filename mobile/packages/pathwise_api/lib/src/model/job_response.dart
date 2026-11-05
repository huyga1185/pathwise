//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'job_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class JobResponse {
  /// Returns a new [JobResponse] instance.
  JobResponse({

    required  this.createdAt,

    required  this.errorCode,

    required  this.finishedAt,

    required  this.id,

    required  this.planId,

    required  this.startedAt,

    required  this.status,

    required  this.type,
  });

  @JsonKey(
    
    name: r'createdAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime createdAt;



  @JsonKey(
    
    name: r'errorCode',
    required: true,
    includeIfNull: true,
  )


  final String? errorCode;



  @JsonKey(
    
    name: r'finishedAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? finishedAt;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'planId',
    required: true,
    includeIfNull: false,
  )


  final int planId;



  @JsonKey(
    
    name: r'startedAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? startedAt;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final JobResponseStatusEnum status;



  @JsonKey(
    
    name: r'type',
    required: true,
    includeIfNull: false,
  )


  final JobResponseTypeEnum type;





    @override
    bool operator ==(Object other) => identical(this, other) || other is JobResponse &&
      other.createdAt == createdAt &&
      other.errorCode == errorCode &&
      other.finishedAt == finishedAt &&
      other.id == id &&
      other.planId == planId &&
      other.startedAt == startedAt &&
      other.status == status &&
      other.type == type;

    @override
    int get hashCode =>
        createdAt.hashCode +
        (errorCode == null ? 0 : errorCode.hashCode) +
        (finishedAt == null ? 0 : finishedAt.hashCode) +
        id.hashCode +
        planId.hashCode +
        (startedAt == null ? 0 : startedAt.hashCode) +
        status.hashCode +
        type.hashCode;

  factory JobResponse.fromJson(Map<String, dynamic> json) => _$JobResponseFromJson(json);

  Map<String, dynamic> toJson() => _$JobResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum JobResponseStatusEnum {
@JsonValue(r'QUEUED')
QUEUED(r'QUEUED'),
@JsonValue(r'RUNNING')
RUNNING(r'RUNNING'),
@JsonValue(r'SUCCEEDED')
SUCCEEDED(r'SUCCEEDED'),
@JsonValue(r'FAILED')
FAILED(r'FAILED');

const JobResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}



enum JobResponseTypeEnum {
@JsonValue(r'GEOCODE_PLAN')
GEOCODE_PLAN(r'GEOCODE_PLAN'),
@JsonValue(r'OPTIMIZE_PLAN')
OPTIMIZE_PLAN(r'OPTIMIZE_PLAN');

const JobResponseTypeEnum(this.value);

final String value;

@override
String toString() => value;
}


