# API_SPEC — Hợp đồng REST API mục tiêu

> Đây là **hợp đồng mục tiêu** cho web và Flutter (D-09). Frontend có thể dựa vào tài liệu này để thiết kế song song.
> Khi implementation khác spec: sửa code cho khớp spec, **hoặc** cập nhật spec trong cùng PR kèm lý do. Không để lệch.
> Cột "Hiện trạng" ở mọi bảng: ✅ khớp spec và có test · 🟡 có nhưng còn lệch (ghi rõ lệch gì) · ❌ chưa có. Lịch sử triển khai theo task nằm ở `CURRENT_STATE.md` §5, không ghi ở đây.
> Lần rà soát contract gần nhất: T44 (2026-10-29, P-32).

## 0. Lệch hiện tại so với spec (cập nhật 2026-10-29, sau T44)

- Chưa có 429 `RATE_LIMITED` cho `/register`, `/email/verify/resend`, `/log-in`, `/password/forgot`, `POST /store/invitations` (T40).
- `GET /admin/users` chưa trả `storeId`, `status`.
- §7: chưa có `optimize` (T25), `publish`, `cancel`, `stops/{stopId}/status` (T26), `driver-locations` (T32), `routes` (T28).
- §8: chưa có endpoint nào (T30–T33).
- Endpoint vận hành ngoài `/api/v1`, giữ có chủ đích: `GET /actuator/health` (kèm `/liveness`, `/readiness`), Public, chỉ trả `status` và tên các `groups` (T05, `ARCHITECTURE.md` §12.1). Mọi endpoint actuator khác tắt.
- Endpoint có trong code nhưng **không thuộc spec**, sẽ bị xoá ở T27: `POST /api/v1/route/optimize`, `POST /api/v1/route/optimize/k` (chỉ OWNER/DISPATCHER). Mã lỗi riêng của chúng (`INVALID_DEPOT`, `COULD_NOT_FIND_*`) bỏ theo.

## 1. Quy ước chung

**Định dạng**
- Base path **`/api/v1`**. JSON UTF-8.
- Thời gian sự kiện: ISO-8601 UTC (`2026-10-22T03:15:00Z`). Ngày: `YYYY-MM-DD`. Giờ trong ngày: đúng dạng `HH:mm`, theo timezone của store (P-14); dạng khác (ví dụ `09:00:30`) → 400 `MALFORMED_REQUEST`. Client đổi thời gian sự kiện sang giờ địa phương bằng `timezone` của resource (ví dụ `PlanDetail.timezone`).
- **Số điện thoại** (P-32): request nhận số VN ở dạng bất kỳ (`0905 123 456`, `+84905123456`); response **luôn trả E.164** (`+84905123456`). Định dạng hiển thị là việc của client. Số sai → 400 với mã `PHONE_NUMBER_*` (`PHONE_NUMBER_INVALID`, `PHONE_NUMBER_WRONG_REGION`, `PHONE_NUMBER_TOO_SHORT`, `PHONE_NUMBER_TOO_LONG`, `PHONE_NUMBER_TYPE_NOT_ALLOWED`). Chuỗi rỗng là số sai; muốn bỏ số thì gửi `null` hoặc không gửi field (F-43).
- Vị trí luôn là object `{address, lat, lon}` (`address` có thể null), ví dụ `depot`, `start`, `end`.
- Đơn vị: mét, giây; capacity và demand là số nguyên (P-14).

**Xác thực và quyền**
- `Authorization: Bearer <accessToken>` trừ endpoint ghi **Public**.
- Header `X-Client-Platform: mobile` → refresh token đi qua body; không có → cookie `refreshToken` (web).
- Ký hiệu quyền: **P** Public · **A** mọi user đã đăng nhập · **O** OWNER · **D** DISPATCHER · **R** người được gán route, tức `role=DRIVER` hoặc `canDrive=true` (D-21) · **AD** ADMIN. Sai role → 403 `FORBIDDEN`; thiếu hoặc sai access token → 401 `UNAUTHORIZED`.
- Dữ liệu của store luôn là store trong access token (BR-01). Tài nguyên của store khác trả 404 như không tồn tại.

**Thành công**
- `{ "data": ..., "message"?: "..." }`.
- Danh sách phân trang: `data = { items, page, size, totalPages, totalElements }`, query `page` (từ 0), `size` (mặc định 20, tối đa 100; lớn hơn thì dùng 100), `sort=field,asc|desc` (whitelist từng endpoint; ngoài whitelist → 400 `INVALID_SORT_FIELD`).
- Danh sách nhỏ theo bản chất (xe, thành viên, lời mời, session của một store hay một user) trả mảng, không phân trang.
- Endpoint sửa một resource trả lại resource đó (200), trừ khi ghi khác.

