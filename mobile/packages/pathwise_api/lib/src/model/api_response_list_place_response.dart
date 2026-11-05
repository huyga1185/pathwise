//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/place_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_list_place_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseListPlaceResponse {
  /// Returns a new [ApiResponseListPlaceResponse] instance.
  ApiResponseListPlaceResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final List<PlaceResponse> data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseListPlaceResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseListPlaceResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseListPlaceResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseListPlaceResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

