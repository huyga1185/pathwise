# API_SPEC — Hợp đồng REST API mục tiêu

> Đây là **hợp đồng mục tiêu** cho web và Flutter. Cột "Hiện trạng" cho biết endpoint tương ứng trong code hiện tại (nếu có). Mọi endpoint chưa có = chưa tồn tại.
> Mọi quyết định nền tảng của contract này đã được duyệt (`DECISIONS.md`, 2026-10-22). Frontend web và Flutter có thể dựa vào tài liệu này để thiết kế song song (D-09).
> Khi implementation khác spec: sửa code cho khớp spec, **hoặc** cập nhật spec trong cùng PR kèm lý do. Không để lệch.

## 0. Tình trạng implementation so với spec (cập nhật 2026-10-28, sau T20)

- ✅ Mọi endpoint đã dùng prefix `/api/v1` (P-02).
- ✅ Body lỗi đã theo `{code, details?}`, kể cả lỗi từ Spring Security: thiếu hoặc sai access token trả 401 `UNAUTHORIZED`, sai role trả 403 `FORBIDDEN`. `exception`/`stackTrace` chỉ có ở profile `dev` (P-03). Có slice test kiểm chứng (`ErrorHandlingWebMvcTest`).
- ✅ Mã HTTP đã khớp §9 cho các lỗi hiện có: sai đăng nhập 401, không tìm thấy user 404 (`USER_NOT_FOUND`), trùng email 409, JSON hỏng 400 `MALFORMED_REQUEST`, path không tồn tại 404 `NOT_FOUND`, sai method 405 `METHOD_NOT_ALLOWED`, vi phạm ràng buộc DB 409 `CONFLICT`, thiếu cookie credential 401 `INVALID_TOKEN`.
- §4: đã có `GET /store` và `PUT /store` (T13, có test, kể cả tenant isolation). Store luôn lấy từ claim `storeId` của access token. Quyền đúng cột Quyền (T17, có test ma trận): `GET` cho OWNER và DISPATCHER, `PUT` chỉ OWNER, role khác (kể cả ADMIN) 403 `FORBIDDEN`. T16 thêm toàn bộ phần còn lại của §4, có test (kể cả quyền và tenant isolation): `GET /store/members` (lọc `role`, `status`, `assignable`), `PATCH /store/members/{userId}`, `PATCH /store/members/me/can-drive`, `POST/GET /store/invitations`, `DELETE /store/invitations/{id}`, `GET /invitations/{code}`, `POST /invitations/accept`. Chi tiết hành vi mà spec chưa ghi:
  - `canDrive` trong member luôn `true` với DRIVER (D-21). `phoneNumber` trả dạng national.
  - `PATCH /store/members/{userId}`: user không thuộc store của actor → 404 `USER_NOT_FOUND`. OWNER chỉ đổi được `canDrive`, đổi `role`/`status` → 422 `CANNOT_MODIFY_OWNER`. `status`/`role` ngoài `ACTIVE|DISABLED` / `DISPATCHER|DRIVER` → 400 `MALFORMED_REQUEST`.
  - `DELETE /store/invitations/{id}` của store khác → 404 `NOT_FOUND`.
  - Lời mời PENDING đã quá `expiresAt` được trả với `status: EXPIRED` (lọc `status=EXPIRED` cũng tính chúng), DB không ghi lại.
  - `POST /invitations/accept`: mọi mã không dùng được (không tồn tại, đã dùng, đã huỷ, hết hạn) → 404 `INVITATION_INVALID` (không có 400, vì một `ErrorCode` chỉ có một HTTP status); `phoneNumber` sai → 400 `INVALID_PHONE_NUMBER`, lời mời vẫn PENDING.
  - `POST /store/invitations`: email lỗi vẫn trả 201 (ARCHITECTURE §7), OWNER mời lại để gửi mã mới. Chưa có 429 (T40).
