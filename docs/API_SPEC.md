# API_SPEC — Hợp đồng REST API mục tiêu

> Đây là **hợp đồng mục tiêu** cho web và Flutter (D-09). Frontend có thể dựa vào tài liệu này để thiết kế song song.
> Khi implementation khác spec: sửa code cho khớp spec, **hoặc** cập nhật spec trong cùng PR kèm lý do. Không để lệch.
> Cột "Hiện trạng" ở mọi bảng: ✅ khớp spec và có test · 🟡 có nhưng còn lệch (ghi rõ lệch gì) · ❌ chưa có. Lịch sử triển khai theo task nằm ở `CURRENT_STATE.md` §5, không ghi ở đây.
> Lần rà soát contract gần nhất: T44 (2026-10-29, P-32).

## 0. Lệch hiện tại so với spec (cập nhật 2026-11-14, sau T62)

- Không còn lệch nào đã biết giữa endpoint và spec (`GET /admin/users` đủ `storeId`, `status` từ T55).
- Request không phải `multipart/form-data` gửi tới endpoint nhận file, hoặc `Content-Type` không được hỗ trợ, trả 400 `MALFORMED_REQUEST` (T61; trước đó rơi vào handler chung).
- Tài liệu OpenAPI sinh tự động (`/v3/api-docs` ở dev/staging; CI xuất artifact `api-contract` gồm `openapi.json` và dữ liệu mẫu) dùng để sinh code client; endpoint (method + path) được kiểm tự động khớp spec (`ApiSpecContractIntegrationTest`). Từ T46 (P-48): mã thành công đúng như spec (201, 202, 204); field response luôn có mặt là `required`, field có thể `null` có kiểu kèm `null`; field của request PATCH gửi `null` để xoá có kiểu kèm `null`. Response thật được kiểm khớp schema đó (`OpenApiNullabilityIntegrationTest`, trên `docs/ux/samples` và các response tài khoản). OpenAPI chưa mô tả response lỗi (dùng §1 và §9). Spec này vẫn là hợp đồng; hướng dẫn cho client ở `docs/ux/UX_BRIEF.md`.
- Endpoint vận hành ngoài `/api/v1`, giữ có chủ đích: `GET /actuator/health` (kèm `/liveness`, `/readiness`), Public, chỉ trả `status` và tên các `groups` (T05, `ARCHITECTURE.md` §12.1). Mọi endpoint actuator khác tắt. Caddy chỉ mở `/actuator/health` (không mở `/liveness`, `/readiness`) ra internet cho dịch vụ uptime bên ngoài (T54, DEPLOYMENT §9).

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

**Header chung** (T41, P-38)
- Mọi response có `X-Request-Id` (UUID do server sinh, không nhận từ client); khi báo lỗi, client gửi kèm giá trị này để đối chiếu log.
- Security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, `Strict-Transport-Security` khi qua HTTPS.
- CORS: chỉ các origin trong `cors.allowed-origins` (danh sách chính xác, kèm credentials); preflight hoặc request từ origin khác → 403 (body không theo format lỗi, do trình duyệt chặn trước). Header client đọc được: `Retry-After`, `X-Request-Id`. App mobile không gửi `Origin` nên không bị ảnh hưởng.

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

**Cửa hàng bị khoá** (T56, P-59, P-62)
- Khi cửa hàng `READ_ONLY` (ADMIN khoá, §3a), mọi request ghi (POST, PUT, PATCH, DELETE) của thành viên → **423 `STORE_READ_ONLY`**, kiểm trước cả validation và quyền theo đối tượng. Đọc vẫn bình thường.
- Vẫn ghi được: tài khoản cá nhân (`/auth/*`: đăng xuất, phiên, đổi mật khẩu; `PATCH /users/me`) và việc của tài xế để giao nốt (`PATCH /driver/routes/{routeId}/stops/{stopId}/status`, `POST /driver/location`, `PUT /driver/location/sharing`).
- `POST /invitations/accept` (công khai) cho lời mời của cửa hàng bị khoá → 423; mã mời vẫn dùng được sau khi mở khoá.
- Client biết trạng thái qua `GET /users/me` (`storeStatus`, `storeLockReason`) để hiện băng "Cửa hàng đang tạm khoá: {lý do}".

**Rate limit** (P-16, P-37)
- Áp dụng cho `POST /auth/log-in` (theo IP và email), `/auth/register` (IP), `/auth/email/verify/resend` và `/auth/password/forgot` (IP và email), `/auth/password/change/otp` (user), `POST /store/invitations` (store), `POST /plans/import` (user, T61). Fixed window; ngưỡng ở `rate-limit.rules.*`, mặc định ghi ở P-37. Mọi request đều được đếm, kể cả thành công, và được đếm trước khi xử lý.
- Vượt ngưỡng → 429 `RATE_LIMITED`, header `Retry-After` = số giây tới khi hết window (làm tròn lên). `/email/verify/resend`, `/password/forgot` vẫn trả 429 với email không tồn tại, nên không lộ email nào đã đăng ký.
- Không kiểm được giới hạn (Redis lỗi) → 503 `SERVICE_UNAVAILABLE` (fail-closed); client thử lại sau.

**PATCH = JSON Merge Patch** (RFC 7396, P-32)
- Mọi `PATCH`: field **vắng** trong JSON thì giữ nguyên; field có mặt với `null` thì **xoá** giá trị.
- Field không xoá được mà gửi `null` → 400 `VALIDATION_ERROR`, detail `{field, code: "NOT_NULLABLE"}`.

**Concurrency và job**
- Optimistic concurrency cho plan: request thay đổi plan gửi `version`; lệch → 409 `PLAN_MODIFIED`. `version` chỉ tăng khi **nội dung plan** đổi (sửa draft, kết quả geocode/optimize, publish, cancel); cập nhật trạng thái stop **không** tăng `version` của plan (P-32).
- Thao tác chạy lâu dùng **job** (D-19): endpoint trả `202 { data: Job }`. Client theo dõi bằng `GET /jobs/{id}` hoặc qua `processingStatus` của resource. Một số job trong MVP hoàn tất ngay (status trả về đã là `SUCCEEDED`), nhưng client **phải luôn xử lý như job bất đồng bộ**.

## 2. Auth — `/api/v1/auth`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `POST /register` | P | `{email, password(8–72), storeName(1–150), fullName?}` | 201 `{email, status: "PENDING_VERIFICATION"}`; gửi OTP `VERIFY_EMAIL` | 400 · 409 `EMAIL_EXISTED` · 429 `RATE_LIMITED` | ✅ |
| `POST /email/verify` | P | `{email, otp}` | 204 | 400 `INVALID_OTP` · 409 `EMAIL_ALREADY_VERIFIED` | ✅ |
| `POST /email/verify/resend` | P | `{email}` | 204 (khi không bị giới hạn) | 429 `RATE_LIMITED` | ✅ |
| `POST /log-in` | P | `{email, password}` | 200 `LoginResponse` (+ cookie nếu web) | 401 `INVALID_EMAIL_OR_PASSWORD` · 403 `EMAIL_NOT_VERIFIED` · 403 `ACCOUNT_DISABLED` · 429 `RATE_LIMITED` | ✅ |
| `POST /refresh` | P | web: cookie · mobile: `{refreshToken}` | 200 `TokenResponse` (+ cookie mới nếu web) | 401 `INVALID_TOKEN` | ✅ |
| `POST /log-out` | P | web: cookie · mobile: `{refreshToken}` | 204 (idempotent; xoá cookie nếu web) | — | ✅ |
| `GET /sessions` | A | — | 200 `[SessionResponse]` | — | ✅ |
| `DELETE /sessions/{sessionId}` | A | — | 204 | 404 `NOT_FOUND` | ✅ |
| `POST /sessions/revoke-others` | A | — | 204 | — | ✅ |
| `POST /password/forgot` | P | `{email}` | 204 (khi không bị giới hạn); gửi OTP `RESET_PASSWORD` nếu email ACTIVE | 429 `RATE_LIMITED` | ✅ |
| `POST /password/reset/verify` | P | `{email, otp}` | 200 `{resetToken, expiresAt}` | 400 `INVALID_OTP` | ✅ |
| `POST /password/reset` | P | `{resetToken, newPassword}` | 204; revoke **mọi** session | 400 `INVALID_RESET_TOKEN` (P-26) | ✅ |
| `POST /password/reset-link/otp` | P | `{token}` (token trong link ADMIN gửi, T57) | 204; gửi OTP `RESET_PASSWORD` về email của tài khoản (không trả email); link vẫn dùng được | 400 `INVALID_RESET_TOKEN` (link sai, hết hạn, đã dùng, tài khoản không còn ACTIVE) · 429 `OTP_COOLDOWN` · 429 `RATE_LIMITED` | ✅ (T57) |
| `POST /password/reset-link` | P | `{token, otp, newPassword}` (8–72) | 204; đổi mật khẩu, revoke **mọi** session, link hết hiệu lực | 400 `INVALID_RESET_TOKEN` · 400 `INVALID_OTP` (link vẫn dùng được) | ✅ (T57) |
| `POST /password/change/otp` | A | `{currentPassword}` | 204; gửi OTP `CHANGE_PASSWORD` | 400 `INVALID_CURRENT_PASSWORD` · 429 `OTP_COOLDOWN` · 429 `RATE_LIMITED` | ✅ |
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
- Không tiết lộ email có tồn tại hay không: `/email/verify` với email không tồn tại → 400 `INVALID_OTP`; `/email/verify/resend`, `/password/forgot` luôn 204 trừ khi bị rate limit (429 như nhau với mọi email; chỉ gửi khi tài khoản đúng trạng thái và hết cooldown OTP); `/log-in` sai mật khẩu luôn 401, chỉ báo `EMAIL_NOT_VERIFIED`/`ACCOUNT_DISABLED` khi mật khẩu đúng; `/password/reset/verify` với email không tồn tại hoặc không ACTIVE → 400 `INVALID_OTP`.
- `/register`: tạo store và OWNER trong một transaction, gửi OTP sau commit; email lỗi vẫn 201, gửi lại bằng `/email/verify/resend`. Email chuẩn hoá (`trim().toLowerCase()`, P-19), trùng khác hoa thường → 409.
- `LoginResponse.user`: `fullName` bỏ khi chưa đặt, `storeId` bỏ với ADMIN, `canDrive` luôn `true` với DRIVER.
- Cookie web: `HttpOnly; Secure; SameSite=<cấu hình>; Path=/api/v1/auth`. Thiếu, sai, hết hạn refresh token, hoặc user không còn ACTIVE → 401 `INVALID_TOKEN`.
- Access token chứa claim `sid` = sessionId (family) để xác định `current` (P-09). `lastUsedAt` = lần log-in hoặc refresh gần nhất của session.
- `DELETE /sessions/{sessionId}`: session của chính user. Nếu là session hiện tại thì xử lý **như `/log-out`** (204, `no-store`, web xoá cookie); client bỏ token như sau khi log-out. Session của user khác, không tồn tại, đã thu hồi hoặc hết hạn → 404.
- `POST /sessions/revoke-others`: access token cấp trước khi có claim `sid` thì thu hồi mọi session.
- `resetToken` là chuỗi opaque, hạn 10 phút, dùng một lần (lần thử đầu tiên tiêu thụ token, kể cả khi sai). `/password/reset` đổi mật khẩu và thu hồi mọi session trong một transaction.
- **Link đặt lại mật khẩu do ADMIN gửi** (T57, P-63): email chứa `{app-url}/app/reset-password?token=…` (token 43 ký tự, **không chứa email**), mở được trong 24 giờ (`auth.reset-link.ttl`), mỗi user chỉ một link còn hiệu lực (link mới thay link cũ). Có link vẫn phải nhập OTP gửi về chính email đó (`/reset-link/otp`, hạn 5 phút, chờ 60 s giữa hai lần) mới đổi được mật khẩu. Trang không bao giờ biết email.
- `/password/change/otp`: sai mật khẩu hiện tại → 400 và không gửi email; gửi email lỗi → 500 `INTERNAL_ERROR` (cooldown đã được xoá nên gọi lại được ngay). `PUT /password`: OTP gắn userId của người gọi; session hiện tại vẫn refresh được.
- OTP: 6 chữ số; sai quá 3 lần → OTP huỷ, phải xin mã mới.

