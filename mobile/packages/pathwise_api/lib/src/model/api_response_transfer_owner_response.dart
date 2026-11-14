//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/transfer_owner_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_transfer_owner_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseTransferOwnerResponse {
  /// Returns a new [ApiResponseTransferOwnerResponse] instance.
  ApiResponseTransferOwnerResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final TransferOwnerResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseTransferOwnerResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseTransferOwnerResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseTransferOwnerResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseTransferOwnerResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

