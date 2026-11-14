//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/admin_member_response.dart';
import 'package:pathwise_api/src/model/member_counts_response.dart';
import 'package:pathwise_api/src/model/store_period_stats_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'admin_store_detail_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AdminStoreDetailResponse {
  /// Returns a new [AdminStoreDetailResponse] instance.
  AdminStoreDetailResponse({

    required  this.createdAt,

    required  this.id,

    required  this.last30Days,

    required  this.lastActivityAt,

    required  this.lockReason,

    required  this.lockedAt,

    required  this.memberCounts,

    required  this.members,

    required  this.name,

    required  this.ownerEmail,

    required  this.plans,

    required  this.status,

    required  this.timezone,

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
    
    name: r'last30Days',
    required: true,
    includeIfNull: false,
  )


  final StorePeriodStatsResponse last30Days;



  @JsonKey(
    
    name: r'lastActivityAt',
    required: true,
    includeIfNull: true,
  )


  final DateTime? lastActivityAt;



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
    
    name: r'memberCounts',
    required: true,
    includeIfNull: false,
  )


  final MemberCountsResponse memberCounts;



  @JsonKey(
    
    name: r'members',
    required: true,
    includeIfNull: false,
  )


  final List<AdminMemberResponse> members;



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


  final AdminStoreDetailResponseStatusEnum status;



  @JsonKey(
    
    name: r'timezone',
    required: true,
    includeIfNull: false,
  )


  final String timezone;



  @JsonKey(
    
    name: r'vehicles',
    required: true,
    includeIfNull: false,
  )


  final int vehicles;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AdminStoreDetailResponse &&
      other.createdAt == createdAt &&
      other.id == id &&
      other.last30Days == last30Days &&
      other.lastActivityAt == lastActivityAt &&
      other.lockReason == lockReason &&
      other.lockedAt == lockedAt &&
      other.memberCounts == memberCounts &&
      other.members == members &&
      other.name == name &&
      other.ownerEmail == ownerEmail &&
      other.plans == plans &&
      other.status == status &&
      other.timezone == timezone &&
      other.vehicles == vehicles;

    @override
    int get hashCode =>
        createdAt.hashCode +
        id.hashCode +
        last30Days.hashCode +
        (lastActivityAt == null ? 0 : lastActivityAt.hashCode) +
        (lockReason == null ? 0 : lockReason.hashCode) +
        (lockedAt == null ? 0 : lockedAt.hashCode) +
        memberCounts.hashCode +
        members.hashCode +
        name.hashCode +
        (ownerEmail == null ? 0 : ownerEmail.hashCode) +
        plans.hashCode +
        status.hashCode +
        timezone.hashCode +
        vehicles.hashCode;

  factory AdminStoreDetailResponse.fromJson(Map<String, dynamic> json) => _$AdminStoreDetailResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AdminStoreDetailResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AdminStoreDetailResponseStatusEnum {
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'READ_ONLY')
READ_ONLY(r'READ_ONLY');

const AdminStoreDetailResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