- §6: `GET /geocoding/search` đã có (T21, có test): quyền OWNER/DISPATCHER, `q` 3–300 ký tự và `limit` 1–10 (sai → 400 `VALIDATION_ERROR`, detail `INVALID_QUERY_LENGTH`/`INVALID_LIMIT`; thiếu `q` → 400 `MALFORMED_REQUEST`), 503 `GEOCODING_BUSY`, 502 `GEOCODING_PROVIDER_ERROR`. Kết quả chỉ trong Việt Nam (`countrycodes=vn`). `GET /jobs/{id}` chưa có.
- §5: đủ 4 endpoint `/vehicles` (T20, có test, kể cả quyền và tenant isolation): OWNER/DISPATCHER, role khác 403; xe của store khác → 404 `NOT_FOUND`; trùng tên trong store → 409 `VEHICLE_NAME_EXISTED`, so sánh theo collation của DB (không phân biệt hoa thường); `PUT` thay toàn bộ, thiếu `plateNumber` thì xoá biển số; `capacityUnit` bắt buộc (D-36); `GET /vehicles` sắp theo tên. Không có endpoint xoá (xe chỉ chuyển `active=false`).
- Các mục §7, §8 (plans, driver) **chưa có endpoint nào trong code**.
- Đã thêm `ErrorCode` ở T13: `EMAIL_NOT_VERIFIED` 403, `ACCOUNT_DISABLED` 403, `EMAIL_ALREADY_VERIFIED` 409, `INVALID_TIMEZONE` 400 (khớp §9). T14 thêm `INVALID_RESET_TOKEN` 400 (P-26): spec §2 trước đó ghi `/password/reset` trả 400 `INVALID_TOKEN`, nhưng `INVALID_TOKEN` là 401 nên đã đổi sang mã mới.
- Endpoint vận hành ngoài `/api/v1`, giữ có chủ đích: `GET /actuator/health` (kèm `/liveness`, `/readiness`), Public, chỉ trả `status` và tên các `groups`, không lộ chi tiết thành phần (T05, `ARCHITECTURE.md` §12.1). Mọi endpoint actuator khác tắt.
- Endpoint đang có trong code nhưng **không thuộc spec**, sẽ bị xoá ở T27: `POST /api/v1/route/optimize` và `POST /api/v1/route/optimize/k` (từ T17 chỉ OWNER/DISPATCHER, role khác 403). Mã lỗi riêng của chúng (`INVALID_DEPOT`, `COULD_NOT_FIND_*`) cũng bỏ theo.
- Cột "Hiện trạng" chỉ có ở §2 và §3. Ký hiệu: ✅ khớp spec và đã có test · 🟡 có nhưng khác spec hoặc có lỗi · ❌ chưa có.

## 1. Quy ước chung

- Base path: **`/api/v1`**. JSON UTF-8. Thời gian sự kiện: ISO-8601 UTC (`2026-10-22T03:15:00Z`). Ngày: `YYYY-MM-DD`. Giờ trong ngày: `HH:mm` theo timezone của store.
- Xác thực: `Authorization: Bearer <accessToken>` trừ endpoint ghi **Public**.
- Header `X-Client-Platform: mobile` → refresh token đi qua body; không có → cookie `refreshToken` (web).
- Thành công: `{ "data": ..., "message"?: "..." }`. Danh sách phân trang: `data = { items, page, size, totalPages, totalElements }`, query `page` (từ 0), `size` (mặc định 20, tối đa 100), `sort=field,asc|desc` (whitelist).
- Lỗi: `{ "code": "ERROR_CODE", "details"?: [ { "field": "...", "code": "..." } ] }` (P-03).
- Optimistic concurrency cho plan: request thay đổi plan gửi `version`; lệch → 409 `PLAN_MODIFIED`.
- Ký hiệu quyền: **P** Public · **A** mọi user đã đăng nhập · **O** OWNER · **D** DISPATCHER · **R** người được gán route, tức `role=DRIVER` hoặc `canDrive=true` (D-21) · **AD** ADMIN.
- Thao tác chạy lâu dùng **job** (D-19): endpoint trả `202 { data: Job }`. Client theo dõi bằng `GET /jobs/{id}` hoặc qua `processingStatus` của resource. Một số job trong MVP hoàn tất ngay (status trả về đã là `SUCCEEDED`), nhưng client **phải luôn xử lý như job bất đồng bộ**.

