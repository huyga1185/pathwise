// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'lock_store_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LockStoreRequest _$LockStoreRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('LockStoreRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['reason']);
      final val = LockStoreRequest(
        reason: $checkedConvert('reason', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$LockStoreRequestToJson(LockStoreRequest instance) =>
    <String, dynamic>{'reason': instance.reason};
