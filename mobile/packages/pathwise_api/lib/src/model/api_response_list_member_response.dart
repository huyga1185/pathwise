//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/member_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_member_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListMemberResponse {
  /// Returns a new [ApiResponseListMemberResponse] instance.
  ApiResponseListMemberResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<MemberResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListMemberResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListMemberResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListMemberResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListMemberResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