## 2. Auth — `/api/v1/auth`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `POST /register` | P | `{email, password(8–72), storeName(1–150), fullName?}` | 201 `{data:{email, status:"PENDING_VERIFICATION"}}`; gửi OTP `VERIFY_EMAIL` | 400 validation · 409 `EMAIL_EXISTED` · 429 | 🟡 Khớp spec (T13, có test): 201 `{email, status}`, tạo store và OWNER trong một transaction, gửi OTP sau commit (email lỗi vẫn 201, gửi lại bằng `/email/verify/resend`), email chuẩn hoá nên trùng khác hoa thường → 409 `EMAIL_EXISTED`. Còn thiếu 429 (T40) |
| `POST /email/verify` | P | `{email, otp}` | 204 | 400 `INVALID_OTP` · 409 `EMAIL_ALREADY_VERIFIED` | ✅ T13: email không tồn tại cũng trả 400 `INVALID_OTP` |
| `POST /email/verify/resend` | P | `{email}` | 204 (luôn, không tiết lộ email tồn tại) | 429 `RATE_LIMITED` | 🟡 T13 (có test): luôn 204; chỉ gửi khi tài khoản đang `PENDING_VERIFICATION` và hết cooldown OTP (cooldown không trả `OTP_COOLDOWN`, để không lộ email có tồn tại). Còn thiếu 429 `RATE_LIMITED` (T40) |
| `POST /log-in` | P | `{email, password}` | 200 `LoginResponse` (+ cookie nếu web) | 401 `INVALID_EMAIL_OR_PASSWORD` · 403 `EMAIL_NOT_VERIFIED` · 403 `ACCOUNT_DISABLED` · 429 | 🟡 Transport và body khớp spec (T10, có test): `accessToken`, `accessTokenExpiresAt`, `refreshTokenExpiresAt`, `refreshToken` chỉ khi mobile, cookie `Path=/api/v1/auth; SameSite` cho web; sai thông tin 401 ✓. T13 (có test): `user` đủ `id`, `email`, `fullName` (bỏ khi chưa đặt), `role`, `storeId` (bỏ với ADMIN), `canDrive`; đúng mật khẩu nhưng chưa verify → 403 `EMAIL_NOT_VERIFIED`, DISABLED → 403 `ACCOUNT_DISABLED` (sai mật khẩu luôn 401, không lộ trạng thái). Còn thiếu 429 (T40) |
| `POST /refresh` | P | web: cookie · mobile: `{refreshToken}` | 200 `TokenResponse` (+ cookie mới nếu web) | 401 `INVALID_TOKEN` | ✅ T10, T13: cookie (web) hoặc body (mobile), trả `TokenResponse`, thiếu/sai/hết hạn token → 401 `INVALID_TOKEN`; user không còn ACTIVE (DISABLED, đã xoá) → 401 `INVALID_TOKEN` (T13, có test) |
| `POST /log-out` | P | web: cookie · mobile: `{refreshToken}` | 204 (idempotent; xoá cookie nếu web) | — | ✅ T10: public, cookie (web) hoặc body (mobile), luôn 204, web xoá cookie |
| `GET /sessions` | A | — | 200 `[SessionResponse]` | — | ✅ T11: mỗi session (family) còn hiệu lực một dòng, `current` theo claim `sid` |
| `DELETE /sessions/{sessionId}` | A | — | 204 | 404 `SESSION_NOT_FOUND` | ✅ T11: `sessionId` có thể là bất kỳ session nào của chính user. Nếu là **session hiện tại** (`current: true`) thì xử lý **như `/log-out`**: cùng thu hồi session, response giống `/log-out` (204, `no-store`, web xoá cookie `refreshToken`); client phải bỏ token như sau khi log-out. Session của user khác, không tồn tại, đã thu hồi hoặc hết hạn đều 404 `SESSION_NOT_FOUND` |
| `POST /sessions/revoke-others` | A | — | 204 | — | ✅ T11 (access token cấp trước T11 không có `sid` thì thu hồi mọi session) |
| `POST /password/forgot` | P | `{email}` | 204 (luôn); gửi OTP `RESET_PASSWORD` nếu email ACTIVE | 429 | 🟡 T14 (có test): luôn 204, chỉ gửi khi tài khoản ACTIVE và hết cooldown OTP; email lỗi vẫn 204. Còn thiếu 429 (T40) |
| `POST /password/reset/verify` | P | `{email, otp}` | 200 `{data:{resetToken, expiresAt}}` | 400 `INVALID_OTP` | ✅ T14: `resetToken` hạn 10 phút; email không tồn tại hoặc không ACTIVE cũng 400 `INVALID_OTP` |
| `POST /password/reset` | P | `{resetToken, newPassword}` | 204; revoke **mọi** session | 400 `INVALID_RESET_TOKEN` (P-26) | ✅ T14: đổi mật khẩu và thu hồi mọi session trong một transaction; token dùng một lần (lần thử đầu tiên tiêu thụ token, kể cả khi sai secret) |
| `POST /password/change/otp` | A | `{currentPassword}` | 204; gửi OTP `CHANGE_PASSWORD` | 400 `INVALID_CURRENT_PASSWORD` · 429 `OTP_COOLDOWN` | ✅ T15: sai mật khẩu hiện tại → 400 và không gửi email; trong cooldown → 429 `OTP_COOLDOWN`. Gửi email lỗi → 500 `INTERNAL_ERROR` (spec không có mã riêng; cooldown đã được xoá nên gọi lại được ngay) |
| `PUT /password` | A | `{otp, newPassword}` | 204; revoke mọi session **khác** | 400 `INVALID_OTP` | ✅ T15: OTP gắn userId của người gọi; đổi mật khẩu và thu hồi mọi session khác trong một transaction, session hiện tại (claim `sid`) vẫn refresh được. Luồng cũ `/update-password*` đã xoá (404) |