**Lỗi** (P-03)
- `{ "code": "ERROR_CODE", "details"?: [ { "field": "...", "code": "...", ...thêm field theo mã } ] }`. `exception`/`stackTrace` chỉ có ở profile `dev`.
- `details[].field` là **đường dẫn tới field trong request** mà client cần sửa, kể cả với kiểm tra chéo nhiều field: ví dụ `stops[0].timeWindow.end` cho `start ≥ end`, `stops[0].address` khi stop không có địa chỉ lẫn toạ độ, `depot.lon` khi thiếu kinh độ (P-32).
- Một mã lỗi có thể thêm field vào từng phần tử `details` khi client cần dữ liệu đó; các field thêm được ghi ở mã lỗi tương ứng (ví dụ `SHIFT_OVERLAP` ở §7).
- 404: tài nguyên trên path không tồn tại hoặc thuộc store khác → `NOT_FOUND`. Mã 404 riêng chỉ dùng khi client phải xử lý khác đi (P-24), hiện chỉ có `INVITATION_INVALID` (P-32).
- Mã HTTP chuẩn: JSON hỏng hoặc giá trị enum lạ → 400 `MALFORMED_REQUEST`; path không tồn tại → 404 `NOT_FOUND`; sai method → 405 `METHOD_NOT_ALLOWED`; vi phạm ràng buộc DB không có mã riêng → 409 `CONFLICT`.

**PATCH = JSON Merge Patch** (RFC 7396, P-32)
- Mọi `PATCH`: field **vắng** trong JSON thì giữ nguyên; field có mặt với `null` thì **xoá** giá trị.
- Field không xoá được mà gửi `null` → 400 `VALIDATION_ERROR`, detail `{field, code: "NOT_NULLABLE"}`.

**Concurrency và job**
- Optimistic concurrency cho plan: request thay đổi plan gửi `version`; lệch → 409 `PLAN_MODIFIED`. `version` chỉ tăng khi **nội dung plan** đổi (sửa draft, kết quả geocode/optimize, publish, cancel); cập nhật trạng thái stop **không** tăng `version` của plan (P-32).
- Thao tác chạy lâu dùng **job** (D-19): endpoint trả `202 { data: Job }`. Client theo dõi bằng `GET /jobs/{id}` hoặc qua `processingStatus` của resource. Một số job trong MVP hoàn tất ngay (status trả về đã là `SUCCEEDED`), nhưng client **phải luôn xử lý như job bất đồng bộ**.

## 2. Auth — `/api/v1/auth`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `POST /register` | P | `{email, password(8–72), storeName(1–150), fullName?}` | 201 `{email, status: "PENDING_VERIFICATION"}`; gửi OTP `VERIFY_EMAIL` | 400 · 409 `EMAIL_EXISTED` · 429 | 🟡 chưa có 429 |
| `POST /email/verify` | P | `{email, otp}` | 204 | 400 `INVALID_OTP` · 409 `EMAIL_ALREADY_VERIFIED` | ✅ |
| `POST /email/verify/resend` | P | `{email}` | 204 (luôn) | 429 `RATE_LIMITED` | 🟡 chưa có 429 |
| `POST /log-in` | P | `{email, password}` | 200 `LoginResponse` (+ cookie nếu web) | 401 `INVALID_EMAIL_OR_PASSWORD` · 403 `EMAIL_NOT_VERIFIED` · 403 `ACCOUNT_DISABLED` · 429 | 🟡 chưa có 429 |
| `POST /refresh` | P | web: cookie · mobile: `{refreshToken}` | 200 `TokenResponse` (+ cookie mới nếu web) | 401 `INVALID_TOKEN` | ✅ |
| `POST /log-out` | P | web: cookie · mobile: `{refreshToken}` | 204 (idempotent; xoá cookie nếu web) | — | ✅ |
| `GET /sessions` | A | — | 200 `[SessionResponse]` | — | ✅ |
| `DELETE /sessions/{sessionId}` | A | — | 204 | 404 `NOT_FOUND` | ✅ |
| `POST /sessions/revoke-others` | A | — | 204 | — | ✅ |
| `POST /password/forgot` | P | `{email}` | 204 (luôn); gửi OTP `RESET_PASSWORD` nếu email ACTIVE | 429 | 🟡 chưa có 429 |
| `POST /password/reset/verify` | P | `{email, otp}` | 200 `{resetToken, expiresAt}` | 400 `INVALID_OTP` | ✅ |
| `POST /password/reset` | P | `{resetToken, newPassword}` | 204; revoke **mọi** session | 400 `INVALID_RESET_TOKEN` (P-26) | ✅ |
| `POST /password/change/otp` | A | `{currentPassword}` | 204; gửi OTP `CHANGE_PASSWORD` | 400 `INVALID_CURRENT_PASSWORD` · 429 `OTP_COOLDOWN` | ✅ |
| `PUT /password` | A | `{otp, newPassword}` | 204; revoke mọi session **khác** | 400 `INVALID_OTP` | ✅ |

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
- Không tiết lộ email có tồn tại hay không: `/email/verify` với email không tồn tại → 400 `INVALID_OTP`; `/email/verify/resend`, `/password/forgot` luôn 204 (chỉ gửi khi tài khoản đúng trạng thái và hết cooldown OTP); `/log-in` sai mật khẩu luôn 401, chỉ báo `EMAIL_NOT_VERIFIED`/`ACCOUNT_DISABLED` khi mật khẩu đúng; `/password/reset/verify` với email không tồn tại hoặc không ACTIVE → 400 `INVALID_OTP`.
- `/register`: tạo store và OWNER trong một transaction, gửi OTP sau commit; email lỗi vẫn 201, gửi lại bằng `/email/verify/resend`. Email chuẩn hoá (`trim().toLowerCase()`, P-19), trùng khác hoa thường → 409.
- `LoginResponse.user`: `fullName` bỏ khi chưa đặt, `storeId` bỏ với ADMIN, `canDrive` luôn `true` với DRIVER.
- Cookie web: `HttpOnly; Secure; SameSite=<cấu hình>; Path=/api/v1/auth`. Thiếu, sai, hết hạn refresh token, hoặc user không còn ACTIVE → 401 `INVALID_TOKEN`.
- Access token chứa claim `sid` = sessionId (family) để xác định `current` (P-09). `lastUsedAt` = lần log-in hoặc refresh gần nhất của session.
- `DELETE /sessions/{sessionId}`: session của chính user. Nếu là session hiện tại thì xử lý **như `/log-out`** (204, `no-store`, web xoá cookie); client bỏ token như sau khi log-out. Session của user khác, không tồn tại, đã thu hồi hoặc hết hạn → 404.
- `POST /sessions/revoke-others`: access token cấp trước khi có claim `sid` thì thu hồi mọi session.
- `resetToken` là chuỗi opaque, hạn 10 phút, dùng một lần (lần thử đầu tiên tiêu thụ token, kể cả khi sai). `/password/reset` đổi mật khẩu và thu hồi mọi session trong một transaction.
- `/password/change/otp`: sai mật khẩu hiện tại → 400 và không gửi email; gửi email lỗi → 500 `INTERNAL_ERROR` (cooldown đã được xoá nên gọi lại được ngay). `PUT /password`: OTP gắn userId của người gọi; session hiện tại vẫn refresh được.
- OTP: 6 chữ số; sai quá 3 lần → OTP huỷ, phải xin mã mới.

