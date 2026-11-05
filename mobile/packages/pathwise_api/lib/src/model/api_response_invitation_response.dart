//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/invitation_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_invitation_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseInvitationResponse {
  /// Returns a new [ApiResponseInvitationResponse] instance.
  ApiResponseInvitationResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final InvitationResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseInvitationResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseInvitationResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseInvitationResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseInvitationResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

