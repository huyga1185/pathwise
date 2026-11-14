import 'package:pathwise_api/src/model/accept_invitation_request.dart';
import 'package:pathwise_api/src/model/admin_get_user_response.dart';
import 'package:pathwise_api/src/model/admin_member_response.dart';
import 'package:pathwise_api/src/model/admin_store_detail_response.dart';
import 'package:pathwise_api/src/model/admin_store_state_response.dart';
import 'package:pathwise_api/src/model/admin_store_summary_response.dart';
import 'package:pathwise_api/src/model/api_response_admin_get_user_response.dart';
import 'package:pathwise_api/src/model/api_response_admin_store_detail_response.dart';
import 'package:pathwise_api/src/model/api_response_admin_store_state_response.dart';
import 'package:pathwise_api/src/model/api_response_auth_token_response.dart';
import 'package:pathwise_api/src/model/api_response_directions_response.dart';
import 'package:pathwise_api/src/model/api_response_driver_route_detail_response.dart';
import 'package:pathwise_api/src/model/api_response_driver_stop_update_response.dart';
import 'package:pathwise_api/src/model/api_response_get_user_response.dart';
import 'package:pathwise_api/src/model/api_response_invitation_preview_response.dart';
import 'package:pathwise_api/src/model/api_response_invitation_response.dart';
import 'package:pathwise_api/src/model/api_response_job_response.dart';
import 'package:pathwise_api/src/model/api_response_list_driver_location_response.dart';
import 'package:pathwise_api/src/model/api_response_list_driver_route_summary_response.dart';
import 'package:pathwise_api/src/model/api_response_list_invitation_response.dart';
import 'package:pathwise_api/src/model/api_response_list_member_response.dart';
import 'package:pathwise_api/src/model/api_response_list_place_response.dart';
import 'package:pathwise_api/src/model/api_response_list_session_response.dart';
import 'package:pathwise_api/src/model/api_response_list_vehicle_response.dart';
import 'package:pathwise_api/src/model/api_response_member_response.dart';
import 'package:pathwise_api/src/model/api_response_page_response_admin_get_user_response.dart';
import 'package:pathwise_api/src/model/api_response_page_response_admin_store_summary_response.dart';
import 'package:pathwise_api/src/model/api_response_page_response_audit_event_response.dart';
import 'package:pathwise_api/src/model/api_response_page_response_plan_summary_response.dart';
import 'package:pathwise_api/src/model/api_response_page_response_store_audit_event_response.dart';
import 'package:pathwise_api/src/model/api_response_plan_detail_response.dart';
import 'package:pathwise_api/src/model/api_response_register_response.dart';
import 'package:pathwise_api/src/model/api_response_reset_token_response.dart';
import 'package:pathwise_api/src/model/api_response_store_response.dart';
import 'package:pathwise_api/src/model/api_response_transfer_owner_response.dart';
import 'package:pathwise_api/src/model/api_response_vehicle_response.dart';
import 'package:pathwise_api/src/model/assignment_request.dart';
import 'package:pathwise_api/src/model/audit_event_response.dart';
import 'package:pathwise_api/src/model/auth_token_response.dart';
import 'package:pathwise_api/src/model/auth_user_response.dart';
import 'package:pathwise_api/src/model/change_password_request.dart';
import 'package:pathwise_api/src/model/create_invitation_request.dart';
import 'package:pathwise_api/src/model/create_vehicle_request.dart';
import 'package:pathwise_api/src/model/depot_request.dart';
import 'package:pathwise_api/src/model/depot_response.dart';
import 'package:pathwise_api/src/model/directions_destination.dart';
import 'package:pathwise_api/src/model/directions_instruction.dart';
import 'package:pathwise_api/src/model/directions_point.dart';
import 'package:pathwise_api/src/model/directions_response.dart';
import 'package:pathwise_api/src/model/driver.dart';
import 'package:pathwise_api/src/model/driver_location_response.dart';
import 'package:pathwise_api/src/model/driver_position.dart';
import 'package:pathwise_api/src/model/driver_route_detail_response.dart';
import 'package:pathwise_api/src/model/driver_route_summary_response.dart';
import 'package:pathwise_api/src/model/driver_sharing.dart';
import 'package:pathwise_api/src/model/driver_stop_response.dart';
import 'package:pathwise_api/src/model/driver_stop_update_response.dart';
import 'package:pathwise_api/src/model/forgot_password_request.dart';
import 'package:pathwise_api/src/model/geocoding_summary_response.dart';
import 'package:pathwise_api/src/model/get_user_response.dart';
import 'package:pathwise_api/src/model/invitation_preview_response.dart';
import 'package:pathwise_api/src/model/invitation_response.dart';
import 'package:pathwise_api/src/model/job_response.dart';
import 'package:pathwise_api/src/model/location_batch_request.dart';
import 'package:pathwise_api/src/model/location_point_request.dart';
import 'package:pathwise_api/src/model/location_response.dart';
import 'package:pathwise_api/src/model/lock_store_request.dart';
import 'package:pathwise_api/src/model/log_in_request.dart';
import 'package:pathwise_api/src/model/member_counts_response.dart';
import 'package:pathwise_api/src/model/member_response.dart';
import 'package:pathwise_api/src/model/page_response_admin_get_user_response.dart';
import 'package:pathwise_api/src/model/page_response_admin_store_summary_response.dart';
import 'package:pathwise_api/src/model/page_response_audit_event_response.dart';
import 'package:pathwise_api/src/model/page_response_plan_summary_response.dart';
import 'package:pathwise_api/src/model/page_response_store_audit_event_response.dart';
import 'package:pathwise_api/src/model/patch_stop_request.dart';
import 'package:pathwise_api/src/model/place_response.dart';
import 'package:pathwise_api/src/model/plan_detail_response.dart';
import 'package:pathwise_api/src/model/plan_draft_request.dart';
import 'package:pathwise_api/src/model/plan_route_response.dart';
import 'package:pathwise_api/src/model/plan_stop_request.dart';
import 'package:pathwise_api/src/model/plan_stop_response.dart';
import 'package:pathwise_api/src/model/plan_summary_response.dart';
import 'package:pathwise_api/src/model/plan_version_request.dart';
import 'package:pathwise_api/src/model/refresh_token_request.dart';
import 'package:pathwise_api/src/model/register_request.dart';
import 'package:pathwise_api/src/model/register_response.dart';
import 'package:pathwise_api/src/model/replace_routes_request.dart';
import 'package:pathwise_api/src/model/request_password_change_otp_request.dart';
import 'package:pathwise_api/src/model/resend_verification_request.dart';
import 'package:pathwise_api/src/model/reset_link_otp_request.dart';
import 'package:pathwise_api/src/model/reset_password_by_link_request.dart';
import 'package:pathwise_api/src/model/reset_password_request.dart';
import 'package:pathwise_api/src/model/reset_token_response.dart';
import 'package:pathwise_api/src/model/route_stops.dart';
import 'package:pathwise_api/src/model/session_response.dart';
import 'package:pathwise_api/src/model/store_audit_event_response.dart';
import 'package:pathwise_api/src/model/store_period_stats_response.dart';
import 'package:pathwise_api/src/model/store_response.dart';
import 'package:pathwise_api/src/model/time_window_request.dart';
import 'package:pathwise_api/src/model/time_window_response.dart';
import 'package:pathwise_api/src/model/transfer_owner_request.dart';
import 'package:pathwise_api/src/model/transfer_owner_response.dart';
import 'package:pathwise_api/src/model/update_can_drive_request.dart';
import 'package:pathwise_api/src/model/update_location_sharing_request.dart';
import 'package:pathwise_api/src/model/update_member_request.dart';
import 'package:pathwise_api/src/model/update_profile_request.dart';
import 'package:pathwise_api/src/model/update_stop_status_request.dart';
import 'package:pathwise_api/src/model/update_store_request.dart';
import 'package:pathwise_api/src/model/update_vehicle_request.dart';
import 'package:pathwise_api/src/model/vehicle.dart';
import 'package:pathwise_api/src/model/vehicle_response.dart';
import 'package:pathwise_api/src/model/verify_email_request.dart';
import 'package:pathwise_api/src/model/verify_reset_otp_request.dart';
import 'package:pathwise_api/src/model/violation_response.dart';

