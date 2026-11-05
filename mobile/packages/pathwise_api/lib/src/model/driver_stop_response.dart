//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/time_window_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'driver_stop_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DriverStopResponse {
  /// Returns a new [DriverStopResponse] instance.
  DriverStopResponse({

    required  this.address,

    required  this.failureReason,

    required  this.id,

    required  this.label,

    required  this.lat,

    required  this.lon,

    required  this.note,

    required  this.plannedArrival,

    required  this.recipientName,

    required  this.recipientPhone,

    required  this.sequence,

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
    
    name: r'failureReason',
    required: true,
    includeIfNull: true,
  )


  final String? failureReason;



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
    includeIfNull: false,
  )


  final double lat;



  @JsonKey(
    
    name: r'lon',
    required: true,
    includeIfNull: false,
  )


  final double lon;



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
    includeIfNull: false,
  )


  final int sequence;



  @JsonKey(
    
    name: r'status',
    required: true,
    includeIfNull: false,
  )


  final DriverStopResponseStatusEnum status;



  @JsonKey(
    
    name: r'timeWindow',
    required: true,
    includeIfNull: true,
  )


  final TimeWindowResponse? timeWindow;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DriverStopResponse &&
      other.address == address &&
      other.failureReason == failureReason &&
      other.id == id &&
      other.label == label &&
      other.lat == lat &&
      other.lon == lon &&
      other.note == note &&
      other.plannedArrival == plannedArrival &&
      other.recipientName == recipientName &&
      other.recipientPhone == recipientPhone &&
      other.sequence == sequence &&
      other.status == status &&
      other.timeWindow == timeWindow;

    @override
    int get hashCode =>
        (address == null ? 0 : address.hashCode) +
        (failureReason == null ? 0 : failureReason.hashCode) +
        id.hashCode +
        label.hashCode +
        lat.hashCode +
        lon.hashCode +
        (note == null ? 0 : note.hashCode) +
        (plannedArrival == null ? 0 : plannedArrival.hashCode) +
        (recipientName == null ? 0 : recipientName.hashCode) +
        (recipientPhone == null ? 0 : recipientPhone.hashCode) +
        sequence.hashCode +
        status.hashCode +
        (timeWindow == null ? 0 : timeWindow.hashCode);

  factory DriverStopResponse.fromJson(Map<String, dynamic> json) => _$DriverStopResponseFromJson(json);

  Map<String, dynamic> toJson() => _$DriverStopResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DriverStopResponseStatusEnum {
@JsonValue(r'PENDING')
PENDING(r'PENDING'),
@JsonValue(r'DELIVERED')
DELIVERED(r'DELIVERED'),
@JsonValue(r'FAILED')
FAILED(r'FAILED'),
@JsonValue(r'SKIPPED')
SKIPPED(r'SKIPPED');

const DriverStopResponseStatusEnum(this.value);

final String value;

@override
String toString() => value;
}


