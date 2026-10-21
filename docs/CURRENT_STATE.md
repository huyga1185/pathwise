# CURRENT_STATE — Hiện trạng đã xác minh & gap analysis

> Khảo sát ngày 2026-10-21 trên branch `develop`, HEAD `de34fcc`, **kèm thay đổi chưa commit** (refactor OTP/token, ~33 file).
> Mọi phát hiện ghi rõ mức độ bằng chứng:
> - **[Verified-run]** — đã kiểm chứng bằng compile/chạy test.
> - **[Verified-code]** — xác nhận bằng đọc code (hành vi suy ra chắc chắn từ code/spec).
> - **[Risk]** — rủi ro tiềm tàng, cần test để xác nhận.
>
> Khi một finding được sửa, cập nhật cột Trạng thái kèm task ID; không xoá dòng.

## 1. Tổng quan

| Hạng mục | Hiện trạng |
|---|---|
| Stack | Spring Boot 4.1.0, Java 21, Maven wrapper, Lombok, MapStruct 1.6.3, Spring Security, JPA/Hibernate (MariaDB), Spring Data Redis, Spring Mail (Brevo SMTP), Thymeleaf (template email), Nimbus JOSE JWT, OR-Tools 9.11, libphonenumber, springdoc 3.0.3. |
| Cấu trúc repo | `be/` (backend duy nhất), `.github/workflows/test.yaml`. Không có frontend. |
| Tài liệu | Trước session này chỉ có `be/HELP.md` (mẫu Spring Initializr). Các file `PROJECT.md`, `PLAN.md`, `TASKS.md`, `AGENTS.md`, `CLAUDE.md` mà brief nhắc tới **không tồn tại**. |
| Schema | `be/db/db.sql` viết tay: `users`, `refresh_tokens`. Không có migration tool. Prod `ddl-auto: validate`, dev `ddl-auto: update`. |
| Cấu hình | `application.yaml` (chung) + `application-dev.yaml` (gitignored, chứa secret thật: SMTP, JWT secrets, DB). |
| Build | Working tree **không compile** [Verified-run]. HEAD compile được [Verified-run]. |
| Test (HEAD) | 34 test chạy: 31 pass, 3 error (`OptimizationControllerTest`) [Verified-run]. Không chạy: `ToApplicationTests` (cần DB+Redis+secret), `NominatimGeocodingProviderTest`, `OSRMMatrixProviderTest`, `RouteOptimizationPipelineTest` (dùng mock, không cần mạng — chưa chạy trong session). |

### Module

| Module | Nội dung | Ghi chú |
|---|---|---|
| `core` | `ApiResponse`, `ErrorCode`/`AppException`/`GlobalExceptionHandler`, `PageResponse`/`PaginationValidator`, `StateStore`/`RedisStateStore` (mới, chưa commit). | Nền tảng hợp lý. |
| `shared.identity` | `UserIdentity(id, email, role)`, `UserRole {ADMIN, USER}`. | Hợp đồng giữa `auth` và `user`. |
| `user` | Entity `User`, `UserService` (implements `UserAccountService` cho auth + `UserProfileService` cho controller), `UserController` (`/users/me`), `AdminUserController` (`/admin/users`), MapStruct mapper, validator số điện thoại VN. | Ổn, cần mở rộng cho store/role/status. |
| `auth` | `AuthController` (`/auth/*`), `AuthService` (orchestrator), JWT access token (HMAC), refresh token opaque xoay vòng theo family (MariaDB, pessimistic lock), OTP (Redis + Lua), OTP token opaque (Redis, GETDEL), email OTP (Thymeleaf + JavaMail), filter JWT/ClientDetail. | Phần lớn công sức nằm ở đây; refactor đang dở. |
| `routeoptimizer` | `POST /route/optimize` (TSP), `POST /route/optimize/k` (VRP theo số xe). Pipeline: Nominatim geocode tuần tự (sleep 1 s/địa chỉ) → OSRM table (distance) → OR-Tools. Không lưu DB. | Prototype stateless; không khớp mô hình sản phẩm (D-04, D-05). |

### Luồng auth hiện có (sau refactor chưa commit)

