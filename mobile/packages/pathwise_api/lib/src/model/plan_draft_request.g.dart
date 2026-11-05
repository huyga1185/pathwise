// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_draft_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanDraftRequest _$PlanDraftRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('PlanDraftRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['name', 'planDate', 'version']);
  final val = PlanDraftRequest(
    assignments: $checkedConvert(
      'assignments',
      (v) => (v as List<dynamic>?)
          ?.map((e) => AssignmentRequest.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    depot: $checkedConvert(
      'depot',
      (v) =>
          v == null ? null : DepotRequest.fromJson(v as Map<String, dynamic>),
    ),
    name: $checkedConvert('name', (v) => v as String),
    planDate: $checkedConvert('planDate', (v) => DateTime.parse(v as String)),
    stops: $checkedConvert(
      'stops',
      (v) => (v as List<dynamic>?)
          ?.map((e) => PlanStopRequest.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    version: $checkedConvert('version', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$PlanDraftRequestToJson(PlanDraftRequest instance) =>
    <String, dynamic>{
      'assignments': ?instance.assignments?.map((e) => e.toJson()).toList(),
      'depot': ?instance.depot?.toJson(),
      'name': instance.name,
      'planDate': instance.planDate.toIso8601String(),
      'stops': ?instance.stops?.map((e) => e.toJson()).toList(),
      'version': instance.version,
    };
