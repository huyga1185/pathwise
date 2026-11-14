//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'jvm.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Jvm {
  /// Returns a new [Jvm] instance.
  Jvm({

    required  this.heapMaxBytes,

    required  this.heapUsedBytes,
  });

  @JsonKey(
    
    name: r'heapMaxBytes',
    required: true,
    includeIfNull: true,
  )


  final int? heapMaxBytes;



  @JsonKey(
    
    name: r'heapUsedBytes',
    required: true,
    includeIfNull: false,
  )


  final int heapUsedBytes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Jvm &&
      other.heapMaxBytes == heapMaxBytes &&
      other.heapUsedBytes == heapUsedBytes;

    @override
    int get hashCode =>
        (heapMaxBytes == null ? 0 : heapMaxBytes.hashCode) +
        heapUsedBytes.hashCode;

  factory Jvm.fromJson(Map<String, dynamic> json) => _$JvmFromJson(json);

  Map<String, dynamic> toJson() => _$JvmToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