```jsonc
// LoginResponse.data
{
  "accessToken": "eyJ...", "accessTokenExpiresAt": "2026-10-22T03:29:00Z",
  "refreshToken": "…",              // CHỈ khi X-Client-Platform: mobile
  "refreshTokenExpiresAt": "2026-11-05T03:15:00Z",
  "user": { "id": 12, "email": "a@b.vn", "fullName": "An", "role": "OWNER", "storeId": 3, "canDrive": false }
}
// TokenResponse.data = LoginResponse.data không có "user"
// SessionResponse
{ "sessionId": "8a6f…-uuid", "userAgent": "…", "ipAddress": "…", "lastUsedAt": "…", "expiresAt": "…", "current": true }
```

Ghi chú:
- Access token chứa claim `sid` = sessionId (family) để xác định `current` (bổ sung cho P-09).
- `lastUsedAt` = thời điểm phát hành refresh token hiện tại của session, tức lần log-in hoặc refresh gần nhất (sai lệch tối đa bằng tuổi access token, 14 phút).
- `resetToken` là chuỗi opaque (client không phân tích).
- OTP: 6 chữ số; sai quá 3 lần → OTP huỷ, phải xin mã mới.

## 3. Users — `/api/v1/users`, `/api/v1/admin/users`

| Method & path | Quyền | Request | Response | Hiện trạng |
|---|---|---|---|---|
| `GET /users/me` | A | — | `{id, email, fullName, phoneNumber, role, storeId, status, canDrive}` | ✅ T13 (có test): đủ field. `canDrive` = true với DRIVER, còn lại theo cờ (D-21) |
| `PUT /users/me` | A | `{fullName?, phoneNumber?}` (số VN, lưu E.164, trả dạng national) | 204 | 🟡 `PUT /api/v1/users/me` `{phoneNumber}`; chưa có fullName |
| `GET /admin/users` | AD | paging; sort `id,email,createdAt,updatedAt` | Page `{id, email, phoneNumber, role, storeId, status, createdAt, updatedAt}` | 🟡 `GET /api/v1/admin/users` → `{id, email, phoneNumber, role, createdAt, updatedAt}`; role đã là `ADMIN/OWNER/DISPATCHER/DRIVER` (T13); thiếu storeId, status |

## 4. Store — `/api/v1/store`, `/api/v1/invitations`

Store luôn là store của actor (không có `storeId` trong path).

