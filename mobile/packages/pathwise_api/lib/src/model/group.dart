//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'group.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Group {
  /// Returns a new [Group] instance.
  Group({

    required  this.group,

    required  this.p95Millis,

    required  this.requestsLastHour,
  });

  @JsonKey(
    
    name: r'group',
    required: true,
    includeIfNull: false,
  )


  final String group;



  @JsonKey(
    
    name: r'p95Millis',
    required: true,
    includeIfNull: true,
  )


  final double? p95Millis;



  @JsonKey(
    
    name: r'requestsLastHour',
    required: true,
    includeIfNull: false,
  )


  final int requestsLastHour;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Group &&
      other.group == group &&
      other.p95Millis == p95Millis &&
      other.requestsLastHour == requestsLastHour;

    @override
    int get hashCode =>
        group.hashCode +
        (p95Millis == null ? 0 : p95Millis.hashCode) +
        requestsLastHour.hashCode;

  factory Group.fromJson(Map<String, dynamic> json) => _$GroupFromJson(json);

  Map<String, dynamic> toJson() => _$GroupToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

