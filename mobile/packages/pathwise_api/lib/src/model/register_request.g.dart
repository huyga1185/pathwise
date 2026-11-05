// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'register_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RegisterRequest _$RegisterRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('RegisterRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['email', 'password', 'storeName']);
      final val = RegisterRequest(
        email: $checkedConvert('email', (v) => v as String),
        fullName: $checkedConvert('fullName', (v) => v as String?),
        password: $checkedConvert('password', (v) => v as String),
        storeName: $checkedConvert('storeName', (v) => v as String),
      );
      return val;
    });

Map<String, dynamic> _$RegisterRequestToJson(RegisterRequest instance) =>
    <String, dynamic>{
      'email': instance.email,
      'fullName': ?instance.fullName,
      'password': instance.password,
      'storeName': instance.storeName,
    };
