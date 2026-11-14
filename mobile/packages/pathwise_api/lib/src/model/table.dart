//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'table.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Table {
  /// Returns a new [Table] instance.
  Table({

    required  this.bytes,

    required  this.name,

    required  this.rows,
  });

  @JsonKey(
    
    name: r'bytes',
    required: true,
    includeIfNull: false,
  )


  final int bytes;



  @JsonKey(
    
    name: r'name',
    required: true,
    includeIfNull: false,
  )


  final String name;



  @JsonKey(
    
    name: r'rows',
    required: true,
    includeIfNull: false,
  )


  final int rows;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Table &&
      other.bytes == bytes &&
      other.name == name &&
      other.rows == rows;

    @override
    int get hashCode =>
        bytes.hashCode +
        name.hashCode +
        rows.hashCode;

  factory Table.fromJson(Map<String, dynamic> json) => _$TableFromJson(json);

  Map<String, dynamic> toJson() => _$TableToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