| Method & path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|
| `GET /store` | O, D | — | `{id, name, depotAddress, depotLat, depotLon, timezone}` | — |
| `PUT /store` | O | `{name, depotAddress?, depotLat?, depotLon?, timezone}` | 200 store | 400 `INVALID_TIMEZONE` |
| `GET /store/members` | O, D | query `role?`, `status?`, `assignable?` (true thì chỉ trả người được gán route: DRIVER hoặc `canDrive`, đang ACTIVE) | `[{id, email, fullName, phoneNumber, role, status, canDrive}]` | — |
| `PATCH /store/members/{userId}` | O | `{status?: ACTIVE|DISABLED, role?: DISPATCHER|DRIVER, canDrive?: boolean}` | 200 member. DISABLED thì revoke mọi session. | 404 · 422 `CANNOT_MODIFY_OWNER` (đổi role/status của OWNER) |
| `PATCH /store/members/me/can-drive` | O | `{canDrive}`. OWNER tự bật hoặc tắt việc tự đi giao. | 200 member | — |
| `POST /store/invitations` | O | `{email, role: DISPATCHER|DRIVER}` | 201 `{id, email, role, status, expiresAt}`; gửi email mã mời | 409 `EMAIL_EXISTED` · 429 |
| `GET /store/invitations` | O | query `status?` | `[invitation]` | — |
| `DELETE /store/invitations/{id}` | O | — | 204 (→ REVOKED) | 404 · 422 `INVITATION_NOT_PENDING` |
| `GET /invitations/{code}` | P | — | `{storeName, email, role, expiresAt}` (Should) | 404 `INVITATION_INVALID` |
| `POST /invitations/accept` | P | `{code, password, fullName, phoneNumber?}` | 204 (user ACTIVE, email verified; sau đó log-in) | 400/404 `INVITATION_INVALID` · 409 `EMAIL_EXISTED` |

## 5. Fleet — `/api/v1/vehicles`

| Method & path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|
| `GET /vehicles` | O, D | query `active?` | `[{id, name, plateNumber, capacity, capacityUnit, active}]` | — |
| `POST /vehicles` | O, D | `{name, plateNumber?, capacity(>0), capacityUnit}` | 201 vehicle | 409 `VEHICLE_NAME_EXISTED` |
| `GET /vehicles/{id}` | O, D | — | vehicle | 404 |
| `PUT /vehicles/{id}` | O, D | `{name, plateNumber?, capacity, capacityUnit, active}` | 200 vehicle | 404 · 409 |

`capacityUnit` (D-36): `KG`, `BOX` (thùng), `PACKAGE` (kiện), `ORDER` (đơn, tức số điểm giao). Giá trị khác → 400 `MALFORMED_REQUEST`. Xe chỉ bị giới hạn theo loại của nó khi optimize.

## 6. Geocoding và Jobs

| Method & path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|
| `GET /geocoding/search` | O, D | `q` (3–300 ký tự), `limit` (1–10, mặc định 5) | `[{displayName, lat, lon}]`. Dùng cho autocomplete hoặc sửa stop geocode lỗi (Should). | 503 `GEOCODING_BUSY` (không lấy được slot rate limit trong 2 s) · 502 `GEOCODING_PROVIDER_ERROR` |
| `GET /jobs/{id}` | O, D | — | `Job` | 404 `JOB_NOT_FOUND` |

```jsonc
// Job
{ "id": 55, "type": "GEOCODE_PLAN|OPTIMIZE_PLAN", "status": "QUEUED|RUNNING|SUCCEEDED|FAILED|CANCELLED",
  "planId": 7, "errorCode": null, "createdAt": "…", "startedAt": "…", "finishedAt": "…" }
```

## 7. Plans (dispatcher) — `/api/v1/plans`

**Vòng đời phía client:** tạo/sửa draft → nếu có stop chỉ có địa chỉ thì server tự tạo job `GEOCODE_PLAN` (`processingStatus = GEOCODING`) → client polling `GET /plans/{id}` cho tới khi `processingStatus = IDLE` → sửa các stop có `geocodeStatus = FAILED` → `optimize` (job) → xem kết quả → `publish`.