## 3. Users — `/api/v1/users`, `/api/v1/admin/users`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /users/me` | A | — | `User` | — | ✅ |
| `PATCH /users/me` | A | merge patch `{fullName?, phoneNumber?}`; `null` xoá giá trị | 200 `User` | 400 (`FULL_NAME_REQUIRED` khi chuỗi trắng, `INVALID_FULL_NAME_LENGTH` ≤ 100, `PHONE_NUMBER_*`) | ✅ |
| `GET /admin/users` | AD | paging; sort `id`, `email`, `createdAt`, `updatedAt` | Page `{id, email, phoneNumber, role, storeId, status, createdAt, updatedAt}` | 400 `INVALID_SORT_FIELD` | 🟡 chưa trả `storeId`, `status` |

```jsonc
// User
{ "id": 12, "email": "a@b.vn", "fullName": "An", "phoneNumber": "+84905123456", "role": "OWNER", "storeId": 3,
  "status": "ACTIVE", "canDrive": false }   // canDrive luôn true với DRIVER (D-21)
```

## 4. Store — `/api/v1/store`, `/api/v1/invitations`

Store luôn là store của actor (không có `storeId` trong path).

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /store` | O, D | — | `Store` | — | ✅ |
| `PUT /store` | O | `{name, depot?: {address?, lat, lon} \| null, timezone}`; `depot` null hoặc vắng = không có depot | 200 `Store` | 400 `INVALID_TIMEZONE` | ✅ |
| `GET /store/members` | O, D | query `role?`, `status?`, `assignable?` (true: chỉ người được gán route, tức DRIVER hoặc `canDrive`, đang ACTIVE) | `[Member]` | — | ✅ |
| `PATCH /store/members/{userId}` | O | merge patch `{status?: ACTIVE\|DISABLED, role?: DISPATCHER\|DRIVER, canDrive?}`; không field nào xoá được (`null` → `NOT_NULLABLE`). DISABLED thì thu hồi mọi session. | 200 `Member` | 404 · 422 `CANNOT_MODIFY_OWNER` | ✅ |
| `PATCH /store/members/me/can-drive` | O | `{canDrive}`: OWNER tự bật/tắt việc tự đi giao | 200 `Member` | — | ✅ |
| `POST /store/invitations` | O | `{email, role: DISPATCHER\|DRIVER}` | 201 `Invitation`; gửi email mã mời | 409 `EMAIL_EXISTED` · 429 | 🟡 chưa có 429 |
| `GET /store/invitations` | O | query `status?` | `[Invitation]` | — | ✅ |
| `POST /store/invitations/{id}/revoke` | O | — | 204; lời mời thành `REVOKED`, vẫn có trong danh sách | 404 · 422 `INVITATION_NOT_PENDING` | ✅ |
| `GET /invitations/{code}` | P | — | `{storeName, email, role, expiresAt}` | 404 `INVITATION_INVALID` | ✅ |
| `POST /invitations/accept` | P | `{code, password, fullName, phoneNumber?}` | 204 (user ACTIVE, email đã xác thực; sau đó log-in) | 404 `INVITATION_INVALID` · 409 `EMAIL_EXISTED` · 400 `INVALID_PHONE_NUMBER` | ✅ |

```jsonc
// Store
{ "id": 3, "name": "Tiệm A", "depot": { "address": "12 Lê Duẩn", "lat": 16.05, "lon": 108.2 }, "timezone": "Asia/Ho_Chi_Minh" }
// depot là null khi store chưa có depot
// Member
{ "id": 21, "email": "…", "fullName": "Tùng", "phoneNumber": "+84905…", "role": "DRIVER", "status": "ACTIVE", "canDrive": true }
// Invitation
{ "id": 5, "email": "…", "role": "DRIVER", "status": "PENDING|ACCEPTED|REVOKED|EXPIRED", "expiresAt": "…" }
```

Ghi chú:
- `depot` trong `PUT /store`: `lat`, `lon` bắt buộc (depot không được geocode), `address` tuỳ chọn ≤ 500. Mã validation: `STORE_NAME_REQUIRED`, `INVALID_STORE_NAME_LENGTH` (≤ 150), `LATITUDE_REQUIRED`, `LONGITUDE_REQUIRED`, `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_ADDRESS_LENGTH`, `TIMEZONE_REQUIRED`. `timezone` là IANA zone id.
- `PATCH /store/members/{userId}`: OWNER chỉ đổi được `canDrive`; đổi `role`/`status` của OWNER → 422 `CANNOT_MODIFY_OWNER`. Giá trị enum ngoài danh sách → 400 `MALFORMED_REQUEST`.
- Lời mời hết hạn sau 7 ngày (D-27). Lời mời PENDING đã quá `expiresAt` được trả với `status: EXPIRED` (lọc `status=EXPIRED` cũng tính chúng). Mời lại cùng email → lời mời PENDING cũ thành `REVOKED`. Email đã thuộc một tài khoản bất kỳ → 409 `EMAIL_EXISTED` (D-02).
- `POST /store/invitations`: gửi email lỗi vẫn 201; OWNER mời lại để gửi mã mới.
- `POST /invitations/accept`: mọi mã không dùng được (không tồn tại, đã dùng, đã huỷ, hết hạn) → 404 `INVITATION_INVALID`; `phoneNumber` sai → 400 `INVALID_PHONE_NUMBER`, lời mời vẫn PENDING. Ở endpoint này chuỗi `phoneNumber` rỗng được coi là không có số (F-43).

## 5. Fleet — `/api/v1/vehicles`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /vehicles` | O, D | query `active?` | `[Vehicle]`, theo tên | — | ✅ |
| `POST /vehicles` | O, D | `{name, plateNumber?, capacity(>0), capacityUnit}` | 201 `Vehicle` | 409 `VEHICLE_NAME_EXISTED` | ✅ |
| `GET /vehicles/{id}` | O, D | — | `Vehicle` | 404 | ✅ |
| `PUT /vehicles/{id}` | O, D | `{name, plateNumber?, capacity, capacityUnit, active}`; thay toàn bộ, thiếu `plateNumber` thì xoá biển số | 200 `Vehicle` | 404 · 409 `VEHICLE_NAME_EXISTED` | ✅ |

