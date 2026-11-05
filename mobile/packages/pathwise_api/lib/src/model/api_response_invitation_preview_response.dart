//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/invitation_preview_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_invitation_preview_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseInvitationPreviewResponse {
  /// Returns a new [ApiResponseInvitationPreviewResponse] instance.
  ApiResponseInvitationPreviewResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final InvitationPreviewResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseInvitationPreviewResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseInvitationPreviewResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseInvitationPreviewResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseInvitationPreviewResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

