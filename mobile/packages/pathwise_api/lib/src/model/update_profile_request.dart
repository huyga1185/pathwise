//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_profile_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateProfileRequest {
  /// Returns a new [UpdateProfileRequest] instance.
  UpdateProfileRequest({

     this.fullName,

     this.phoneNumber,
  });

  @JsonKey(
    
    name: r'fullName',
    required: false,
    includeIfNull: false,
  )


  final String? fullName;



  @JsonKey(
    
    name: r'phoneNumber',
    required: false,
    includeIfNull: false,
  )


  final String? phoneNumber;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateProfileRequest &&
      other.fullName == fullName &&
      other.phoneNumber == phoneNumber;

    @override
    int get hashCode =>
        (fullName == null ? 0 : fullName.hashCode) +
        (phoneNumber == null ? 0 : phoneNumber.hashCode);

  factory UpdateProfileRequest.fromJson(Map<String, dynamic> json) => _$UpdateProfileRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateProfileRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

