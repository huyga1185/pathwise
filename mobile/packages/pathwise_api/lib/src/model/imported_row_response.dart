//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/field_error_detail.dart';
import 'package:pathwise_api/src/model/imported_stop_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'imported_row_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ImportedRowResponse {
  /// Returns a new [ImportedRowResponse] instance.
  ImportedRowResponse({

    required  this.errors,

    required  this.row,

    required  this.stop,
  });

  @JsonKey(
    
    name: r'errors',
    required: true,
    includeIfNull: false,
  )


  final List<FieldErrorDetail> errors;



  @JsonKey(
    
    name: r'row',
    required: true,
    includeIfNull: false,
  )


  final int row;



  @JsonKey(
    
    name: r'stop',
    required: true,
    includeIfNull: false,
  )


  final ImportedStopResponse stop;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ImportedRowResponse &&
      other.errors == errors &&
      other.row == row &&
      other.stop == stop;

    @override
    int get hashCode =>
        errors.hashCode +
        row.hashCode +
        stop.hashCode;

  factory ImportedRowResponse.fromJson(Map<String, dynamic> json) => _$ImportedRowResponseFromJson(json);

  Map<String, dynamic> toJson() => _$ImportedRowResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

