//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/store_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'api_response_store_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ApiResponseStoreResponse {
  /// Returns a new [ApiResponseStoreResponse] instance.
  ApiResponseStoreResponse({

    required  this.data,

     this.message,
  });

  @JsonKey(
    
    name: r'data',
    required: true,
    includeIfNull: false,
  )


  final StoreResponse data;



  @JsonKey(
    
    name: r'message',
    required: false,
    includeIfNull: false,
  )


  final String? message;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ApiResponseStoreResponse &&
      other.data == data &&
      other.message == message;

    @override
    int get hashCode =>
        data.hashCode +
        message.hashCode;

  factory ApiResponseStoreResponse.fromJson(Map<String, dynamic> json) => _$ApiResponseStoreResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ApiResponseStoreResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

