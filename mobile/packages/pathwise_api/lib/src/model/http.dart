//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/group.dart';
import 'package:json_annotation/json_annotation.dart';

part 'http.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Http {
  /// Returns a new [Http] instance.
  Http({

    required  this.clientErrorRate,

    required  this.groups,

    required  this.requestsLastHour,

    required  this.requestsPerMinute,

    required  this.serverErrorRate,
  });

  @JsonKey(
    
    name: r'clientErrorRate',
    required: true,
    includeIfNull: true,
  )


  final double? clientErrorRate;



  @JsonKey(
    
    name: r'groups',
    required: true,
    includeIfNull: false,
  )


  final List<Group> groups;



  @JsonKey(
    
    name: r'requestsLastHour',
    required: true,
    includeIfNull: false,
  )


  final int requestsLastHour;



  @JsonKey(
    
    name: r'requestsPerMinute',
    required: true,
    includeIfNull: false,
  )


  final double requestsPerMinute;



  @JsonKey(
    
    name: r'serverErrorRate',
    required: true,
    includeIfNull: true,
  )


  final double? serverErrorRate;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Http &&
      other.clientErrorRate == clientErrorRate &&
      other.groups == groups &&
      other.requestsLastHour == requestsLastHour &&
      other.requestsPerMinute == requestsPerMinute &&
      other.serverErrorRate == serverErrorRate;

    @override
    int get hashCode =>
        (clientErrorRate == null ? 0 : clientErrorRate.hashCode) +
        groups.hashCode +
        requestsLastHour.hashCode +
        requestsPerMinute.hashCode +
        (serverErrorRate == null ? 0 : serverErrorRate.hashCode);

  factory Http.fromJson(Map<String, dynamic> json) => _$HttpFromJson(json);

  Map<String, dynamic> toJson() => _$HttpToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

