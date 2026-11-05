//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:pathwise_api/src/model/plan_summary_response.dart';
import 'package:json_annotation/json_annotation.dart';

part 'page_response_plan_summary_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PageResponsePlanSummaryResponse {
  /// Returns a new [PageResponsePlanSummaryResponse] instance.
  PageResponsePlanSummaryResponse({

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


  final List<PlanSummaryResponse> items;



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
    bool operator ==(Object other) => identical(this, other) || other is PageResponsePlanSummaryResponse &&
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

  factory PageResponsePlanSummaryResponse.fromJson(Map<String, dynamic> json) => _$PageResponsePlanSummaryResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PageResponsePlanSummaryResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

