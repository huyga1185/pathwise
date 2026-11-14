//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'member_counts_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class MemberCountsResponse {
  /// Returns a new [MemberCountsResponse] instance.
  MemberCountsResponse({

    required  this.dispatchers,

    required  this.drivers,

    required  this.owners,
  });

  @JsonKey(
    
    name: r'dispatchers',
    required: true,
    includeIfNull: false,
  )


  final int dispatchers;



  @JsonKey(
    
    name: r'drivers',
    required: true,
    includeIfNull: false,
  )


  final int drivers;



  @JsonKey(
    
    name: r'owners',
    required: true,
    includeIfNull: false,
  )


  final int owners;





    @override
    bool operator ==(Object other) => identical(this, other) || other is MemberCountsResponse &&
      other.dispatchers == dispatchers &&
      other.drivers == drivers &&
      other.owners == owners;

    @override
    int get hashCode =>
        dispatchers.hashCode +
        drivers.hashCode +
        owners.hashCode;

  factory MemberCountsResponse.fromJson(Map<String, dynamic> json) => _$MemberCountsResponseFromJson(json);

  Map<String, dynamic> toJson() => _$MemberCountsResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

