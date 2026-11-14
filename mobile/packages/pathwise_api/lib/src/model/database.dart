//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/table.dart';
import 'package:json_annotation/json_annotation.dart';

part 'database.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Database {
  /// Returns a new [Database] instance.
  Database({

    required  this.activeConnections,

    required  this.idleConnections,

    required  this.largestTables,

    required  this.maxConnections,

    required  this.sizeBytes,
  });

  @JsonKey(
    
    name: r'activeConnections',
    required: true,
    includeIfNull: true,
  )


  final int? activeConnections;



  @JsonKey(
    
    name: r'idleConnections',
    required: true,
    includeIfNull: true,
  )


  final int? idleConnections;



  @JsonKey(
    
    name: r'largestTables',
    required: true,
    includeIfNull: false,
  )


  final List<Table> largestTables;



  @JsonKey(
    
    name: r'maxConnections',
    required: true,
    includeIfNull: true,
  )


  final int? maxConnections;



  @JsonKey(
    
    name: r'sizeBytes',
    required: true,
    includeIfNull: false,
  )


  final int sizeBytes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Database &&
      other.activeConnections == activeConnections &&
      other.idleConnections == idleConnections &&
      other.largestTables == largestTables &&
      other.maxConnections == maxConnections &&
      other.sizeBytes == sizeBytes;

    @override
    int get hashCode =>
        (activeConnections == null ? 0 : activeConnections.hashCode) +
        (idleConnections == null ? 0 : idleConnections.hashCode) +
        largestTables.hashCode +
        (maxConnections == null ? 0 : maxConnections.hashCode) +
        sizeBytes.hashCode;

  factory Database.fromJson(Map<String, dynamic> json) => _$DatabaseFromJson(json);

  Map<String, dynamic> toJson() => _$DatabaseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

