//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/register_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_register_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseRegisterResponse {
  /// Returns a new [ApiResponseRegisterResponse] instance.
  ApiResponseRegisterResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final RegisterResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseRegisterResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseRegisterResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseRegisterResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseRegisterResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

