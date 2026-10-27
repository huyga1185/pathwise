# ARCHITECTURE — Kiến trúc mục tiêu & quy chuẩn

> Tài liệu này mô tả **kiến trúc mục tiêu** của backend và các **quy chuẩn bắt buộc**. Hiện trạng và khoảng cách: `CURRENT_STATE.md`. Quyết định và trạng thái phê duyệt: `DECISIONS.md` (đã duyệt toàn bộ ngày 2026-10-22).
>
> **Nguyên tắc của chủ dự án:** làm phương án đơn giản trước, nhưng luôn để sẵn điểm mở rộng (interface, cột dữ liệu, contract) để không phải đập đi xây lại.
>
> Nguyên tắc: giữ kiến trúc hiện có (modular monolith theo nghiệp vụ, controller → service → repository/manager, abstraction cho hạ tầng ngoài), chỉ thay đổi khi có vấn đề cụ thể.

## 1. Tổng thể

**Modular monolith** Spring Boot, một deployable, một database MariaDB, Redis cho trạng thái ngắn hạn.

```
            Web (SPA)          Flutter app
                 \               /
                  HTTPS  /api/v1/**
                         |
   ┌──────────────────── Spring Boot (be/) ────────────────────┐
   │  auth ── user ── store ── fleet ── planning ── routing     │
   │     \______ notification ______/        (driver API)       │
   │                 core / shared                              │
   └───────────┬───────────────┬─────────────────┬─────────────┘
           MariaDB           Redis        SMTP · Nominatim · OSRM (· Google sau)
```

| Thành phần | Dùng cho |
|---|---|
| MariaDB | Dữ liệu bền: users, stores, invitations, vehicles, plans/routes/stops, refresh tokens. Schema do Flyway quản lý (P-01). |
| Redis | OTP, OTP/reset token, cooldown, rate limit, cache geocoding, vị trí cuối của tài xế. Mọi key **phải có TTL**. |
| SMTP (Brevo) | Email OTP, lời mời. |
| Nominatim | Geocoding search (≤1 req/s, cache) — sẽ thay bằng Google (D-10). |
| OSRM | Ma trận distance/duration; (Should) route geometry/steps. |
| OR-Tools | Solver VRPTW in-process. |

## 2. Module và ranh giới

Package gốc `uk.huy.pathwise`. Mỗi module là một package cấp 1.

| Module | Trách nhiệm | Trạng thái |
|---|---|---|
| `core` | Hạ tầng dùng chung, không chứa nghiệp vụ: envelope response, `ErrorCode`/`AppException`/handler, pagination, `StateStore`, `core.security` (`Hasher` SHA-256, `OpaqueTokenGenerator`; P-28), rate limiter, **job framework** (`core.job`, §8.4), cấu hình web/JSON. | Có — mở rộng |
| `shared` | Kiểu dữ liệu chia sẻ giữa module: `UserIdentity`, `UserRole`, `UserStatus`, principal `AuthenticatedUser` (P-25). Chỉ record/enum, **không** bean. | Có — sửa |
| `user` | Tài khoản người dùng: entity `User`, mật khẩu, hồ sơ, trạng thái. | Có — mở rộng |
| `auth` | Xác thực: đăng ký (orchestrate user + store), đăng nhập, token (access/refresh/OTP token), OTP, session, đổi/quên mật khẩu, security filter chain. | Có — sửa |
| `notification` | Gửi email theo template (chuyển từ `auth.email.infrastructure` sang, để `store` gửi được lời mời mà không phụ thuộc `auth`). API công khai: `EmailNotificationService.send(templateName, to, variables)`, lỗi gửi là `EmailDeliveryException`. | Có (T12) |
| `store` | Store, thành viên, lời mời. API công khai: `StoreProvisioningService.createStore(name)` (cho đăng ký, tham gia transaction của bên gọi), `StoreMemberQuery.findActiveMember(storeId, userId)`, event `MemberDisabledEvent` (P-27, auth nghe để thu hồi session). Đọc và ghi user qua `StoreUserService` của `user`. | Có (T13, T16) |
| `fleet` | Xe. | Mới — T20 |
| `routing` | Adapter định tuyến **không có DB**: `GeocodingProvider`, `MatrixProvider`, `SolverProvider`, (Should) `DirectionsProvider`; endpoint geocoding search. Đổi tên từ `routeoptimizer`. | Có — geocoding (T21), matrix (T22), solver (T23) |
| `planning` | Route plan, route, stop; optimize, publish, trạng thái; API cho dispatcher **và** API cho driver; vị trí tài xế. | Mới — T24+ |

### 2.1 Quy tắc phụ thuộc giữa module (bắt buộc)

```
core, shared  ←  mọi module
user          →  (core, shared)
notification  →  (core)
store         →  user, notification
auth          →  user, store, notification
fleet         →  (core, shared)
routing       →  (core)
planning      →  store, fleet, routing
```

