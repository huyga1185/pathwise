//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/member_counts_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'admin_store_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AdminStoreSummaryResponse {
  /// Returns a new [AdminStoreSummaryResponse] instance.
  AdminStoreSummaryResponse({

    required  this.createdAt,

    required  this.id,

    required  this.lastActivityAt,

    required  this.memberCounts,

    required  this.name,

    required  this.ownerEmail,

    required  this.plans,

    required  this.status,

    required  this.vehicles,
  });

  @JsonKey(
    
    name: r'createdAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime createdAt;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'lastActivityAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? lastActivityAt;



  @JsonKey(
    
    name: r'memberCounts',
    required: true,
    includeIfNull: false,
  )


  final MemberCountsResponse memberCounts;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'ownerEmail',
    required: true,
    includeIfNull: true,
  )


  final String? ownerEmail;



  @JsonKey(
    
    name: r'plans',
    required: true,
    includeIfNull: false,
  )


  final int plans;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final AdminStoreSummaryResponseStatusEnum status;



  @JsonKey(
    
    name: r'vehicles',
    required: true,
    includeIfNull: false,
  )


  final int vehicles;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AdminStoreSummaryResponse &&
      other.createdAt == createdAt &&
      other.id == id &&
      other.lastActivityAt == lastActivityAt &&
      other.memberCounts == memberCounts &&
      other.name == name &&
      other.ownerEmail == ownerEmail &&
      other.plans == plans &&
      other.status == status &&
      other.vehicles == vehicles;

    @override
    int get hashCode =>
        createdAt.hashCode +
        id.hashCode +
        (lastActivityAt == null ? 0 : lastActivityAt.hashCode) +
        memberCounts.hashCode +
        name.hashCode +
        (ownerEmail == null ? 0 : ownerEmail.hashCode) +
        plans.hashCode +
        status.hashCode +
        vehicles.hashCode;

  factory AdminStoreSummaryResponse.fromJson(Map<String, dynamic> json) => _$AdminStoreSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AdminStoreSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AdminStoreSummaryResponseStatusEnum {
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'READ_ONLY')
READ_ONLY(r'READ_ONLY');

const AdminStoreSummaryResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


