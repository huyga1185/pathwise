//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/time_window_request.dart';
import 'package:json_annotation/json_annotation.dart';

part 'patch_stop_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PatchStopRequest {
  /// Returns a new [PatchStopRequest] instance.
  PatchStopRequest({

     this.address,

     this.demands,

     this.label,

     this.lat,

     this.lon,

     this.note,

     this.recipientName,

     this.recipientPhone,

     this.serviceTimeSeconds,

     this.timeWindow,

    required  this.version,
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
    
    name: r'label',
    required: false,
    includeIfNull: false,
  )


  final String? label;



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



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PatchStopRequest &&
      other.address == address &&
      other.demands == demands &&
      other.label == label &&
      other.lat == lat &&
      other.lon == lon &&
      other.note == note &&
      other.recipientName == recipientName &&
      other.recipientPhone == recipientPhone &&
      other.serviceTimeSeconds == serviceTimeSeconds &&
      other.timeWindow == timeWindow &&
      other.version == version;

    @override
    int get hashCode =>
        (address == null ? 0 : address.hashCode) +
        demands.hashCode +
        label.hashCode +
        (lat == null ? 0 : lat.hashCode) +
        (lon == null ? 0 : lon.hashCode) +
        (note == null ? 0 : note.hashCode) +
        (recipientName == null ? 0 : recipientName.hashCode) +
        (recipientPhone == null ? 0 : recipientPhone.hashCode) +
        (serviceTimeSeconds == null ? 0 : serviceTimeSeconds.hashCode) +
        (timeWindow == null ? 0 : timeWindow.hashCode) +
        version.hashCode;

  factory PatchStopRequest.fromJson(Map<String, dynamic> json) => _$PatchStopRequestFromJson(json);

  Map<String, dynamic> toJson() => _$PatchStopRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