- **Không có vòng phụ thuộc.** `user` không biết `auth`/`store`; `routing` không biết `planning`.
- Module khác chỉ được dùng **service interface công khai** và **DTO/record** của module đích (ví dụ `UserAccountService`, `StoreMemberQuery`). **Cấm** inject repository, entity hoặc class trong `infrastructure` của module khác.
- Entity JPA **không** đi ra khỏi module sở hữu (không trả entity qua service interface công khai, không tạo quan hệ `@ManyToOne` tới entity module khác — dùng cột id `Long storeId`, `Long driverId`).
- Thêm một mũi tên phụ thuộc mới vào sơ đồ trên = quyết định kiến trúc → ghi vào `DECISIONS.md`.
- Module phía dưới cần báo cho module phía trên (ví dụ store → auth) thì phát **event đồng bộ** là record public của mình; module phía trên nghe bằng `@EventListener`, chạy trong cùng transaction (P-27).

### 2.2 Cấu trúc trong một module

```
<module>/
  controller/        @RestController — chỉ HTTP
  dto/request/       record request + Bean Validation
  dto/response/      record response
  service/           use-case service (+ interface công khai nếu module khác cần)
  model/             entity JPA, enum, value object của module
  repository/        Spring Data JPA
  store/             Redis/KV store (nếu có)
  manager/           thành phần quản lý vòng đời một khả năng kỹ thuật (token, OTP...) — chỉ trong auth hiện tại
  infrastructure/    adapter ngoài, config, mapper, validator, utils kỹ thuật
```

Không bắt buộc module nào cũng đủ thư mục; chỉ tạo khi có nội dung.

## 3. Trách nhiệm từng loại thành phần

| Loại | Được làm | Không được làm | Được phụ thuộc vào |
|---|---|---|---|
| `*Controller` | Map HTTP ↔ DTO, `@Valid`, đọc `@AuthenticationPrincipal AuthenticatedUser` (P-25), cookie/header, chọn status code, `@PreAuthorize` theo role. | Business logic, gọi repository, `@Transactional`, try/catch để đổi lỗi (để handler toàn cục làm). | Service của chính module. |
| `*Service` | Use case: kiểm tra quyền sở hữu/tenant, business rule, ranh giới transaction, orchestrate manager/repository/provider, map entity → DTO. | Biết về HTTP (cookie, header, `HttpServletRequest`). | Repository/store/manager/provider của module mình; service interface công khai của module khác. |
| `*Manager` | Đóng gói vòng đời một khả năng kỹ thuật dùng lại được (sinh/verify/thu hồi token, OTP). | Biết HTTP; kiểm tra quyền nghiệp vụ; được gọi từ controller. | Store/repository/generator/hasher. |
| `*Repository` | Truy vấn JPA. Mọi truy vấn dữ liệu thuộc store **phải có tham số `storeId`**. | Logic nghiệp vụ. | — |
| `*Store` | Thao tác Redis có TTL; fail-closed bằng `StateStoreException`. | Logic nghiệp vụ ngoài thao tác atomic cần thiết (ví dụ Lua verify OTP). | `RedisTemplate`. |
| `*Provider` | Adapter tới hệ thống ngoài; dịch lỗi ngoài thành `ExternalServiceException` (T03). | Biết entity/DTO API của module khác. | HTTP client. |
| Mapper (MapStruct) | Entity ↔ DTO trong cùng module. | Gọi service/repository. | — |

**Optional, kết quả và lỗi (P-24):**

| Tình huống | Cách biểu diễn | Ví dụ |
|---|---|---|
| Tra cứu có thể không có dữ liệu | `Optional<T>` (chỉ làm kiểu trả về) | `UserRepository.findByEmail`, `StateStore.find` |
| Kiểm tra/quyết định có nhiều kết quả cần xử lý hoặc log khác nhau, hoặc nhánh thành công mang dữ liệu | `sealed interface` + `record` theo nghiệp vụ (không dùng `Result<T, E>` chung), lý do là enum, xử lý bằng `switch`; chỉ dùng giữa manager và service | `RotationResult` (`Rotated`/`Rejected(reason)`), `AuthenticationResult` |
| Kiểm tra chỉ có đúng/sai, không cần lý do | `boolean` | `OtpStore.verifyOtp` |
| Lỗi nghiệp vụ trả cho client | `AppException(ErrorCode)`, chỉ ném từ service (controller khi kiểm tra input) | `AppException(ErrorCode.EMAIL_EXISTED)` |
| Lỗi kỹ thuật | Exception cấp thấp trong `infrastructure`; handler map hoặc service dịch; lỗi bug/cấu hình để về 500 | `ExternalServiceException`, `StateStoreException` |

Chỉ thêm `ErrorCode` khi client phải hành động khác đi; lý do chi tiết để ở kiểu kết quả và log. Không tạo exception riêng cho từng lỗi nghiệp vụ (chỉ subclass `AppException` khi cần dữ liệu cho `details`). Không dùng `Optional` làm field/tham số; dùng `orElseThrow`/`map` thay cho `isEmpty()` + `get()`.

**Quy ước tên:** `XxxController`, `XxxService` (interface công khai giữ tên nghiệp vụ, ví dụ `UserAccountService`; implementation có thể là `UserService`), `XxxManager`, `XxxRepository`, `RedisXxxStore`, `NominatimGeocodingProvider`. Request DTO: `CreatePlanRequest`; response: `PlanResponse`, `PlanSummaryResponse`.