## 3. Users — `/api/v1/users`, `/api/v1/admin/users`

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /users/me` | A | — | `User` | — | ✅ |
| `PATCH /users/me` | A | merge patch `{fullName?, phoneNumber?}`; `null` xoá giá trị | 200 `User` | 400 (`FULL_NAME_REQUIRED` khi chuỗi trắng, `INVALID_FULL_NAME_LENGTH` ≤ 100, `PHONE_NUMBER_*`) | ✅ |
| `GET /admin/users` | AD | `storeId?`, `role?` (`ADMIN\|OWNER\|DISPATCHER\|DRIVER`), `status?` (`PENDING_VERIFICATION\|ACTIVE\|DISABLED`), paging; sort `id` (mặc định), `email`, `createdAt`, `updatedAt` | Page `{id, email, phoneNumber, role, storeId, status, createdAt, updatedAt}`; `storeId` là `null` với ADMIN | 400 `INVALID_SORT_FIELD` · 400 `MALFORMED_REQUEST` (giá trị enum sai) | ✅ (lọc và `storeId`, `status` từ T55) |
| `POST /admin/users/{id}/disable` | AD | — | 200 `{id, email, phoneNumber, role, storeId, status: DISABLED, createdAt, updatedAt}`; thu hồi mọi phiên; ghi `USER_DISABLED` | 404 · 422 `CANNOT_MODIFY_SELF` (tự khoá mình, nên luôn còn ADMIN) | ✅ (T56) |
| `POST /admin/users/{id}/enable` | AD | — | 200 như trên; `status` về `ACTIVE`, hoặc `PENDING_VERIFICATION` nếu email chưa từng xác thực; ghi `USER_ENABLED` | 404 | ✅ (T56) |
| `POST /admin/users/{id}/sessions/revoke` | AD | — | 204; mọi refresh token bị thu hồi (buộc đăng xuất; access token cũ hết hạn trong ≤ 15 phút); ghi `USER_SESSIONS_REVOKED` | 404 | ✅ (T56) |
| `POST /admin/users/{id}/verification-email` | AD | — | 204; gửi lại OTP `VERIFY_EMAIL` cho tài khoản chờ xác thực (đếm chung giới hạn gửi lại theo email); ghi `VERIFICATION_EMAIL_SENT` | 404 · 409 `EMAIL_ALREADY_VERIFIED` · 403 `ACCOUNT_DISABLED` · 429 `OTP_COOLDOWN`, `RATE_LIMITED` · 500 khi gửi email lỗi | ✅ (T57) |
| `POST /admin/users/{id}/password-reset-email` | AD | — | 204, không trả gì (ADMIN không thấy link, mã hay mật khẩu); gửi email link đặt lại mật khẩu (§2, P-63); ghi `PASSWORD_RESET_LINK_SENT` | 404 · 403 `ACCOUNT_DISABLED` · 403 `EMAIL_NOT_VERIFIED` (tài khoản chờ xác thực: dùng `verification-email`) · 429 `RATE_LIMITED` (đếm chung với Quên mật khẩu theo email) · 500 khi gửi email lỗi (link bị huỷ) | ✅ (T57) |

```jsonc
// User
{ "id": 12, "email": "a@b.vn", "fullName": "An", "phoneNumber": "+84905123456", "role": "OWNER", "storeId": 3,
  "status": "ACTIVE", "canDrive": false,     // canDrive luôn true với DRIVER (D-21)
  "storeStatus": "ACTIVE", "storeLockReason": null }   // T56: trạng thái cửa hàng của user; cả hai null với ADMIN
```

## 3a. Quản trị (ADMIN) — `/api/v1/admin/stores`, `/api/v1/admin/audit`, `/api/v1/admin/metrics`

Module `admin` (P-60). ADMIN chỉ thấy **số tổng và tài khoản thành viên**, không thấy nội dung của cửa hàng: không depot, plan, xe, điểm giao, người nhận, tên hay số điện thoại của thành viên (P-59, BR-01).

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /admin/stores` | AD | `q?` (chứa trong tên cửa hàng **hoặc** email OWNER, không phân biệt hoa thường; `%`, `_` khớp đúng ký tự), `status?` (`ACTIVE\|READ_ONLY`), paging; sort `id` (mặc định), `name`, `createdAt` | Page `AdminStoreSummary` | 400 `INVALID_SORT_FIELD` · 400 `MALFORMED_REQUEST` | ✅ (T55) |
| `GET /admin/stores/{id}` | AD | — | `AdminStoreDetail`; ghi nhật ký `STORE_VIEWED` (T58, P-61) | 404 `NOT_FOUND` | ✅ (T55) |
| `POST /admin/stores/{id}/lock` | AD | `{reason}` (bắt buộc, cắt khoảng trắng, ≤ 500) | 200 `{id, status: READ_ONLY, lockReason, lockedAt}`; khoá lại thì thay lý do; ghi `STORE_LOCKED` | 400 `LOCK_REASON_REQUIRED`, `INVALID_LOCK_REASON_LENGTH` · 404 | ✅ (T56) |
| `POST /admin/stores/{id}/unlock` | AD | — | 200 `{id, status: ACTIVE, lockReason: null, lockedAt: null}`; cửa hàng đang ACTIVE thì không đổi, không ghi nhật ký; ghi `STORE_UNLOCKED` | 404 | ✅ (T56) |
| `POST /admin/stores/{id}/owner` | AD | `{userId}` (thành viên ACTIVE của cửa hàng) | 200 `{storeId, ownerId, ownerEmail, ownerCanDrive, previousOwnerId, previousOwnerEmail}`; OWNER cũ thành DISPATCHER và bị đăng xuất mọi nơi; DRIVER thành OWNER thì `canDrive = true`; email báo cả chủ mới và chủ cũ (thư cho chủ cũ không nêu chủ mới); ghi `STORE_OWNER_TRANSFERRED` (P-63) | 404 (cửa hàng không có, user không thuộc cửa hàng) · 400 `VALIDATION_ERROR` trên `userId`: `USER_ID_REQUIRED`, `NEW_OWNER_NOT_ACTIVE`, `ALREADY_OWNER` | ✅ (T57) |
| `GET /admin/audit` | AD | `storeId?`, `actorId?`, `action?` (`AuditAction`), `from?`, `to?` (`YYYY-MM-DD`, cả hai ngày tính, giờ Asia/Ho_Chi_Minh), paging; sort `occurredAt` (mặc định, mới nhất trước), `id` | Page `AuditEvent` | 400 `INVALID_SORT_FIELD` · 400 `MALFORMED_REQUEST` | ✅ (T58) |
| `GET /admin/metrics/business` | AD | `from?`, `to?` (`YYYY-MM-DD`, cả hai ngày tính, giờ Asia/Ho_Chi_Minh; mặc định 30 ngày tới hôm nay; tối đa 92 ngày) | `BusinessMetrics` (cache ~60 s, `computedAt`) | 400 `VALIDATION_ERROR` trên `from`: `INVALID_DATE_RANGE`, `DATE_RANGE_TOO_LONG` | ✅ (T60) |
| `GET /admin/metrics/technical` | AD | — | `TechnicalMetrics` (số của instance đang chạy) | — | ✅ (T60) |

