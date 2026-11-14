//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'package:dio/dio.dart';
import 'package:pathwise_api/src/auth/api_key_auth.dart';
import 'package:pathwise_api/src/auth/basic_auth.dart';
import 'package:pathwise_api/src/auth/bearer_auth.dart';
import 'package:pathwise_api/src/auth/oauth.dart';
import 'package:pathwise_api/src/api/admin_audit_controller_api.dart';
import 'package:pathwise_api/src/api/admin_store_controller_api.dart';
import 'package:pathwise_api/src/api/admin_user_controller_api.dart';
import 'package:pathwise_api/src/api/auth_controller_api.dart';
import 'package:pathwise_api/src/api/driver_location_controller_api.dart';
import 'package:pathwise_api/src/api/driver_route_controller_api.dart';
import 'package:pathwise_api/src/api/geocoding_controller_api.dart';
import 'package:pathwise_api/src/api/invitation_controller_api.dart';
import 'package:pathwise_api/src/api/job_controller_api.dart';
import 'package:pathwise_api/src/api/plan_controller_api.dart';
import 'package:pathwise_api/src/api/store_audit_controller_api.dart';
import 'package:pathwise_api/src/api/store_controller_api.dart';
import 'package:pathwise_api/src/api/store_invitation_controller_api.dart';
import 'package:pathwise_api/src/api/store_member_controller_api.dart';
import 'package:pathwise_api/src/api/user_controller_api.dart';
import 'package:pathwise_api/src/api/vehicle_controller_api.dart';

class PathwiseApi {
  static const String basePath = r'http://localhost';

  final Dio dio;
  PathwiseApi({
    Dio? dio,
    String? basePathOverride,
    List<Interceptor>? interceptors,
  })  : 
        this.dio = dio ??
            Dio(BaseOptions(
              baseUrl: basePathOverride ?? basePath,
              connectTimeout: const Duration(milliseconds: 5000),
              receiveTimeout: const Duration(milliseconds: 3000),
            )) {
    if (interceptors == null) {
      this.dio.interceptors.addAll([
        OAuthInterceptor(),
        BasicAuthInterceptor(),
        BearerAuthInterceptor(),
        ApiKeyAuthInterceptor(),
      ]);
    } else {
      this.dio.interceptors.addAll(interceptors);
    }
  }

  void setOAuthToken(String name, String token) {
    if (this.dio.interceptors.any((i) => i is OAuthInterceptor)) {
      (this.dio.interceptors.firstWhere((i) => i is OAuthInterceptor) as OAuthInterceptor).tokens[name] = token;
    }
  }

  void setBearerAuth(String name, String token) {
    if (this.dio.interceptors.any((i) => i is BearerAuthInterceptor)) {
      (this.dio.interceptors.firstWhere((i) => i is BearerAuthInterceptor) as BearerAuthInterceptor).tokens[name] = token;
    }
  }

  void setBasicAuth(String name, String username, String password) {
    if (this.dio.interceptors.any((i) => i is BasicAuthInterceptor)) {
      (this.dio.interceptors.firstWhere((i) => i is BasicAuthInterceptor) as BasicAuthInterceptor).authInfo[name] = BasicAuthInfo(username, password);
    }
  }

  void setApiKey(String name, String apiKey) {
    if (this.dio.interceptors.any((i) => i is ApiKeyAuthInterceptor)) {
      (this.dio.interceptors.firstWhere((element) => element is ApiKeyAuthInterceptor) as ApiKeyAuthInterceptor).apiKeys[name] = apiKey;
    }
  }

  /// Get AdminAuditControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  AdminAuditControllerApi getAdminAuditControllerApi() {
    return AdminAuditControllerApi(dio);
  }

  /// Get AdminStoreControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  AdminStoreControllerApi getAdminStoreControllerApi() {
    return AdminStoreControllerApi(dio);
  }

  /// Get AdminUserControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  AdminUserControllerApi getAdminUserControllerApi() {
    return AdminUserControllerApi(dio);
  }

  /// Get AuthControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  AuthControllerApi getAuthControllerApi() {
    return AuthControllerApi(dio);
  }

  /// Get DriverLocationControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  DriverLocationControllerApi getDriverLocationControllerApi() {
    return DriverLocationControllerApi(dio);
  }

  /// Get DriverRouteControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  DriverRouteControllerApi getDriverRouteControllerApi() {
    return DriverRouteControllerApi(dio);
  }

  /// Get GeocodingControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  GeocodingControllerApi getGeocodingControllerApi() {
    return GeocodingControllerApi(dio);
  }

  /// Get InvitationControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  InvitationControllerApi getInvitationControllerApi() {
    return InvitationControllerApi(dio);
  }

  /// Get JobControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  JobControllerApi getJobControllerApi() {
    return JobControllerApi(dio);
  }

  /// Get PlanControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  PlanControllerApi getPlanControllerApi() {
    return PlanControllerApi(dio);
  }

  /// Get StoreAuditControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  StoreAuditControllerApi getStoreAuditControllerApi() {
    return StoreAuditControllerApi(dio);
  }

  /// Get StoreControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  StoreControllerApi getStoreControllerApi() {
    return StoreControllerApi(dio);
  }

  /// Get StoreInvitationControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  StoreInvitationControllerApi getStoreInvitationControllerApi() {
    return StoreInvitationControllerApi(dio);
  }

  /// Get StoreMemberControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  StoreMemberControllerApi getStoreMemberControllerApi() {
    return StoreMemberControllerApi(dio);
  }

  /// Get UserControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  UserControllerApi getUserControllerApi() {
    return UserControllerApi(dio);
  }

  /// Get VehicleControllerApi instance, base route and serializer can be overridden by a given but be careful,
  /// by doing that all interceptors will not be executed
  VehicleControllerApi getVehicleControllerApi() {
    return VehicleControllerApi(dio);
  }
}
