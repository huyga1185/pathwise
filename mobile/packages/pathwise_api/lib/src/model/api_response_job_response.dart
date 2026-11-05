//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/job_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_job_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseJobResponse {
  /// Returns a new [ApiResponseJobResponse] instance.
  ApiResponseJobResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final JobResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseJobResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseJobResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseJobResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseJobResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

