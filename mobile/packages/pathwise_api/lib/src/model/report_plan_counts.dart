//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'report_plan_counts.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReportPlanCounts {
  /// Returns a new [ReportPlanCounts] instance.
  ReportPlanCounts({

    required  this.cancelled,

    required  this.completed,

    required  this.draft,

    required  this.inProgress,

    required  this.published,

    required  this.total,
  });

  @JsonKey(
    
    name: r'cancelled',
    required: true,
    includeIfNull: false,
  )


  final int cancelled;



  @JsonKey(
    
    name: r'completed',
    required: true,
    includeIfNull: false,
  )


  final int completed;



  @JsonKey(
    
    name: r'draft',
    required: true,
    includeIfNull: false,
  )


  final int draft;



  @JsonKey(
    
    name: r'inProgress',
    required: true,
    includeIfNull: false,
  )


  final int inProgress;



  @JsonKey(
    
    name: r'published',
    required: true,
    includeIfNull: false,
  )


  final int published;



  @JsonKey(
    
    name: r'total',
    required: true,
    includeIfNull: false,
  )


  final int total;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReportPlanCounts &&
      other.cancelled == cancelled &&
      other.completed == completed &&
      other.draft == draft &&
      other.inProgress == inProgress &&
      other.published == published &&
      other.total == total;

    @override
    int get hashCode =>
        cancelled.hashCode +
        completed.hashCode +
        draft.hashCode +
        inProgress.hashCode +
        published.hashCode +
        total.hashCode;

  factory ReportPlanCounts.fromJson(Map<String, dynamic> json) => _$ReportPlanCountsFromJson(json);

  Map<String, dynamic> toJson() => _$ReportPlanCountsToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

