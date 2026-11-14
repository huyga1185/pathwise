//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'lock_store_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LockStoreRequest {
  /// Returns a new [LockStoreRequest] instance.
  LockStoreRequest({

    required  this.reason,
  });

  @JsonKey(
    
    name: r'reason',
    required: true,
    includeIfNull: false,
  )


  final String reason;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LockStoreRequest &&
      other.reason == reason;

    @override
    int get hashCode =>
        reason.hashCode;

  factory LockStoreRequest.fromJson(Map<String, dynamic> json) => _$LockStoreRequestFromJson(json);

  Map<String, dynamic> toJson() => _$LockStoreRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

