//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'update_can_drive_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class UpdateCanDriveRequest {
  /// Returns a new [UpdateCanDriveRequest] instance.
  UpdateCanDriveRequest({

    required  this.canDrive,
  });

  @JsonKey(
    
    name: r'canDrive',
    required: true,
    includeIfNull: false,
  )


  final bool canDrive;





    @override
    bool operator ==(Object other) => identical(this, other) || other is UpdateCanDriveRequest &&
      other.canDrive == canDrive;

    @override
    int get hashCode =>
        canDrive.hashCode;

  factory UpdateCanDriveRequest.fromJson(Map<String, dynamic> json) => _$UpdateCanDriveRequestFromJson(json);

  Map<String, dynamic> toJson() => _$UpdateCanDriveRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