| Method & path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|
| `POST /plans` | O, D | `PlanDraftRequest` | 201 `PlanDetail` (DRAFT). Có stop chỉ có địa chỉ thì `processingStatus = GEOCODING` và `activeJob` khác null. | 400 · 422 `STORE_DEPOT_REQUIRED` · 422 `INVALID_ASSIGNMENT` · 422 `PLAN_TOO_LARGE` |
| `GET /plans` | O, D | `date?`, `status?`, paging | Page `PlanSummary` | — |
| `GET /plans/{id}` | O, D | — | `PlanDetail` | 404 |
| `PUT /plans/{id}` | O, D | `PlanDraftRequest` + `version`. Thay toàn bộ draft. Stop có `id` cũ và địa chỉ/toạ độ không đổi thì **giữ nguyên kết quả geocode**. Xoá kết quả tối ưu. | 200 `PlanDetail` | 409 `PLAN_MODIFIED` · 409 `PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` |
| `PATCH /plans/{id}/stops/{stopId}` | O, D | `{version, address?, lat?, lon?, …các field stop khác}`. Dùng để sửa một stop, chủ yếu là stop geocode lỗi. Có toạ độ thì `geocodeStatus = PROVIDED`; chỉ đổi địa chỉ thì geocode lại. | 200 `PlanDetail` | 409 · 422 `PLAN_NOT_EDITABLE` |
| `POST /plans/{id}/geocode` | O, D | `{version}`. Thử lại geocode cho mọi stop đang `FAILED`/`PENDING`. | 202 `Job` | 409 `PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` |
| `POST /plans/{id}/optimize` | O, D | `{version}` | 202 `Job` (MVP: thường đã `SUCCEEDED` hoặc `FAILED` ngay khi trả về). Kết quả đọc qua `GET /plans/{id}`. | 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `STOPS_NOT_GEOCODED` · 422 `PLAN_TOO_LARGE` |
| `POST /plans/{id}/publish` | O, D | `{version}` | 200 `PlanDetail` (PUBLISHED) | 409 · 422 `PLAN_NOT_OPTIMIZED` · 422 `SHIFT_OVERLAP` (`details` liệt kê driverId/vehicleId và plan bị trùng ca) |
| `POST /plans/{id}/cancel` | O, D | `{version}` | 200 `PlanDetail` (CANCELLED) | 409 · 422 `PLAN_NOT_CANCELLABLE` |
| `PATCH /plans/{id}/stops/{stopId}/status` | O, D | `{status: "PENDING"}` | 200 stop | 404 · 422 `INVALID_STOP_TRANSITION` |
| `GET /plans/{id}/driver-locations` | O, D | — | `[{driverId, routeId, lat, lon, accuracyMeters, headingDegrees, speedMps, recordedAt}]` (Should) | 404 |
| `PUT /plans/{id}/routes` | O, D | `{version, routes:[{routeId, stopIds:[...]}], unassignedStopIds:[...]}`. Chỉnh tay, tính lại ETA và quãng đường (Should, D-24). | 200 `PlanDetail`, có `violations[]` | 409 · 422 |

Job lỗi thì `Job.errorCode` có thể là `ROUTING_PROVIDER_ERROR`, `GEOCODING_PROVIDER_ERROR`, `INTERRUPTED` hoặc `INTERNAL_ERROR`, và plan quay về `processingStatus = IDLE`.

