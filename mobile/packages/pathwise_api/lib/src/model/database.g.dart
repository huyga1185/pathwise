// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'database.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Database _$DatabaseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('Database', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'activeConnections',
          'idleConnections',
          'largestTables',
          'maxConnections',
          'sizeBytes',
        ],
      );
      final val = Database(
        activeConnections: $checkedConvert(
          'activeConnections',
          (v) => (v as num?)?.toInt(),
        ),
        idleConnections: $checkedConvert(
          'idleConnections',
          (v) => (v as num?)?.toInt(),
        ),
        largestTables: $checkedConvert(
          'largestTables',
          (v) => (v as List<dynamic>)
              .map((e) => Table.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        maxConnections: $checkedConvert(
          'maxConnections',
          (v) => (v as num?)?.toInt(),
        ),
        sizeBytes: $checkedConvert('sizeBytes', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$DatabaseToJson(Database instance) => <String, dynamic>{
  'activeConnections': instance.activeConnections,
  'idleConnections': instance.idleConnections,
  'largestTables': instance.largestTables.map((e) => e.toJson()).toList(),
  'maxConnections': instance.maxConnections,
  'sizeBytes': instance.sizeBytes,
};
