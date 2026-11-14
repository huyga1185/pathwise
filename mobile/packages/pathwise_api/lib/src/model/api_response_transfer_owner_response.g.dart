// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_transfer_owner_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseTransferOwnerResponse _$ApiResponseTransferOwnerResponseFromJson(
  Map<String, dynamic> json,
) =>
    $checkedCreate('ApiResponseTransferOwnerResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseTransferOwnerResponse(
        data: $checkedConvert(
          'data',
          (v) => TransferOwnerResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseTransferOwnerResponseToJson(
  ApiResponseTransferOwnerResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
