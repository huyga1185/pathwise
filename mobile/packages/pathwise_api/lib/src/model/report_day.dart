//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'report_day.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReportDay {
  /// Returns a new [ReportDay] instance.
  ReportDay({

    required  this.date,

    required  this.delivered,

    required  this.failed,

    required  this.pending,

    required  this.plans,

    required  this.skipped,
  });

  @JsonKey(
    
    name: r'date',
    required: true,
    includeIfNull: false,
  )


  final String date;



  @JsonKey(
    
    name: r'delivered',
    required: true,
    includeIfNull: false,
  )


  final int delivered;



  @JsonKey(
    
    name: r'failed',
    required: true,
    includeIfNull: false,
  )


  final int failed;



  @JsonKey(
    
    name: r'pending',
    required: true,
    includeIfNull: false,
  )


  final int pending;



  @JsonKey(
    
    name: r'plans',
    required: true,
    includeIfNull: false,
  )


  final int plans;



  @JsonKey(
    
    name: r'skipped',
    required: true,
    includeIfNull: false,
  )


  final int skipped;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReportDay &&
      other.date == date &&
      other.delivered == delivered &&
      other.failed == failed &&
      other.pending == pending &&
      other.plans == plans &&
      other.skipped == skipped;

    @override
    int get hashCode =>
        date.hashCode +
        delivered.hashCode +
        failed.hashCode +
        pending.hashCode +
        plans.hashCode +
        skipped.hashCode;

  factory ReportDay.fromJson(Map<String, dynamic> json) => _$ReportDayFromJson(json);

  Map<String, dynamic> toJson() => _$ReportDayToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

