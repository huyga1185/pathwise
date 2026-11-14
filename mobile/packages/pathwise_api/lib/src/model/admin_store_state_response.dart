//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'admin_store_state_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AdminStoreStateResponse {
  /// Returns a new [AdminStoreStateResponse] instance.
  AdminStoreStateResponse({

    required  this.id,

    required  this.lockReason,

    required  this.lockedAt,

    required  this.status,
  });

  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'lockReason',
    required: true,
    includeIfNull: true,
  )


  final String? lockReason;



  @JsonKey(
    
    name: r'lockedAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? lockedAt;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final AdminStoreStateResponseStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AdminStoreStateResponse &&
      other.id == id &&
      other.lockReason == lockReason &&
      other.lockedAt == lockedAt &&
      other.status == status;

    @override
    int get hashCode =>
        id.hashCode +
        (lockReason == null ? 0 : lockReason.hashCode) +
        (lockedAt == null ? 0 : lockedAt.hashCode) +
        status.hashCode;

  factory AdminStoreStateResponse.fromJson(Map<String, dynamic> json) => _$AdminStoreStateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AdminStoreStateResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AdminStoreStateResponseStatusEnum {
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'READ_ONLY')
READ_ONLY(r'READ_ONLY');

const AdminStoreStateResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