## 4. DTO, validation, mapping

- Request/response là **Java record**. Không dùng entity làm request/response.
- Validation cú pháp ở request DTO bằng Bean Validation; `message` của constraint là **mã lỗi** UPPER_SNAKE (ví dụ `EMAIL_REQUIRED`), không phải câu văn (T03 đã chuyển mọi constraint hiện có sang mã).
- Validation nghiệp vụ (tham chiếu tồn tại, trạng thái, quyền) ở service → `AppException(ErrorCode.X)`.
- Thời gian trong API: `Instant` ISO-8601 UTC cho sự kiện; `LocalDate` cho `planDate`; `LocalTime` (`HH:mm`) cho time window/ca, hiểu theo timezone của store (P-14).
- Cấu hình mới dùng `@ConfigurationProperties` (record) theo module, không rải `@Value`.

## 5. Xử lý lỗi (P-03)

- Mọi lỗi nghiệp vụ: `throw new AppException(ErrorCode.X)`. `ErrorCode` mang HTTP status. Chỉ **thêm** giá trị mới vào cuối nhóm; không đổi tên giá trị đã có client dùng.
- Body lỗi thống nhất:
  ```json
  { "code": "VALIDATION_ERROR", "details": [{ "field": "email", "code": "EMAIL_REQUIRED" }] }
  ```
  Profile `dev` được thêm `exception`, `stackTrace`. Không profile nào khác lộ chi tiết nội bộ.
- Map chuẩn: 400 validation/request sai · 401 chưa xác thực/token sai/sai thông tin đăng nhập · 403 không đủ quyền · 404 không tồn tại **hoặc thuộc store khác** · 405 sai HTTP method · 409 xung đột trạng thái/trùng/optimistic lock · 422 vi phạm business rule hợp lệ cú pháp (ví dụ publish plan chưa tối ưu) · 429 rate limit/cooldown · 502/503 provider ngoài lỗi/hết thời gian · 500 lỗi không lường trước.
- `AuthenticationEntryPoint` và `AccessDeniedHandler` của Security ghi cùng format: `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler` (trong `auth.infrastructure.security`), cùng dùng `ErrorResponseFactory` với `GlobalExceptionHandler`.
- **Quy tắc (P-23):** những gì `SecurityConfig` hoặc `GlobalExceptionHandler` cần (filter, entry point, `ErrorResponseFactory`) **không là Spring bean** mà được khởi tạo trực tiếp. Lý do: `@WebMvcTest` chỉ nạp controller, advice, filter và cấu hình MVC; một `@Component` thường sẽ thiếu trong slice và làm hỏng mọi controller test.
- Provider ngoài: timeout kết nối/đọc cấu hình (mặc định 3 s / 10 s); lỗi → `ExternalServiceException` → 502 `ROUTING_PROVIDER_ERROR` hoặc `GEOCODING_PROVIDER_ERROR`.

## 6. Authentication & authorization

### 6.1 Token

| Token | Dạng | Lưu | TTL | Ghi chú |
|---|---|---|---|---|
| Access token | JWT HS256, claim `sub`(userId), `email`, `role`, `storeId` (không có với ADMIN), `sid`(sessionId = familyId), `canDrive` (P-09) | Client (memory/secure storage) | 14 phút | Header `Authorization: Bearer`. Không thu hồi được trước hạn. |
| Refresh token | Opaque 256-bit, lưu SHA-256 | MariaDB `refresh_tokens` | 14 ngày, trượt: rotate bình thường → token con hạn = now + 14 ngày (giữ hành vi hiện tại); nhánh grace giữ nguyên hạn của token active | Xoay vòng mỗi lần dùng; family = session (P-07). Web: cookie; mobile: body (P-04). |
| OTP | 6 chữ số, lưu SHA-256 + số lần thử | Redis `otp:{purpose}:{userId}` | `otp.ttl`, mặc định 5 phút | Verify atomic bằng Lua, huỷ sau `otp.max-attempts` (3) lần sai; cooldown `otp.cooldown` (60 s) `otp:cooldown:{purpose}:{userId}`, được xoá nếu email gửi lỗi. `OtpManager` (generate, lưu, gửi email, verify) trên `OtpStore` (chỉ Redis). |
| Reset token | Opaque (ticket + secret), client nhận một chuỗi `{ticket}.{secret}` | Redis `otpToken:{purpose}:{ticket}` → `{userId}:{hash}` | 10 phút (TTL do bên gọi `OtpTokenManager.generateToken` truyền vào) | Một lần (GETDEL), bind userId + purpose (P-06). `verifyOtpToken` trả `OtpTokenVerification` (`Verified(userId)`/`Rejected(reason)`); giá trị Redis hỏng ném `IllegalStateException` (500), không coi là token sai (P-24). |
| Invitation code | Opaque, lưu SHA-256 | MariaDB `store_invitations` | 7 ngày (D-27) | Một lần. |

OTP purpose (enum): `VERIFY_EMAIL`, `RESET_PASSWORD`, `CHANGE_PASSWORD`. Key OTP dùng **userId**, không dùng email (email có thể đổi).

