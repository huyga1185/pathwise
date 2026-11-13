//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/time_window_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_stop_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanStopResponse {
  /// Returns a new [PlanStopResponse] instance.
  PlanStopResponse({

    required  this.address,

    required  this.demands,

    required  this.dropReason,

    required  this.failureReason,

    required  this.geocodeError,

    required  this.geocodeStatus,

    required  this.geocodedDisplayName,

    required  this.id,

    required  this.label,

    required  this.lat,

    required  this.lon,

    required  this.note,

    required  this.plannedArrival,

    required  this.recipientName,

    required  this.recipientPhone,

    required  this.sequence,

    required  this.serviceTimeSeconds,

    required  this.status,

    required  this.timeWindow,
  });

  @JsonKey(
    
    name: r'address',
    required: true,
    includeIfNull: true,
  )


  final String? address;



  @JsonKey(
    
    name: r'demands',
    required: true,
    includeIfNull: false,
  )


  final Map<String, int> demands;



  @JsonKey(
    
    name: r'dropReason',
    required: true,
    includeIfNull: true,
  )


  final PlanStopResponseDropReasonEnum? dropReason;



  @JsonKey(
    
    name: r'failureReason',
    required: true,
    includeIfNull: true,
  )


  final String? failureReason;



  @JsonKey(
    
    name: r'geocodeError',
    required: true,
    includeIfNull: true,
  )


  final PlanStopResponseGeocodeErrorEnum? geocodeError;



  @JsonKey(
    
    name: r'geocodeStatus',
    required: true,
    includeIfNull: false,
  )


  final PlanStopResponseGeocodeStatusEnum geocodeStatus;



  @JsonKey(
    
    name: r'geocodedDisplayName',
    required: true,
    includeIfNull: true,
  )


  final String? geocodedDisplayName;



  @JsonKey(
    
    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final int id;



  @JsonKey(
    
    name: r'label',
    required: true,
    includeIfNull: false,
  )


  final String label;



  @JsonKey(
    
    name: r'lat',
    required: true,
    includeIfNull: true,
  )


  final double? lat;



  @JsonKey(
    
    name: r'lon',
    required: true,
    includeIfNull: true,
  )


  final double? lon;



  @JsonKey(
    
    name: r'note',
    required: true,
    includeIfNull: true,
  )


  final String? note;



  @JsonKey(
    
    name: r'plannedArrival',
    required: true,
    includeIfNull: true,
  )


  final DateTime? plannedArrival;



  @JsonKey(
    
    name: r'recipientName',
    required: true,
    includeIfNull: true,
  )


  final String? recipientName;



  @JsonKey(
    
    name: r'recipientPhone',
    required: true,
    includeIfNull: true,
  )


  final String? recipientPhone;



  @JsonKey(
    
    name: r'sequence',
    required: true,
    includeIfNull: true,
  )


  final int? sequence;



  @JsonKey(
    
    name: r'serviceTimeSeconds',
    required: true,
    includeIfNull: false,
  )


  final int serviceTimeSeconds;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final PlanStopResponseStatusEnum status;



  @JsonKey(
    
    name: r'timeWindow',
    required: true,
    includeIfNull: true,
  )


  final TimeWindowResponse? timeWindow;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanStopResponse &&
      other.address == address &&
      other.demands == demands &&
      other.dropReason == dropReason &&
      other.failureReason == failureReason &&
      other.geocodeError == geocodeError &&
      other.geocodeStatus == geocodeStatus &&
      other.geocodedDisplayName == geocodedDisplayName &&
      other.id == id &&
      other.label == label &&
      other.lat == lat &&
      other.lon == lon &&
      other.note == note &&
      other.plannedArrival == plannedArrival &&
      other.recipientName == recipientName &&
      other.recipientPhone == recipientPhone &&
      other.sequence == sequence &&
      other.serviceTimeSeconds == serviceTimeSeconds &&
      other.status == status &&
      other.timeWindow == timeWindow;

    @override
    int get hashCode =>
        (address == null ? 0 : address.hashCode) +
        demands.hashCode +
        (dropReason == null ? 0 : dropReason.hashCode) +
        (failureReason == null ? 0 : failureReason.hashCode) +
        (geocodeError == null ? 0 : geocodeError.hashCode) +
        geocodeStatus.hashCode +
        (geocodedDisplayName == null ? 0 : geocodedDisplayName.hashCode) +
        id.hashCode +
        label.hashCode +
        (lat == null ? 0 : lat.hashCode) +
        (lon == null ? 0 : lon.hashCode) +
        (note == null ? 0 : note.hashCode) +
        (plannedArrival == null ? 0 : plannedArrival.hashCode) +
        (recipientName == null ? 0 : recipientName.hashCode) +
        (recipientPhone == null ? 0 : recipientPhone.hashCode) +
        (sequence == null ? 0 : sequence.hashCode) +
        serviceTimeSeconds.hashCode +
        status.hashCode +
        (timeWindow == null ? 0 : timeWindow.hashCode);

  factory PlanStopResponse.fromJson(Map<String, dynamic> json) => _$PlanStopResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanStopResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum PlanStopResponseDropReasonEnum {
@JsonValue(r'NOT_OPTIMIZED')
NOT_OPTIMIZED(r'NOT_OPTIMIZED'),
@JsonValue(r'INFEASIBLE')
INFEASIBLE(r'INFEASIBLE'),
@JsonValue(r'MANUAL')
MANUAL(r'MANUAL');

const PlanStopResponseDropReasonEnum(this.value);

final String value;

@override
String toString() => value;
}



enum PlanStopResponseGeocodeErrorEnum {
@JsonValue(r'NOT_FOUND')
NOT_FOUND(r'NOT_FOUND'),
@JsonValue(r'PROVIDER_ERROR')
PROVIDER_ERROR(r'PROVIDER_ERROR');

const PlanStopResponseGeocodeErrorEnum(this.value);

final String value;

@override
String toString() => value;
}



enum PlanStopResponseGeocodeStatusEnum {
@JsonValue(r'PROVIDED')
PROVIDED(r'PROVIDED'),
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'RESOLVED')
RESOLVED(r'RESOLVED'),
@JsonValue(r'FAILED')
FAILED(r'FAILED');

const PlanStopResponseGeocodeStatusEnum(this.value);

final String value;

@override
String toString() => value;
}



enum PlanStopResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'DELIVERED')
DELIVERED(r'DELIVERED'),
@JsonValue(r'FAILED')
FAILED(r'FAILED'),
@JsonValue(r'SKIPPED')
SKIPPED(r'SKIPPED');

const PlanStopResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


