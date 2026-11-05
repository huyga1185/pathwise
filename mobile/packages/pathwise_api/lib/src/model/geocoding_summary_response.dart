//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'geocoding_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class GeocodingSummaryResponse {
  /// Returns a new [GeocodingSummaryResponse] instance.
  GeocodingSummaryResponse({

    required  this.failed,

    required  this.pending,

    required  this.provided,

    required  this.resolved,

    required  this.total,
  });

  @JsonKey(
    
    name: r'failed',
    required: true,
    includeIfNull: false,
  )


  final int failed;



  @JsonKey(
    
    name: r'pending',
    required: true,
    includeIfNull: false,
  )


  final int pending;



  @JsonKey(
    
    name: r'provided',
    required: true,
    includeIfNull: false,
  )


  final int provided;



  @JsonKey(
    
    name: r'resolved',
    required: true,
    includeIfNull: false,
  )


  final int resolved;



  @JsonKey(
    
    name: r'total',
    required: true,
    includeIfNull: false,
  )


  final int total;





    @override
    bool operator ==(Object other) => identical(this, other) || other is GeocodingSummaryResponse &&
      other.failed == failed &&
      other.pending == pending &&
      other.provided == provided &&
      other.resolved == resolved &&
      other.total == total;

    @override
    int get hashCode =>
        failed.hashCode +
        pending.hashCode +
        provided.hashCode +
        resolved.hashCode +
        total.hashCode;

  factory GeocodingSummaryResponse.fromJson(Map<String, dynamic> json) => _$GeocodingSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$GeocodingSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