1. `POST /auth/register {email, password}` → tạo user role USER (không xác thực email).
2. `POST /auth/log-in` → JWT access token (body) + refresh token (cookie `refreshToken`, `Path=/auth/refresh`).
3. `POST /auth/refresh` (cookie) → xoay vòng, grace 3 s cho request đồng thời, reuse → revoke family.
4. `POST /auth/log-out` (cookie + yêu cầu access token) → revoke family.
5. `GET /auth/sessions` → list refresh token ACTIVE của user.
6. Đổi mật khẩu 3 bước: `GET /auth/update-password` (gửi OTP) → `POST /auth/update-password/verify {otp}` (set cookie token+ticket) → `POST /auth/update-password {password}` (cookie).

## 2. Phát hiện

Mức độ: **Critical** (bảo mật/chặn build) · **High** (sai chức năng cốt lõi) · **Medium** · **Low**.
Nhóm: (1) nghiệp vụ/hiểu sai yêu cầu · (2) implementation · (3) kiến trúc/nhất quán · (4) dữ liệu/API/tích hợp · (5) test/kiểm chứng · (6) thiếu yêu cầu · (7) tuỳ chọn.

| ID | Mức | Nhóm | Phát hiện | Bằng chứng | Trạng thái |
|---|---|---|---|---|---|
| F-00 | Critical | 2 | Working tree không compile: `ErrorCode.SERVER_ERROR` bị xoá nhưng vẫn dùng. | `NominatimGeocodingProvider.java:50` [Verified-run] | Đã sửa (T00): thêm `INTERNAL_ERROR(500)` |
| F-01 | Critical | 2 | **OTP token không bind user** (regression của refactor). `OtpTokenManager.generateToken(purpose)` lưu key `otpToken:{ticket}:{purpose}`; `updateUserPassword` lấy userId từ JWT. Kẻ có access token bị lộ của nạn nhân có thể tự verify OTP trên tài khoản của mình rồi đổi mật khẩu nạn nhân. Bản HEAD có bind identity. | `OtpTokenManager.java`, `AuthService.updateUserPassword` [Verified-code] | Đã vá (T00): token lưu `{userId}:{hash}`, `AuthService` so userId; T15 thay luồng |
| F-02 | High | 2 | Refresh token **hết hạn vẫn xoay vòng được**: `rotateToken` không kiểm tra `expiresAt`; trạng thái `EXPIRED` không bao giờ được gán. Nhánh grace cũng rotate token active mà không kiểm tra hạn. | `RefreshTokenManager.rotateToken/handleUsed` [Verified-code] | Mở → T10 |
| F-03 | High | 4 | **Logout không hoạt động từ browser**: cookie `refreshToken` có `Path=/auth/refresh` nên không được gửi tới `/auth/log-out` → `MissingRequestCookieException` → 400. Ngoài ra `/auth/log-out` không nằm trong `permitAll` → cần access token còn hạn mới logout được. | `AuthController`, `SecurityConfig` [Verified-code, theo RFC 6265 path matching] | Mở → T10 |
| F-04 | High | 2 | `OtpTokenManager` trả `expiresAfter = Instant.ofEpochSecond(300)` (= 1970-01-01T00:05Z). Cookie nhận `Max-Age` âm → Spring bỏ `Max-Age` → thành session cookie (chạy được do may mắn). Test `OtpTokenManagerTest` **assert đúng giá trị sai**. | `OtpTokenManager.java`, `OtpTokenManagerTest.java:56` [Verified-code] | Đã sửa (T00): `expiresAfter = now + TTL`, test cập nhật |
| F-05 | High | 1 | Đổi mật khẩu **không thu hồi session** nào; không yêu cầu mật khẩu cũ. | `AuthService.updateUserPassword` [Verified-code] | Mở → T15 |
| F-06 | High | 5 | Mọi `@WebMvcTest` fail: `JwtAuthFilter`/`ClientDetailFilter` được annotate `@Configuration` nên bị nạp vào slice nhưng dependency (`AccessTokenService`/`Manager`) thì không. | Surefire: `NoSuchBeanDefinitionException ... jwtAuthFilter` [Verified-run] | Mở → T01 |
| F-07 | High | 5 | CI không thể xanh: `contextLoads` cần Redis, SMTP, `token.jwt.*` secrets — workflow chỉ cung cấp MariaDB. | `.github/workflows/test.yaml` [Verified-code; chưa xem kết quả trên GitHub] | Mở → T01 |
| F-08 | Medium | 1 | Danh sách session trả cả token ACTIVE đã hết hạn; "session id" là id của refresh-token row, đổi sau mỗi lần refresh; không có API thu hồi từng session. | `RefreshTokenManager.getListInfoByUserId` [Verified-code] | Mở → T11 |
| F-09 | Medium | 2 | TTL OTP hardcode 300 s trong `RedisOtpStore`, trong khi email hiển thị `otp.expiration-time` → có thể lệch. Cooldown 60 s và 3 lần thử cũng hardcode. | `RedisOtpStore.java`, `VerifyOtp.lua` [Verified-code] | Mở → T12 |
| F-10 | Medium | 2 | Nếu gửi email lỗi, OTP và cooldown vẫn còn trong Redis → user phải chờ 60 s dù không nhận được mã. | `AuthService.getUpdateUserPasswordToken` [Verified-code] | Mở → T12 |
| F-11 | Medium | 3 | Hai interface chồng chéo: `OtpStore` (javadoc mô tả là "facade/manager", thực tế làm cả generate + store + cooldown) và `OtpManager` (stub trả `null`, chưa dùng). | `otp/store`, `otp/manager` [Verified-code] | Mở → T00/T12 |
| F-12 | Medium | 3 | Lỗi do Spring Security (chưa đăng nhập, token sai) không đi qua `GlobalExceptionHandler`; không cấu hình `AuthenticationEntryPoint` → body/HTTP status không theo format chung (nhiều khả năng 403 thay vì 401). | `SecurityConfig` [Risk — cần test] | Mở → T03 |
| F-13 | Medium | 4 | `GET /auth/update-password` có side effect (gửi email) — vi phạm ngữ nghĩa HTTP, dễ bị prefetch/CSRF kích hoạt. | `AuthController` [Verified-code] | Mở → T15 (thay endpoint) |
| F-14 | Medium | 4 | Cookie không đặt `SameSite`; CSRF đã tắt; endpoint `/auth/refresh` dùng cookie → có thể bị kích hoạt cross-site. | `AuthCookieBuilder` [Verified-code] | Mở → T10 |
| F-15 | Medium | 2 | Đăng ký trùng email đồng thời → `DataIntegrityViolationException` → 500 (check-then-insert không atomic). Không chuẩn hoá hoa/thường email ở tầng ứng dụng. | `UserService.createUser` [Verified-code] | Mở → T03, T13 |
| F-16 | Medium | 4 | Route optimizer: request không có validation (`@Valid` thiếu, list rỗng/null → NPE/500); geocode tuần tự + `Thread.sleep(1000)` mỗi địa chỉ trên request thread; `System.out.println` trong interceptor; không timeout HTTP client; OSRM trả `null` → NPE. | `routeoptimizer/**` [Verified-code] | Mở → T21, T22, T27 |
| F-17 | Medium | 1 | Route optimizer chỉ tối ưu theo **khoảng cách**, không có time window/capacity/ca; không lưu kết quả; `/route/optimize` yêu cầu đăng nhập nhưng không gắn với store. Không khớp D-04/D-05. | `ORToolsSolver`, `OptimizationController` [Verified-code] | Mở → T22–T27 (Replace) |
| F-18 | Medium | 3 | Qualifier không nhất quán: `@Qualifier("ORToolSolver")` trên class nhưng inject `@Qualifier("ORToolsSolver")` — chỉ chạy được vì Spring fallback theo bean name. | `ORToolsSolver`, `RouteOptimizationPipelineConfig` [Verified-code] | Mở → T27 |
| F-19 | Medium | 3 | Bean `hmacOtpTokenGenerator` + config `token.jwt.otp-token-secret` và DTO `GeneratedOtpToken` không còn được dùng sau refactor nhưng vẫn bắt buộc khi khởi động. | `JwtGeneratorConfig` [Verified-code] | Đã sửa (T00) |
| F-20 | Low | 3 | Package `auth.infastructure` sai chính tả (các module khác dùng `infrastructure`). | [Verified-code] | Mở → T04 |
| F-21 | Low | 4 | `ClientDetailFilter` tin `X-Forwarded-For` vô điều kiện → IP trong danh sách session có thể bị giả mạo. | [Verified-code] | Mở → T41 |
| F-22 | Low | 2 | `SecureRandomOtpGenerator` gọi `Security.setProperty(...)` toàn cục trong constructor. | [Verified-code] | Mở → T12 |
| F-23 | Low | 1 | Template email OTP ghi "login request" và "valid for N **seconds**" cho luồng đổi mật khẩu; tiếng Anh. | `templates/email/otp.html` [Verified-code] | Mở → T12 |
| F-24 | Low | 4 | `ErrorCode` dùng 400 cho "not found" (`COULD_NOT_FIND_USER`), `INVALID_EMAIL_OR_PASSWORD` 400 thay vì 401. | `ErrorCode.java` [Verified-code] | Mở → T03 |
| F-25 | Low | 2 | `UserService.deleteUser` sẽ lỗi FK nếu user có refresh token (FK không cascade). Hiện không có endpoint gọi. | `db.sql` [Verified-code] | Mở → ghi chú, không có task (không có use case) |
| F-26 | Low | 4 | Claim `role` trong JWT được set bằng enum object (`Map.of("role", identity.role())`); đọc lại bằng `getClaimAsString` — chưa có test chứng minh Nimbus serialize enum thành string. | `AccessTokenManager` [Risk] | Đã kiểm chứng (T00): `AccessTokenManagerTest` round-trip pass, không phải lỗi |
| F-27 | Low | 4 | Lịch sử git (commit đầu `53239c0`) từng chứa username/password DB trong `application.yaml`. `application-dev.yaml` hiện tại (gitignored) chứa secret SMTP/JWT thật. | `git show 173bd14` [Verified-run] | Khuyến nghị: đổi mật khẩu DB nếu từng dùng ở nơi khác; không có task code |
| F-28 | Low | 5 | Không có test cho `AuthService`, `AuthController`, `UserService`, Lua script OTP, `JwtAuthFilter`, `HMACJwtTokenGenerator`. | [Verified-code] | Mở → T01, các task auth |
| F-30 | Low | 3 | Entity `RefreshToken` dùng `@JoinColumn` cho cột thường (`familyId`, `userAgent`, `ipAddress`) thay vì `@Column`; chạy được nhờ naming strategy mặc định (camelCase → snake_case). | `RefreshToken.java` [Verified-code] | Mở → T10 |
| F-29 | Info | 4 | Race đã biết: hai request refresh đồng thời trong grace 3 s nhận hai token khác nhau; nếu browser giữ token "cũ hơn" thì lần refresh sau (>3 s) bị coi là reuse → revoke family (đăng xuất). | `RefreshTokenManager.handleUsed` [Risk] | Chấp nhận cho MVP; ghi trong ARCHITECTURE |

