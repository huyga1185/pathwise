//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'register_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RegisterRequest {
  /// Returns a new [RegisterRequest] instance.
  RegisterRequest({

    required  this.email,

     this.fullName,

    required  this.password,

    required  this.storeName,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'fullName',
    required: false,
    includeIfNull: false,
  )


  final String? fullName;



  @JsonKey(
    
    name: r'password',
    required: true,
    includeIfNull: false,
  )


  final String password;



  @JsonKey(
    
    name: r'storeName',
    required: true,
    includeIfNull: false,
  )


  final String storeName;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RegisterRequest &&
      other.email == email &&
      other.fullName == fullName &&
      other.password == password &&
      other.storeName == storeName;

    @override
    int get hashCode =>
        email.hashCode +
        fullName.hashCode +
        password.hashCode +
        storeName.hashCode;

  factory RegisterRequest.fromJson(Map<String, dynamic> json) => _$RegisterRequestFromJson(json);

  Map<String, dynamic> toJson() => _$RegisterRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

