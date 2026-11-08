// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'register_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RegisterResponse _$RegisterResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('RegisterResponse', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['email', 'status']);
      final val = RegisterResponse(
        email: $checkedConvert('email', (v) => v as String),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$RegisterResponseStatusEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$RegisterResponseToJson(RegisterResponse instance) =>
    <String, dynamic>{
      'email': instance.email,
      'status': _$RegisterResponseStatusEnumEnumMap[instance.status]!,
    };

const _$RegisterResponseStatusEnumEnumMap = {
  RegisterResponseStatusEnum.PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  RegisterResponseStatusEnum.ACTIVE: 'ACTIVE',
  RegisterResponseStatusEnum.DISABLED: 'DISABLED',
};
