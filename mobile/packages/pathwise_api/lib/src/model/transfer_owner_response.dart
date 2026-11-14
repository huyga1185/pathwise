//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'transfer_owner_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class TransferOwnerResponse {
  /// Returns a new [TransferOwnerResponse] instance.
  TransferOwnerResponse({

    required  this.ownerCanDrive,

    required  this.ownerEmail,

    required  this.ownerId,

    required  this.previousOwnerEmail,

    required  this.previousOwnerId,

    required  this.storeId,
  });

  @JsonKey(
    
    name: r'ownerCanDrive',
    required: true,
    includeIfNull: false,
  )


  final bool ownerCanDrive;



  @JsonKey(
    
    name: r'ownerEmail',
    required: true,
    includeIfNull: false,
  )


  final String ownerEmail;



  @JsonKey(
    
    name: r'ownerId',
    required: true,
    includeIfNull: false,
  )


  final int ownerId;



  @JsonKey(
    
    name: r'previousOwnerEmail',
    required: true,
    includeIfNull: true,
  )


  final String? previousOwnerEmail;



  @JsonKey(
    
    name: r'previousOwnerId',
    required: true,
    includeIfNull: true,
  )


  final int? previousOwnerId;



  @JsonKey(
    
    name: r'storeId',
    required: true,
    includeIfNull: false,
  )


  final int storeId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is TransferOwnerResponse &&
      other.ownerCanDrive == ownerCanDrive &&
      other.ownerEmail == ownerEmail &&
      other.ownerId == ownerId &&
      other.previousOwnerEmail == previousOwnerEmail &&
      other.previousOwnerId == previousOwnerId &&
      other.storeId == storeId;

    @override
    int get hashCode =>
        ownerCanDrive.hashCode +
        ownerEmail.hashCode +
        ownerId.hashCode +
        (previousOwnerEmail == null ? 0 : previousOwnerEmail.hashCode) +
        (previousOwnerId == null ? 0 : previousOwnerId.hashCode) +
        storeId.hashCode;

  factory TransferOwnerResponse.fromJson(Map<String, dynamic> json) => _$TransferOwnerResponseFromJson(json);

  Map<String, dynamic> toJson() => _$TransferOwnerResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

