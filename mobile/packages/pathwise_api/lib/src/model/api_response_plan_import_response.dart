//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/plan_import_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_plan_import_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponsePlanImportResponse {
  /// Returns a new [ApiResponsePlanImportResponse] instance.
  ApiResponsePlanImportResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final PlanImportResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponsePlanImportResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponsePlanImportResponse.fromJson(Map<String, dynamic> json) => _$ApiResponsePlanImportResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponsePlanImportResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

