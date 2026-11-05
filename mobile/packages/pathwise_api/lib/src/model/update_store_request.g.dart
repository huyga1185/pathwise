// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_store_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateStoreRequest _$UpdateStoreRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('UpdateStoreRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['name', 'timezone']);
      final val = UpdateStoreRequest(
        depot: $checkedConvert(
          'depot',
          (v) => v == null
              ? null
              : DepotRequest.fromJson(v as Map<String, dynamic>),
        ),
        name: $checkedConvert('name', (v) => v as String),
        timezone: $checkedConvert('timezone', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$UpdateStoreRequestToJson(UpdateStoreRequest instance) =>
    <String, dynamic>{
      'depot': ?instance.depot?.toJson(),
      'name': instance.name,
      'timezone': instance.timezone,
    };