```jsonc
// Vehicle
{ "id": 5, "name": "Xe 1", "plateNumber": "43A-123.45", "capacity": 40, "capacityUnit": "BOX", "active": true }
```

- `capacityUnit` (D-36): `KG`, `BOX` (thùng), `PACKAGE` (kiện), `ORDER` (đơn, tức số điểm giao). Giá trị khác → 400 `MALFORMED_REQUEST`. Xe chỉ bị giới hạn theo loại của nó khi optimize.
- Tên unique trong store, so sánh không phân biệt hoa thường; store khác được dùng cùng tên. Không có endpoint xoá: xe chỉ chuyển `active = false`.

## 6. Geocoding và Jobs

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /geocoding/search` | O, D | `q` (3–300 ký tự), `limit` (1–10, mặc định 5) | `[{displayName, lat, lon}]`, chỉ trong Việt Nam. Dùng cho autocomplete hoặc sửa stop geocode lỗi. | 400 (`INVALID_QUERY_LENGTH`, `INVALID_LIMIT`; thiếu `q` → `MALFORMED_REQUEST`) · 503 `GEOCODING_BUSY` (không lấy được slot rate limit trong 2 s) · 502 `GEOCODING_PROVIDER_ERROR` | ✅ |
| `GET /jobs/{id}` | O, D | — | `Job` | 404 | ✅ |

```jsonc
// Job
{ "id": 55, "type": "GEOCODE_PLAN|OPTIMIZE_PLAN", "status": "QUEUED|RUNNING|SUCCEEDED|FAILED",
  "planId": 7, "errorCode": null, "createdAt": "…", "startedAt": "…", "finishedAt": "…" }
