// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'session_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SessionResponse _$SessionResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('SessionResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'current',
          'expiresAt',
          'ipAddress',
          'lastUsedAt',
          'sessionId',
          'userAgent',
        ],
      );
      final val = SessionResponse(
        current: $checkedConvert('current', (v) => v as bool),
        expiresAt: $checkedConvert(
          'expiresAt',
          (v) => DateTime.parse(v as String),
        ),
        ipAddress: $checkedConvert('ipAddress', (v) => v as String?),
        lastUsedAt: $checkedConvert(
          'lastUsedAt',
          (v) => DateTime.parse(v as String),
        ),
        sessionId: $checkedConvert('sessionId', (v) => v as String),
        userAgent: $checkedConvert('userAgent', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$SessionResponseToJson(SessionResponse instance) =>
    <String, dynamic>{
      'current': instance.current,
      'expiresAt': instance.expiresAt.toIso8601String(),
      'ipAddress': instance.ipAddress,
      'lastUsedAt': instance.lastUsedAt.toIso8601String(),
      'sessionId': instance.sessionId,
      'userAgent': instance.userAgent,
    };