```jsonc
// AdminStoreSummary
{ "id": 3, "name": "Tiệm Hoa", "ownerEmail": "chu@tiem.vn", "status": "ACTIVE", "createdAt": "2026-11-01T02:00:00Z",
  "memberCounts": { "owners": 1, "dispatchers": 2, "drivers": 5 },
  "vehicles": 4, "plans": 37, "lastActivityAt": "2026-11-14T09:12:30.123456Z" }   // null khi chưa có plan

// AdminStoreDetail = AdminStoreSummary, thêm:
{ "lockReason": null, "lockedAt": null,            // có giá trị khi READ_ONLY (T56)
  "timezone": "Asia/Ho_Chi_Minh",
  "last30Days": { "since": "2026-10-15T09:20:00Z", "plansCreated": 22, "plansPublished": 20, "plansCompleted": 19,
                  "stopsDelivered": 410, "stopsFailed": 12, "stopsSkipped": 3, "deliveredRate": 0.9647 },  // null khi chưa có kết quả
  "members": [ { "id": 12, "email": "chu@tiem.vn", "role": "OWNER", "status": "ACTIVE", "createdAt": "2026-11-01T02:00:00Z" } ] }
```

Ghi chú:
- `memberCounts` đếm thành viên mọi trạng thái; `members` liệt kê theo id, mọi trạng thái. `vehicles` chỉ đếm xe đang dùng (`active`). `plans` đếm plan mọi trạng thái.
- `lastActivityAt` (P-60): mốc mới nhất giữa plan được tạo hoặc sửa (`route_plans.updated_at`) và tài xế báo kết quả điểm giao (`status_updated_at`).
- `last30Days`: từ `since` (= lúc gọi − 30 ngày) tới nay. Plan đếm theo thời điểm tạo, publish, hoàn thành; điểm giao đếm theo lúc tài xế báo kết quả. `deliveredRate` = delivered / (delivered + failed + skipped), làm tròn 4 chữ số.
- Store không tồn tại → 404. OWNER, DISPATCHER, DRIVER → 403.

**Thông số nền tảng (T60, P-64, FR-A16).** Chỉ số tổng, không nội dung cửa hàng.

```jsonc
// BusinessMetrics: từ DB, theo ngày ở `timezone`
{ "from": "2026-10-16", "to": "2026-11-14", "timezone": "Asia/Ho_Chi_Minh", "computedAt": "2026-11-14T03:00:00Z",
  "now": { "stores": 12, "activeStores7d": 8, "activeStores30d": 10, "lockedStores": 1,   // hoạt động = có plan publish
           "admins": 1, "owners": 12, "dispatchers": 9, "drivers": 40 },                 // user mọi trạng thái
  "totals": { "newStores": 3, "plansCreated": 210, "plansPublished": 200, "plansCompleted": 190,
              "stopsDelivered": 4100, "stopsFailed": 120, "stopsSkipped": 30,
              "deliveredRate": 0.9647, "failedRate": 0.0282, "skippedRate": 0.0071,          // 0–1; null khi chưa có kết quả
              "optimizations": 260, "optimizationsFailed": 4, "optimizationFailureRate": 0.0154 },
  "days": [ { "date": "2026-10-16", "newStores": 0, "plansCreated": 7, "plansPublished": 7, "plansCompleted": 6,
              "stopsDelivered": 140, "stopsFailed": 4, "stopsSkipped": 1, "optimizations": 9, "optimizationsFailed": 0 } ] }  // đủ mọi ngày, kể cả 0

// TechnicalMetrics: request, lỗi, rate limit là của instance này từ `startedAt` (về 0 khi app khởi động lại)
{ "startedAt": "2026-11-14T01:00:00Z", "uptimeSeconds": 7200, "measuredAt": "2026-11-14T03:00:00Z",
  "http": { "requestsPerMinute": 12.4,      // trung bình 15 phút gần nhất
            "requestsLastHour": 744, "clientErrorRate": 0.012, "serverErrorRate": 0.0,   // trên request của 60 phút; null khi chưa có
            "groups": [ { "group": "plans", "requestsLastHour": 320, "p95Millis": 85.3 } ] },  // group = đoạn đầu sau /api/v1; p95 khoảng 1 giờ gần nhất
  "jobs": { "unfinished": 0, "failedLast24h": 1 },                 // từ DB
  "failuresLast24h": { "email": 0, "routing": 1, "geocoding": 0, "job": 1 },   // FailureLog (T54)
  "rateLimitedLast24h": 3,
  "jvm": { "heapUsedBytes": 180000000, "heapMaxBytes": 402653184 },
  "database": { "activeConnections": 1, "idleConnections": 9, "maxConnections": 10, "sizeBytes": 52428800,
                "largestTables": [ { "name": "route_plan_stops", "bytes": 20971520, "rows": 41000 } ] } }   // 5 bảng lớn nhất; rows là ước lượng
```

- Không có tuổi bản backup: script giám sát T54 đã cảnh báo backup cũ (chủ dự án chọn, P-64).
- Số lần tối ưu lấy từ job; job được giữ 100 ngày (T53, P-64) nên biểu đồ 90 ngày đủ dữ liệu.

**Nhật ký thao tác (T58, P-61, FR-A14).** Mỗi thao tác dưới ghi đúng một dòng, trong cùng transaction với thay đổi (thao tác lỗi thì không có dòng nào); giữ 1 năm (DATA_MODEL §6).

| `action` | Ai làm | `targetType` | `details` |
|---|---|---|---|
| `MEMBER_UPDATED` | OWNER, `PATCH /store/members/{userId}` khi có field thật sự đổi | `USER` | `{email, changes: {role?\|status?\|canDrive?: {from, to}}}` |
| `INVITATION_REVOKED` | OWNER, `POST /store/invitations/{id}/revoke` | `INVITATION` | `{email, role}` |
| `PLAN_PUBLISHED` | OWNER/DISPATCHER, `POST /plans/{id}/publish` | `PLAN` | `{}` |
| `PLAN_CANCELLED` | OWNER/DISPATCHER, `POST /plans/{id}/cancel` | `PLAN` | `{from}` (trạng thái trước khi huỷ) |
| `PLAN_ROUTES_EDITED` | OWNER/DISPATCHER, `PUT /plans/{id}/routes` | `PLAN` | `{violations}` (số vi phạm sau khi chỉnh) |
| `STORE_VIEWED` | ADMIN, `GET /admin/stores/{id}` (danh sách thì không) | `STORE` | `{}` |
| `STORE_LOCKED` | ADMIN, `POST /admin/stores/{id}/lock` | `STORE` | `{reason}` |
| `STORE_UNLOCKED` | ADMIN, `POST /admin/stores/{id}/unlock` | `STORE` | `{reason}` (lý do của lần khoá vừa gỡ) |
| `USER_DISABLED` | ADMIN, `POST /admin/users/{id}/disable` | `USER` | `{email}` |
| `USER_ENABLED` | ADMIN, `POST /admin/users/{id}/enable` | `USER` | `{email, status}` |
| `USER_SESSIONS_REVOKED` | ADMIN, `POST /admin/users/{id}/sessions/revoke` | `USER` | `{email}` |
| `VERIFICATION_EMAIL_SENT` | ADMIN, `POST /admin/users/{id}/verification-email` | `USER` | `{email}` |
| `PASSWORD_RESET_LINK_SENT` | ADMIN, `POST /admin/users/{id}/password-reset-email` | `USER` | `{email}` |
| `STORE_OWNER_TRANSFERRED` | ADMIN, `POST /admin/stores/{id}/owner` | `USER` (chủ mới) | `{from, to}` (email chủ cũ, chủ mới) |

```jsonc
// AuditEvent
{ "id": 91, "occurredAt": "2026-11-14T03:12:45.123456Z", "actorId": 12, "actorEmail": "chu@tiem.vn", "actorRole": "OWNER",
  "storeId": 3, "action": "MEMBER_UPDATED", "targetType": "USER", "targetId": 15,
  "details": { "email": "taixe@tiem.vn", "changes": { "status": { "from": "ACTIVE", "to": "DISABLED" } } },
  "requestId": "6f1c…", "ip": "203.0.113.7" }   // requestId, ip null ngoài HTTP request
```

- `details` không chứa dữ liệu cá nhân ngoài email; quá 2000 ký tự thì thành `{"truncated": "<đầu chuỗi JSON>"}` và thao tác vẫn thành công.
- Hành động trên user ghi kèm `storeId` của user đó, nên OWNER thấy ADMIN đã làm gì với thành viên của mình.
- Danh sách `action` chỉ được thêm; client gặp giá trị lạ thì hiện mã.

## 4. Store — `/api/v1/store`, `/api/v1/invitations`