### 6.2 Session (P-07)

Session = refresh token family (`family_id`). Đăng xuất = revoke family. "Đăng xuất mọi thiết bị khác" = revoke mọi family ACTIVE của user trừ family hiện tại. Đổi mật khẩu → revoke mọi family khác; reset mật khẩu / vô hiệu hoá user → revoke tất cả.

Race đã biết (F-29): request refresh đồng thời trong grace 3 s có thể dẫn tới đăng xuất ở lần refresh sau. Chấp nhận trong MVP; client nên serialize refresh (một refresh tại một thời điểm).

### 6.3 Authorization (P-08)

Hai lớp, **cả hai bắt buộc**:

1. **Role** — `@PreAuthorize` ở controller.
2. **Tenant + ownership** — ở service: mọi truy vấn dữ liệu store dùng `actor.storeId()`; DRIVER chỉ truy cập route có `driver_id = actor.id()`. Không tìm thấy trong store của actor → **404** (không tiết lộ tồn tại).

| Hành động | ADMIN | OWNER | DISPATCHER | DRIVER |
|---|---|---|---|---|
| Hồ sơ/mật khẩu/session của chính mình | ✓ | ✓ | ✓ | ✓ |
| Xem/sửa store, depot | — | ✓ | xem | — |
| Mời/huỷ lời mời, quản lý thành viên | — | ✓ | — | — |
| Xem danh sách tài xế (để phân công) | — | ✓ | ✓ | — |
| CRUD xe | — | ✓ | ✓ | — |
| CRUD/optimize/publish/cancel plan | — | ✓ | ✓ | — |
| Xem plan & vị trí tài xế | — | ✓ | ✓ | — |
| Xem route được giao, cập nhật trạng thái stop, gửi vị trí | — | ✓ nếu `canDrive` | ✓ nếu `canDrive` | ✓ (chỉ route của mình) |
| Bật/tắt `canDrive` của thành viên | — | ✓ | — | — |
| Đặt lại stop về PENDING | — | ✓ | ✓ | — |
| Danh sách user toàn hệ thống | ✓ | — | — | — |

ADMIN không có `storeId` và **không** truy cập API của store trong MVP.

"Người được gán route" = user ACTIVE của store có `role=DRIVER` hoặc `canDrive=true` (D-21). Quy tắc này chỉ viết ở một chỗ: `UserIdentity.assignableToRoutes()` (T17). API `/driver/**` kiểm tra bằng `@PreAuthorize("@authz.canDrive(principal)")`, dùng role và claim `canDrive`; bean `authz` (`AuthorizationRules`) khai báo trong `SecurityConfig` nên slice test `@Import(SecurityConfig.class)` có sẵn. Khi chọn người cho một plan, service dùng `StoreMemberQuery` (kiểm tra ACTIVE và cùng store) rồi áp cùng quy tắc. Quyền trên từng route kiểm tra `route.driver_id = actor.id()` ở service.

### 6.4 Web vs mobile (P-04)

- Header `X-Client-Platform: mobile` → refresh token đi trong body request/response, không set cookie.
- Không có header → refresh token chỉ trong cookie `refreshToken`: `HttpOnly; Secure; SameSite=<auth.cookie.same-site, mặc định Strict>; Path=/api/v1/auth; Max-Age=<còn lại>`. Web và API cùng site dưới `huy-vu.uk` (D-22).
- CORS: whitelist origin web cấu hình qua env, `allowCredentials=true`. Mobile không bị CORS.
- CSRF tắt. An toàn dựa vào `SameSite=Strict` cộng access token ở header. Nếu sau này web và API khác site thì phải thêm cơ chế chống CSRF cho `/refresh` và `/log-out` trước khi đổi `SameSite=None`.

## 7. Persistence, transaction, concurrency

- Schema chỉ thay đổi qua Flyway `be/src/main/resources/db/migration/V{n}__{mô_tả}.sql` (P-01). Không sửa migration đã merge. `ddl-auto: validate` ở mọi profile.
- Số version migration được **cấp trước** trong `IMPLEMENTATION_PLAN.md` để agent song song không đụng nhau.
- Mọi bảng thuộc store có `store_id BIGINT NOT NULL` + FK + index.
- `@Transactional` đặt ở method public của service; `readOnly = true` cho truy vấn đọc.
- **Không gọi hệ thống ngoài (SMTP, OSRM, Nominatim) bên trong transaction DB.** Mẫu chuẩn:
  - Đăng ký/mời: transaction tạo dữ liệu → commit → gửi email. Email lỗi → user dùng endpoint gửi lại (không rollback dữ liệu).
  - Optimize: tx đọc input → gọi matrix + solver ngoài tx → tx ghi kết quả, kiểm tra `version` của plan (optimistic lock); plan đã đổi → 409 `PLAN_MODIFIED`.
