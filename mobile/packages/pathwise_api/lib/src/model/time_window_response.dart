//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'time_window_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class TimeWindowResponse {
  /// Returns a new [TimeWindowResponse] instance.
  TimeWindowResponse({

    required  this.end,

    required  this.start,
  });

  @JsonKey(
    
    name: r'end',
    required: true,
    includeIfNull: false,
  )


  final String end;



  @JsonKey(
    
    name: r'start',
    required: true,
    includeIfNull: false,
  )


  final String start;





    @override
    bool operator ==(Object other) => identical(this, other) || other is TimeWindowResponse &&
      other.end == end &&
      other.start == start;

    @override
    int get hashCode =>
        end.hashCode +
        start.hashCode;

  factory TimeWindowResponse.fromJson(Map<String, dynamic> json) => _$TimeWindowResponseFromJson(json);

  Map<String, dynamic> toJson() => _$TimeWindowResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

