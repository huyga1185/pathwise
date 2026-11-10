//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/location_point_request.dart';
import 'package:json_annotation/json_annotation.dart';

part 'location_batch_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LocationBatchRequest {
  /// Returns a new [LocationBatchRequest] instance.
  LocationBatchRequest({

    required  this.points,
  });

  @JsonKey(
    
    name: r'points',
    required: true,
    includeIfNull: false,
  )


  final List<LocationPointRequest> points;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LocationBatchRequest &&
      other.points == points;

    @override
    int get hashCode =>
        points.hashCode;

  factory LocationBatchRequest.fromJson(Map<String, dynamic> json) => _$LocationBatchRequestFromJson(json);

  Map<String, dynamic> toJson() => _$LocationBatchRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

