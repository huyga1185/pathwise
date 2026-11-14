//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/page_response_audit_event_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_page_response_audit_event_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePageResponseAuditEventResponse {
  /// Returns a new [ApiResponsePageResponseAuditEventResponse] instance.
  ApiResponsePageResponseAuditEventResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PageResponseAuditEventResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePageResponseAuditEventResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePageResponseAuditEventResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePageResponseAuditEventResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePageResponseAuditEventResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

