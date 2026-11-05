//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'session_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SessionResponse {
  /// Returns a new [SessionResponse] instance.
  SessionResponse({

    required  this.current,

    required  this.expiresAt,

    required  this.ipAddress,

    required  this.lastUsedAt,

    required  this.sessionId,

    required  this.userAgent,
  });

  @JsonKey(
    
    name: r'current',
    required: true,
    includeIfNull: false,
  )


  final bool current;



  @JsonKey(
    
    name: r'expiresAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiresAt;



  @JsonKey(
    
    name: r'ipAddress',
    required: true,
    includeIfNull: true,
  )


  final String? ipAddress;



  @JsonKey(
    
    name: r'lastUsedAt',
    required: true,
    includeIfNull: false,
  )


  final DateTime lastUsedAt;



  @JsonKey(
    
    name: r'sessionId',
    required: true,
    includeIfNull: false,
  )


  final String sessionId;



  @JsonKey(
    
    name: r'userAgent',
    required: true,
    includeIfNull: true,
  )


  final String? userAgent;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SessionResponse &&
      other.current == current &&
      other.expiresAt == expiresAt &&
      other.ipAddress == ipAddress &&
      other.lastUsedAt == lastUsedAt &&
      other.sessionId == sessionId &&
      other.userAgent == userAgent;

    @override
    int get hashCode =>
        current.hashCode +
        expiresAt.hashCode +
        (ipAddress == null ? 0 : ipAddress.hashCode) +
        lastUsedAt.hashCode +
        sessionId.hashCode +
        (userAgent == null ? 0 : userAgent.hashCode);

  factory SessionResponse.fromJson(Map<String, dynamic> json) => _$SessionResponseFromJson(json);

  Map<String, dynamic> toJson() => _$SessionResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