Store luôn là store của actor (không có `storeId` trong path).

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /store` | O, D | — | `Store` | — | ✅ |
| `PUT /store` | O | `{name, depot?: {address?, lat, lon} \| null, timezone}`; `depot` null hoặc vắng = không có depot | 200 `Store` | 400 `INVALID_TIMEZONE` | ✅ |
| `GET /store/members` | O, D | query `role?`, `status?`, `assignable?` (true: chỉ người được gán route, tức DRIVER hoặc `canDrive`, đang ACTIVE) | `[Member]` | — | ✅ |
| `PATCH /store/members/{userId}` | O | merge patch `{status?: ACTIVE\|DISABLED, role?: DISPATCHER\|DRIVER, canDrive?}`; không field nào xoá được (`null` → `NOT_NULLABLE`). DISABLED thì thu hồi mọi session. | 200 `Member` | 404 · 422 `CANNOT_MODIFY_OWNER` | ✅ |
| `PATCH /store/members/me/can-drive` | O | `{canDrive}`: OWNER tự bật/tắt việc tự đi giao | 200 `Member` | — | ✅ |
| `POST /store/invitations` | O | `{email, role: DISPATCHER\|DRIVER}` | 201 `Invitation`; gửi email mã mời | 409 `EMAIL_EXISTED` · 429 `RATE_LIMITED` | ✅ |
| `GET /store/invitations` | O | query `status?` | `[Invitation]` | — | ✅ |
| `POST /store/invitations/{id}/revoke` | O | — | 204; lời mời thành `REVOKED`, vẫn có trong danh sách | 404 · 422 `INVITATION_NOT_PENDING` | ✅ |
| `GET /store/audit` | O | `actorId?`, `action?`, `from?`, `to?` (`YYYY-MM-DD`, cả hai ngày tính, theo `timezone` của cửa hàng), paging; sort `occurredAt` (mặc định, mới nhất trước), `id` | Page `StoreAuditEvent` = `AuditEvent` bỏ `storeId`; `ip` là `null` với hành động của ADMIN (P-59) | 400 `INVALID_SORT_FIELD` · 400 `MALFORMED_REQUEST` | ✅ (T58) |
| `GET /invitations/{code}` | P | — | `{storeName, email, role, expiresAt}` | 404 `INVITATION_INVALID` | ✅ |
| `POST /invitations/accept` | P | `{code, password, fullName, phoneNumber?}` | 204 (user ACTIVE, email đã xác thực; sau đó log-in) | 404 `INVITATION_INVALID` · 409 `EMAIL_EXISTED` · 400 `INVALID_PHONE_NUMBER` · 423 `STORE_READ_ONLY` (cửa hàng bị khoá, T56) | ✅ |

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
- Lời mời chưa nhận đã hết hạn hoặc bị huỷ quá 90 ngày bị xoá (T53, DATA_MODEL §6): không còn trong `GET /store/invitations`. Lời mời `ACCEPTED` giữ lại.
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
- Job đã kết thúc quá 100 ngày bị xoá (T53; 100 ngày từ T60, DATA_MODEL §6), trừ job còn là job hiện tại của plan: `GET /jobs/{id}` của job đó → 404.
- Plan về `processingStatus = IDLE` ngay trước khi job ghi trạng thái cuối, nên với job chạy nền có thể có một khoảng rất ngắn plan đã `IDLE` mà job vẫn `RUNNING` (F-45). Client theo dõi job thì polling `GET /jobs/{id}` tới khi `status` là `SUCCEEDED` hoặc `FAILED`; client theo dõi plan thì dừng khi `processingStatus = IDLE`.
- `errorCode` khi `FAILED`: `ROUTING_PROVIDER_ERROR`, `GEOCODING_PROVIDER_ERROR`, `INTERRUPTED` (app khởi động lại khi job còn `QUEUED`/`RUNNING`), `JOB_QUEUE_FULL` (hàng đợi job nền đầy, thử lại sau) (P-30), `PLAN_MODIFIED` (plan đổi trong lúc optimize), `NO_SOLUTION` (solver không tìm được lời giải nào, P-33) hoặc `INTERNAL_ERROR`. Job lỗi thì plan quay về `processingStatus = IDLE` và kết quả tối ưu cũ giữ nguyên.

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
| `POST /plans/{id}/optimize` | O, D | `{version}` | 202 `Job` (mode inline mặc định: job đã chạy xong khi trả về). Kết quả đọc qua `GET /plans/{id}`. | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `PLAN_INCOMPLETE` · 422 `STOPS_NOT_GEOCODED` · 422 `INVALID_ASSIGNMENT` | ✅ |
| `POST /plans/{id}/publish` | O, D | `{version}` | 200 `PlanDetail` (PUBLISHED) | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `PLAN_NOT_OPTIMIZED` · 422 `INVALID_ASSIGNMENT` · 422 `SHIFT_OVERLAP` | ✅ |
| `POST /plans/{id}/cancel` | O, D | `{version}` | 200 `PlanDetail` (CANCELLED) | 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_CANCELLABLE` | ✅ |
| `GET /plans/{id}/driver-locations` | O, D | — | `DriverLocation[]` (xem dưới, P-53): mỗi route có stop một phần tử, theo thứ tự `routes`; plan không PUBLISHED/IN_PROGRESS → `[]` | 404 | ✅ |
| `PUT /plans/{id}/routes` | O, D | `{version, routes:[{routeId, stopIds:[...]}], unassignedStopIds:[...]}`. Chỉnh tay sau optimize: chuyển stop giữa route, đổi thứ tự, bỏ ra ngoài; tính lại ETA và quãng đường bằng matrix, không giải lại (Should, D-24, P-58). | 200 `PlanDetail` (có `violations[]`) | 400 · 404 · 409 `PLAN_MODIFIED`/`PLAN_BUSY` · 422 `PLAN_NOT_EDITABLE` · 422 `PLAN_NOT_OPTIMIZED` · 502 `ROUTING_PROVIDER_ERROR` | ✅ T28 |
| `POST /plans/{id}/copy` | O, D | `{planDate, name?, stops: ALL\|NOT_DELIVERED}` (T62, P-67, P-68) | 201 `PlanCopy` `{plan: PlanDetail (DRAFT mới), skippedAssignments: [{driverId, vehicleId, reason}]}` | 400 · 404 · 422 `NOTHING_TO_COPY` · 423 `STORE_READ_ONLY` | ✅ T62 |
| `POST /plans/import` | O, D | `multipart/form-data`, phần `file`: `.xlsx` (sheet đầu) hoặc `.csv`, ≤ 1 MB, ≤ 100 dòng (T61, P-67, P-68) | 200 `PlanImport` (xem dưới). **Không lưu gì.** | 400 `MALFORMED_REQUEST` (thiếu `file`, không phải multipart) · 413 `IMPORT_FILE_TOO_LARGE` · 422 `INVALID_IMPORT_FILE` · 422 `PLAN_TOO_LARGE` · 423 `STORE_READ_ONLY` · 429 `RATE_LIMITED` | ✅ T61 |
| `GET /plans/import/template.xlsx` | O, D | — | 200 file mẫu Excel (`Content-Disposition: attachment`), một dòng tiêu đề và một dòng ví dụ, mọi ô là chữ | — | ✅ T61 |
| `GET /plans/import/template.csv` | O, D | — | 200 file mẫu CSV UTF-8 có BOM, cùng nội dung | — | ✅ T61 |

**Vị trí tài xế (T32, P-15, D-26, P-53):** dispatcher polling cùng nhịp với `GET /plans/{id}` khi plan đang giao.

```jsonc
// DriverLocation
{ "routeId": 63, "driverId": 451,
  "location": { "lat": 16.0605, "lon": 108.1985, "accuracyMeters": 12.0, "headingDegrees": 90.0, "speedMps": 7.8,
                "recordedAt": "2026-12-01T04:01:20Z" },       // điểm cuối của tài xế trong 10 phút qua; null nếu không có
  "sharing": { "state": "DECLINED", "updatedAt": "2026-12-01T03:40:05Z" } }   // trạng thái app báo cho đúng route này; null nếu không có
```

