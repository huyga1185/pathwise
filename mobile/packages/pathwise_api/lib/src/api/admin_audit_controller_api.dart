//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:pathwise_api/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:pathwise_api/src/model/api_response_page_response_audit_event_response.dart';

class AdminAuditControllerApi {

  final Dio _dio;

  const AdminAuditControllerApi(this._dio);

  /// listEvents1
  /// 
  ///
  /// Parameters:
  /// * [storeId] 
  /// * [actorId] 
  /// * [action] 
  /// * [from] 
  /// * [to] 
  /// * [page] - Zero-based page index (0..N)
  /// * [size] - The size of the page to be returned
  /// * [sort] - Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [ApiResponsePageResponseAuditEventResponse] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<ApiResponsePageResponseAuditEventResponse>> listEvents1({ 
    int? storeId,
    int? actorId,
    String? action,
    String? from,
    String? to,
    int? page = 0,
    int? size = 10,
    List<String>? sort,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/api/v1/admin/audit';
    final _options = Options(
      method: r'GET',
      headers: <String, dynamic>{
        ...?headers,
      },
      extra: <String, dynamic>{
        'secure': <Map<String, String>>[
          {
            'type': 'http',
            'scheme': 'bearer',
            'name': 'bearer',
          },
        ],
        ...?extra,
      },
      validateStatus: validateStatus,
    );

    final _queryParameters = <String, dynamic>{
      if (storeId != null) r'storeId': storeId,
      if (actorId != null) r'actorId': actorId,
      if (action != null) r'action': action,
      if (from != null) r'from': from,
      if (to != null) r'to': to,
      if (page != null) r'page': page,
      if (size != null) r'size': size,
      if (sort != null) r'sort': sort,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    ApiResponsePageResponseAuditEventResponse? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<ApiResponsePageResponseAuditEventResponse, ApiResponsePageResponseAuditEventResponse>(rawData, 'ApiResponsePageResponseAuditEventResponse', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<ApiResponsePageResponseAuditEventResponse>(
      data: _responseData,
      headers: _response.headers,
      isRedirect: _response.isRedirect,
      requestOptions: _response.requestOptions,
      redirects: _response.redirects,
      statusCode: _response.statusCode,
      statusMessage: _response.statusMessage,
      extra: _response.extra,
    );
  }

}
