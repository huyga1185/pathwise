// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'field_error_detail.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

FieldErrorDetail _$FieldErrorDetailFromJson(Map<String, dynamic> json) =>
    $checkedCreate('FieldErrorDetail', json, ($checkedConvert) {
      final val = FieldErrorDetail(
        code: $checkedConvert('code', (v) => v as String?),
        field: $checkedConvert('field', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$FieldErrorDetailToJson(FieldErrorDetail instance) =>
    <String, dynamic>{'code': ?instance.code, 'field': ?instance.field};
