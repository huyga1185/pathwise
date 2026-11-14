//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'transfer_owner_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class TransferOwnerRequest {
  /// Returns a new [TransferOwnerRequest] instance.
  TransferOwnerRequest({

    required  this.userId,
  });

  @JsonKey(
    
    name: r'userId',
    required: true,
    includeIfNull: false,
  )


  final int userId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is TransferOwnerRequest &&
      other.userId == userId;

    @override
    int get hashCode =>
        userId.hashCode;

  factory TransferOwnerRequest.fromJson(Map<String, dynamic> json) => _$TransferOwnerRequestFromJson(json);

  Map<String, dynamic> toJson() => _$TransferOwnerRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

