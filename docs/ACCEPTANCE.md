# ACCEPTANCE — Nghiệm thu MVP

> Đối chiếu từng tiêu chí của `REQUIREMENTS.md` §6 với bằng chứng (T43). Mỗi lần nghiệm thu lại (release mới), chạy lại các mục, cập nhật cột Bằng chứng và thêm một dòng vào §4.
> Ký hiệu: ✅ đạt, có bằng chứng · ⏳ chờ chủ dự án làm · ⚠️ đạt một phần, ghi rõ phần thiếu.
> Lần nghiệm thu gần nhất: 2026-11-03, `develop` tại `3850534` cộng branch `task/T43-acceptance`.

## 1. Tiêu chí MVP (`REQUIREMENTS.md` §6)

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Build và toàn bộ test xanh trên CI. | ✅ | `ci` trên `develop` xanh sau mỗi lần merge, gần nhất run 41604355307 (merge PR #55, `3850534`). Local `./mvnw clean verify` trên branch T43: 397/397 test, 49 class. |
| 2 | Kịch bản E2E: OWNER đăng ký → … → plan IN_PROGRESS → COMPLETED. | ✅ | Tự động: `acceptance/MvpScenarioIntegrationTest` chạy cả kịch bản chỉ qua API (24 stop: 20 chỉ có địa chỉ, 4 có toạ độ, time window, demand; geocode nền, sửa stop lỗi, optimize bằng OR-Tools thật, kiểm capacity/time window/ca, stop không xếp được, publish, mỗi tài xế thấy đúng route kèm toạ độ, cập nhật trạng thái tới COMPLETED, `version` không đổi). Thủ công trên stack thật: §2. |
| 3 | FR-A01..A08 có integration test cho nhánh thành công và lỗi chính. | ✅ | §3. |
| 4 | Tenant isolation: store A không đọc/sửa được xe/plan/stop/thành viên của store B (404). | ✅ | §3. |
| 5 | Không còn lỗi Critical/High mở trong `CURRENT_STATE.md`. | ✅ | F-00, F-01 (Critical) và F-02..F-07, F-34 (High) đều ở trạng thái đã sửa; mọi F-xx đang mở là Medium/Low/Info. |
| 6 | `docker compose --profile app up` chạy từ repo sạch theo README; image `develop-<sha>` có trên GHCR và deploy được theo `docs/DEPLOYMENT.md`. | ⚠️ | Compose: clone sạch `develop` (`3850534`), `cp .env.example .env` (chỉ đổi cổng để không đụng stack dev), `docker compose --profile app up -d --build` xong trong 2 phút 16 giây, `/actuator/health` = `UP`, chạy được kịch bản §2. GHCR: `image` run 41604355430 đẩy `ghcr.io/huyga1185/pathwise:develop-3850534`; `docker manifest inspect` đọc được không cần đăng nhập. **Deploy VPS: ⏳ chủ dự án** (checklist §2.2). |
| 7 | `API_SPEC.md` khớp implementation (kiểm bằng OpenAPI sinh ra). | ✅ | Tự động: `acceptance/ApiSpecContractIntegrationTest` so 45 endpoint (method + path) của OpenAPI sinh ra với mọi dòng không ❌ của `API_SPEC.md`, hai chiều; test fail khi một dòng spec lệch (đã thử). Thủ công: so field của mọi schema request/response trong `target/openapi.json` với JSON mẫu của spec: khớp, trừ `GET /admin/users` thiếu `storeId`, `status` (đã ghi ở API_SPEC §0). Tài liệu OpenAPI còn vài điểm không đúng thực tế: F-47. |

## 2. Demo thủ công trên stack thật

Script: `scripts/acceptance_demo.py` (chỉ dùng thư viện chuẩn Python). Chạy cùng kịch bản §1 mục 2 với Nominatim và OSRM thật, đọc OTP và mã mời từ Mailpit, in từng bước `[ok]`, dừng ở bước sai đầu tiên.

```bash
cp .env.example .env
docker compose --profile app up -d --build
python3 -I scripts/acceptance_demo.py http://localhost:8080/api/v1 http://localhost:8025
```

### 2.1 Lần chạy 2026-11-03 (local, stack từ clone sạch)

| Bước | Kết quả |
|---|---|
| OWNER đăng ký, xác thực bằng OTP trong Mailpit, đăng nhập, đặt depot | ✅ |
| Mời 1 DISPATCHER, 2 DRIVER; chấp nhận bằng mã trong email; đăng nhập | ✅ |
| DISPATCHER tạo 2 xe (6 BOX mỗi xe) | ✅ |
| Plan 10 stop (8 chỉ địa chỉ thật ở Đà Nẵng, 2 toạ độ, có time window), geocode nền | ⚠️ cả 8 địa chỉ `FAILED/PROVIDER_ERROR`: máy chạy demo không kết nối được `nominatim.openstreetmap.org:443` (bị từ chối ngay, cả từ host lẫn container; OSRM và site khác vẫn vào được). Nhánh lỗi chạy đúng spec: job `SUCCEEDED`, stop `FAILED` kèm lý do, plan về `IDLE` sau 8 s (1 req/s). **Geocode thành công với Nominatim thật chưa kiểm chứng: ⏳ chạy lại ở máy khác hoặc VPS.** |
| Sửa stop lỗi bằng toạ độ (`PATCH` stop) | ✅ |
| Optimize: OSRM `router.project-osrm.org` + OR-Tools | ✅ 11 s; 10,1 km. Route A tải 4/6, route B 6/6; stop có time window đến trong window; stop window 21:00–22:00 nằm ngoài ca → `INFEASIBLE` |
| Publish; mỗi DRIVER chỉ thấy route của mình, stop có `lat`/`lon` dựng được link Google Maps | ✅ |
| Cập nhật trạng thái: sau stop đầu plan `IN_PROGRESS`, sau stop cuối `COMPLETED`; DISPATCHER thấy khi tải lại | ✅ |
| CORS (`http://localhost:3000` 200, origin khác 403), `X-Request-Id`, `Referrer-Policy` | ✅ |

### 2.2 Checklist cho chủ dự án

- [ ] Chạy `scripts/acceptance_demo.py` ở máy vào được Nominatim: bước geocode phải có `resolved` ≥ 7 (chỉ địa chỉ giả cuối cùng `FAILED/NOT_FOUND`), toạ độ in ra nằm ở Đà Nẵng.
- [ ] Deploy `develop-<sha>` lên VPS staging theo `docs/DEPLOYMENT.md` (kể cả `CORS_ALLOWED_ORIGINS`); `curl https://<domain>/actuator/health` = `UP`.
- [ ] Trên staging: đăng ký bằng email thật, nhận OTP qua SMTP thật; `GET /api/v1/auth/sessions` cho thấy IP thật của máy bạn (không phải IP của proxy), tức forward headers hoạt động sau Caddy.
- [ ] Ghi ngày, tag và kết quả vào §4.

## 3. Bằng chứng theo tiêu chí 3 và 4

**FR-A01..A08** (tiêu chí 3):

| FR | Test (integration, Testcontainers) |
|---|---|
| A01 Đăng ký | `auth/RegistrationIntegrationTest`: `register_whenValid_createPendingOwnerWithStoreAndEmailOtp`, `register_whenEmailExistsInAnotherCase_return409AndCreateNoStore`, `register_whenEmailCannotBeSent_return201AndResendWorksRightAway` |
| A02 Xác thực email, gửi lại OTP có cooldown | `auth/RegistrationIntegrationTest`: `verifyEmail_whenCorrectOtp_activateAccountAndLogInWorks`, `verifyEmail_whenWrongOtp_return400InvalidOtpAndKeepPending`, `verifyEmail_whenAlreadyVerified_return409EmailAlreadyVerified`, `resend_whenCooldownRunning_return204AndSendNothing`, `resend_whenUnknownOrVerifiedEmail_return204AndSendNothing`, `logIn_whenEmailNotVerified_return403EmailNotVerified`; `auth/OtpManagerIntegrationTest` (sai 3 lần huỷ OTP, dùng một lần, gắn purpose và user) |
| A03 Log-in web/mobile | `auth/AuthTokenFlowIntegrationTest`: `logIn_whenWebClient_setRefreshCookieOnAuthPathAndOmitTokenFromBody`, `logIn_whenMobileClient_returnRefreshTokenInBodyWithoutCookie`; `auth/RegistrationIntegrationTest`: `logIn_whenEmailNotVerifiedButPasswordWrong_return401NotRevealingStatus`, `logIn_whenAccountDisabled_return403AccountDisabled` |
| A04 Refresh xoay vòng, tái sử dụng, hết hạn, user bị vô hiệu hoá | `auth/AuthTokenFlowIntegrationTest`: `refresh_whenMobileClient_rotateTokenThroughBody`, `refresh_whenWebCookie_rotateTokenAndSetNewCookie`, `refresh_whenTokenExpired_return401AndMarkTokenExpired`, `refresh_whenUsedTokenReusedAfterGrace_return401AndRevokeFamily`; `auth/RegistrationIntegrationTest.refresh_whenAccountDisabledAfterLogIn_return401InvalidToken` |
| A05 Log-out | `auth/AuthTokenFlowIntegrationTest`: `logOut_whenWebCookieWithoutAccessToken_return204ClearCookieAndRevokeFamily`, `logOut_whenMobileBody_return204AndRevokeFamily`, `logOut_whenNoRefreshToken_return204` |
| A06 Session | `auth/AuthSessionIntegrationTest` (10 test: danh sách kèm `current`, session hết hạn bị loại, thu hồi session khác/hiện tại, session của user khác → 404, thu hồi các session khác) |
| A07 Quên mật khẩu | `auth/PasswordResetIntegrationTest` (9 test: email lạ hoặc chưa xác thực vẫn 204, OTP sai, token dùng hai lần/hết hạn/sai → 400 `INVALID_RESET_TOKEN`, đặt lại thu hồi mọi session) |
| A08 Đổi mật khẩu | `auth/ChangePasswordIntegrationTest` (7 test: mật khẩu hiện tại sai không gửi email, OTP sai hoặc của user khác, cooldown 429, giữ session hiện tại và thu hồi session khác) |

Rate limit của các luồng này: `core/ratelimit/RateLimitIntegrationTest` (T40).

**Tenant isolation** (tiêu chí 4), mỗi test dùng hai store thật:

| Tài nguyên | Test |
|---|---|
| Xe | `fleet/VehicleIntegrationTest.vehicle_whenOfAnotherStore_return404AndNeverListed` |
| Thành viên | `store/StoreMemberIntegrationTest.updateMember_whenUserOfAnotherStore_return404AndChangeNothing`, `listMembers_whenOwnerOrDispatcher_returnOwnStoreMembersOnly` |
| Lời mời | `store/StoreInvitationIntegrationTest.revoke_whenInvitationOfAnotherStore_return404AndKeepIt` |
| Store | `store/StoreIntegrationTest.updateStore_whenOtherStoreUpdated_ownStoreUnchanged` |
| Plan và stop | `planning/PlanIntegrationTest.plan_whenOfAnotherStore_return404AndNeverListed` (đọc, `PUT`, `PATCH` stop, danh sách; stop của plan khác cùng store), `create_whenDriverOrVehicleOfAnotherStore_return422InvalidAssignment` |
| Geocode, optimize, publish, cancel, job | `planning/GeocodePlanJobIntegrationTest.job_whenOfAnotherStore_return404AndDriverGets403`, `planning/OptimizePlanIntegrationTest.optimize_whenPlanOfAnotherStoreOrDriver_return404Or403`, `planning/PublishPlanIntegrationTest.publishAndCancel_whenPlanOfAnotherStoreOrDriver_return404Or403` |
| Route và stop của tài xế | `planning/DriverRouteIntegrationTest.routes_whenRouteOfAnotherDriverOrStore_notListedAnd404`, `planning/DriverStopStatusIntegrationTest.updateStopStatus_whenRouteOrStopNotCallers_return404` |
| Quyền theo role | `auth/AuthorizationMatrixIntegrationTest` |

## 4. Lịch sử nghiệm thu

| Ngày | Phiên bản | Người chạy | Kết quả |
|---|---|---|---|
| 2026-11-03 | `develop` `3850534` + `task/T43-acceptance` | agent (T43) | Tiêu chí 1–5, 7 đạt; 6 đạt phần compose và GHCR, chờ deploy VPS; demo thật chưa kiểm được geocode thành công với Nominatim (mạng máy chạy demo chặn Nominatim). |