- `location` là vị trí cuối của **tài xế** (không gắn với route); `accuracyMeters`, `headingDegrees` (0–360, theo chiều kim đồng hồ từ hướng bắc), `speedMps` có thể `null`.
- `sharing` chỉ có khi trạng thái tài xế báo gần nhất (`PUT /driver/location/sharing`, giữ 12 giờ) là của route này. Client hiển thị: có `location` → vẽ vị trí; không có → lý do theo `sharing.state` (`DECLINED` tài xế tắt chia sẻ, `NO_PERMISSION` máy chưa cấp quyền, `GPS_OFF` GPS tắt); không có `sharing` hoặc `SHARING` → chưa có vị trí trong 10 phút (có thể mất sóng).
- Plan của store khác → 404.

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
// Violation (một phần tử của PlanDetail.violations, T28, P-58)
{ "code": "CAPACITY_EXCEEDED|TIME_WINDOW_MISSED|SHIFT_EXCEEDED|MAX_DISTANCE_EXCEEDED",
  "routeId": 11, "stopId": 101,   // stopId chỉ có với TIME_WINDOW_MISSED, null với code khác
  "excess": 3 }                   // vượt bao nhiêu: theo capacityUnit của xe (CAPACITY), giây (TIME_WINDOW: tới muộn hơn time window end; SHIFT: về muộn hơn shiftEnd), mét (MAX_DISTANCE)
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
    { "id": 11, "driver": { "id": 21, "fullName": "Tùng", "role": "DRIVER", "phoneNumber": "+84905123019" },   // phoneNumber: E.164 hoặc null, để gọi nhắc (P-53, T32) "vehicle": { "id": 5, "name": "Xe 1", "capacity": 40, "capacityUnit": "BOX" },
      "status": "PENDING|IN_PROGRESS|COMPLETED",            // của route, như §8 (P-35, P-39); plan DRAFT luôn PENDING
      "shiftStart": "08:00", "shiftEnd": "17:00", "maxDistanceMeters": 80000,
      "start": { "address": "…", "lat": 16.05, "lon": 108.2 }, "end": { "address": "…", "lat": 16.05, "lon": 108.2 },
      "plannedDistanceMeters": 30100, "plannedDurationSeconds": 5400,
      "plannedStart": "…", "plannedEnd": "…", "load": 31,   // theo capacityUnit của xe (D-36); null khi chưa optimize
      "stops": [ PlanStop, … ] }                            // theo sequence
  ],
  "unassignedStops": [ PlanStop, … ],  // stop chưa gán route: plan chưa optimize, solver đã bỏ (P-13), hoặc dispatcher bỏ ra (T28); theo id
  "violations": [ Violation, … ]       // ràng buộc bị vi phạm sau khi chỉnh tay (T28, P-58); rỗng khi không có
}
// PlanStop (cùng một dạng trong routes[].stops và unassignedStops)
{ "id": 101, "sequence": 1, "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "+84905…",
  "address": "…", "lat": 16.06, "lon": 108.22,
  "geocodeStatus": "PROVIDED|PENDING|RESOLVED|FAILED", "geocodeError": "NOT_FOUND|PROVIDER_ERROR|null",
  "geocodedDisplayName": "…|null", "demands": { "BOX": 3 }, "serviceTimeSeconds": 300,
  "timeWindow": { "start": "09:00", "end": "11:00" } | null, "note": "…", "plannedArrival": "…|null",
  "status": "PENDING|DELIVERED|FAILED|SKIPPED", "failureReason": null,
  "dropReason": "NOT_OPTIMIZED|INFEASIBLE|MANUAL|null" }   // null trong route
```

`PlanDetail`:
- `routes` theo id. `PUT` tạo lại mọi route từ `assignments`, nên id route đổi sau mỗi `PUT`; client tải lại `PlanDetail` sau khi `PUT`.
- `routes[].driver` (`fullName`, `role`) và `routes[].vehicle.name` là giá trị hiện tại, kể cả khi thành viên đã bị vô hiệu hoá hay xe đã tắt; `vehicle.capacity`, `vehicle.capacityUnit` là snapshot lúc phân công.
- `PlanStop.demands` chỉ có các loại khác 0. `dropReason`: `NOT_OPTIMIZED` khi plan chưa có kết quả tối ưu, `INFEASIBLE` khi solver không xếp được, `MANUAL` khi dispatcher bỏ ra khỏi route bằng `PUT /plans/{id}/routes` (T28).
- `violations` (T28, P-58): luôn có mặt, rỗng khi không có vi phạm; tính từ route đang lưu mỗi lần đọc (không lưu riêng), rỗng khi plan chưa tối ưu. Kết quả của optimize không vi phạm; vi phạm chỉ xuất hiện sau khi chỉnh tay. Thứ tự: theo `routes`, rồi theo `sequence` của stop.

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

**Sao chép plan (`POST /plans/{id}/copy`, T62, P-67, P-68):**
- Chép từ plan ở **mọi trạng thái** (kể cả CANCELLED) sang một plan **DRAFT mới** chưa tối ưu, `version` 0; plan cũ không đổi. `planDate` bắt buộc (`PLAN_DATE_REQUIRED`); `name` tuỳ chọn (≤ 150, `INVALID_PLAN_NAME_LENGTH`), trống thì "{tên cũ} (bản sao)" (tên cũ cắt bớt cho vừa 150 ký tự); `stops` bắt buộc (`COPY_SCOPE_REQUIRED`), giá trị khác `ALL`/`NOT_DELIVERED` → 400 `MALFORMED_REQUEST`.
- `ALL`: mọi stop. `NOT_DELIVERED`: stop `FAILED`, `SKIPPED` và stop còn `PENDING` (bỏ stop `DELIVERED`); không còn stop nào → 422 `NOTHING_TO_COPY`. `ALL` của plan không có stop vẫn tạo được draft rỗng.
- Mỗi stop chép: nhãn, người nhận, số điện thoại, địa chỉ, toạ độ **cùng `geocodeStatus`, `geocodeError` và tên địa chỉ provider** (không geocode lại; stop `FAILED` giữ `FAILED` để sửa), demand, thời gian phục vụ, time window, ghi chú. Stop còn `PENDING` geocode thì plan mới `GEOCODING` và chạy job như khi lưu draft. Không chép: route, thứ tự, ETA, trạng thái giao (về `PENDING`), lý do thất bại.
- Depot: snapshot của plan cũ; timezone: của cửa hàng lúc chép. Phân công (theo thứ tự `routes` của plan cũ) chỉ chép khi tài xế còn là thành viên ACTIVE giao được (BR-07) và xe còn dùng; capacity theo xe hiện tại; ca và quãng đường tối đa giữ nguyên. Phân công bị bỏ nằm trong `skippedAssignments` với `reason` `DRIVER_UNAVAILABLE` (xét trước) hoặc `VEHICLE_INACTIVE`. Không kiểm tra trùng ca lúc chép (kiểm ở publish, D-23).

**Nhập điểm giao từ file (`POST /plans/import`, T61, P-67, P-68):**
- Chỉ đọc file, không ghi DB. Client thêm các dòng không lỗi vào draft rồi lưu bằng `POST /plans` / `PUT /plans/{id}` như thường (geocode, validation và giới hạn 100 stop của draft áp dụng lúc lưu).
- Định dạng theo **đuôi tên file** (không phân biệt hoa thường): `.xlsx` (Excel 2007+, sheet đầu tiên; công thức không được tính lại, lấy giá trị Excel đã lưu; ô số và ô giờ đọc theo kiểu ô) hoặc `.csv` (UTF-8 có/không BOM; không phải UTF-8 hợp lệ thì đọc Windows-1258; dấu phân cách `,`, `;` hoặc tab, chọn theo dòng đầu; ô có ngoặc kép theo RFC 4180). `.xls` cũ và đuôi khác → `INVALID_IMPORT_FILE`.
- Dòng đầu tiên không trống là tiêu đề. Tiêu đề so khớp không phân biệt hoa thường, dấu, khoảng trắng và ký tự khác chữ/số; thứ tự cột tuỳ ý. Cột (tên trong file mẫu → field): `Nhãn / mã đơn` (cũng nhận `Nhãn`, `Mã đơn`; **bắt buộc có cột**) → `label`, `Người nhận` → `recipientName`, `Số điện thoại` (`SĐT`, `Điện thoại`) → `recipientPhone`, `Địa chỉ` → `address`, `Vĩ độ` (`lat`) → `lat`, `Kinh độ` (`lon`, `lng`) → `lon`, `KG` / `Thùng` / `Kiện` → `demands.KG` / `.BOX` / `.PACKAGE` (số nguyên), `Phút phục vụ` → `serviceTimeSeconds` (phút × 60, 0–1440 phút), `Giao từ` / `Giao đến` → `timeWindow.start` / `.end` (`HH:mm`, `H:mm`, `9h30`, hoặc ô giờ của Excel), `Ghi chú` → `note`. Tiêu đề không khớp cột nào, hoặc lặp lại một cột đã có, nằm trong `ignoredColumns`. Số thập phân nhận cả `16.06` và `16,06`.
- Dòng trống bị bỏ qua. Không có dòng dữ liệu → `INVALID_IMPORT_FILE` (`NO_ROWS`); hơn 100 dòng dữ liệu → 422 `PLAN_TOO_LARGE`.
- `INVALID_IMPORT_FILE` có `details` `[{field: "file", code}]`, `code` là `UNSUPPORTED_FILE_TYPE`, `UNREADABLE_FILE` (file hỏng, không đúng định dạng của đuôi, hoặc `.xlsx` giải nén ra quá 10 MB), `NO_ROWS`, `LABEL_COLUMN_REQUIRED`.
- Lỗi theo dòng dùng **cùng mã validation** với stop của `PlanDraftRequest` (§6.2 UX_BRIEF), `field` là tên field của stop; mỗi field một lỗi. Thêm mã `INVALID_TIME_FORMAT` (ô giờ không đọc được) trên `timeWindow.start`/`.end`. Ô số không đọc được là `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_DEMAND`, `INVALID_SERVICE_TIME`.
- Rate limit 30 lần/giờ/user (`plan-import-user`), đếm trước khi đọc file.

```jsonc
// PlanImport
{ "rows": [
    { "row": 2,                                   // số dòng trong file (1 là dòng tiêu đề)
      "stop": { "label": "ĐH-1", "recipientName": "Chị Lan", "recipientPhone": "+84905123456",   // E.164 khi hợp lệ, không thì như trong file
                "address": "12 Lê Duẩn, Đà Nẵng", "lat": null, "lon": null, "demands": { "BOX": 2 },
                "serviceTimeSeconds": 300, "timeWindow": { "start": "09:00", "end": "11:30" }, "note": null },
      "errors": [] },                             // [{field, code}], rỗng = thêm được
    { "row": 3, "stop": { "label": null, … }, "errors": [ { "field": "label", "code": "STOP_LABEL_REQUIRED" } ] } ],
  "validCount": 1, "errorCount": 1,
  "ignoredColumns": ["Cột lạ"] }
