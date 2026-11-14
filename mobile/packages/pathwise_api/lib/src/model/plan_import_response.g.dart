// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_import_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanImportResponse _$PlanImportResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('PlanImportResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['errorCount', 'ignoredColumns', 'rows', 'validCount'],
  );
  final val = PlanImportResponse(
    errorCount: $checkedConvert('errorCount', (v) => (v as num).toInt()),
    ignoredColumns: $checkedConvert(
      'ignoredColumns',
      (v) => (v as List<dynamic>).map((e) => e as String).toList(),
    ),
    rows: $checkedConvert(
      'rows',
      (v) => (v as List<dynamic>)
          .map((e) => ImportedRowResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    validCount: $checkedConvert('validCount', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$PlanImportResponseToJson(PlanImportResponse instance) =>
    <String, dynamic>{
      'errorCount': instance.errorCount,
      'ignoredColumns': instance.ignoredColumns,
      'rows': instance.rows.map((e) => e.toJson()).toList(),
      'validCount': instance.validCount,
    };
