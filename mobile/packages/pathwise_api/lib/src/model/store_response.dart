//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/depot_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'store_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class StoreResponse {
  /// Returns a new [StoreResponse] instance.
  StoreResponse({

    required  this.depot,

    required  this.id,

    required  this.name,

    required  this.timezone,
  });

  @JsonKey(
    
    name: r'depot',
    required: true,
    includeIfNull: true,
  )


  final DepotResponse? depot;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'timezone',
    required: true,
    includeIfNull: false,
  )


  final String timezone;





    @override
    bool operator ==(Object other) => identical(this, other) || other is StoreResponse &&
      other.depot == depot &&
      other.id == id &&
      other.name == name &&
      other.timezone == timezone;

    @override
    int get hashCode =>
        (depot == null ? 0 : depot.hashCode) +
        id.hashCode +
        name.hashCode +
        timezone.hashCode;

  factory StoreResponse.fromJson(Map<String, dynamic> json) => _$StoreResponseFromJson(json);

  Map<String, dynamic> toJson() => _$StoreResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