```

**Thứ tự kiểm tra** của `PUT`, `PATCH`, `POST /geocode`: 404 → `PLAN_NOT_EDITABLE` → `PLAN_BUSY` → `PLAN_MODIFIED`. Mỗi lần sửa thành công tăng `version` đúng 1.

**Geocode nền:**
- Mọi lần lưu (`POST`, `PUT`, `PATCH` stop) còn stop `PENDING` → tạo job `GEOCODE_PLAN`; response có `processingStatus = GEOCODING`, `activeJob` (thường `QUEUED`). Stop `FAILED` không tự chạy lại; dùng `POST /plans/{id}/geocode` hoặc sửa stop.
- `POST /plans/{id}/geocode`: stop `FAILED` về `PENDING`, plan `GEOCODING`, `version` tăng 1. Không có stop `PENDING`/`FAILED` nào → 422 `NOTHING_TO_GEOCODE` (P-30).
- Stop không tìm thấy → `FAILED/NOT_FOUND`; provider lỗi → `FAILED/PROVIDER_ERROR`; job vẫn `SUCCEEDED`. Kết quả ghi theo lô nên `geocodingSummary` thay đổi dần khi polling.
- Job kết thúc thì plan về `IDLE`, `activeJob = null`, **`version` tăng 1** (P-30): client polling `GET /plans/{id}` tới khi `IDLE` rồi dùng `version` mới. Ví dụ tạo plan có stop chỉ có địa chỉ: response `version = 0`, geocode xong `version = 1`.
- Trong lúc `GEOCODING`, `PUT`, `PATCH` stop, `POST /geocode` → 409 `PLAN_BUSY`.

**Optimize (T25, ARCHITECTURE §8.3):**
- Kiểm tra theo thứ tự: 404 → `PLAN_NOT_EDITABLE` → `PLAN_BUSY` → `PLAN_MODIFIED` → `PLAN_INCOMPLETE` → `STOPS_NOT_GEOCODED` → `INVALID_ASSIGNMENT`. Giới hạn kích thước đã kiểm khi lưu draft.
- `PLAN_INCOMPLETE` (P-33): plan không có stop hoặc không có phân công; `details` chỉ ra thiếu gì: `{field: "stops", code: "STOPS_REQUIRED"}`, `{field: "assignments", code: "ASSIGNMENTS_REQUIRED"}`. Plan 1 stop vẫn optimize được.
- `STOPS_NOT_GEOCODED`: còn stop chưa có toạ độ (`PENDING` hoặc `FAILED`); `geocodingSummary` cho biết bao nhiêu.
- `INVALID_ASSIGNMENT`: tài xế không còn ACTIVE hoặc không còn được gán route, hoặc xe đã tắt, kể từ lúc lưu draft; mỗi phân công sai là một phần tử `details` `{field: "assignments[i].driverId" | "assignments[i].vehicleId", code: "INVALID_ASSIGNMENT"}`, `i` theo thứ tự `PlanDetail.routes`.
- Request thành công: plan `OPTIMIZING`, `version` tăng 1. Job ghi kết quả và đưa plan về `IDLE` trong cùng một bước, `version` tăng thêm 1. Job lỗi: plan về `IDLE`, kết quả cũ giữ nguyên, `version` tăng 1.
- Kết quả: mỗi route có `stops` theo `sequence`, `plannedArrival` = giờ bắt đầu phục vụ (nằm trong time window), `plannedStart`/`plannedEnd` = giờ rời và về điểm đầu/cuối, `plannedDistanceMeters`, `plannedDurationSeconds` (lái, phục vụ và chờ), `load`. Route không được dùng: `stops` rỗng, `plannedDistanceMeters = 0`, `plannedDurationSeconds = 0`, `plannedStart`/`plannedEnd` null, `load = 0`. `totalDistanceMeters`/`totalDurationSeconds` là tổng các route. Stop không xếp được nằm trong `unassignedStops` với `dropReason = INFEASIBLE` (P-13).
- Capacity theo snapshot của phân công (D-36), không theo giá trị hiện tại của xe.
- Solver chạy đúng time limit cấu hình (mặc định 10 s), nên mỗi lần optimize có stop mất khoảng đó.
- Solver không tìm được lời giải nào → job `FAILED/NO_SOLUTION`, kết quả cũ giữ nguyên (P-33). Thường do ca hoặc quãng đường tối đa quá chặt.

**Vòng đời plan và stop (P-34):** `DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED`, và `DRAFT | PUBLISHED → CANCELLED`. Plan PUBLISHED chuyển thẳng sang COMPLETED nếu lần cập nhật đầu tiên đã kết thúc mọi stop. Stop đi một chiều `PENDING → DELIVERED | FAILED | SKIPPED`. **Không có đảo ngược**: stop đã kết thúc và plan COMPLETED/CANCELLED không đổi được nữa; không có endpoint đặt lại stop về PENDING. Giao lại thì tạo stop trong một plan mới.

**Chỉnh tay route (`PUT /plans/{id}/routes`, T28, D-24, P-58):**
- Chỉ plan DRAFT đã có kết quả tối ưu. Kiểm tra theo thứ tự: 404 → `PLAN_NOT_EDITABLE` (không phải DRAFT) → `PLAN_BUSY` → `PLAN_MODIFIED` → `PLAN_NOT_OPTIMIZED` → 400 validation.
- `routes` phải liệt kê **mọi** route của plan đúng một lần (kể cả route không có stop, gửi `stopIds: []`); mỗi stop của plan nằm đúng một lần trong `routes[].stopIds` hoặc `unassignedStopIds`. Lỗi là 400 `VALIDATION_ERROR` với `details`: `routes[i].routeId` `INVALID_ROUTE_ID` (không phải route của plan, hoặc lặp); `routes` `ROUTES_INCOMPLETE` (thiếu route); `routes[i].stopIds[j]` hoặc `unassignedStopIds[j]` `INVALID_STOP_ID` (không phải stop của plan, hoặc lặp); `stops` `STOPS_INCOMPLETE` (có stop không được nhắc tới); `routes[i].stopIds[j]` `STOP_NOT_GEOCODED` (stop chưa có toạ độ không được vào route). Mã field thiếu: `VERSION_REQUIRED`, `ROUTES_REQUIRED`, `UNASSIGNED_STOP_IDS_REQUIRED`.
- Tính lại bằng `MatrixProvider` (cùng node như optimize), gọi ngoài transaction; ghi trong transaction kiểm tra `version` (đổi giữa chừng → 409 `PLAN_MODIFIED`); matrix lỗi → 502 `ROUTING_PROVIDER_ERROR`, không đổi gì. Mỗi route có stop: rời điểm đầu lúc `shiftStart`, hoặc muộn hơn để tới stop đầu đúng lúc time window của nó mở (không chờ ở stop đầu); tới sớm thì chờ tới đầu time window; `plannedArrival` = giờ bắt đầu phục vụ; `plannedEnd` = giờ về điểm cuối; `plannedDurationSeconds` = `plannedEnd − plannedStart` (lái, phục vụ, chờ); `plannedDistanceMeters` theo matrix. Route không còn stop: như route không được dùng của optimize. `totalDistanceMeters`/`totalDurationSeconds` tính lại; `optimizedAt` giữ nguyên; `version` tăng 1.
- Lưu được cả khi vi phạm ràng buộc (vượt tải, trễ time window, quá ca, quá `maxDistanceMeters`): response có `violations`. Stop trong `unassignedStopIds` có `dropReason = MANUAL` tới lần optimize sau; optimize lại thay mọi chỉnh tay bằng kết quả mới.

**Publish (T26):**
- Kiểm tra theo thứ tự: 404 → `PLAN_NOT_EDITABLE` (không phải DRAFT) → `PLAN_BUSY` → `PLAN_MODIFIED` → `PLAN_NOT_OPTIMIZED` (chưa có kết quả tối ưu còn hiệu lực, BR-06) → `INVALID_ASSIGNMENT` (tài xế/xe của route có stop không còn dùng được, `details` như optimize) → `SHIFT_OVERLAP`.
- Trùng ca (D-23, BR-12): với mỗi route **có stop**, ca `[shiftStart, shiftEnd)` không được giao với ca của cùng tài xế hoặc cùng xe trong một route có stop của plan PUBLISHED/IN_PROGRESS khác **cùng `planDate`** trong store. Ca nửa mở: 08:00–12:00 và 12:00–16:00 không trùng. Route không có stop không tính (P-34). Plan CANCELLED, COMPLETED, DRAFT không tính.
- `SHIFT_OVERLAP` trả mỗi xung đột là một phần tử `details`: `{field: "assignments[i].driverId" | "assignments[i].vehicleId", code: "SHIFT_OVERLAP", conflictPlanId}` (P-32); `i` là thứ tự route trong `PlanDetail.routes`. Trùng cả tài xế lẫn xe với một plan thì có hai phần tử.
- Plan còn stop trong `unassignedStops` (`INFEASIBLE`, `MANUAL`) vẫn publish được (P-13); client cảnh báo dựa vào `unassignedStops`. Plan còn `violations` cũng publish được (P-58); client hỏi xác nhận trước khi gửi.
- Thành công: `status = PUBLISHED`, `version` tăng 1. Sau đó `PUT`, `PATCH` stop, `geocode`, `optimize`, `publish` → 422 `PLAN_NOT_EDITABLE`.

**Cancel (T26):** được từ DRAFT và PUBLISHED; IN_PROGRESS, COMPLETED, CANCELLED → 422 `PLAN_NOT_CANCELLABLE`. Kiểm tra theo thứ tự: 404 → `PLAN_NOT_CANCELLABLE` → `PLAN_BUSY` → `PLAN_MODIFIED`. Kết quả tối ưu giữ lại để tra cứu; `version` tăng 1.

**Mã validation (`details[].code`) của plan:** `STOPS_REQUIRED`, `ASSIGNMENTS_REQUIRED` (trong `PLAN_INCOMPLETE`), `PLAN_NAME_REQUIRED`, `INVALID_PLAN_NAME_LENGTH` (≤ 150), `PLAN_DATE_REQUIRED`, `VERSION_REQUIRED` (`PUT`, `PATCH`, `POST /geocode`), `LATITUDE_REQUIRED`, `LONGITUDE_REQUIRED`, `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_ADDRESS_LENGTH` (≤ 500), `STOP_LABEL_REQUIRED`, `INVALID_STOP_LABEL_LENGTH` (≤ 150), `INVALID_RECIPIENT_NAME_LENGTH` (≤ 100), `PHONE_NUMBER_*`, `COORDINATES_INCOMPLETE` (trên field còn thiếu, `lat` hoặc `lon`), `ADDRESS_OR_COORDINATES_REQUIRED` (trên `address`), `INVALID_DEMAND` (số nguyên ≥ 0), `ORDER_DEMAND_NOT_ALLOWED` (trên `demands`), `INVALID_SERVICE_TIME` (0–86400), `TIME_WINDOW_START_REQUIRED`, `TIME_WINDOW_END_REQUIRED`, `INVALID_TIME_WINDOW` (trên `timeWindow.end`, `start < end`), `INVALID_NOTE_LENGTH` (≤ 500), `DRIVER_REQUIRED`, `VEHICLE_REQUIRED`, `SHIFT_START_REQUIRED`, `SHIFT_END_REQUIRED`, `INVALID_SHIFT` (trên `shiftEnd`, `shiftStart < shiftEnd`), `INVALID_MAX_DISTANCE` (> 0), `INVALID_STOP_ID`, `NOT_NULLABLE`, `INVALID_ASSIGNMENT` (trong `INVALID_ASSIGNMENT` của optimize).

## 8. Driver — `/api/v1/driver`

Người gọi phải có `role=DRIVER` hoặc `canDrive=true` (D-21); người khác → 403. Tài xế chỉ thấy route có `driverId = mình`, **có ít nhất một stop** (P-34), thuộc plan PUBLISHED, IN_PROGRESS hoặc COMPLETED của store mình. Route khác (của tài xế khác, store khác, không có stop, plan DRAFT hoặc CANCELLED) không có trong danh sách và trả 404; riêng route của mình thuộc plan **đã publish rồi bị huỷ** thì `GET /driver/routes/{routeId}` trả 422 `PLAN_NOT_ACTIVE` (T48, P-52), như `PATCH` (P-36).

`status` của route (P-35) là trạng thái **riêng của route**, tính từ các stop của nó, không phải trạng thái plan: `PENDING` (chưa stop nào rời PENDING), `IN_PROGRESS`, `COMPLETED` (mọi stop đã kết thúc). Tài xế đã giao xong thấy `COMPLETED` dù route khác của plan vẫn đang chạy. Dispatcher thấy cùng giá trị ở `PlanDetail.routes[].status` (§7, P-39). `completedCount` đếm stop đã kết thúc (DELIVERED, FAILED, SKIPPED).

| Method & path | Quyền | Request | Response | Lỗi chính | Hiện trạng |
|---|---|---|---|---|---|
| `GET /driver/routes` | R | `date?` (`YYYY-MM-DD`, mặc định hôm nay theo timezone store) | `[{routeId, planId, planName, planDate, timezone, status, stopCount, completedCount, plannedStart}]`, theo `plannedStart`; `timezone` là timezone của plan (T47) | 400 `MALFORMED_REQUEST` (ngày sai dạng) | ✅ |
| `GET /driver/routes/{routeId}` | R | — | `DriverRouteDetail` | 404 · 422 `PLAN_NOT_ACTIVE` (plan đã publish rồi bị huỷ, T48) | ✅ |
| `PATCH /driver/routes/{routeId}/stops/{stopId}/status` | R | `{status: DELIVERED\|FAILED\|SKIPPED, failureReason?}` | 200 `{stop: DriverStop, routeStatus, planStatus}` | 400 · 404 · 422 `INVALID_STOP_TRANSITION` · 422 `FAILURE_REASON_REQUIRED` · 422 `PLAN_NOT_ACTIVE` | ✅ |
| `POST /driver/location` | R | `{points:[{lat, lon, accuracyMeters?, headingDegrees?, speedMps?, recordedAt}]}`, 1–100 điểm (D-26), thứ tự tuỳ ý | 204. MVP chỉ giữ điểm có `recordedAt` mới nhất (xem dưới). | 400 | ✅ |
| `PUT /driver/location/sharing` | R | `{routeId, state: SHARING\|DECLINED\|NO_PERMISSION\|GPS_OFF}` (P-53) | 204 | 400 · 404 · 422 `PLAN_NOT_ACTIVE` | ✅ |
| `GET /driver/routes/{routeId}/directions` | R | query `lat`, `lon` (vị trí hiện tại, bắt buộc), `toStopId?` (P-57) | `Directions`: đường từ vị trí hiện tại tới điểm đích (Should, FR-D05) | 400 · 404 · 422 `PLAN_NOT_ACTIVE` · 429 `RATE_LIMITED` · 502 `ROUTING_PROVIDER_ERROR` | ✅ T33 |

**Vị trí (T32):**
- `POST /driver/location`: mã validation `POINTS_REQUIRED`, `TOO_MANY_POINTS` (> 100), `POINT_REQUIRED`, `LATITUDE_REQUIRED`, `LONGITUDE_REQUIRED`, `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_ACCURACY` (< 0), `INVALID_HEADING` (ngoài `[0, 360)`), `INVALID_SPEED` (< 0), `RECORDED_AT_REQUIRED` (trên `points[i].<field>`). Lô hợp lệ luôn 204, kể cả khi không điểm nào được giữ: điểm có `recordedAt` muộn hơn giờ server quá 2 phút bị bỏ (đồng hồ máy chạy nhanh); điểm cũ hơn 10 phút không lưu; điểm không mới hơn điểm đang lưu không ghi đè. Gửi được khi không có route đang chạy (dispatcher chỉ đọc qua route của plan đang giao).
- `PUT /driver/location/sharing`: route phải là route tài xế thấy được như `GET /driver/routes/{routeId}` (khác thì 404); plan COMPLETED hoặc đã publish rồi huỷ → 422 `PLAN_NOT_ACTIVE`. Mã validation `ROUTE_ID_REQUIRED`, `SHARING_STATE_REQUIRED`; `state` ngoài enum → 400 `MALFORMED_REQUEST`. Mỗi tài xế giữ một trạng thái (route mới nhất được báo). Lựa chọn "Hỏi / Luôn / Không" của tài xế chỉ ở trên máy.

```jsonc
// DriverRouteDetail
{
  "routeId": 11, "planId": 7, "planName": "Giao sáng 22/10", "planDate": "2026-10-22", "timezone": "Asia/Ho_Chi_Minh",   // timezone: của plan (P-14, T47)
  "status": "PENDING|IN_PROGRESS|COMPLETED",   // status: của route
  "start": { "address": "…", "lat": 16.05, "lon": 108.2 }, "end": { "address": "…", "lat": 16.05, "lon": 108.2 },  // D-20
  "stops": [ DriverStop, … ]   // theo sequence
}
// DriverStop
{ "id": 101, "sequence": 1, "label": "ĐH-1024", "recipientName": "Chị Lan", "recipientPhone": "+84905…",
  "address": "…", "lat": 16.06, "lon": 108.22, "plannedArrival": "…",
  "timeWindow": { "start": "09:00", "end": "11:00" }, "note": "…", "status": "PENDING", "failureReason": null }
