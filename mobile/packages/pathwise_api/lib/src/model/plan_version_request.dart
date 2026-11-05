//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'plan_version_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanVersionRequest {
  /// Returns a new [PlanVersionRequest] instance.
  PlanVersionRequest({

    required  this.version,
  });

  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanVersionRequest &&
      other.version == version;

    @override
    int get hashCode =>
        version.hashCode;

  factory PlanVersionRequest.fromJson(Map<String, dynamic> json) => _$PlanVersionRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PlanVersionRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