```

- `startedAt`, `finishedAt` là `null` khi chưa tới bước đó.
- `errorCode` khi `FAILED`: `ROUTING_PROVIDER_ERROR`, `GEOCODING_PROVIDER_ERROR`, `INTERRUPTED` (app khởi động lại khi job còn `QUEUED`/`RUNNING`), `JOB_QUEUE_FULL` (hàng đợi job nền đầy, thử lại sau) hoặc `INTERNAL_ERROR` (P-30). Job lỗi thì plan quay về `processingStatus = IDLE`.

## 7. Plans (dispatcher) — `/api/v1/plans`

**Vòng đời phía client:** tạo/sửa draft → nếu có stop chỉ có địa chỉ thì server tự tạo job `GEOCODE_PLAN` (`processingStatus = GEOCODING`) → client polling `GET /plans/{id}` cho tới khi `processingStatus = IDLE` → sửa các stop có `geocodeStatus = FAILED` → `optimize` (job) → xem kết quả → `publish`.

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `POST /plans` | O, D | `PlanDraftRequest` | 201 `PlanDetail` (DRAFT) | 400 · 422 `STORE_DEPOT_REQUIRED` · 422 `INVALID_ASSIGNMENT` · 422 `PLAN_TOO_LARGE` | ✅ |
| `GET /plans` | O, D | `date?` (`YYYY-MM-DD`), `status?`, paging; sort `planDate` (mặc định, mới nhất trước), `createdAt`, `name` | Page `PlanSummary` | 400 `INVALID_SORT_FIELD` | ✅ |
| `GET /plans/{id}` | O, D | — | `PlanDetail` | 404 | ✅ |
| `PUT /plans/{id}` | O, D | `PlanDraftRequest` + `version`. Thay toàn bộ draft, xoá kết quả tối ưu. | 200 `PlanDetail` | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 400 `INVALID_STOP_ID` | ✅ |
| `PATCH /plans/{id}/stops/{stopId}` | O, D | merge patch `{version, …field của stop}` (xem dưới). Xoá kết quả tối ưu. | 200 `PlanDetail` | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` | ✅ |
| `POST /plans/{id}/geocode` | O, D | `{version}`. Geocode lại mọi stop `FAILED`/`PENDING`. | 202 `Job` (mode inline: job đã chạy xong khi trả về) | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `NOTHING_TO_GEOCODE` | ✅ |
| `POST /plans/{id}/optimize` | O, D | `{version}` | 202 `Job` (MVP: thường đã `SUCCEEDED` hoặc `FAILED` khi trả về). Kết quả đọc qua `GET /plans/{id}`. | 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `STOPS_NOT_GEOCODED` · 422 `PLAN_TOO_LARGE` | ❌ T25 |
| `POST /plans/{id}/publish` | O, D | `{version}` | 200 `PlanDetail` (PUBLISHED) | 409 · 422 `PLAN_NOT_OPTIMIZED` · 422 `SHIFT_OVERLAP` | ❌ T26 |
| `POST /plans/{id}/cancel` | O, D | `{version}` | 200 `PlanDetail` (CANCELLED) | 409 · 422 `PLAN_NOT_CANCELLABLE` | ❌ T26 |
| `PATCH /plans/{id}/stops/{stopId}/status` | O, D | `{status: "PENDING"}` (đặt lại stop về PENDING) | 200 `PlanDetail` | 404 · 422 `INVALID_STOP_TRANSITION` | ❌ T26 |
| `GET /plans/{id}/driver-locations` | O, D | — | `[{driverId, routeId, lat, lon, accuracyMeters, headingDegrees, speedMps, recordedAt}]` (Should) | 404 | ❌ T32 |
| `PUT /plans/{id}/routes` | O, D | `{version, routes:[{routeId, stopIds:[...]}], unassignedStopIds:[...]}`. Chỉnh tay, tính lại ETA và quãng đường (Should, D-24). | 200 `PlanDetail`, có `violations[]` (hình dạng chốt ở T28) | 409 · 422 | ❌ T28 |

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
      "demands": { "KG": 12, "BOX": 2 },  // tuỳ chọn, theo loại capacity (D-36); thiếu loại nào là 0. Không nhập ORDER.
      "serviceTimeSeconds": 300,          // tuỳ chọn, mặc định 0
      "timeWindow": { "start": "09:00", "end": "11:00" },  // tuỳ chọn
      "note": "Gọi trước 10 phút" }
  ],
  "assignments": [
    { "driverId": 21, "vehicleId": 5, "shiftStart": "08:00", "shiftEnd": "17:00", "maxDistanceMeters": 80000 }
    // MVP: start/end của route = depot của plan. Sau này có thể thêm "start"/"end" riêng cho từng route (D-20).
  ]
}
// PlanSummary (một phần tử của GET /plans)
{ "id": 7, "name": "…", "planDate": "2026-10-22", "status": "DRAFT", "processingStatus": "IDLE", "version": 3,
  "stopCount": 30, "routeCount": 2, "optimizedAt": "…|null", "totalDistanceMeters": 52340, "totalDurationSeconds": 9800,
  "createdAt": "…" }
