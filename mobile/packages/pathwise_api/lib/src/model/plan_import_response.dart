//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/imported_row_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'plan_import_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PlanImportResponse {
  /// Returns a new [PlanImportResponse] instance.
  PlanImportResponse({

    required  this.errorCount,

    required  this.ignoredColumns,

    required  this.rows,

    required  this.validCount,
  });

  @JsonKey(
    
    name: r'errorCount',
    required: true,
    includeIfNull: false,
  )


  final int errorCount;



  @JsonKey(
    
    name: r'ignoredColumns',
    required: true,
    includeIfNull: false,
  )


  final List<String> ignoredColumns;



  @JsonKey(
    
    name: r'rows',
    required: true,
    includeIfNull: false,
  )


  final List<ImportedRowResponse> rows;



  @JsonKey(
    
    name: r'validCount',
    required: true,
    includeIfNull: false,
  )


  final int validCount;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PlanImportResponse &&
      other.errorCount == errorCount &&
      other.ignoredColumns == ignoredColumns &&
      other.rows == rows &&
      other.validCount == validCount;

    @override
    int get hashCode =>
        errorCount.hashCode +
        ignoredColumns.hashCode +
        rows.hashCode +
        validCount.hashCode;

  factory PlanImportResponse.fromJson(Map<String, dynamic> json) => _$PlanImportResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PlanImportResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