- Concurrency:
  - `route_plans.version` (`@Version`) cho mọi sửa plan.
  - Cập nhật trạng thái stop: `UPDATE ... WHERE id=? AND status=?` hoặc `@Version` trên stop; thao tác lặp lại cùng trạng thái là idempotent.
  - Refresh token: giữ pessimistic lock khi rotate.
  - Đăng ký/mời trùng email: dựa vào unique constraint; `DataIntegrityViolationException` → 409 `EMAIL_EXISTED`.
  - OTP: atomic trong Lua; reset token: GETDEL.

## 8. Routing, geocoding, optimization

### 8.1 Luồng plan

```
              ┌─ có stop chỉ có địa chỉ ─▶ processing=GEOCODING (job nền) ─▶ IDLE
lưu draft ────┤
              └─ mọi stop đã có toạ độ ─▶ IDLE
DRAFT ──optimize(job)──▶ DRAFT có kết quả ──publish──▶ PUBLISHED ──stop đầu tiên──▶ IN_PROGRESS ──mọi stop xong──▶ COMPLETED
  │  ▲ sửa input thì xoá kết quả
  └──┴────────── cancel (được từ DRAFT và PUBLISHED) ──▶ CANCELLED
```

- `processing_status` khác `IDLE` thì mọi thao tác ghi lên plan trả 409 `PLAN_BUSY`.
- Optimize chỉ chạy khi mọi stop có toạ độ (`PROVIDED` hoặc `RESOLVED`). Nếu chưa thì trả 422 `STOPS_NOT_GEOCODED`.
- Publish kiểm tra **trùng ca** (D-23): với mỗi người được gán và mỗi xe, khoảng `[shiftStart, shiftEnd)` của plan này không được giao với khoảng ca của plan PUBLISHED/IN_PROGRESS khác **cùng `plan_date`** trong store. Vi phạm thì trả 422 `SHIFT_OVERLAP`. Kiểm tra trong transaction publish, khoá các route liên quan bằng `SELECT … FOR UPDATE` theo driver_id/vehicle_id để hai lần publish đồng thời không cùng lọt qua.

### 8.2 Geocoding khi lưu plan (D-18)

1. Lưu draft trong một transaction. Stop có lat/lon thì `geocode_status = PROVIDED`. Stop chỉ có địa chỉ thì `PENDING`. Nếu có ít nhất một stop `PENDING` thì tạo job `GEOCODE_PLAN` và đặt `processing_status = GEOCODING`. Commit.
2. Sau commit, job chạy trên executor nền. Với mỗi stop `PENDING`:
   - Gọi `GeocodingProvider.search(address, 1)`. Lớp cache nằm trước provider, và **chỉ cache miss mới tốn slot rate limit**.
   - Có kết quả thì `RESOLVED`, ghi lat/lon và `geocoded_display_name`. Không có kết quả thì `FAILED/NOT_FOUND`. Provider lỗi thì `FAILED/PROVIDER_ERROR`, job vẫn tiếp tục với các stop còn lại.
   - Ghi kết quả theo lô nhỏ, mỗi lô một transaction ngắn, để client polling thấy tiến độ.
3. Xong thì job `SUCCEEDED`, kể cả khi có stop FAILED, vì đó là lỗi dữ liệu chứ không phải lỗi job. Sau đó `processing_status = IDLE`.
4. Dispatcher sửa stop FAILED bằng `PATCH` stop (địa chỉ mới hoặc toạ độ) hoặc gọi `POST /plans/{id}/geocode`.
5. `PUT` draft giữ kết quả geocode của stop có `id` cũ khi địa chỉ và toạ độ không đổi.

### 8.3 Optimize (D-19, P-13, P-14)

1. Kiểm tra: plan DRAFT, `IDLE`, mọi stop có toạ độ, có ≥ 1 stop và ≥ 1 assignment, trong giới hạn kích thước, dữ liệu thoả BR-07..BR-09.
2. Tạo job `OPTIMIZE_PLAN` và đặt `processing_status = OPTIMIZING`, commit. Gửi job cho `JobExecutor`. Trong MVP, executor của loại job này là **inline**, nghĩa là chạy ngay trong thread của request (§8.4).
3. Job: transaction đọc input. Sau đó, **ngoài transaction**: dựng danh sách node gồm các điểm start/end riêng của từng route (MVP đều là depot) cộng stop, gọi `MatrixProvider.matrix(nodes)` lấy `distanceMeters` và `durationSeconds`, rồi gọi `SolverProvider.solve(VrpProblem)`. Cuối cùng là transaction ghi kết quả có kiểm tra `version`.
4. `VrpProblem` (model của `routing`, không dùng entity):
   - Xe k có `startNode`, `endNode` **riêng** (D-20, điểm mở rộng cho multi-depot), `capacity`, `shiftStart/End` (tính bằng giây từ 00:00 của `planDate`), `maxDistance?`.
   - Dimension Capacity theo demand.
   - Dimension Time với transit = `duration[i][j] + serviceTime[i]`, có slack để cho phép chờ, cumul nằm trong time window; start và end của xe nằm trong ca.
   - Dimension Distance giới hạn bằng `maxDistance`.
   - Arc cost tính theo duration.
   - Mỗi stop có disjunction kèm penalty lớn, nên stop không xếp được sẽ rơi vào `dropped` (P-13).
   - First solution `PATH_CHEAPEST_ARC`, metaheuristic `GUIDED_LOCAL_SEARCH`, time limit cấu hình (mặc định 10 giây). GLS chạy tới hết time limit, nên mỗi lần optimize có stop luôn mất đúng khoảng đó.
   - Mỗi node của ma trận là node của đúng một stop hoặc là start/end của xe (nhiều xe dùng chung depot được); vi phạm thì `IllegalArgumentException`. Horizon của dimension Time = giờ lớn nhất trong các ca và các time window, để stop có window nằm ngoài mọi ca bị drop thay vì làm OR-Tools lỗi khi dựng model. Giờ xuất phát được đẩy muộn nhất và giờ về sớm nhất có thể (finalizer). Penalty drop lớn hơn chi phí của mọi xe chạy hết horizon.
   - `VrpSolution`: một route cho mỗi xe theo thứ tự đầu vào (xe không dùng thì không có visit), ETA = giờ bắt đầu phục vụ, `droppedStops`; `NO_SOLUTION` khi OR-Tools không tìm ra lời giải nào (ví dụ xe không kịp đi từ start tới end trong ca).
