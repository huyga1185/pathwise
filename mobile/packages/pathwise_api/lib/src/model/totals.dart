//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'totals.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Totals {
  /// Returns a new [Totals] instance.
  Totals({

    required  this.deliveredRate,

    required  this.failedRate,

    required  this.newStores,

    required  this.optimizationFailureRate,

    required  this.optimizations,

    required  this.optimizationsFailed,

    required  this.plansCompleted,

    required  this.plansCreated,

    required  this.plansPublished,

    required  this.skippedRate,

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
    
    name: r'failedRate',
    required: true,
    includeIfNull: true,
  )


  final double? failedRate;



  @JsonKey(
    
    name: r'newStores',
    required: true,
    includeIfNull: false,
  )


  final int newStores;



  @JsonKey(
    
    name: r'optimizationFailureRate',
    required: true,
    includeIfNull: true,
  )


  final double? optimizationFailureRate;



  @JsonKey(
    
    name: r'optimizations',
    required: true,
    includeIfNull: false,
  )


  final int optimizations;



  @JsonKey(
    
    name: r'optimizationsFailed',
    required: true,
    includeIfNull: false,
  )


  final int optimizationsFailed;



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
    
    name: r'skippedRate',
    required: true,
    includeIfNull: true,
  )


  final double? skippedRate;



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
    bool operator ==(Object other) => identical(this, other) || other is Totals &&
      other.deliveredRate == deliveredRate &&
      other.failedRate == failedRate &&
      other.newStores == newStores &&
      other.optimizationFailureRate == optimizationFailureRate &&
      other.optimizations == optimizations &&
      other.optimizationsFailed == optimizationsFailed &&
      other.plansCompleted == plansCompleted &&
      other.plansCreated == plansCreated &&
      other.plansPublished == plansPublished &&
      other.skippedRate == skippedRate &&
      other.stopsDelivered == stopsDelivered &&
      other.stopsFailed == stopsFailed &&
      other.stopsSkipped == stopsSkipped;

    @override
    int get hashCode =>
        (deliveredRate == null ? 0 : deliveredRate.hashCode) +
        (failedRate == null ? 0 : failedRate.hashCode) +
        newStores.hashCode +
        (optimizationFailureRate == null ? 0 : optimizationFailureRate.hashCode) +
        optimizations.hashCode +
        optimizationsFailed.hashCode +
        plansCompleted.hashCode +
        plansCreated.hashCode +
        plansPublished.hashCode +
        (skippedRate == null ? 0 : skippedRate.hashCode) +
        stopsDelivered.hashCode +
        stopsFailed.hashCode +
        stopsSkipped.hashCode;

  factory Totals.fromJson(Map<String, dynamic> json) => _$TotalsFromJson(json);

  Map<String, dynamic> toJson() => _$TotalsToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

