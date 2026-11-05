//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'register_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RegisterResponse {
  /// Returns a new [RegisterResponse] instance.
  RegisterResponse({

    required  this.email,

    required  this.status,
  });

  @JsonKey(
    
    name: r'email',
    required: true,
    includeIfNull: false,
  )


  final String email;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final RegisterResponseStatusEnum status;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RegisterResponse &&
      other.email == email &&
      other.status == status;

    @override
    int get hashCode =>
        email.hashCode +
        status.hashCode;

  factory RegisterResponse.fromJson(Map<String, dynamic> json) => _$RegisterResponseFromJson(json);

  Map<String, dynamic> toJson() => _$RegisterResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum RegisterResponseStatusEnum {
@JsonValue(r'PENDING_VERIFICATION')
PENDING_VERIFICATION(r'PENDING_VERIFICATION'),
@JsonValue(r'ACTIVE')
ACTIVE(r'ACTIVE'),
@JsonValue(r'DISABLED')
DISABLED(r'DISABLED');

const RegisterResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


