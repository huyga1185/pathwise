//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/plan_copy_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_plan_copy_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePlanCopyResponse {
  /// Returns a new [ApiResponsePlanCopyResponse] instance.
  ApiResponsePlanCopyResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PlanCopyResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePlanCopyResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePlanCopyResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePlanCopyResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePlanCopyResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