```jsonc
// PlanDraftRequest
{
  "name": "Giao sáng 22/10",
  "planDate": "2026-10-22",
  "depot": { "address": "…", "lat": 16.05, "lon": 108.2 },   // tuỳ chọn; mặc định là depot của store (D-20)
  "stops": [
    { "id": null,                        // id của stop cũ khi PUT, để giữ kết quả geocode
      "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "0905…",
      "address": "12 Lê Duẩn, Hải Châu, Đà Nẵng",
      "lat": null, "lon": null,          // tuỳ chọn; có thì bỏ qua geocode (D-18). Phải có address hoặc lat+lon.
      "demands": { "KG": 12, "BOX": 2 },  // tuỳ chọn, theo loại capacity (D-36); thiếu loại nào là 0. Không nhập ORDER: mỗi stop tự tính 1.
      "serviceTimeSeconds": 300,
      "timeWindow": { "start": "09:00", "end": "11:00" },  // tuỳ chọn
      "note": "Gọi trước 10 phút" }
  ],
  "assignments": [
    { "driverId": 21, "vehicleId": 5, "shiftStart": "08:00", "shiftEnd": "17:00", "maxDistanceMeters": 80000 }
    // MVP: start/end của route = depot của plan. Sau này có thể thêm "start"/"end" riêng cho từng route (D-20).
  ]
}
// PlanDetail
{
  "id": 7, "name": "…", "planDate": "2026-10-22", "status": "DRAFT", "version": 3,
  "processingStatus": "IDLE|GEOCODING|OPTIMIZING", "activeJob": null,   // hoặc object Job
  "geocodingSummary": { "total": 30, "resolved": 27, "provided": 1, "pending": 0, "failed": 2 },
  "depot": { "address": "…", "lat": 16.05, "lon": 108.2 }, "timezone": "Asia/Ho_Chi_Minh",
  "optimizedAt": "…|null", "totalDistanceMeters": 52340, "totalDurationSeconds": 9800,
  "routes": [
    { "id": 11, "driver": { "id": 21, "fullName": "Tùng", "role": "DRIVER" }, "vehicle": { "id": 5, "name": "Xe 1", "capacity": 40, "capacityUnit": "BOX" },
      "shiftStart": "08:00", "shiftEnd": "17:00", "maxDistanceMeters": 80000,
      "start": { "lat": 16.05, "lon": 108.2, "address": "…" }, "end": { "lat": 16.05, "lon": 108.2, "address": "…" },
      "plannedDistanceMeters": 30100, "plannedDurationSeconds": 5400,
      "plannedStart": "…", "plannedEnd": "…", "load": 31,   // theo capacityUnit của xe (D-36)
      "stops": [ { "id": 101, "sequence": 1, "label": "ĐH-1024", "lat": 16.06, "lon": 108.22,
                   "plannedArrival": "…", "timeWindow": {…}, "demands": { "BOX": 3 }, "status": "PENDING", "failureReason": null } ] }
  ],
  "unassignedStops": [   // stop chưa được gán route: plan chưa optimize, hoặc solver đã bỏ (P-13)
    { "id": 109, "label": "…", "address": "…", "lat": null, "lon": null,
      "geocodeStatus": "PROVIDED|PENDING|RESOLVED|FAILED", "geocodeError": "NOT_FOUND",
      "geocodedDisplayName": null, "dropReason": "NOT_OPTIMIZED|INFEASIBLE" }
  ]
}
```

Mọi stop trong `PlanDetail` (cả trong `routes[].stops`) đều có `address`, `geocodeStatus`, `geocodeError`, `geocodedDisplayName`. Ví dụ trên rút gọn bớt.

Demand (D-36): mỗi giá trị trong `demands` là số nguyên ≥ 0, khoá là `KG`, `BOX`, `PACKAGE` (khoá khác hoặc `ORDER` → 400). Xe tính theo `ORDER` chở tối đa `capacity` stop.

Giới hạn (D-19): ≤ 100 stop, ≤ 20 assignment mỗi plan. Mỗi người được gán (DRIVER hoặc `canDrive`) và mỗi xe chỉ xuất hiện một lần trong một plan.

## 8. Driver — `/api/v1/driver`

Người gọi phải có `role=DRIVER` hoặc `canDrive=true` (D-21), và chỉ thấy route có `driverId = mình` thuộc plan PUBLISHED, IN_PROGRESS hoặc COMPLETED.

| Method & path | Quyền | Request | Response | Lỗi chính |
|---|---|---|---|---|
| `GET /driver/routes` | R | `date` (mặc định hôm nay theo timezone store) | `[{routeId, planId, planName, planDate, status, stopCount, completedCount, plannedStart}]` | — |
| `GET /driver/routes/{routeId}` | R | — | `DriverRouteDetail` | 404 |
| `PATCH /driver/routes/{routeId}/stops/{stopId}/status` | R | `{status: DELIVERED|FAILED|SKIPPED, failureReason?}` | 200 stop | 404 · 422 `INVALID_STOP_TRANSITION` · 422 `FAILURE_REASON_REQUIRED` · 422 `PLAN_NOT_ACTIVE` |
| `POST /driver/location` | R | `{points:[{lat, lon, accuracyMeters?, headingDegrees?, speedMps?, recordedAt}]}`, gồm 1–100 điểm theo thứ tự thời gian. App gom các điểm bị mất mạng rồi gửi một lần (D-26). | 204 (Should). MVP chỉ giữ điểm có `recordedAt` mới nhất. | 400 |
| `GET /driver/routes/{routeId}/directions` | R | — | `DirectionsResponse` (Should, T33) | 404 · 502 |

