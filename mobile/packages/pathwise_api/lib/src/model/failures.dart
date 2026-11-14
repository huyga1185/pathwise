//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'failures.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Failures {
  /// Returns a new [Failures] instance.
  Failures({

    required  this.email,

    required  this.geocoding,

    required  this.job,

    required  this.routing,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final int email;



  @JsonKey(
    
    name: r'geocoding',
    required: true,
    includeIfNull: false,
  )


  final int geocoding;



  @JsonKey(
    
    name: r'job',
    required: true,
    includeIfNull: false,
  )


  final int job;



  @JsonKey(
    
    name: r'routing',
    required: true,
    includeIfNull: false,
  )


  final int routing;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Failures &&
      other.email == email &&
      other.geocoding == geocoding &&
      other.job == job &&
      other.routing == routing;

    @override
    int get hashCode =>
        email.hashCode +
        geocoding.hashCode +
        job.hashCode +
        routing.hashCode;

  factory Failures.fromJson(Map<String, dynamic> json) => _$FailuresFromJson(json);

  Map<String, dynamic> toJson() => _$FailuresToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

