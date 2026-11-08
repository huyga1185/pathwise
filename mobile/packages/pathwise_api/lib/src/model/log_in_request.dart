//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'log_in_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LogInRequest {
  /// Returns a new [LogInRequest] instance.
  LogInRequest({

    required  this.email,

    required  this.password,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'password',
    required: true,
    includeIfNull: false,
  )


  final String password;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LogInRequest &&
      other.email == email &&
      other.password == password;

    @override
    int get hashCode =>
        email.hashCode +
        password.hashCode;

  factory LogInRequest.fromJson(Map<String, dynamic> json) => _$LogInRequestFromJson(json);

  Map<String, dynamic> toJson() => _$LogInRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