5. Ghi kết quả: `sequence`, `plannedArrival`, chỉ số từng route, `optimized_at`. Stop bị drop có `route_id = NULL`. Job `SUCCEEDED`, plan về `IDLE`. Nếu provider lỗi thì job `FAILED` (`ROUTING_PROVIDER_ERROR`), plan về `IDLE`, kết quả cũ giữ nguyên.

### 8.4 Job framework (`core.job`, D-19)

- Entity và repository `BackgroundJob` (bảng `background_jobs`). Interface `JobHandler` được mỗi loại job implement (`GeocodePlanJobHandler`, `OptimizePlanJobHandler` nằm trong `planning`).
- `JobExecutor.submit(jobId)` có hai chiến lược, chọn theo từng loại job bằng cấu hình `jobs.<type>.mode = inline | async`:
  - `inline`: chạy ngay trong thread hiện tại. MVP dùng cho `OPTIMIZE_PLAN`.
  - `async`: `ThreadPoolTaskExecutor` có giới hạn (core 2, queue 50). MVP dùng cho `GEOCODE_PLAN`.
- Job luôn được submit **sau khi commit** transaction tạo job.
- Handler chịu trách nhiệm cập nhật `processing_status` của plan khi kết thúc, kể cả khi lỗi (dùng `finally`).
- Khi khởi động: job `RUNNING` chuyển thành `FAILED/INTERRUPTED`, và plan liên quan về `IDLE`.
- Chuyển optimize sang chạy nền: đổi `jobs.optimize-plan.mode=async`. **Không đổi API.** Client vốn đã polling.
- Giới hạn đã biết: chỉ một instance. Muốn chạy nhiều instance thì phải claim job bằng `UPDATE … WHERE status='QUEUED'` (Could).

### 8.5 Provider

| Interface | MVP | Sau MVP |
|---|---|---|
| `GeocodingProvider.search(query, limit)` | Nominatim (`countrycodes=vn`) với `CachingGeocodingProvider` (Redis 24 giờ) bọc ngoài và rate limiter dùng chung (P-11). Cấu hình `routing.geocoding.nominatim.base-url`, `.user-agent`, `.connect-timeout`, `.read-timeout`, `routing.geocoding.nominatim.rate-limit.enabled`, `.per-second`, `.max-wait` (D-25), `routing.geocoding.cache-ttl`. Redis lỗi thì fail-closed (500), không coi là cache miss, vì rate limiter cũng cần Redis. Rate limiter (`RedisRequestThrottle`) đặt slot bằng Lua `ReserveThrottleSlot.lua`: một round trip atomic trả về thời gian phải chờ tới slot của mình, theo thứ tự đến và theo đồng hồ Redis, nên không polling và không tranh slot; chờ quá `max-wait` thì không đặt slot (trade-off kỹ thuật như Lua verify OTP: logic gắn với Redis, chỉ test bằng Testcontainers). | Google (I-03) |
| `MatrixProvider.matrix(points)` → `TravelMatrix(distanceMeters, durationSeconds)` (int) | OSRM table `annotations=distance,duration`. Cấu hình `routing.matrix.provider`, `routing.matrix.osrm.base-url`, `.user-agent`, `.profile`, `.max-table-size`, `.connect-timeout`, `.read-timeout`. Ô `null` (không có đường) hay response thiếu → 502 `ROUTING_PROVIDER_ERROR`. Server OSRM giới hạn số điểm (`--max-table-size`): quá `max-table-size` điểm thì app chia thành khối `max-table-size / 2` và lấy từng cặp khối bằng `sources`/`destinations` (F-41), nên giá trị cấu hình phải khớp server. | Google Distance Matrix (I-03) |
| `SolverProvider.solve(VrpProblem)` → `VrpSolution` | OR-Tools (`OrToolsVrpSolver`, T23). Cấu hình `routing.solver.provider`, `routing.solver.time-limit`. | — |
| `DirectionsProvider.route(coords)` (Should) | OSRM route `steps=true&overview=full&geometries=polyline6`, cache theo route và version | — |

