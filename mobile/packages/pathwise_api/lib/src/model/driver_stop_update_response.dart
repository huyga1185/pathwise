//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/driver_stop_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'driver_stop_update_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverStopUpdateResponse {
  /// Returns a new [DriverStopUpdateResponse] instance.
  DriverStopUpdateResponse({

    required  this.planStatus,

    required  this.routeStatus,

    required  this.stop,
  });

  @JsonKey(
    
    name: r'planStatus',
    required: true,
    includeIfNull: false,
  )


  final DriverStopUpdateResponsePlanStatusEnum planStatus;



  @JsonKey(
    
    name: r'routeStatus',
    required: true,
    includeIfNull: false,
  )


  final DriverStopUpdateResponseRouteStatusEnum routeStatus;



  @JsonKey(
    
    name: r'stop',
    required: true,
    includeIfNull: false,
  )


  final DriverStopResponse stop;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverStopUpdateResponse &&
      other.planStatus == planStatus &&
      other.routeStatus == routeStatus &&
      other.stop == stop;

    @override
    int get hashCode =>
        planStatus.hashCode +
        routeStatus.hashCode +
        stop.hashCode;

  factory DriverStopUpdateResponse.fromJson(Map<String, dynamic> json) => _$DriverStopUpdateResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DriverStopUpdateResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverStopUpdateResponsePlanStatusEnum {
@JsonValue(r'DRAFT')
DRAFT(r'DRAFT'),
@JsonValue(r'PUBLISHED')
PUBLISHED(r'PUBLISHED'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED'),
@JsonValue(r'CANCELLED')
CANCELLED(r'CANCELLED');

const DriverStopUpdateResponsePlanStatusEnum(this.value);

final String value;

@override
String toString() => value;
}



enum DriverStopUpdateResponseRouteStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'IN_PROGRESS')
IN_PROGRESS(r'IN_PROGRESS'),
@JsonValue(r'COMPLETED')
COMPLETED(r'COMPLETED');

const DriverStopUpdateResponseRouteStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


