//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_stop_status_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateStopStatusRequest {
  /// Returns a new [UpdateStopStatusRequest] instance.
  UpdateStopStatusRequest({

     this.failureReason,

    required  this.status,
  });

  @JsonKey(
    
    name: r'failureReason',
    required: false,
    includeIfNull: false,
  )


  final String? failureReason;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final UpdateStopStatusRequestStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateStopStatusRequest &&
      other.failureReason == failureReason &&
      other.status == status;

    @override
    int get hashCode =>
        failureReason.hashCode +
        status.hashCode;

  factory UpdateStopStatusRequest.fromJson(Map<String, dynamic> json) => _$UpdateStopStatusRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateStopStatusRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum UpdateStopStatusRequestStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'DELIVERED')
DELIVERED(r'DELIVERED'),
@JsonValue(r'FAILED')
FAILED(r'FAILED'),
@JsonValue(r'SKIPPED')
SKIPPED(r'SKIPPED');

const UpdateStopStatusRequestStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


