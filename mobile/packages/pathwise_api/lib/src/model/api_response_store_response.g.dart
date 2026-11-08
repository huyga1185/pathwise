// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_store_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseStoreResponse _$ApiResponseStoreResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseStoreResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseStoreResponse(
    data: $checkedConvert(
      'data',
      (v) => StoreResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseStoreResponseToJson(
  ApiResponseStoreResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