// PlanDetail
{
  "id": 7, "name": "…", "planDate": "2026-10-22", "status": "DRAFT", "version": 3,
  "processingStatus": "IDLE|GEOCODING|OPTIMIZING", "activeJob": null,   // Job khi processingStatus khác IDLE
  "geocodingSummary": { "total": 30, "resolved": 27, "provided": 1, "pending": 0, "failed": 2 },
  "depot": { "address": "…", "lat": 16.05, "lon": 108.2 }, "timezone": "Asia/Ho_Chi_Minh",
  "optimizedAt": "…|null", "totalDistanceMeters": 52340, "totalDurationSeconds": 9800,
  "routes": [
    { "id": 11, "driver": { "id": 21, "fullName": "Tùng", "role": "DRIVER" }, "vehicle": { "id": 5, "name": "Xe 1", "capacity": 40, "capacityUnit": "BOX" },
      "shiftStart": "08:00", "shiftEnd": "17:00", "maxDistanceMeters": 80000,
      "start": { "address": "…", "lat": 16.05, "lon": 108.2 }, "end": { "address": "…", "lat": 16.05, "lon": 108.2 },
      "plannedDistanceMeters": 30100, "plannedDurationSeconds": 5400,
      "plannedStart": "…", "plannedEnd": "…", "load": 31,   // theo capacityUnit của xe (D-36); null khi chưa optimize
      "stops": [ PlanStop, … ] }                            // theo sequence
  ],
  "unassignedStops": [ PlanStop, … ]   // stop chưa gán route: plan chưa optimize, hoặc solver đã bỏ (P-13); theo id
}
// PlanStop (cùng một dạng trong routes[].stops và unassignedStops)
{ "id": 101, "sequence": 1, "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "+84905…",
  "address": "…", "lat": 16.06, "lon": 108.22,
  "geocodeStatus": "PROVIDED|PENDING|RESOLVED|FAILED", "geocodeError": "NOT_FOUND|PROVIDER_ERROR|null",
  "geocodedDisplayName": "…|null", "demands": { "BOX": 3 }, "serviceTimeSeconds": 300,
  "timeWindow": { "start": "09:00", "end": "11:00" } | null, "note": "…", "plannedArrival": "…|null",
  "status": "PENDING|DELIVERED|FAILED|SKIPPED", "failureReason": null,
  "dropReason": "NOT_OPTIMIZED|INFEASIBLE|null" }   // null trong route
