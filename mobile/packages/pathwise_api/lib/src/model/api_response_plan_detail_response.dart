//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/plan_detail_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_plan_detail_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePlanDetailResponse {
  /// Returns a new [ApiResponsePlanDetailResponse] instance.
  ApiResponsePlanDetailResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PlanDetailResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePlanDetailResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePlanDetailResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePlanDetailResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePlanDetailResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

