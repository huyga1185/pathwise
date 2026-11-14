//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/day.dart';
import 'package:pathwise_api/src/model/now.dart';
import 'package:pathwise_api/src/model/totals.dart';
import 'package:json_annotation/json_annotation.dart';

part 'business_metrics_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class BusinessMetricsResponse {
  /// Returns a new [BusinessMetricsResponse] instance.
  BusinessMetricsResponse({

    required  this.computedAt,

    required  this.days,

    required  this.from,

    required  this.now,

    required  this.timezone,

    required  this.to,

    required  this.totals,
  });

  @JsonKey(
    
    name: r'computedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime computedAt;



  @JsonKey(
    
    name: r'days',
    required: true,
    includeIfNull: false,
  )


  final List<Day> days;



  @JsonKey(
    
    name: r'from',
    required: true,
    includeIfNull: false,
  )


  final String from;



  @JsonKey(
    
    name: r'now',
    required: true,
    includeIfNull: false,
  )


  final Now now;



  @JsonKey(
    
    name: r'timezone',
    required: true,
    includeIfNull: false,
  )


  final String timezone;



  @JsonKey(
    
    name: r'to',
    required: true,
    includeIfNull: false,
  )


  final String to;



  @JsonKey(
    
    name: r'totals',
    required: true,
    includeIfNull: false,
  )


  final Totals totals;





    @override
    bool operator ==(Object other) => identical(this, other) || other is BusinessMetricsResponse &&
      other.computedAt == computedAt &&
      other.days == days &&
      other.from == from &&
      other.now == now &&
      other.timezone == timezone &&
      other.to == to &&
      other.totals == totals;

    @override
    int get hashCode =>
        computedAt.hashCode +
        days.hashCode +
        from.hashCode +
        now.hashCode +
        timezone.hashCode +
        to.hashCode +
        totals.hashCode;

  factory BusinessMetricsResponse.fromJson(Map<String, dynamic> json) => _$BusinessMetricsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$BusinessMetricsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

