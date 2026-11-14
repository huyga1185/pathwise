//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/time_window_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'imported_stop_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ImportedStopResponse {
  /// Returns a new [ImportedStopResponse] instance.
  ImportedStopResponse({

    required  this.address,

    required  this.demands,

    required  this.label,

    required  this.lat,

    required  this.lon,

    required  this.note,

    required  this.recipientName,

    required  this.recipientPhone,

    required  this.serviceTimeSeconds,

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
    
    name: r'label',
    required: true,
    includeIfNull: true,
  )


  final String? label;



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
    
    name: r'serviceTimeSeconds',
    required: true,
    includeIfNull: true,
  )


  final int? serviceTimeSeconds;



  @JsonKey(
    
    name: r'timeWindow',
    required: true,
    includeIfNull: true,
  )


  final TimeWindowResponse? timeWindow;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ImportedStopResponse &&
      other.address == address &&
      other.demands == demands &&
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
        (address == null ? 0 : address.hashCode) +
        demands.hashCode +
        (label == null ? 0 : label.hashCode) +
        (lat == null ? 0 : lat.hashCode) +
        (lon == null ? 0 : lon.hashCode) +
        (note == null ? 0 : note.hashCode) +
        (recipientName == null ? 0 : recipientName.hashCode) +
        (recipientPhone == null ? 0 : recipientPhone.hashCode) +
        (serviceTimeSeconds == null ? 0 : serviceTimeSeconds.hashCode) +
        (timeWindow == null ? 0 : timeWindow.hashCode);

  factory ImportedStopResponse.fromJson(Map<String, dynamic> json) => _$ImportedStopResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ImportedStopResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