final _regList = RegExp(r'^List<(.*)>$');
final _regSet = RegExp(r'^Set<(.*)>$');
final _regMap = RegExp(r'^Map<String,(.*)>$');

  ReturnType deserialize<ReturnType, BaseType>(dynamic value, String targetType, {bool growable= true}) {
      switch (targetType) {
        case 'String':
          return '$value' as ReturnType;
        case 'int':
          return (value is int ? value : int.parse('$value')) as ReturnType;
        case 'bool':
          if (value is bool) {
            return value as ReturnType;
          }
          final valueString = '$value'.toLowerCase();
          return (valueString == 'true' || valueString == '1') as ReturnType;
        case 'double':
          return (value is double ? value : double.parse('$value')) as ReturnType;
        case 'AcceptInvitationRequest':
          return AcceptInvitationRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AdminGetUserResponse':
          return AdminGetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AdminMemberResponse':
          return AdminMemberResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AdminStoreDetailResponse':
          return AdminStoreDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AdminStoreStateResponse':
          return AdminStoreStateResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AdminStoreSummaryResponse':
          return AdminStoreSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseAdminGetUserResponse':
          return ApiResponseAdminGetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseAdminStoreDetailResponse':
          return ApiResponseAdminStoreDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseAdminStoreStateResponse':
          return ApiResponseAdminStoreStateResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseAuthTokenResponse':
          return ApiResponseAuthTokenResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseDirectionsResponse':
          return ApiResponseDirectionsResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseDriverRouteDetailResponse':
          return ApiResponseDriverRouteDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseDriverStopUpdateResponse':
          return ApiResponseDriverStopUpdateResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseGetUserResponse':
          return ApiResponseGetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseInvitationPreviewResponse':
          return ApiResponseInvitationPreviewResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseInvitationResponse':
          return ApiResponseInvitationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseJobResponse':
          return ApiResponseJobResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListDriverLocationResponse':
          return ApiResponseListDriverLocationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListDriverRouteSummaryResponse':
          return ApiResponseListDriverRouteSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListInvitationResponse':
          return ApiResponseListInvitationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListMemberResponse':
          return ApiResponseListMemberResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListPlaceResponse':
          return ApiResponseListPlaceResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListSessionResponse':
          return ApiResponseListSessionResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseListVehicleResponse':
          return ApiResponseListVehicleResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseMemberResponse':
          return ApiResponseMemberResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePageResponseAdminGetUserResponse':
          return ApiResponsePageResponseAdminGetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePageResponseAdminStoreSummaryResponse':
          return ApiResponsePageResponseAdminStoreSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePageResponseAuditEventResponse':
          return ApiResponsePageResponseAuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePageResponsePlanSummaryResponse':
          return ApiResponsePageResponsePlanSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePageResponseStoreAuditEventResponse':
          return ApiResponsePageResponseStoreAuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponsePlanDetailResponse':
          return ApiResponsePlanDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseRegisterResponse':
          return ApiResponseRegisterResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseResetTokenResponse':
          return ApiResponseResetTokenResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseStoreResponse':
          return ApiResponseStoreResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseTransferOwnerResponse':
          return ApiResponseTransferOwnerResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ApiResponseVehicleResponse':
          return ApiResponseVehicleResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AssignmentRequest':
          return AssignmentRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AuditEventResponse':
          return AuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AuthTokenResponse':
          return AuthTokenResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'AuthUserResponse':
          return AuthUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ChangePasswordRequest':
          return ChangePasswordRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'CreateInvitationRequest':
          return CreateInvitationRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'CreateVehicleRequest':
          return CreateVehicleRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DepotRequest':
          return DepotRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DepotResponse':
          return DepotResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DirectionsDestination':
          return DirectionsDestination.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DirectionsInstruction':
          return DirectionsInstruction.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DirectionsPoint':
          return DirectionsPoint.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DirectionsResponse':
          return DirectionsResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Driver':
          return Driver.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverLocationResponse':
          return DriverLocationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverPosition':
          return DriverPosition.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverRouteDetailResponse':
          return DriverRouteDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverRouteSummaryResponse':
          return DriverRouteSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverSharing':
          return DriverSharing.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverStopResponse':
          return DriverStopResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'DriverStopUpdateResponse':
          return DriverStopUpdateResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ForgotPasswordRequest':
          return ForgotPasswordRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'GeocodingSummaryResponse':
          return GeocodingSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'GetUserResponse':
          return GetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'InvitationPreviewResponse':
          return InvitationPreviewResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'InvitationResponse':
          return InvitationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'JobResponse':
          return JobResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LocationBatchRequest':
          return LocationBatchRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LocationPointRequest':
          return LocationPointRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LocationResponse':
          return LocationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LockStoreRequest':
          return LockStoreRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'LogInRequest':
          return LogInRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'MemberCountsResponse':
          return MemberCountsResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'MemberResponse':
          return MemberResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PageResponseAdminGetUserResponse':
          return PageResponseAdminGetUserResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PageResponseAdminStoreSummaryResponse':
          return PageResponseAdminStoreSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PageResponseAuditEventResponse':
          return PageResponseAuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PageResponsePlanSummaryResponse':
          return PageResponsePlanSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PageResponseStoreAuditEventResponse':
          return PageResponseStoreAuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PatchStopRequest':
          return PatchStopRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlaceResponse':
          return PlaceResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanDetailResponse':
          return PlanDetailResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanDraftRequest':
          return PlanDraftRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanRouteResponse':
          return PlanRouteResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanStopRequest':
          return PlanStopRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanStopResponse':
          return PlanStopResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanSummaryResponse':
          return PlanSummaryResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'PlanVersionRequest':
          return PlanVersionRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RefreshTokenRequest':
          return RefreshTokenRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RegisterRequest':
          return RegisterRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RegisterResponse':
          return RegisterResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ReplaceRoutesRequest':
          return ReplaceRoutesRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RequestPasswordChangeOtpRequest':
          return RequestPasswordChangeOtpRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResendVerificationRequest':
          return ResendVerificationRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResetLinkOtpRequest':
          return ResetLinkOtpRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResetPasswordByLinkRequest':
          return ResetPasswordByLinkRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResetPasswordRequest':
          return ResetPasswordRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ResetTokenResponse':
          return ResetTokenResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'RouteStops':
          return RouteStops.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'SessionResponse':
          return SessionResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'StoreAuditEventResponse':
          return StoreAuditEventResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'StorePeriodStatsResponse':
          return StorePeriodStatsResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'StoreResponse':
          return StoreResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'TimeWindowRequest':
          return TimeWindowRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'TimeWindowResponse':
          return TimeWindowResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'TransferOwnerRequest':
          return TransferOwnerRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'TransferOwnerResponse':
          return TransferOwnerResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateCanDriveRequest':
          return UpdateCanDriveRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateLocationSharingRequest':
          return UpdateLocationSharingRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateMemberRequest':
          return UpdateMemberRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateProfileRequest':
          return UpdateProfileRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateStopStatusRequest':
          return UpdateStopStatusRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateStoreRequest':
          return UpdateStoreRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'UpdateVehicleRequest':
          return UpdateVehicleRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'Vehicle':
          return Vehicle.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'VehicleResponse':
          return VehicleResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'VerifyEmailRequest':
          return VerifyEmailRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'VerifyResetOtpRequest':
          return VerifyResetOtpRequest.fromJson(value as Map<String, dynamic>) as ReturnType;
        case 'ViolationResponse':
          return ViolationResponse.fromJson(value as Map<String, dynamic>) as ReturnType;
        default:
          RegExpMatch? match;

          if (value is List && (match = _regList.firstMatch(targetType)) != null) {
            targetType = match![1]!; // ignore: parameter_assignments
            return value
              .map<BaseType>((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable))
              .toList(growable: growable) as ReturnType;
          }
          if (value is Set && (match = _regSet.firstMatch(targetType)) != null) {
            targetType = match![1]!; // ignore: parameter_assignments
            return value
              .map<BaseType>((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable))
              .toSet() as ReturnType;
          }
          if (value is Map && (match = _regMap.firstMatch(targetType)) != null) {
            targetType = match![1]!.trim(); // ignore: parameter_assignments
            return Map<String, BaseType>.fromIterables(
              value.keys as Iterable<String>,
              value.values.map((dynamic v) => deserialize<BaseType, BaseType>(v, targetType, growable: growable)),
            ) as ReturnType;
          }
          break;
    }
    throw Exception('Cannot deserialize');
  }