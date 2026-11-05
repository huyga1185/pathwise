//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/time_window_request.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_stop_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanStopRequest {
  /// Returns a new [PlanStopRequest] instance.
  PlanStopRequest({

     this.address,

     this.demands,

     this.id,

    required  this.label,

     this.lat,

     this.lon,

     this.note,

     this.recipientName,

     this.recipientPhone,

     this.serviceTimeSeconds,

     this.timeWindow,
  });

  @JsonKey(
    
    name: r'address',
    required: false,
    includeIfNull: false,
  )


  final String? address;



  @JsonKey(
    
    name: r'demands',
    required: false,
    includeIfNull: false,
  )


  final Map<String, int>? demands;



  @JsonKey(
    
    name: r'id',
    required: false,
    includeIfNull: false,
  )


  final int? id;



  @JsonKey(
    
    name: r'label',
    required: true,
    includeIfNull: false,
  )


  final String label;



          // minimum: -90
          // maximum: 90
  @JsonKey(
    
    name: r'lat',
    required: false,
    includeIfNull: false,
  )


  final double? lat;



          // minimum: -180
          // maximum: 180
  @JsonKey(
    
    name: r'lon',
    required: false,
    includeIfNull: false,
  )


  final double? lon;



  @JsonKey(
    
    name: r'note',
    required: false,
    includeIfNull: false,
  )


  final String? note;



  @JsonKey(
    
    name: r'recipientName',
    required: false,
    includeIfNull: false,
  )


  final String? recipientName;



  @JsonKey(
    
    name: r'recipientPhone',
    required: false,
    includeIfNull: false,
  )


  final String? recipientPhone;



          // minimum: 0
          // maximum: 86400
  @JsonKey(
    
    name: r'serviceTimeSeconds',
    required: false,
    includeIfNull: false,
  )


  final int? serviceTimeSeconds;



  @JsonKey(
    
    name: r'timeWindow',
    required: false,
    includeIfNull: false,
  )


  final TimeWindowRequest? timeWindow;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanStopRequest &&
      other.address == address &&
      other.demands == demands &&
      other.id == id &&
      other.label == label &&
      other.lat == lat &&
      other.lon == lon &&
      other.note == note &&
      other.recipientName == recipientName &&
      other.recipientPhone == recipientPhone &&
      other.serviceTimeSeconds == serviceTimeSeconds &&
      other.timeWindow == timeWindow;

    @override
    int get hashCode =>
        address.hashCode +
        demands.hashCode +
        id.hashCode +
        label.hashCode +
        lat.hashCode +
        lon.hashCode +
        note.hashCode +
        recipientName.hashCode +
        recipientPhone.hashCode +
        serviceTimeSeconds.hashCode +
        timeWindow.hashCode;

  factory PlanStopRequest.fromJson(Map<String, dynamic> json) => _$PlanStopRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PlanStopRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

