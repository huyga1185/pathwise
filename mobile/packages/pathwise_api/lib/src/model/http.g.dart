// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'http.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Http _$HttpFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Http', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'clientErrorRate',
          'groups',
          'requestsLastHour',
          'requestsPerMinute',
          'serverErrorRate',
        ],
      );
      final val = Http(
        clientErrorRate: $checkedConvert(
          'clientErrorRate',
          (v) => (v as num?)?.toDouble(),
        ),
        groups: $checkedConvert(
          'groups',
          (v) => (v as List<dynamic>)
              .map((e) => Group.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        requestsLastHour: $checkedConvert(
          'requestsLastHour',
          (v) => (v as num).toInt(),
        ),
        requestsPerMinute: $checkedConvert(
          'requestsPerMinute',
          (v) => (v as num).toDouble(),
        ),
        serverErrorRate: $checkedConvert(
          'serverErrorRate',
          (v) => (v as num?)?.toDouble(),
        ),
      );
      return val;
    });

Map<String, dynamic> _$HttpToJson(Http instance) => <String, dynamic>{
  'clientErrorRate': instance.clientErrorRate,
  'groups': instance.groups.map((e) => e.toJson()).toList(),
  'requestsLastHour': instance.requestsLastHour,
  'requestsPerMinute': instance.requestsPerMinute,
  'serverErrorRate': instance.serverErrorRate,
};
