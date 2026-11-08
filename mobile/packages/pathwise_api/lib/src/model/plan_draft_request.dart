//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/depot_request.dart';
import 'package:pathwise_api/src/model/plan_stop_request.dart';
import 'package:pathwise_api/src/model/assignment_request.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_draft_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanDraftRequest {
  /// Returns a new [PlanDraftRequest] instance.
  PlanDraftRequest({

     this.assignments,

     this.depot,

    required  this.name,

    required  this.planDate,

     this.stops,

    required  this.version,
  });

  @JsonKey(
    
    name: r'assignments',
    required: false,
    includeIfNull: false,
  )


  final List<AssignmentRequest>? assignments;



  @JsonKey(
    
    name: r'depot',
    required: false,
    includeIfNull: false,
  )


  final DepotRequest? depot;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'planDate',
    required: true,
    includeIfNull: false,
  )


  final String planDate;



  @JsonKey(
    
    name: r'stops',
    required: false,
    includeIfNull: false,
  )


  final List<PlanStopRequest>? stops;



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanDraftRequest &&
      other.assignments == assignments &&
      other.depot == depot &&
      other.name == name &&
      other.planDate == planDate &&
      other.stops == stops &&
      other.version == version;

    @override
    int get hashCode =>
        assignments.hashCode +
        depot.hashCode +
        name.hashCode +
        planDate.hashCode +
        stops.hashCode +
        version.hashCode;

  factory PlanDraftRequest.fromJson(Map<String, dynamic> json) => _$PlanDraftRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PlanDraftRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

