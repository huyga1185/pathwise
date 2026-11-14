// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transfer_owner_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TransferOwnerResponse _$TransferOwnerResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('TransferOwnerResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'ownerCanDrive',
      'ownerEmail',
      'ownerId',
      'previousOwnerEmail',
      'previousOwnerId',
      'storeId',
    ],
  );
  final val = TransferOwnerResponse(
    ownerCanDrive: $checkedConvert('ownerCanDrive', (v) => v as bool),
    ownerEmail: $checkedConvert('ownerEmail', (v) => v as String),
    ownerId: $checkedConvert('ownerId', (v) => (v as num).toInt()),
    previousOwnerEmail: $checkedConvert(
      'previousOwnerEmail',
      (v) => v as String?,
    ),
    previousOwnerId: $checkedConvert(
      'previousOwnerId',
      (v) => (v as num?)?.toInt(),
    ),
    storeId: $checkedConvert('storeId', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$TransferOwnerResponseToJson(
  TransferOwnerResponse instance,
) => <String, dynamic>{
  'ownerCanDrive': instance.ownerCanDrive,
  'ownerEmail': instance.ownerEmail,
  'ownerId': instance.ownerId,
  'previousOwnerEmail': instance.previousOwnerEmail,
  'previousOwnerId': instance.previousOwnerId,
  'storeId': instance.storeId,
};
