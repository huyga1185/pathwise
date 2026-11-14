//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/audit_event_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'page_response_audit_event_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PageResponseAuditEventResponse {
  /// Returns a new [PageResponseAuditEventResponse] instance.
  PageResponseAuditEventResponse({

    required  this.items,

    required  this.page,

    required  this.size,

    required  this.totalElements,

    required  this.totalPages,
  });

  @JsonKey(
    
    name: r'items',
    required: true,
    includeIfNull: false,
  )


  final List<AuditEventResponse> items;



  @JsonKey(
    
    name: r'page',
    required: true,
    includeIfNull: false,
  )


  final int page;



  @JsonKey(
    
    name: r'size',
    required: true,
    includeIfNull: false,
  )


  final int size;



  @JsonKey(
    
    name: r'totalElements',
    required: true,
    includeIfNull: false,
  )


  final int totalElements;



  @JsonKey(
    
    name: r'totalPages',
    required: true,
    includeIfNull: false,
  )


  final int totalPages;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PageResponseAuditEventResponse &&
      other.items == items &&
      other.page == page &&
      other.size == size &&
      other.totalElements == totalElements &&
      other.totalPages == totalPages;

    @override
    int get hashCode =>
        items.hashCode +
        page.hashCode +
        size.hashCode +
        totalElements.hashCode +
        totalPages.hashCode;

  factory PageResponseAuditEventResponse.fromJson(Map<String, dynamic> json) => _$PageResponseAuditEventResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PageResponseAuditEventResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