```jsonc
// DriverRouteDetail
{
  "routeId": 11, "planId": 7, "planDate": "2026-10-22", "status": "IN_PROGRESS",
  "depot": { "lat": 16.05, "lon": 108.2, "address": "…" },
  "stops": [
    { "id": 101, "sequence": 1, "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "+84905…",
      "address": "…", "lat": 16.06, "lon": 108.22, "plannedArrival": "…",
      "timeWindow": { "start": "09:00", "end": "11:00" }, "note": "…", "status": "PENDING",
      "navigationUrl": "https://www.google.com/maps/dir/?api=1&destination=16.06,108.22&travelmode=driving" }
  ]
}
// DirectionsResponse (Should)
{ "legs": [ { "fromStopId": null, "toStopId": 101, "distanceMeters": 2100, "durationSeconds": 360,
              "geometry": "<polyline6>",
              "steps": [ { "maneuver": "turn", "modifier": "left", "roadName": "Nguyễn Văn Linh",
                           "distanceMeters": 400, "durationSeconds": 60, "location": [108.21, 16.055] } ] } ] }
```

## 9. Mã lỗi (`ErrorCode`)

| Code | HTTP | | Code | HTTP |
|---|---|---|---|---|
| `VALIDATION_ERROR` | 400 | | `PLAN_MODIFIED` | 409 |
| `MALFORMED_REQUEST` | 400 | | `PLAN_NOT_EDITABLE` | 422 |
| `UNAUTHORIZED` | 401 | | `PLAN_NOT_OPTIMIZED` | 422 |
| `INVALID_TOKEN` | 401 | | `PLAN_NOT_CANCELLABLE` | 422 |
| `INVALID_EMAIL_OR_PASSWORD` | 401 | | `PLAN_NOT_ACTIVE` | 422 |
| `FORBIDDEN` | 403 | | `PLAN_TOO_LARGE` | 422 |
| `EMAIL_NOT_VERIFIED` | 403 | | `STORE_DEPOT_REQUIRED` | 422 |
| `ACCOUNT_DISABLED` | 403 | | `INVALID_ASSIGNMENT` | 422 |
| `NOT_FOUND` / `*_NOT_FOUND` (kể cả `JOB_NOT_FOUND`) | 404 | | `INVALID_STOP_TRANSITION` | 422 |
| `EMAIL_EXISTED` | 409 | | `FAILURE_REASON_REQUIRED` | 422 |
| `EMAIL_ALREADY_VERIFIED` | 409 | | `CANNOT_MODIFY_OWNER` | 422 |
| `VEHICLE_NAME_EXISTED` | 409 | | `INVITATION_INVALID` | 404 |
| `INVALID_OTP` | 400 | | `INVITATION_NOT_PENDING` | 422 |
| `INVALID_CURRENT_PASSWORD` | 400 | | `INVALID_TIMEZONE` | 400 |
| `INVALID_SORT_FIELD` | 400 | | `INVALID_PHONE_NUMBER` | 400 |
| `OTP_COOLDOWN` | 429 | | `RATE_LIMITED` | 429 |
| `GEOCODING_BUSY` | 503 | | `GEOCODING_PROVIDER_ERROR` | 502 |
| `ROUTING_PROVIDER_ERROR` | 502 | | `INTERNAL_ERROR` | 500 |
| `PLAN_BUSY` | 409 | | `STOPS_NOT_GEOCODED` | 422 |
| `CONFLICT` | 409 | | `METHOD_NOT_ALLOWED` | 405 |
| `SHIFT_OVERLAP` | 422 | | `INVALID_RESET_TOKEN` | 400 |

Mã bị bỏ so với hiện tại: `INVALID_DEPOT`, `COULD_NOT_FIND_CHEAPEST_ROUTE`, `COULD_NOT_FIND_ADDRESS`, `COULD_NOT_UPDATE_USER_PASSWORD` (không còn dùng từ T15, giữ trong enum dạng `@Deprecated` vì `ErrorCode` chỉ được thêm). `COULD_NOT_FIND_USER` → `USER_NOT_FOUND` (404).

`CONFLICT` (vi phạm ràng buộc DB không có mã nghiệp vụ riêng, ví dụ hai request đăng ký đồng thời) và `METHOD_NOT_ALLOWED` được thêm ở T03 để không có lỗi framework nào rơi vào 500.

Validation detail codes (trong `details[].code`) dùng UPPER_SNAKE, ví dụ `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `INVALID_PASSWORD_LENGTH`, `PHONE_NUMBER_WRONG_REGION`.
