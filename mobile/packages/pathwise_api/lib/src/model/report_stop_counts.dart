//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'report_stop_counts.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReportStopCounts {
  /// Returns a new [ReportStopCounts] instance.
  ReportStopCounts({

    required  this.delivered,

    required  this.deliveredRate,

    required  this.failed,

    required  this.pending,

    required  this.skipped,

    required  this.total,
  });

  @JsonKey(
    
    name: r'delivered',
    required: true,
    includeIfNull: false,
  )


  final int delivered;



  @JsonKey(
    
    name: r'deliveredRate',
    required: true,
    includeIfNull: true,
  )


  final double? deliveredRate;



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
    
    name: r'skipped',
    required: true,
    includeIfNull: false,
  )


  final int skipped;



  @JsonKey(
    
    name: r'total',
    required: true,
    includeIfNull: false,
  )


  final int total;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReportStopCounts &&
      other.delivered == delivered &&
      other.deliveredRate == deliveredRate &&
      other.failed == failed &&
      other.pending == pending &&
      other.skipped == skipped &&
      other.total == total;

    @override
    int get hashCode =>
        delivered.hashCode +
        (deliveredRate == null ? 0 : deliveredRate.hashCode) +
        failed.hashCode +
        pending.hashCode +
        skipped.hashCode +
        total.hashCode;

  factory ReportStopCounts.fromJson(Map<String, dynamic> json) => _$ReportStopCountsFromJson(json);

  Map<String, dynamic> toJson() => _$ReportStopCountsToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

