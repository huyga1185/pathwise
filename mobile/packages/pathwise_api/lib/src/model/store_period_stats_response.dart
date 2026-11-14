//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'store_period_stats_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class StorePeriodStatsResponse {
  /// Returns a new [StorePeriodStatsResponse] instance.
  StorePeriodStatsResponse({

    required  this.deliveredRate,

    required  this.plansCompleted,

    required  this.plansCreated,

    required  this.plansPublished,

    required  this.since,

    required  this.stopsDelivered,

    required  this.stopsFailed,

    required  this.stopsSkipped,
  });

  @JsonKey(
    
    name: r'deliveredRate',
    required: true,
    includeIfNull: true,
  )


  final double? deliveredRate;



  @JsonKey(
    
    name: r'plansCompleted',
    required: true,
    includeIfNull: false,
  )


  final int plansCompleted;



  @JsonKey(
    
    name: r'plansCreated',
    required: true,
    includeIfNull: false,
  )


  final int plansCreated;



  @JsonKey(
    
    name: r'plansPublished',
    required: true,
    includeIfNull: false,
  )


  final int plansPublished;



  @JsonKey(
    
    name: r'since',
    required: true,
    includeIfNull: false,
  )


  final DateTime since;



  @JsonKey(
    
    name: r'stopsDelivered',
    required: true,
    includeIfNull: false,
  )


  final int stopsDelivered;



  @JsonKey(
    
    name: r'stopsFailed',
    required: true,
    includeIfNull: false,
  )


  final int stopsFailed;



  @JsonKey(
    
    name: r'stopsSkipped',
    required: true,
    includeIfNull: false,
  )


  final int stopsSkipped;





    @override
    bool operator ==(Object other) => identical(this, other) || other is StorePeriodStatsResponse &&
      other.deliveredRate == deliveredRate &&
      other.plansCompleted == plansCompleted &&
      other.plansCreated == plansCreated &&
      other.plansPublished == plansPublished &&
      other.since == since &&
      other.stopsDelivered == stopsDelivered &&
      other.stopsFailed == stopsFailed &&
      other.stopsSkipped == stopsSkipped;

    @override
    int get hashCode =>
        (deliveredRate == null ? 0 : deliveredRate.hashCode) +
        plansCompleted.hashCode +
        plansCreated.hashCode +
        plansPublished.hashCode +
        since.hashCode +
        stopsDelivered.hashCode +
        stopsFailed.hashCode +
        stopsSkipped.hashCode;

  factory StorePeriodStatsResponse.fromJson(Map<String, dynamic> json) => _$StorePeriodStatsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$StorePeriodStatsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