```

`PlanDetail`:
- `routes` theo id. `PUT` tạo lại mọi route từ `assignments`, nên id route đổi sau mỗi `PUT`; client tải lại `PlanDetail` sau khi `PUT`.
- `routes[].driver` (`fullName`, `role`) và `routes[].vehicle.name` là giá trị hiện tại, kể cả khi thành viên đã bị vô hiệu hoá hay xe đã tắt; `vehicle.capacity`, `vehicle.capacityUnit` là snapshot lúc phân công.
- `PlanStop.demands` chỉ có các loại khác 0. `dropReason`: `NOT_OPTIMIZED` khi plan chưa có kết quả tối ưu, `INFEASIBLE` khi solver không xếp được.

**Lưu draft (`POST`, `PUT`):**
- Không có `depot` thì snapshot depot hiện tại của store; store chưa có depot → 422 `STORE_DEPOT_REQUIRED`. `depot` trong request phải có `lat`, `lon`. Timezone luôn snapshot từ store mỗi lần lưu.
- Stop có `id` được sửa tại chỗ; stop của plan không có trong request bị xoá. `id` không phải stop của plan này hoặc lặp lại → 400 `VALIDATION_ERROR`, detail `{field: "stops[i].id", code: "INVALID_STOP_ID"}` (cùng một lỗi cho mọi trường hợp, không lộ stop của store khác).
- Stop có `id` giữ kết quả geocode khi địa chỉ không đổi và: hoặc request gửi đúng toạ độ đang lưu, hoặc request không gửi toạ độ và toạ độ đang lưu không do client nhập (`geocodeStatus` khác `PROVIDED`). Ngược lại: có toạ độ → `PROVIDED`, chỉ có địa chỉ → `PENDING`.
- Phân công: driver phải là thành viên ACTIVE của store và là DRIVER hoặc có `canDrive`; xe phải ACTIVE và thuộc store; mỗi driver và mỗi xe một lần. Sai → 422 `INVALID_ASSIGNMENT`. Capacity và `capacityUnit` của xe được snapshot.
- Giới hạn (D-19): ≤ 100 stop, ≤ 20 assignment, quá → 422 `PLAN_TOO_LARGE`. `stops`, `assignments` bỏ trống = danh sách rỗng (draft được phép chưa có stop hay phân công).
- Demand (D-36): số nguyên ≥ 0, khoá `KG`, `BOX`, `PACKAGE`; `ORDER` → 400 `ORDER_DEMAND_NOT_ALLOWED`, khoá lạ → 400 `MALFORMED_REQUEST`. Xe tính theo `ORDER` chở tối đa `capacity` stop.

**`PATCH /plans/{id}/stops/{stopId}`** (merge patch, §1): `timeWindow: null` bỏ time window, `demands: null` bỏ mọi demand, `serviceTimeSeconds: null` về 0, `recipientName`/`recipientPhone`/`note`/`address: null` bỏ giá trị, `"lat": null, "lon": null` bỏ toạ độ và geocode lại từ địa chỉ. `label` không xoá được (`null` → `NOT_NULLABLE`, chuỗi trắng → `STOP_LABEL_REQUIRED`); `lat` và `lon` gửi cùng nhau (`COORDINATES_INCOMPLETE`). Vị trí sau khi patch:
- gửi toạ độ khác `null` → `PROVIDED` với toạ độ đó;
- không gửi toạ độ, địa chỉ đổi sang giá trị khác → toạ độ cũ bị bỏ, `PENDING` (geocode lại);
- không gửi toạ độ, `address: null` → giữ toạ độ hiện tại, thành `PROVIDED`;
- địa chỉ và toạ độ không đổi → giữ nguyên kết quả geocode (kể cả `FAILED`);
- stop không còn địa chỉ lẫn toạ độ → 400, detail `{field: "address", code: "ADDRESS_OR_COORDINATES_REQUIRED"}`.

**Thứ tự kiểm tra** của `PUT`, `PATCH`, `POST /geocode`: 404 → `PLAN_NOT_EDITABLE` → `PLAN_BUSY` → `PLAN_MODIFIED`. Mỗi lần sửa thành công tăng `version` đúng 1.

**Geocode nền:**
- Mọi lần lưu (`POST`, `PUT`, `PATCH` stop) còn stop `PENDING` → tạo job `GEOCODE_PLAN`; response có `processingStatus = GEOCODING`, `activeJob` (thường `QUEUED`). Stop `FAILED` không tự chạy lại; dùng `POST /plans/{id}/geocode` hoặc sửa stop.
- `POST /plans/{id}/geocode`: stop `FAILED` về `PENDING`, plan `GEOCODING`, `version` tăng 1. Không có stop `PENDING`/`FAILED` nào → 422 `NOTHING_TO_GEOCODE` (P-30).
- Stop không tìm thấy → `FAILED/NOT_FOUND`; provider lỗi → `FAILED/PROVIDER_ERROR`; job vẫn `SUCCEEDED`. Kết quả ghi theo lô nên `geocodingSummary` thay đổi dần khi polling.
- Job kết thúc thì plan về `IDLE`, `activeJob = null`, **`version` tăng 1** (P-30): client polling `GET /plans/{id}` tới khi `IDLE` rồi dùng `version` mới. Ví dụ tạo plan có stop chỉ có địa chỉ: response `version = 0`, geocode xong `version = 1`.
- Trong lúc `GEOCODING`, `PUT`, `PATCH` stop, `POST /geocode` → 409 `PLAN_BUSY`.

**Publish (T26):** `SHIFT_OVERLAP` (D-23) trả mỗi xung đột là một phần tử `details`: `{field: "assignments[i].driverId" | "assignments[i].vehicleId", code: "SHIFT_OVERLAP", conflictPlanId}` (P-32); `i` là thứ tự route trong `PlanDetail.routes`.

**Mã validation (`details[].code`) của plan:** `PLAN_NAME_REQUIRED`, `INVALID_PLAN_NAME_LENGTH` (≤ 150), `PLAN_DATE_REQUIRED`, `VERSION_REQUIRED` (`PUT`, `PATCH`, `POST /geocode`), `LATITUDE_REQUIRED`, `LONGITUDE_REQUIRED`, `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_ADDRESS_LENGTH` (≤ 500), `STOP_LABEL_REQUIRED`, `INVALID_STOP_LABEL_LENGTH` (≤ 150), `INVALID_RECIPIENT_NAME_LENGTH` (≤ 100), `PHONE_NUMBER_*`, `COORDINATES_INCOMPLETE` (trên field còn thiếu, `lat` hoặc `lon`), `ADDRESS_OR_COORDINATES_REQUIRED` (trên `address`), `INVALID_DEMAND` (số nguyên ≥ 0), `ORDER_DEMAND_NOT_ALLOWED` (trên `demands`), `INVALID_SERVICE_TIME` (0–86400), `TIME_WINDOW_START_REQUIRED`, `TIME_WINDOW_END_REQUIRED`, `INVALID_TIME_WINDOW` (trên `timeWindow.end`, `start < end`), `INVALID_NOTE_LENGTH` (≤ 500), `DRIVER_REQUIRED`, `VEHICLE_REQUIRED`, `SHIFT_START_REQUIRED`, `SHIFT_END_REQUIRED`, `INVALID_SHIFT` (trên `shiftEnd`, `shiftStart < shiftEnd`), `INVALID_MAX_DISTANCE` (> 0), `INVALID_STOP_ID`, `NOT_NULLABLE`.

## 8. Driver — `/api/v1/driver`

Người gọi phải có `role=DRIVER` hoặc `canDrive=true` (D-21), và chỉ thấy route có `driverId = mình` thuộc plan PUBLISHED, IN_PROGRESS hoặc COMPLETED.

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /driver/routes` | R | `date` (mặc định hôm nay theo timezone store) | `[{routeId, planId, planName, planDate, status, stopCount, completedCount, plannedStart}]` | — | ❌ T30 |
| `GET /driver/routes/{routeId}` | R | — | `DriverRouteDetail` | 404 | ❌ T30 |
| `PATCH /driver/routes/{routeId}/stops/{stopId}/status` | R | `{status: DELIVERED\|FAILED\|SKIPPED, failureReason?}` | 200 `{stop: DriverStop, routeStatus, planStatus}` | 404 · 422 `INVALID_STOP_TRANSITION` · 422 `FAILURE_REASON_REQUIRED` · 422 `PLAN_NOT_ACTIVE` | ❌ T31 |
| `POST /driver/location` | R | `{points:[{lat, lon, accuracyMeters?, headingDegrees?, speedMps?, recordedAt}]}`, 1–100 điểm theo thứ tự thời gian (D-26) | 204 (Should). MVP chỉ giữ điểm có `recordedAt` mới nhất. | 400 | ❌ T32 |
| `GET /driver/routes/{routeId}/directions` | R | — | `DirectionsResponse` (Should) | 404 · 502 | ❌ T33 |

