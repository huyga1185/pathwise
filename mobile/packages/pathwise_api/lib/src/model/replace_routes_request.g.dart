// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'replace_routes_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ReplaceRoutesRequest _$ReplaceRoutesRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ReplaceRoutesRequest', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['routes', 'unassignedStopIds', 'version'],
  );
  final val = ReplaceRoutesRequest(
    routes: $checkedConvert(
      'routes',
      (v) => (v as List<dynamic>)
          .map((e) => RouteStops.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    unassignedStopIds: $checkedConvert(
      'unassignedStopIds',
      (v) => (v as List<dynamic>).map((e) => (e as num).toInt()).toList(),
    ),
    version: $checkedConvert('version', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$ReplaceRoutesRequestToJson(
  ReplaceRoutesRequest instance,
) => <String, dynamic>{
  'routes': instance.routes.map((e) => e.toJson()).toList(),
  'unassignedStopIds': instance.unassignedStopIds,
  'version': instance.version,
};