Chọn implementation bằng property (`routing.geocoding.provider=nominatim`), **không** dùng chuỗi `@Qualifier` rải rác.

## 9. Vị trí tài xế (P-15, D-26)

- `POST /driver/location {points:[…]}` → `DriverLocationService` → danh sách `LocationSink`.
- MVP có một sink, `LastKnownLocationSink`: ghi điểm có `recordedAt` mới nhất vào Redis `driver:location:{storeId}:{driverId}` (TTL 10 phút), bỏ qua điểm cũ hơn điểm đang lưu.
- Mở rộng sau này (I-05): thêm `LocationHistorySink` ghi lịch sử vào DB hoặc time-series. Không đổi API hay client, vì client đã gửi đủ chuỗi điểm.
- Dispatcher đọc vị trí của người được gán route trong plan PUBLISHED hoặc IN_PROGRESS.
- Realtime push để sau (I-04).

## 10. Cấu hình & bảo mật vận hành

- Secret chỉ qua biến môi trường (`SPRING_DATASOURCE_PASSWORD`, `SPRING_MAIL_PASSWORD`, `TOKEN_JWT_ACCESS_TOKEN_SECRET`...). `application-dev.yaml` gitignored; cung cấp `application-dev.example.yaml` không có secret (T05).
- JWT secret ≥ 256 bit, Base64.
- `ClientDetailFilter` chỉ tin `X-Forwarded-For` khi bật `server.forward-headers-strategy` sau reverse proxy tin cậy (T41).
- Log: SLF4J, không `System.out`; MDC `requestId`, `userId`, `storeId`. **Cấm log** mật khẩu, OTP, token, ticket, refresh token, secret.

## 11. Chiến lược kiểm thử

| Tầng | Công cụ | Phạm vi bắt buộc |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Service (business rule, quyền), manager token/OTP, solver (ma trận nhỏ cố định, kiểm tra capacity/time window/drop), state machine plan/stop, mapper có logic. |
| Web slice | `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@MockitoBean AccessTokenManager` (stub token → `AccessTokenVerification.Authenticated(new AuthenticatedUser(...))`, gửi header `Authorization: Bearer …` để đi qua filter thật). Mẫu: `OptimizationControllerTest`, `ErrorHandlingWebMvcTest`. Controller có DTO dùng custom validator thì `@Import` thêm bean mà validator cần (ví dụ `PhoneNumberProcessor`), nếu không validator lỗi và test có thể pass sai lý do (500). | Mỗi controller: validation → 400 + code, chưa đăng nhập → 401, sai role → 403, happy path, cookie/header web vs mobile. |
| Integration | `@SpringBootTest` + **Testcontainers** MariaDB + Redis (P-21); `EmailSender` thay bằng fake lưu email để đọc OTP; `MatrixProvider` fake | Luồng auth đầy đủ, Lua OTP, Flyway migrate, tenant isolation, plan lifecycle end-to-end, optimistic lock. |
| Hợp đồng provider | `MockRestServiceServer` | Parse response/lỗi Nominatim, OSRM (đã có, giữ). |

Quy tắc:
- Không test nào gọi dịch vụ ngoài thật (Nominatim, OSRM, SMTP).
- Profile `test` với secret giả trong `src/test/resources/application-test.yaml`.
- Tên test: `method_whenCondition_expectedResult` (theo quy ước hiện có).
- Bug fix phải kèm test tái hiện bug.
- Mỗi endpoint store-scoped mới phải có ít nhất 1 test tenant isolation (user store khác → 404).
- `./mvnw test` phải chạy được chỉ với Docker (không cần dịch vụ cài sẵn).

## 12. Container, CI/CD và triển khai (D-30 → D-34)

### 12.1 Image (`be/Dockerfile`, multi-stage)

| Stage | Base | Nội dung |
|---|---|---|
| `build` | `maven:3.9-eclipse-temurin-21` | Copy `pom.xml` trước rồi chạy `dependency:go-offline` để cache layer dependency, sau đó copy source và `package -DskipTests`. Test chạy ở CI, không chạy trong build image. |
| `extract` | cùng image build | Tách Spring Boot jar thành các layer (`java -Djarmode=tools -jar app.jar extract --layers --launcher`) để rebuild nhanh. |
| `runtime` | `eclipse-temurin:21-jre` (Ubuntu, có glibc vì OR-Tools cần; **không dùng Alpine**) | Chạy bằng user không phải root, `JAVA_OPTS` cấu hình được, `HEALTHCHECK` gọi `/actuator/health`, `EXPOSE 8080`. |

- Thêm `spring-boot-starter-actuator`, chỉ expose `health` (gồm liveness và readiness). Mọi endpoint actuator khác tắt.
- Image không chứa secret hay `application-dev.yaml`. Toàn bộ cấu hình đi qua biến môi trường.
- Có file `.dockerignore` (bỏ `target/`, `.idea/`, các file `application-dev*.yaml` local). `application-staging.yaml` và `application-prod.yaml` không chứa secret và được đóng vào image.

### 12.2 Compose (`compose.yaml` ở root)

