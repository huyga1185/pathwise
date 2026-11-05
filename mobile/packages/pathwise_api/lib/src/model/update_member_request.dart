//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_member_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateMemberRequest {
  /// Returns a new [UpdateMemberRequest] instance.
  UpdateMemberRequest({

     this.canDrive,

     this.role,

     this.status,
  });

  @JsonKey(
    
    name: r'canDrive',
    required: false,
    includeIfNull: false,
  )


  final bool? canDrive;



  @JsonKey(
    
    name: r'role',
    required: false,
    includeIfNull: false,
  )


  final UpdateMemberRequestRoleEnum? role;



  @JsonKey(
    
    name: r'status',
    required: false,
    includeIfNull: false,
  )


  final UpdateMemberRequestStatusEnum? status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateMemberRequest &&
      other.canDrive == canDrive &&
      other.role == role &&
      other.status == status;

    @override
    int get hashCode =>
        canDrive.hashCode +
        role.hashCode +
        status.hashCode;

  factory UpdateMemberRequest.fromJson(Map<String, dynamic> json) => _$UpdateMemberRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateMemberRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum UpdateMemberRequestRoleEnum {
@JsonValue(r'DISPATCHER')
DISPATCHER(r'DISPATCHER'),
@JsonValue(r'DRIVER')
DRIVER(r'DRIVER');

const UpdateMemberRequestRoleEnum(this.value);

final String value;

@override
String toString() => value;
}



enum UpdateMemberRequestStatusEnum {
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const UpdateMemberRequestStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


