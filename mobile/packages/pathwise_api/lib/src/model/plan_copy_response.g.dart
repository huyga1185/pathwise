// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_copy_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanCopyResponse _$PlanCopyResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanCopyResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['plan', 'skippedAssignments']);
      final val = PlanCopyResponse(
        plan: $checkedConvert(
          'plan',
          (v) => PlanDetailResponse.fromJson(v as Map<String, dynamic>),
        ),
        skippedAssignments: $checkedConvert(
          'skippedAssignments',
          (v) => (v as List<dynamic>)
              .map(
                (e) => SkippedAssignmentResponse.fromJson(
                  e as Map<String, dynamic>,
                ),
              )
              .toList(),
        ),
      );
      return val;
    });

Map<String, dynamic> _$PlanCopyResponseToJson(PlanCopyResponse instance) =>
    <String, dynamic>{
      'plan': instance.plan.toJson(),
      'skippedAssignments': instance.skippedAssignments
          .map((e) => e.toJson())
          .toList(),
    };
