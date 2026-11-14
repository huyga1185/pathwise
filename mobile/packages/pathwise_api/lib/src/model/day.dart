//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'day.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Day {
  /// Returns a new [Day] instance.
  Day({

    required  this.date,

    required  this.newStores,

    required  this.optimizations,

    required  this.optimizationsFailed,

    required  this.plansCompleted,

    required  this.plansCreated,

    required  this.plansPublished,

    required  this.stopsDelivered,

    required  this.stopsFailed,

    required  this.stopsSkipped,
  });

  @JsonKey(
    
    name: r'date',
    required: true,
    includeIfNull: false,
  )


  final String date;



  @JsonKey(
    
    name: r'newStores',
    required: true,
    includeIfNull: false,
  )


  final int newStores;



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
    bool operator ==(Object other) => identical(this, other) || other is Day &&
      other.date == date &&
      other.newStores == newStores &&
      other.optimizations == optimizations &&
      other.optimizationsFailed == optimizationsFailed &&
      other.plansCompleted == plansCompleted &&
      other.plansCreated == plansCreated &&
      other.plansPublished == plansPublished &&
      other.stopsDelivered == stopsDelivered &&
      other.stopsFailed == stopsFailed &&
      other.stopsSkipped == stopsSkipped;

    @override
    int get hashCode =>
        date.hashCode +
        newStores.hashCode +
        optimizations.hashCode +
        optimizationsFailed.hashCode +
        plansCompleted.hashCode +
        plansCreated.hashCode +
        plansPublished.hashCode +
        stopsDelivered.hashCode +
        stopsFailed.hashCode +
        stopsSkipped.hashCode;

  factory Day.fromJson(Map<String, dynamic> json) => _$DayFromJson(json);

  Map<String, dynamic> toJson() => _$DayToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