## 3. Điểm làm tốt (giữ lại)

- Chia module theo nghiệp vụ (`auth`, `user`, `routeoptimizer`) với `controller/service/repository/dto/model/infrastructure` — dễ hiểu.
- `auth` phụ thuộc `user` qua interface hẹp `UserAccountService` + `UserIdentity`; `user` không phụ thuộc `auth`.
- Refresh token: lưu hash, xoay vòng theo family, phát hiện reuse, pessimistic lock khi rotate — thiết kế đúng hướng.
- OTP: lưu hash, verify atomic bằng Lua, giới hạn số lần thử, cooldown; fail-closed khi Redis lỗi.
- Abstraction provider (`GeocodingProvider`, `MatrixProvider`, `SolverProvider`) — đúng nhu cầu thực tế (D-10 đổi sang Google).
- Validation số điện thoại VN qua libphonenumber, lưu E.164.
- Phân trang có whitelist sort field.

## 4. Gap analysis (hiện trạng → thiết kế mục tiêu)

| Thành phần | Phân loại | Lý do / việc cần làm | Task |
|---|---|---|---|
| `core.apiresponse.ApiResponse` | Keep | Envelope `{message?, data}` dùng tiếp. | — |
| `core.exception.*` | Modify | Đổi field `message`→`code` (P-03), map HTTP status đúng, xử lý `DataIntegrityViolation`, lỗi Security, lỗi provider. | T03 |
| `core.pagination.*` | Keep | | — |
| `core.statestore.*` | Keep | Nền cho các store Redis. | — |
| `shared.identity.UserIdentity/UserRole` | Modify | Thêm `storeId`; role → `ADMIN, OWNER, DISPATCHER, DRIVER`. | T13, T17 |
| `user.model.User` + bảng `users` | Extend | `store_id`, `full_name`, `status`; role mới. | T13 |
| `user.service.UserService` | Extend | Tạo user theo store/role, status, chuẩn hoá email, kiểm tra mật khẩu hiện tại. | T13, T15, T16 |
| `user.controller.*` | Keep/Extend | `/users/me` thêm `fullName`; admin list giữ. | T13 |
| `auth.controller.AuthController` | Modify | Path `/api/v1/auth`, cookie/body theo platform, endpoint mới, bỏ luồng cookie đổi mật khẩu. | T10, T11, T13–T15 |
| `auth.service.AuthService` | Modify + Extend | Tách theo luồng nếu file quá lớn (gợi ý: `AuthService`, `PasswordService`, `RegistrationService`). | T13–T15 |
| `auth...refreshtoken.*` | Modify | Kiểm tra hạn, session = family, revoke theo user/family, revoke-all-except. | T10, T11 |
| `auth...jwt.*` | Modify | Thêm claim `storeId`; xoá generator OTP thừa. | T00, T17 |
| `auth.otp.*` | Refactor | Gộp `OtpStore` (Redis primitive) + `OtpManager` (orchestration: generate, cooldown, gửi email, verify); purpose là enum; TTL cấu hình. | T00, T12 |
| `auth...otptoken.*` | Modify | Bind subject (userId); TTL đúng; dùng cho reset password (P-06). | T00, T14 |
| `auth.email.*` | Extend | Template theo purpose (verify email, reset, change password, invitation); tiếng Việt. | T12, T16 |
| `auth.infastructure.filter.*` | Refactor | Bỏ `@Configuration`, đăng ký đúng cách; sửa tên package. | T01, T04 |
| `auth.infastructure.config.SecurityConfig` | Modify | Entry point/access denied JSON, permitAll mới, CORS. | T03, T10, T41 |
| `routeoptimizer.infrastructure.geocoding` | Modify | Bỏ geocode hàng loạt; dùng cho endpoint search có rate limit + cache. | T21 |
| `routeoptimizer.infrastructure.matrix` | Modify | Trả distance + duration; timeout; lỗi provider rõ ràng. | T22 |
| `routeoptimizer.infrastructure.solver` | Replace | Solver VRPTW mới (capacity, time windows, ca, max distance, drop penalty, time limit). Giữ interface `SolverProvider` nhưng đổi signature sang model đầu vào/ra riêng. | T23 |
| `routeoptimizer.infrastructure.routeoptimizationpipeline` | Replace | Logic chuyển vào `planning` service; không còn pipeline geocode→solve. | T25 |
| `routeoptimizer.controller/service/dto` (`/route/optimize*`) | Replace | Thay bằng API plan; xoá endpoint cũ. | T27 |
| Module `store` (mới) | Extend | Store, thành viên, lời mời. | T13, T16 |
| Module `fleet` (mới) | Extend | Xe. | T20 |
| `core.job` (mới) | Extend | Job framework inline/async dùng chung (D-19). | T24, T29 |
| Module `planning` (mới) | Extend | Plan, route, stop, optimize, publish, trạng thái. | T24–T26 |
| Module `driver` (mới — hoặc controller trong `planning`) | Extend | API tài xế, vị trí. | T30–T33 |
| `be/db/db.sql` | Replace | → Flyway `V1__baseline.sql`; giữ file cũ chỉ đến khi CI chuyển xong. | T02 |
| CI workflow | Replace | T01 sửa tối thiểu để CI xanh; T06 thay bằng `ci.yml` và `quality.yml`; T07 thêm `image.yml`. | T01, T06, T07 |
| Container & deployment | Extend | Dockerfile multi-stage, `compose.yaml` có profiles `app`/`routing`, `compose.prod.yaml`, runbook deploy thủ công. | T05, T07, T08 |
| Rate limiting | Extend | | T40 |

### Breaking changes (chấp nhận được vì chưa có client)

- Mọi path đổi sang `/api/v1/...` (P-02).
- Error body: `message` → `code` (P-03).
- Cookie `refreshToken` đổi path; luồng đổi mật khẩu thay hoàn toàn.
- `UserRole.USER` bị bỏ. Dữ liệu cũ không được giữ, dev phải reset DB (D-29).
- `/route/optimize`, `/route/optimize/k` bị xoá.
