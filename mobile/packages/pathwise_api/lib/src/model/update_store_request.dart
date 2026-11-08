//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/depot_request.dart';
import 'package:json_annotation/json_annotation.dart';

part 'update_store_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateStoreRequest {
  /// Returns a new [UpdateStoreRequest] instance.
  UpdateStoreRequest({

     this.depot,

    required  this.name,

    required  this.timezone,
  });

  @JsonKey(
    
    name: r'depot',
    required: false,
    includeIfNull: false,
  )


  final DepotRequest? depot;



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
    bool operator ==(Object other) => identical(this, other) || other is UpdateStoreRequest &&
      other.depot == depot &&
      other.name == name &&
      other.timezone == timezone;

    @override
    int get hashCode =>
        depot.hashCode +
        name.hashCode +
        timezone.hashCode;

  factory UpdateStoreRequest.fromJson(Map<String, dynamic> json) => _$UpdateStoreRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateStoreRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

