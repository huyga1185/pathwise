//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'copy_plan_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class CopyPlanRequest {
  /// Returns a new [CopyPlanRequest] instance.
  CopyPlanRequest({

     this.name,

    required  this.planDate,

    required  this.stops,
  });

  @JsonKey(
    
    name: r'name',
    required: false,
    includeIfNull: false,
  )


  final String? name;



  @JsonKey(
    
    name: r'planDate',
    required: true,
    includeIfNull: false,
  )


  final String planDate;



  @JsonKey(
    
    name: r'stops',
    required: true,
    includeIfNull: false,
  )


  final CopyPlanRequestStopsEnum stops;





    @override
    bool operator ==(Object other) => identical(this, other) || other is CopyPlanRequest &&
      other.name == name &&
      other.planDate == planDate &&
      other.stops == stops;

    @override
    int get hashCode =>
        name.hashCode +
        planDate.hashCode +
        stops.hashCode;

  factory CopyPlanRequest.fromJson(Map<String, dynamic> json) => _$CopyPlanRequestFromJson(json);

  Map<String, dynamic> toJson() => _$CopyPlanRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum CopyPlanRequestStopsEnum {
@JsonValue(r'ALL')
ALL(r'ALL'),
@JsonValue(r'NOT_DELIVERED')
NOT_DELIVERED(r'NOT_DELIVERED');

const CopyPlanRequestStopsEnum(this.value);

final String value;

@override
String toString() => value;
}


