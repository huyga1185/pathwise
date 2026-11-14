//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/store_audit_event_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'page_response_store_audit_event_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PageResponseStoreAuditEventResponse {
  /// Returns a new [PageResponseStoreAuditEventResponse] instance.
  PageResponseStoreAuditEventResponse({

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


  final List<StoreAuditEventResponse> items;



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
    bool operator ==(Object other) => identical(this, other) || other is PageResponseStoreAuditEventResponse &&
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

  factory PageResponseStoreAuditEventResponse.fromJson(Map<String, dynamic> json) => _$PageResponseStoreAuditEventResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PageResponseStoreAuditEventResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

