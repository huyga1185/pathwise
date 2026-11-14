//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'now.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Now {
  /// Returns a new [Now] instance.
  Now({

    required  this.activeStores30d,

    required  this.activeStores7d,

    required  this.admins,

    required  this.dispatchers,

    required  this.drivers,

    required  this.lockedStores,

    required  this.owners,

    required  this.stores,
  });

  @JsonKey(
    
    name: r'activeStores30d',
    required: true,
    includeIfNull: false,
  )


  final int activeStores30d;



  @JsonKey(
    
    name: r'activeStores7d',
    required: true,
    includeIfNull: false,
  )


  final int activeStores7d;



  @JsonKey(
    
    name: r'admins',
    required: true,
    includeIfNull: false,
  )


  final int admins;



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
    
    name: r'lockedStores',
    required: true,
    includeIfNull: false,
  )


  final int lockedStores;



  @JsonKey(
    
    name: r'owners',
    required: true,
    includeIfNull: false,
  )


  final int owners;



  @JsonKey(
    
    name: r'stores',
    required: true,
    includeIfNull: false,
  )


  final int stores;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Now &&
      other.activeStores30d == activeStores30d &&
      other.activeStores7d == activeStores7d &&
      other.admins == admins &&
      other.dispatchers == dispatchers &&
      other.drivers == drivers &&
      other.lockedStores == lockedStores &&
      other.owners == owners &&
      other.stores == stores;

    @override
    int get hashCode =>
        activeStores30d.hashCode +
        activeStores7d.hashCode +
        admins.hashCode +
        dispatchers.hashCode +
        drivers.hashCode +
        lockedStores.hashCode +
        owners.hashCode +
        stores.hashCode;

  factory Now.fromJson(Map<String, dynamic> json) => _$NowFromJson(json);

  Map<String, dynamic> toJson() => _$NowToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

