//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'report_failure_reason.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ReportFailureReason {
  /// Returns a new [ReportFailureReason] instance.
  ReportFailureReason({

    required  this.count,

    required  this.reason,
  });

  @JsonKey(
    
    name: r'count',
    required: true,
    includeIfNull: false,
  )


  final int count;



  @JsonKey(
    
    name: r'reason',
    required: true,
    includeIfNull: false,
  )


  final String reason;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ReportFailureReason &&
      other.count == count &&
      other.reason == reason;

    @override
    int get hashCode =>
        count.hashCode +
        reason.hashCode;

  factory ReportFailureReason.fromJson(Map<String, dynamic> json) => _$ReportFailureReasonFromJson(json);

  Map<String, dynamic> toJson() => _$ReportFailureReasonToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