```jsonc
// DriverRouteDetail
{
  "routeId": 11, "planId": 7, "planDate": "2026-10-22", "status": "IN_PROGRESS",
  "start": { "address": "…", "lat": 16.05, "lon": 108.2 }, "end": { "address": "…", "lat": 16.05, "lon": 108.2 },  // D-20
  "stops": [ DriverStop, … ]   // theo sequence
}
// DriverStop
{ "id": 101, "sequence": 1, "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "+84905…",
  "address": "…", "lat": 16.06, "lon": 108.22, "plannedArrival": "…",
  "timeWindow": { "start": "09:00", "end": "11:00" }, "note": "…", "status": "PENDING", "failureReason": null }
// DirectionsResponse (Should; hình dạng sẽ chốt lại ở T33, xem IMPLEMENTATION_PLAN)
{ "legs": [ { "fromStopId": null, "toStopId": 101, "distanceMeters": 2100, "durationSeconds": 360,
              "geometry": "<polyline6>",
              "steps": [ { "maneuver": "turn", "modifier": "left", "roadName": "Nguyễn Văn Linh",
                           "distanceMeters": 400, "durationSeconds": 60, "location": [108.21, 16.055] } ] } ] }
```

- Dẫn đường (D-07): client tự dựng deep link Google Maps hoặc Apple Maps theo nền tảng từ `lat`/`lon` của stop; server không trả URL (P-32).
- `PATCH …/status` của tài xế trả `routeStatus` và `planStatus` để app biết route/plan đã chuyển `IN_PROGRESS` hay `COMPLETED` (BR-11). Cập nhật trạng thái stop không tăng `version` của plan (§1).

## 9. Mã lỗi (`ErrorCode`)

| Nhóm | Code | HTTP |
|---|---|---|
| Request | `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_SORT_FIELD`, `INVALID_TIMEZONE`, `INVALID_PHONE_NUMBER` | 400 |
| Xác thực | `UNAUTHORIZED`, `INVALID_TOKEN`, `INVALID_EMAIL_OR_PASSWORD` | 401 |
| Xác thực | `INVALID_OTP`, `INVALID_CURRENT_PASSWORD`, `INVALID_RESET_TOKEN` | 400 |
| Quyền | `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED` | 403 |
| Không tìm thấy | `NOT_FOUND`, `INVITATION_INVALID` | 404 |
| Method | `METHOD_NOT_ALLOWED` | 405 |
| Xung đột | `EMAIL_EXISTED`, `EMAIL_ALREADY_VERIFIED`, `VEHICLE_NAME_EXISTED`, `PLAN_MODIFIED`, `PLAN_BUSY`, `CONFLICT` | 409 |
| Nghiệp vụ | `CANNOT_MODIFY_OWNER`, `INVITATION_NOT_PENDING`, `STORE_DEPOT_REQUIRED`, `INVALID_ASSIGNMENT`, `PLAN_TOO_LARGE`, `PLAN_NOT_EDITABLE`, `NOTHING_TO_GEOCODE`, `STOPS_NOT_GEOCODED`, `PLAN_NOT_OPTIMIZED`, `PLAN_NOT_CANCELLABLE`, `PLAN_NOT_ACTIVE`, `SHIFT_OVERLAP`, `INVALID_STOP_TRANSITION`, `FAILURE_REASON_REQUIRED` | 422 |
| Giới hạn | `OTP_COOLDOWN`, `RATE_LIMITED` | 429 |
| Provider | `GEOCODING_PROVIDER_ERROR`, `ROUTING_PROVIDER_ERROR` | 502 |
| Provider | `GEOCODING_BUSY` | 503 |
| Khác | `INTERNAL_ERROR` | 500 |

- `CONFLICT`: vi phạm ràng buộc DB không có mã nghiệp vụ riêng, ví dụ hai request đăng ký đồng thời.
- Không còn trả (giữ trong enum dạng `@Deprecated` vì `ErrorCode` chỉ được thêm): `USER_NOT_FOUND`, `SESSION_NOT_FOUND`, `JOB_NOT_FOUND` (thay bằng `NOT_FOUND`, P-32), `COULD_NOT_FIND_USER`, `COULD_NOT_UPDATE_USER_PASSWORD`. Bỏ cùng `/route/optimize*` ở T27: `INVALID_DEPOT`, `COULD_NOT_FIND_CHEAPEST_ROUTE`, `COULD_NOT_FIND_ADDRESS`.
- `Job.errorCode` (§6) có thêm các giá trị không phải `ErrorCode`: `INTERRUPTED`, `JOB_QUEUE_FULL`.
- Mã validation trong `details[].code` dùng UPPER_SNAKE, ví dụ `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `INVALID_PASSWORD_LENGTH`, `PHONE_NUMBER_WRONG_REGION`, `NOT_NULLABLE`.
