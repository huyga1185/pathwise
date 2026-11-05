// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StoreResponse _$StoreResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('StoreResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['depot', 'id', 'name', 'timezone']);
      final val = StoreResponse(
        depot: $checkedConvert(
          'depot',
          (v) => v == null
              ? null
              : DepotResponse.fromJson(v as Map<String, dynamic>),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        name: $checkedConvert('name', (v) => v as String),
        timezone: $checkedConvert('timezone', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$StoreResponseToJson(StoreResponse instance) =>
    <String, dynamic>{
      'depot': instance.depot?.toJson(),
      'id': instance.id,
      'name': instance.name,
      'timezone': instance.timezone,
    };