| Service | Profile | Ghi chú |
|---|---|---|
| `mariadb` (`mariadb:11.8`, D-35) | mặc định | Có volume và healthcheck. Cổng host `${MARIADB_PORT:-3307}`. |
| `redis` (7) | mặc định | Bật persistence AOF; mọi key đều có TTL nên mất dữ liệu cũng chấp nhận được. |
| `mailpit` | mặc định | SMTP giả ở cổng 1025, giao diện web ở 8025 để xem email OTP và lời mời khi dev. |
| `app` | `app` | Build từ `be/Dockerfile` (target `runtime`), phụ thuộc mariadb và redis ở trạng thái healthy, đọc `.env`. Đây là bản chạy đủ từ A đến Z. |
| `osrm` | `routing` | `osrm-backend` dùng dữ liệu `vietnam-latest.osm.pbf` (Geofabrik). Lần đầu chạy, service init `osrm-prepare` thực hiện extract, partition, customize vào volume. |
| `nominatim` | `routing` | `mediagis/nominatim` import dữ liệu Việt Nam. Lần đầu mất khoảng 30–90 phút và vài GB disk. |

Cách dùng:
- `docker compose up -d`: chỉ hạ tầng, app chạy từ IDE với profile dev.
- `docker compose --profile app up -d --build`: chạy toàn bộ stack.
- Thêm `--profile routing`: chạy kèm OSRM và Nominatim local. App trỏ tới chúng qua `ROUTING_GEOCODING_NOMINATIM_BASE_URL`, `ROUTING_MATRIX_OSRM_BASE_URL`, và đặt `...RATE_LIMIT_ENABLED=false`.
- `.env.example` liệt kê mọi biến môi trường (không chứa secret thật). File `.env` được gitignore.
- `compose.prod.yaml`: dùng cho VPS, gồm `app` (image từ `ghcr.io`, không build tại chỗ), `mariadb`, `redis`, không có mailpit, dùng SMTP thật qua env. App kết nối DB bằng user riêng (không phải root); chỉ cổng app được publish, ở `127.0.0.1`. Reverse proxy TLS (Caddy/Nginx) nằm ngoài phạm vi repo; cookie `Secure` yêu cầu HTTPS.

### 12.3 GitHub Actions

| Workflow | Trigger | Job |
|---|---|---|
| `ci.yml` | `pull_request` vào `develop`/`main`; `push` lên `develop`/`main` | `./mvnw -B verify` (compile, test bằng Testcontainers, JaCoCo report). Upload test report và coverage làm artifact, ghi tóm tắt coverage vào job summary. **Đây là check bắt buộc.** |
| `quality.yml` | `pull_request` | `actions/dependency-review-action` (chặn dependency mới có lỗ hổng high trở lên); `gitleaks` quét secret. |
| `image.yml` | `push` lên `develop`; tag `v*` | Build image multi-stage (Buildx, cache GHA), quét bằng **Trivy** (fail khi có lỗ hổng CRITICAL đã có bản sửa), push lên `ghcr.io/<owner>/pathwise`. Tag cho `develop`: `develop`, `develop-<shortsha>`. Tag cho `v*`: `x.y.z`, `x.y`, `latest`. Dùng `GITHUB_TOKEN` với quyền `packages: write`, không cần secret khác. |
| `dependabot.yml` | theo lịch hằng tuần | Ecosystem `maven`, `github-actions`, `docker`. Gộp PR: Maven minor/patch thành một PR, GitHub Actions thành một PR. Không đề xuất đổi phiên bản Java của Docker image (bỏ qua major `eclipse-temurin` và image `maven`). |

- **Thay đổi chỉ đụng docs** (`docs/**`, `*.md`, `LICENSE`): job `ci` và `dependency-review` bị skip (composite action `.github/actions/detect-code-changes`; job skip vẫn đạt check bắt buộc `ci`, còn lọc ở trigger thì check không bao giờ báo và PR bị kẹt); `image` lọc bằng `paths-ignore` ở trigger (không phải check bắt buộc, tag `v*` luôn build). `gitleaks` luôn chạy. Không xác định được thay đổi thì vẫn chạy đủ.
- Không có workflow deploy (D-31). Chủ dự án deploy thủ công theo `docs/DEPLOYMENT.md`: `docker compose -f compose.prod.yaml pull && up -d`.
- Coverage: MVP chỉ báo cáo, **chưa đặt ngưỡng chặn**. Ngưỡng sẽ thêm sau T43 khi coverage đã ổn định. (Đây là điểm mở rộng: thêm rule `jacoco:check` mà không đổi workflow.)
- Bảo vệ branch (chủ dự án tự cấu hình): `develop` và `main` yêu cầu PR, check `ci` phải xanh, không cho force push.

### 12.4 Môi trường

| Môi trường | Nguồn | Profile Spring |
|---|---|---|
| Local dev | IDE + compose hạ tầng | `dev` |
| Local full | `compose --profile app` | `dev` (mail dùng mailpit) |
| Staging | image `develop` trên VPS | `staging` |
| Prod | image `x.y.z` trên VPS | `prod` |

OpenAPI/Swagger chỉ bật ở `dev` và `staging`.
