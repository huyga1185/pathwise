//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/session_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_session_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListSessionResponse {
  /// Returns a new [ApiResponseListSessionResponse] instance.
  ApiResponseListSessionResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<SessionResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListSessionResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListSessionResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListSessionResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListSessionResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

