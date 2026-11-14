//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/skipped_assignment_response.dart';
import 'package:pathwise_api/src/model/plan_detail_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_copy_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanCopyResponse {
  /// Returns a new [PlanCopyResponse] instance.
  PlanCopyResponse({

    required  this.plan,

    required  this.skippedAssignments,
  });

  @JsonKey(
    
    name: r'plan',
    required: true,
    includeIfNull: false,
  )


  final PlanDetailResponse plan;



  @JsonKey(
    
    name: r'skippedAssignments',
    required: true,
    includeIfNull: false,
  )


  final List<SkippedAssignmentResponse> skippedAssignments;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanCopyResponse &&
      other.plan == plan &&
      other.skippedAssignments == skippedAssignments;

    @override
    int get hashCode =>
        plan.hashCode +
        skippedAssignments.hashCode;

  factory PlanCopyResponse.fromJson(Map<String, dynamic> json) => _$PlanCopyResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanCopyResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

