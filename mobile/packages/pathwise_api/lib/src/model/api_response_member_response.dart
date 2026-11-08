//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/member_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_member_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseMemberResponse {
  /// Returns a new [ApiResponseMemberResponse] instance.
  ApiResponseMemberResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final MemberResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseMemberResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseMemberResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseMemberResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseMemberResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

