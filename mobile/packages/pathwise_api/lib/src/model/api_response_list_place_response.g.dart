// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_list_place_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseListPlaceResponse _$ApiResponseListPlaceResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseListPlaceResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseListPlaceResponse(
    data: $checkedConvert(
      'data',
      (v) => (v as List<dynamic>)
          .map((e) => PlaceResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseListPlaceResponseToJson(
  ApiResponseListPlaceResponse instance,
) => <String, dynamic>{
  'data': instance.data.map((e) => e.toJson()).toList(),
  'message': ?instance.message,
};
