// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'table.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Table _$TableFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Table', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['bytes', 'name', 'rows']);
      final val = Table(
        bytes: $checkedConvert('bytes', (v) => (v as num).toInt()),
        name: $checkedConvert('name', (v) => v as String),
        rows: $checkedConvert('rows', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$TableToJson(Table instance) => <String, dynamic>{
  'bytes': instance.bytes,
  'name': instance.name,
  'rows': instance.rows,
};
