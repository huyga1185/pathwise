//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'field_error_detail.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class FieldErrorDetail {
  /// Returns a new [FieldErrorDetail] instance.
  FieldErrorDetail({

     this.code,

     this.field,
  });

  @JsonKey(
    
    name: r'code',
    required: false,
    includeIfNull: false,
  )


  final String? code;



  @JsonKey(
    
    name: r'field',
    required: false,
    includeIfNull: false,
  )


  final String? field;





    @override
    bool operator ==(Object other) => identical(this, other) || other is FieldErrorDetail &&
      other.code == code &&
      other.field == field;

    @override
    int get hashCode =>
        code.hashCode +
        field.hashCode;

  factory FieldErrorDetail.fromJson(Map<String, dynamic> json) => _$FieldErrorDetailFromJson(json);

  Map<String, dynamic> toJson() => _$FieldErrorDetailToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

