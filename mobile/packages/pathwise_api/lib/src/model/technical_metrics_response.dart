//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/jobs.dart';
import 'package:pathwise_api/src/model/http.dart';
import 'package:pathwise_api/src/model/database.dart';
import 'package:pathwise_api/src/model/failures.dart';
import 'package:pathwise_api/src/model/jvm.dart';
import 'package:json_annotation/json_annotation.dart';

part 'technical_metrics_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class TechnicalMetricsResponse {
  /// Returns a new [TechnicalMetricsResponse] instance.
  TechnicalMetricsResponse({

    required  this.database,

    required  this.failuresLast24h,

    required  this.http,

    required  this.jobs,

    required  this.jvm,

    required  this.measuredAt,

    required  this.rateLimitedLast24h,

    required  this.startedAt,

    required  this.uptimeSeconds,
  });

  @JsonKey(
    
    name: r'database',
    required: true,
    includeIfNull: false,
  )


  final Database database;



  @JsonKey(
    
    name: r'failuresLast24h',
    required: true,
    includeIfNull: false,
  )


  final Failures failuresLast24h;



  @JsonKey(
    
    name: r'http',
    required: true,
    includeIfNull: false,
  )


  final Http http;



  @JsonKey(
    
    name: r'jobs',
    required: true,
    includeIfNull: false,
  )


  final Jobs jobs;



  @JsonKey(
    
    name: r'jvm',
    required: true,
    includeIfNull: false,
  )


  final Jvm jvm;



  @JsonKey(
    
    name: r'measuredAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime measuredAt;



  @JsonKey(
    
    name: r'rateLimitedLast24h',
    required: true,
    includeIfNull: false,
  )


  final int rateLimitedLast24h;



  @JsonKey(
    
    name: r'startedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime startedAt;



  @JsonKey(
    
    name: r'uptimeSeconds',
    required: true,
    includeIfNull: false,
  )


  final int uptimeSeconds;





    @override
    bool operator ==(Object other) => identical(this, other) || other is TechnicalMetricsResponse &&
      other.database == database &&
      other.failuresLast24h == failuresLast24h &&
      other.http == http &&
      other.jobs == jobs &&
      other.jvm == jvm &&
      other.measuredAt == measuredAt &&
      other.rateLimitedLast24h == rateLimitedLast24h &&
      other.startedAt == startedAt &&
      other.uptimeSeconds == uptimeSeconds;

    @override
    int get hashCode =>
        database.hashCode +
        failuresLast24h.hashCode +
        http.hashCode +
        jobs.hashCode +
        jvm.hashCode +
        measuredAt.hashCode +
        rateLimitedLast24h.hashCode +
        startedAt.hashCode +
        uptimeSeconds.hashCode;

  factory TechnicalMetricsResponse.fromJson(Map<String, dynamic> json) => _$TechnicalMetricsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$TechnicalMetricsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

