// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transfer_owner_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TransferOwnerRequest _$TransferOwnerRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('TransferOwnerRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['userId']);
  final val = TransferOwnerRequest(
    userId: $checkedConvert('userId', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$TransferOwnerRequestToJson(
  TransferOwnerRequest instance,
) => <String, dynamic>{'userId': instance.userId};
