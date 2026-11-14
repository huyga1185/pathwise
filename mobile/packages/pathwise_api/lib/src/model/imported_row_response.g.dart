// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'imported_row_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ImportedRowResponse _$ImportedRowResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ImportedRowResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['errors', 'row', 'stop']);
      final val = ImportedRowResponse(
        errors: $checkedConvert(
          'errors',
          (v) => (v as List<dynamic>)
              .map((e) => FieldErrorDetail.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        row: $checkedConvert('row', (v) => (v as num).toInt()),
        stop: $checkedConvert(
          'stop',
          (v) => ImportedStopResponse.fromJson(v as Map<String, dynamic>),
        ),
      );
      return val;
    });

Map<String, dynamic> _$ImportedRowResponseToJson(
  ImportedRowResponse instance,
) => <String, dynamic>{
  'errors': instance.errors.map((e) => e.toJson()).toList(),
  'row': instance.row,
  'stop': instance.stop.toJson(),
};
