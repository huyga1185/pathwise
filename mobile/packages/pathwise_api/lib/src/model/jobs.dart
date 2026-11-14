//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'jobs.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Jobs {
  /// Returns a new [Jobs] instance.
  Jobs({

    required  this.failedLast24h,

    required  this.unfinished,
  });

  @JsonKey(
    
    name: r'failedLast24h',
    required: true,
    includeIfNull: false,
  )


  final int failedLast24h;



  @JsonKey(
    
    name: r'unfinished',
    required: true,
    includeIfNull: false,
  )


  final int unfinished;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Jobs &&
      other.failedLast24h == failedLast24h &&
      other.unfinished == unfinished;

    @override
    int get hashCode =>
        failedLast24h.hashCode +
        unfinished.hashCode;

  factory Jobs.fromJson(Map<String, dynamic> json) => _$JobsFromJson(json);

  Map<String, dynamic> toJson() => _$JobsToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