// Directions (T33, P-57): đường lái xe từ vị trí hiện tại tới một điểm, không phụ thuộc nhà cung cấp
{ "destination": { "stopId": 101, "lat": 16.06, "lon": 108.22 },   // stopId null: điểm kết thúc của tuyến (route.end)
  "distanceMeters": 2100, "durationSeconds": 360,
  "polyline": "…",               // Encoded Polyline Algorithm, độ chính xác 6 chữ số thập phân (1e-6), thứ tự [lat, lon]
  "steps": [
    { "maneuver": "DEPART", "exitNumber": null, "roadName": "Lê Duẩn",
      "distanceMeters": 400, "durationSeconds": 60, "location": { "lat": 16.0544, "lon": 108.2022 } },
    { "maneuver": "LEFT", "exitNumber": null, "roadName": "Nguyễn Văn Linh",
      "distanceMeters": 1700, "durationSeconds": 300, "location": { "lat": 16.0571, "lon": 108.2051 } },
    { "maneuver": "ARRIVE", "exitNumber": null, "roadName": null,
      "distanceMeters": 0, "durationSeconds": 0, "location": { "lat": 16.06, "lon": 108.22 } } ] }
```

- `timezone` (T47) là bản chụp timezone của cửa hàng lúc lưu plan, như `PlanDetail.timezone`: app hiện `plannedStart`, `plannedArrival` theo timezone này, không theo máy. Tài xế không đọc được `GET /store`.
- Dẫn đường (D-07): client tự dựng deep link Google Maps hoặc Apple Maps theo nền tảng từ `lat`/`lon` của stop; server không trả URL (P-32). Đây cũng là đường lùi khi `GET …/directions` lỗi.
- **`GET /driver/routes/{routeId}/directions`** (T33, P-57): app gọi khi mở màn dẫn đường và mỗi lần tài xế đi lệch đường; không cache vì điểm đầu là vị trí hiện tại.
  - `lat`, `lon`: vị trí hiện tại của máy, bắt buộc. Thiếu hoặc không phải số → 400 `MALFORMED_REQUEST` (như `q` của tìm địa chỉ); ngoài khoảng → 400 `VALIDATION_ERROR`, mã `INVALID_LATITUDE`/`INVALID_LONGITUDE` trên field `lat`/`lon`. `toStopId` không phải số → `MALFORMED_REQUEST`.
  - `toStopId`: điểm đích, phải là stop của route này (khác → 404), kể cả stop đã kết thúc. Không gửi → stop `PENDING` đầu tiên theo `sequence`; hết stop `PENDING` → điểm kết thúc của route (`destination.stopId = null`).
  - Route tìm như `GET /driver/routes/{routeId}` (404 như ở đó); plan `COMPLETED`, hoặc đã publish rồi huỷ → 422 `PLAN_NOT_ACTIVE`.
  - Mỗi bước: `maneuver` xảy ra tại `location` (đầu bước), `distanceMeters`/`durationSeconds` là đoạn đi sau thao tác đó tới bước kế tiếp. Bước đầu luôn `DEPART`, bước cuối luôn `ARRIVE` (0 m). `roadName` là tên đường sau thao tác, `null` khi không có tên và với `ARRIVE`. `exitNumber` chỉ có với `ROUNDABOUT` (lối ra thứ mấy, từ 1; `null` khi không biết).
  - `maneuver`: `DEPART`, `ARRIVE`, `STRAIGHT`, `SLIGHT_LEFT`, `SLIGHT_RIGHT`, `LEFT`, `RIGHT`, `SHARP_LEFT`, `SHARP_RIGHT`, `UTURN`, `KEEP_LEFT`, `KEEP_RIGHT` (rẽ nhánh, nhập làn, lên/xuống đường dẫn), `ROUNDABOUT`. Câu chỉ dẫn do client dựng từ enum (tiếng Việt ở client). Client sinh từ OpenAPI không đọc được giá trị lạ, nên **thêm giá trị mới là thay đổi phá vỡ contract** với app cũ.
  - Giới hạn 30 request mỗi 5 phút cho mỗi tài xế (rule `directions-user`) → 429 `RATE_LIMITED`. Nhà cung cấp lỗi, quá thời gian, hoặc không có đường (ví dụ vị trí nằm ngoài vùng dữ liệu bản đồ `ROUTING_BBOX`) → 502 `ROUTING_PROVIDER_ERROR`; app mở Google/Apple Maps thay thế.
- `PATCH …/status` của tài xế trả `routeStatus` (cùng nghĩa với `status` của route ở trên) và `planStatus` để app biết route/plan đã chuyển `IN_PROGRESS` hay `COMPLETED` (BR-11). Cập nhật trạng thái stop không tăng `version` của plan (§1). Chỉ đi từ `PENDING`; stop đã kết thúc không đổi được nữa (P-34).
  - Route tìm được như `GET` (P-36, T48): route của plan **đã publish rồi bị huỷ** trả 422 `PLAN_NOT_ACTIVE` thay vì 404, để app biết plan đã huỷ. Route của plan chưa từng publish, của tài xế khác hay store khác, và stop không thuộc route đó → 404 `NOT_FOUND`.
  - Thứ tự kiểm tra: 404 → `INVALID_STOP_TRANSITION` (gửi `PENDING`) → **idempotent**: stop đã ở đúng trạng thái gửi lên → 200, không đổi gì (kể cả `failureReason`), kể cả khi plan đã `COMPLETED`, để app gửi lại được khi mất response → `PLAN_NOT_ACTIVE` (plan `COMPLETED`, `CANCELLED`) → `INVALID_STOP_TRANSITION` (stop đã kết thúc với trạng thái khác) → `FAILURE_REASON_REQUIRED` (`FAILED` mà `failureReason` trống hoặc toàn khoảng trắng).
  - `failureReason` được cắt khoảng trắng hai đầu, chỉ lưu với `FAILED`; gửi kèm `DELIVERED`/`SKIPPED` thì bị bỏ qua.
  - Plan chuyển `IN_PROGRESS` khi stop đầu tiên rời `PENDING`, `COMPLETED` (ghi `completedAt`) khi mọi stop đã gán route đều kết thúc; stop trong `unassignedStops` không tính. Hai tài xế cập nhật cùng lúc được xử lý tuần tự theo plan (khoá dòng plan), nên stop cuối luôn đưa plan sang `COMPLETED`.
  - Mã validation: `STOP_STATUS_REQUIRED` (`status`), `INVALID_FAILURE_REASON_LENGTH` (`failureReason` ≤ 255). `status` không phải trạng thái stop → 400 `MALFORMED_REQUEST`.

## 9. Mã lỗi (`ErrorCode`)

| Nhóm | Code | HTTP |
|---|---|---|
| Request | `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_SORT_FIELD`, `INVALID_TIMEZONE`, `INVALID_PHONE_NUMBER` | 400 |
| Xác thực | `UNAUTHORIZED`, `INVALID_TOKEN`, `INVALID_EMAIL_OR_PASSWORD` | 401 |
| Xác thực | `INVALID_OTP`, `INVALID_CURRENT_PASSWORD`, `INVALID_RESET_TOKEN` | 400 |
| Quyền | `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED` | 403 |
| File quá lớn | `IMPORT_FILE_TOO_LARGE` (T61) | 413 |
| Cửa hàng bị khoá | `STORE_READ_ONLY` (T56, P-62) | 423 |
| Không tìm thấy | `NOT_FOUND`, `INVITATION_INVALID` | 404 |
| Method | `METHOD_NOT_ALLOWED` | 405 |
| Xung đột | `EMAIL_EXISTED`, `EMAIL_ALREADY_VERIFIED`, `VEHICLE_NAME_EXISTED`, `PLAN_MODIFIED`, `PLAN_BUSY`, `CONFLICT` | 409 |
| Nghiệp vụ | `CANNOT_MODIFY_OWNER`, `CANNOT_MODIFY_SELF`, `INVITATION_NOT_PENDING`, `STORE_DEPOT_REQUIRED`, `INVALID_ASSIGNMENT`, `PLAN_TOO_LARGE`, `INVALID_IMPORT_FILE` (T61), `NOTHING_TO_COPY` (T62), `PLAN_NOT_EDITABLE`, `NOTHING_TO_GEOCODE`, `PLAN_INCOMPLETE`, `STOPS_NOT_GEOCODED`, `PLAN_NOT_OPTIMIZED`, `PLAN_NOT_CANCELLABLE`, `PLAN_NOT_ACTIVE`, `SHIFT_OVERLAP`, `INVALID_STOP_TRANSITION`, `FAILURE_REASON_REQUIRED` | 422 |
| Giới hạn | `OTP_COOLDOWN`, `RATE_LIMITED` | 429 |
| Provider | `GEOCODING_PROVIDER_ERROR`, `ROUTING_PROVIDER_ERROR` | 502 |
| Provider / hạ tầng | `GEOCODING_BUSY`, `SERVICE_UNAVAILABLE` | 503 |
| Khác | `INTERNAL_ERROR` | 500 |

- `CONFLICT`: vi phạm ràng buộc DB không có mã nghiệp vụ riêng, ví dụ hai request đăng ký đồng thời.
- `SERVICE_UNAVAILABLE`: endpoint có rate limit không kiểm được giới hạn vì Redis lỗi (P-37).
- Không còn trả (giữ trong enum dạng `@Deprecated` vì `ErrorCode` chỉ được thêm): `USER_NOT_FOUND`, `SESSION_NOT_FOUND`, `JOB_NOT_FOUND` (thay bằng `NOT_FOUND`, P-32), `COULD_NOT_FIND_USER`, `COULD_NOT_UPDATE_USER_PASSWORD`. Đã bỏ cùng `/route/optimize*` ở T27 (`@Deprecated`): `INVALID_DEPOT`, `COULD_NOT_FIND_CHEAPEST_ROUTE`, `COULD_NOT_FIND_ADDRESS`.
- `Job.errorCode` (§6) có thêm các giá trị không phải `ErrorCode`: `INTERRUPTED`, `JOB_QUEUE_FULL`, `NO_SOLUTION`.
- Mã validation trong `details[].code` dùng UPPER_SNAKE, ví dụ `EMAIL_REQUIRED`, `INVALID_EMAIL_FORMAT`, `INVALID_PASSWORD_LENGTH`, `PHONE_NUMBER_WRONG_REGION`, `NOT_NULLABLE`.
