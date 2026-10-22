# CURRENT_STATE — Hiện trạng đã xác minh & gap analysis

> Khảo sát ban đầu ngày 2026-10-21 trên `develop` tại `de34fcc`, kèm refactor OTP/token chưa commit.
> **Cập nhật lần cuối: 2026-10-22, `develop` tại `b33e9be`** (chưa push), sau T00, T04, T02, T01 và T03. Lịch sử cập nhật: §5.
> Mọi phát hiện ghi rõ mức độ bằng chứng:
> - **[Verified-run]** — đã kiểm chứng bằng compile/chạy test.
> - **[Verified-code]** — xác nhận bằng đọc code (hành vi suy ra chắc chắn từ code/spec).
> - **[Risk]** — rủi ro tiềm tàng, cần test để xác nhận.
>
> Khi một finding được sửa, cập nhật cột Trạng thái kèm task ID; không xoá dòng.

## 1. Tổng quan

| Hạng mục | Hiện trạng |
|---|---|
| Stack | Spring Boot 4.1.1 (override Tomcat 11.0.27, F-34), Java 21, Maven wrapper, Lombok, MapStruct 1.6.3, Spring Security, Spring Boot Actuator (chỉ `health`, T05), JPA/Hibernate (MariaDB), Spring Data Redis, Spring Mail (Brevo SMTP), Thymeleaf (template email), Nimbus JOSE JWT, OR-Tools 9.15, libphonenumber, springdoc 3.1.1; Maven 3.10.0 (wrapper). Cập nhật bởi Dependabot, xem §5. |
| Cấu trúc repo | `be/` (backend duy nhất, có `be/Dockerfile` multi-stage), `compose.yaml` ở root (hạ tầng + profile `app`), `compose.prod.yaml` (VPS, T07), `README.md`, `.github/workflows/ci.yml` và `quality.yml`, `.github/dependabot.yml` (T06), `.github/workflows/image.yml` (T07). Không có frontend. |
| Tài liệu | `AGENTS.md`, `CLAUDE.md` và `docs/*` (commit `17b44d2`); `docs/CONTRIBUTING.md` (quy trình branch, CI; T06); `docs/DEPLOYMENT.md` (runbook deploy thủ công; T07). Trước đó chỉ có `be/HELP.md` (mẫu Spring Initializr). |
| Schema | Flyway quản lý schema (T02): `db/migration/V1__baseline.sql` gồm `users` và `refresh_tokens`, `baseline-on-migrate` cho DB tạo trước từ `db.sql` [Verified-run: DB trống và DB có sẵn đều pass Hibernate validate]. `be/db/db.sql` đã xoá (T01). `application.yaml` dùng `ddl-auto: validate`; file local `application-dev.yaml` (gitignored) vẫn đặt `update` và chủ dự án phải tự sửa (F-32). |
| Cấu hình | `application.yaml` (chung) + `application-dev.yaml` (gitignored, chứa secret thật: SMTP, JWT secrets, DB). Mẫu không chứa secret: `application-dev.example.yaml` (trỏ tới hạ tầng compose) và `.env.example` (biến cho compose và container app); `.env` gitignored (T05). Profile `staging` (bật Swagger, log app `debug`) và `prod` (tắt Swagger, log `info`) nằm trong image; `.env.prod.example` cho VPS (T07). |
| Build | `develop` compile được [Verified-run]. Refactor OTP/token đã commit trong T00 (`810d71d`). |
| Test | **89/89 pass** [Verified-run, branch `task/T03-error-handling`]: unit test, test provider dùng mock, slice test qua security thật (`OptimizationControllerTest`, `ErrorHandlingWebMvcTest`) và `ToApplicationTests` (Testcontainers). `./mvnw test` chỉ cần Docker. Coverage JaCoCo (`./mvnw verify`, T06): line 53,1%, branch 46,6% [Verified-run], chỉ báo cáo, chưa có ngưỡng. CI GitHub Actions xanh sau T03 (run 39713873474, push `develop` tới `4111d8f`). |

### Module

| Module | Nội dung | Ghi chú |
|---|---|---|
| `core` | `ApiResponse`, `ErrorCode`/`AppException`/`GlobalExceptionHandler`, `PageResponse`/`PaginationValidator`, `StateStore`/`RedisStateStore`. | Nền tảng hợp lý. |
| `shared.identity` | `UserIdentity(id, email, role)`, `UserRole {ADMIN, USER}`. | Hợp đồng giữa `auth` và `user`. |
| `user` | Entity `User`, `UserService` (implements `UserAccountService` cho auth + `UserProfileService` cho controller), `UserController` (`/api/v1/users/me`), `AdminUserController` (`/api/v1/admin/users`), MapStruct mapper, validator số điện thoại VN. | Ổn, cần mở rộng cho store/role/status. |
| `auth` | `AuthController` (`/api/v1/auth/*`), `AuthService` (orchestrator), JWT access token (HMAC), refresh token opaque xoay vòng theo family (MariaDB, pessimistic lock), OTP (Redis + Lua), OTP token opaque (Redis, GETDEL), email OTP (Thymeleaf + JavaMail), filter JWT/ClientDetail (package `auth.infrastructure`). | Phần lớn công sức nằm ở đây. Refactor đã commit; `OtpManager`/`InternalOtpManager` vẫn là stub chưa dùng (F-11, T12). |
| `routeoptimizer` | `POST /api/v1/route/optimize` (TSP), `POST /api/v1/route/optimize/k` (VRP theo số xe). Pipeline: Nominatim geocode tuần tự (sleep 1 s/địa chỉ) → OSRM table (distance) → OR-Tools. Không lưu DB. | Prototype stateless; không khớp mô hình sản phẩm (D-04, D-05). |

### Luồng auth hiện có (tại `b33e9be`, sau T03)

1. `POST /api/v1/auth/register {email, password}` → tạo user role USER (không xác thực email).
2. `POST /api/v1/auth/log-in` → JWT access token (body) + refresh token (cookie `refreshToken`, `Path=/api/v1/auth/refresh`).
3. `POST /api/v1/auth/refresh` (cookie) → xoay vòng, grace 3 s cho request đồng thời, reuse → revoke family.
4. `POST /api/v1/auth/log-out` (cookie + yêu cầu access token) → revoke family.
5. `GET /api/v1/auth/sessions` → list refresh token ACTIVE của user.
6. Đổi mật khẩu 3 bước: `GET /api/v1/auth/update-password` (gửi OTP) → `POST /api/v1/auth/update-password/verify {otp}` (set cookie token+ticket, token gắn userId) → `POST /api/v1/auth/update-password {password}` (cookie; từ chối token của user khác).

## 2. Phát hiện

Mức độ: **Critical** (bảo mật/chặn build) · **High** (sai chức năng cốt lõi) · **Medium** · **Low**.
Nhóm: (1) nghiệp vụ/hiểu sai yêu cầu · (2) implementation · (3) kiến trúc/nhất quán · (4) dữ liệu/API/tích hợp · (5) test/kiểm chứng · (6) thiếu yêu cầu · (7) tuỳ chọn.

| ID | Mức | Nhóm | Phát hiện | Bằng chứng | Trạng thái |
|---|---|---|---|---|---|
| F-00 | Critical | 2 | Working tree không compile: `ErrorCode.SERVER_ERROR` bị xoá nhưng vẫn dùng. | `NominatimGeocodingProvider.java:50` [Verified-run] | Đã sửa (T00): thêm `INTERNAL_ERROR(500)` |
| F-01 | Critical | 2 | **OTP token không bind user** (regression của refactor). `OtpTokenManager.generateToken(purpose)` lưu key `otpToken:{ticket}:{purpose}`; `updateUserPassword` lấy userId từ JWT. Kẻ có access token bị lộ của nạn nhân có thể tự verify OTP trên tài khoản của mình rồi đổi mật khẩu nạn nhân. Bản HEAD có bind identity. | `OtpTokenManager.java`, `AuthService.updateUserPassword` [Verified-code] | Đã vá (T00): token lưu `{userId}:{hash}`, `AuthService` so userId; T15 thay luồng |
| F-02 | High | 2 | Refresh token **hết hạn vẫn xoay vòng được**: `rotateToken` không kiểm tra `expiresAt`; trạng thái `EXPIRED` không bao giờ được gán. Nhánh grace cũng rotate token active mà không kiểm tra hạn. | `RefreshTokenManager.rotateToken/handleUsed` [Verified-code] | Mở → T10 |
| F-03 | High | 4 | **Logout không hoạt động từ browser**: cookie `refreshToken` có `Path=/auth/refresh` nên không được gửi tới `/auth/log-out` → `MissingRequestCookieException` → 400. Ngoài ra `/auth/log-out` không nằm trong `permitAll` → cần access token còn hạn mới logout được. | `AuthController`, `SecurityConfig` [Verified-code, theo RFC 6265 path matching] | Mở → T10. Sau T03: path thành `/api/v1/auth/refresh` và `/api/v1/auth/log-out`, lỗi vẫn còn nhưng thiếu cookie giờ trả 401 `INVALID_TOKEN` thay vì 400 [Verified-code] |
| F-04 | High | 2 | `OtpTokenManager` trả `expiresAfter = Instant.ofEpochSecond(300)` (= 1970-01-01T00:05Z). Cookie nhận `Max-Age` âm → Spring bỏ `Max-Age` → thành session cookie (chạy được do may mắn). Test `OtpTokenManagerTest` **assert đúng giá trị sai**. | `OtpTokenManager.java`, `OtpTokenManagerTest.java:56` [Verified-code] | Đã sửa (T00): `expiresAfter = now + TTL`, test cập nhật |
| F-05 | High | 1 | Đổi mật khẩu **không thu hồi session** nào; không yêu cầu mật khẩu cũ. | `AuthService.updateUserPassword` [Verified-code] | Mở → T15 |
| F-06 | High | 5 | Mọi `@WebMvcTest` fail: `JwtAuthFilter`/`ClientDetailFilter` được annotate `@Configuration` nên bị nạp vào slice nhưng dependency (`AccessTokenService`/`Manager`) thì không. | Surefire: `NoSuchBeanDefinitionException ... jwtAuthFilter` [Verified-run] | Đã sửa (T01): filter không còn là bean, `SecurityConfig` tự khởi tạo; slice test dùng `@Import(SecurityConfig)` + mock `AccessTokenManager` + `spring-boot-starter-security-test` |
| F-07 | High | 5 | CI không thể xanh: `contextLoads` cần Redis, SMTP, `token.jwt.*` secrets — workflow chỉ cung cấp MariaDB. | `.github/workflows/test.yaml` [Verified-code; chưa xem kết quả trên GitHub] | Đã sửa (T01) [Verified-run]: GitHub Actions `test` xanh ở `62b7a85` (run 39666360032) |
| F-08 | Medium | 1 | Danh sách session trả cả token ACTIVE đã hết hạn; "session id" là id của refresh-token row, đổi sau mỗi lần refresh; không có API thu hồi từng session. | `RefreshTokenManager.getListInfoByUserId` [Verified-code] | Mở → T11 |
| F-09 | Medium | 2 | TTL OTP hardcode 300 s trong `RedisOtpStore`, trong khi email hiển thị `otp.expiration-time` → có thể lệch. Cooldown 60 s và 3 lần thử cũng hardcode. | `RedisOtpStore.java`, `VerifyOtp.lua` [Verified-code] | Mở → T12 |
| F-10 | Medium | 2 | Nếu gửi email lỗi, OTP và cooldown vẫn còn trong Redis → user phải chờ 60 s dù không nhận được mã. | `AuthService.getUpdateUserPasswordToken` [Verified-code] | Mở → T12 |
| F-11 | Medium | 3 | Hai interface chồng chéo: `OtpStore` (javadoc mô tả là "facade/manager", thực tế làm cả generate + store + cooldown) và `OtpManager` (stub trả `null`, chưa dùng). | `otp/store`, `otp/manager` [Verified-code] | Mở → T00/T12 |
| F-12 | Medium | 3 | Lỗi do Spring Security (chưa đăng nhập, token sai) không đi qua `GlobalExceptionHandler`; không cấu hình `AuthenticationEntryPoint` → body/HTTP status không theo format chung (nhiều khả năng 403 thay vì 401). | `SecurityConfig` [Risk — cần test] | Đã sửa (T03): `JsonAuthenticationEntryPoint`/`JsonAccessDeniedHandler` trả 401/403 theo format chung [Verified-run] |
| F-13 | Medium | 4 | `GET /auth/update-password` có side effect (gửi email) — vi phạm ngữ nghĩa HTTP, dễ bị prefetch/CSRF kích hoạt. | `AuthController` [Verified-code] | Mở → T15 (thay endpoint) |
| F-14 | Medium | 4 | Cookie không đặt `SameSite`; CSRF đã tắt; endpoint `/auth/refresh` dùng cookie → có thể bị kích hoạt cross-site. | `AuthCookieBuilder` [Verified-code] | Mở → T10 |
| F-15 | Medium | 2 | Đăng ký trùng email đồng thời → `DataIntegrityViolationException` → 500 (check-then-insert không atomic). Không chuẩn hoá hoa/thường email ở tầng ứng dụng. | `UserService.createUser` [Verified-code] | Một phần (T03): `DataIntegrityViolationException` → 409 `CONFLICT` [Verified-run]; chuẩn hoá email còn ở T13 |
| F-16 | Medium | 4 | Route optimizer: request không có validation (`@Valid` thiếu, list rỗng/null → NPE/500); geocode tuần tự + `Thread.sleep(1000)` mỗi địa chỉ trên request thread; `System.out.println` trong interceptor; không timeout HTTP client; OSRM trả `null` → NPE. | `routeoptimizer/**` [Verified-code] | Mở → T21, T22, T27 |
| F-17 | Medium | 1 | Route optimizer chỉ tối ưu theo **khoảng cách**, không có time window/capacity/ca; không lưu kết quả; `/route/optimize` yêu cầu đăng nhập nhưng không gắn với store. Không khớp D-04/D-05. | `ORToolsSolver`, `OptimizationController` [Verified-code] | Mở → T22–T27 (Replace) |
| F-18 | Medium | 3 | Qualifier không nhất quán: `@Qualifier("ORToolSolver")` trên class nhưng inject `@Qualifier("ORToolsSolver")` — chỉ chạy được vì Spring fallback theo bean name. | `ORToolsSolver`, `RouteOptimizationPipelineConfig` [Verified-code] | Mở → T27 |
| F-19 | Medium | 3 | Bean `hmacOtpTokenGenerator` + config `token.jwt.otp-token-secret` và DTO `GeneratedOtpToken` không còn được dùng sau refactor nhưng vẫn bắt buộc khi khởi động. | `JwtGeneratorConfig` [Verified-code] | Đã sửa (T00) |
| F-20 | Low | 3 | Package `auth.infastructure` sai chính tả (các module khác dùng `infrastructure`). | [Verified-code] | Đã sửa (T04) |
| F-21 | Low | 4 | `ClientDetailFilter` tin `X-Forwarded-For` vô điều kiện → IP trong danh sách session có thể bị giả mạo. | [Verified-code] | Mở → T41 |
| F-22 | Low | 2 | `SecureRandomOtpGenerator` gọi `Security.setProperty(...)` toàn cục trong constructor. | [Verified-code] | Mở → T12 |
| F-23 | Low | 1 | Template email OTP ghi "login request" và "valid for N **seconds**" cho luồng đổi mật khẩu; tiếng Anh. | `templates/email/otp.html` [Verified-code] | Mở → T12 |
| F-24 | Low | 4 | `ErrorCode` dùng 400 cho "not found" (`COULD_NOT_FIND_USER`), `INVALID_EMAIL_OR_PASSWORD` 400 thay vì 401. | `ErrorCode.java` [Verified-code] | Đã sửa (T03): `INVALID_EMAIL_OR_PASSWORD` 401, `EMAIL_EXISTED` 409, thêm `USER_NOT_FOUND` 404 (`COULD_NOT_FIND_USER` deprecated) [Verified-run] |
| F-25 | Low | 2 | `UserService.deleteUser` sẽ lỗi FK nếu user có refresh token (FK không cascade). Hiện không có endpoint gọi. | `db/migration/V1__baseline.sql` (trước đây `db.sql`) [Verified-code] | Mở → ghi chú, không có task (không có use case) |
| F-26 | Low | 4 | Claim `role` trong JWT được set bằng enum object (`Map.of("role", identity.role())`); đọc lại bằng `getClaimAsString` — chưa có test chứng minh Nimbus serialize enum thành string. | `AccessTokenManager` [Risk] | Đã kiểm chứng (T00): `AccessTokenManagerTest` round-trip pass, không phải lỗi |
| F-27 | Low | 4 | Lịch sử git (commit đầu `53239c0`) từng chứa username/password DB trong `application.yaml`. `application-dev.yaml` hiện tại (gitignored) chứa secret SMTP/JWT thật. | `git show 173bd14` [Verified-run] | Khuyến nghị: đổi mật khẩu DB nếu từng dùng ở nơi khác; không có task code |
| F-28 | Low | 5 | Không có test cho `AuthService`, `AuthController`, `UserService`, Lua script OTP, `JwtAuthFilter`, `HMACJwtTokenGenerator`. | [Verified-code] | Mở → T01, các task auth |
| F-30 | Low | 3 | Entity `RefreshToken` dùng `@JoinColumn` cho cột thường (`familyId`, `userAgent`, `ipAddress`) thay vì `@Column`; chạy được nhờ naming strategy mặc định (camelCase → snake_case). | `RefreshToken.java` [Verified-code] | Mở → T10 |
| F-31 | Info | 4 | Flyway 11 cảnh báo MariaDB 11.8 (image `mariadb:11`) mới hơn bản đã kiểm định (11.7). Migration vẫn chạy bình thường. | Log khởi động [Verified-run] | Đã xử lý một phần: CI ghim `mariadb:11.8` (D-35); Testcontainers ghim `mariadb:11.8` (T01 ✓); compose ghim `mariadb:11.8` (T05 ✓). Cảnh báo của Flyway vẫn còn vì Flyway chưa kiểm định 11.8; chấp nhận, migration chạy bình thường [Verified-run, T05] |
| F-32 | Low | 3 | `application-dev.yaml` local (gitignored) vẫn đặt `ddl-auto: update`, đè lên `validate`. Dev có thể tạo schema lệch với migration. | Đọc file local [Verified-code] | Đã sửa (T05): `application-dev.example.yaml` dùng `validate`; file local của chủ dự án đã đổi sang `validate` và trỏ tới MariaDB/Redis trong compose [Verified-run: app profile dev khởi động, Flyway "up to date"] |
| F-35 | Low | 3 | Spring Framework cảnh báo khi chạy test: lớp lồng `AbstractIntegrationTest.EmailTestConfiguration` (đang được `@Import`) bị bỏ qua khi phát hiện cấu hình mặc định, nhưng "từ Spring Framework 7.1 sẽ không còn bị bỏ qua". Khi nâng lên Spring 7.1, cấu hình test có thể đổi hành vi (ví dụ đăng ký trùng bean email giả). | Log `./mvnw verify` [Verified-run]; hậu quả trên 7.1 [Risk] | Mở: xử lý khi nâng Spring Boot lên bản dùng Spring 7.1 (tách lớp ra file riêng hoặc theo hướng dẫn "default-config" của Spring) |
| F-34 | High | 4 | Image dựa trên Spring Boot 4.1.0 có 6 lỗ hổng CRITICAL đã có bản sửa: Tomcat 11.0.22 (CVE-2026-65182, -65905, -68525), Spring WebMVC 7.0.8 (CVE-2026-47884, -47890), Netty handler 4.2.15 (CVE-2026-75595). Trivy gate của `image.yml` (T07) sẽ fail. | Trivy 0.56.1 quét image local [Verified-run] | Đã sửa (branch `fix/deps-security`): Boot 4.1.1 (Spring 7.0.9, Netty 4.2.17), override `tomcat.version` 11.0.27 vì Boot 4.1.1 chỉ có 11.0.24; Trivy CRITICAL có bản sửa còn 0 [Verified-run]. Còn 14 HIGH có bản sửa trong jar (không chặn theo §12.3). Bỏ override khi Boot quản lý Tomcat ≥ 11.0.25 |
| F-33 | Medium | 2 | Path không tồn tại và sai HTTP method rơi vào handler `Exception` chung nên trả 500 thay vì 404/405. | `GlobalExceptionHandler` trước T03 [Verified-code] | Đã sửa (T03): thêm handler `NoResourceFoundException` → 404, `HttpRequestMethodNotSupportedException` → 405 [Verified-run] |
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
| `core.exception.*` | Modify | Đổi field `message`→`code` (P-03), map HTTP status đúng, xử lý `DataIntegrityViolation`, lỗi Security, lỗi provider. | T03 ✓ |
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
| `auth.infrastructure.filter.*` | Refactor | Bỏ `@Configuration`, filter do `SecurityConfig` khởi tạo; sửa tên package. | T01 ✓, T04 ✓ |
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
| `be/db/db.sql` | Replace | Đã thay bằng Flyway `V1__baseline.sql` (T02) và xoá file cũ (T01). | T02 ✓, T01 ✓ |
| CI workflow | Replace | T01 ✓ sửa tối thiểu để CI xanh (`./mvnw test` + Testcontainers); T06 ✓ thay bằng `ci.yml` (verify + JaCoCo), `quality.yml` (dependency review, gitleaks), `dependabot.yml`; T07 ✓ thêm `image.yml` (Buildx + cache GHA, Trivy, push GHCR). | T01 ✓, T06 ✓, T07 ✓ |
| Container & deployment | Extend | Dockerfile multi-stage, `compose.yaml` có profiles `app`/`routing`, `compose.prod.yaml`, runbook deploy thủ công. T05 ✓: Dockerfile, `compose.yaml` (hạ tầng + `app`), `.env.example`, README; T07 ✓: `compose.prod.yaml`, `docs/DEPLOYMENT.md`. Còn `routing` (T08). | T05 ✓, T07 ✓, T08 |
| Rate limiting | Extend | | T40 |

### Breaking changes (chấp nhận được vì chưa có client)

- Mọi path đổi sang `/api/v1/...` (P-02).
- Error body: `message` → `code` (P-03).
- Cookie `refreshToken` đổi path; luồng đổi mật khẩu thay hoàn toàn.
- `UserRole.USER` bị bỏ. Dữ liệu cũ không được giữ, dev phải reset DB (D-29).
- `/route/optimize`, `/route/optimize/k` bị xoá.

## 5. Lịch sử cập nhật

| Ngày | Commit | Task | Thay đổi trạng thái |
|---|---|---|---|
| 2026-10-21 | `de34fcc` + WIP | — | Khảo sát ban đầu. |
| 2026-10-22 | `17b44d2` | — | Thêm bộ tài liệu. |
| 2026-10-22 | `810d71d` | T00 | F-00, F-01 (vá), F-04, F-19 đã sửa; F-26 kiểm chứng không phải lỗi. Unit test 67/67 pass. |
| 2026-10-22 | `8225a87` | T04 | F-20 đã sửa. |
| 2026-10-22 | — | — | Đối chiếu lại cột Hiện trạng của `API_SPEC.md` với code: sửa đánh dấu sai ở `/admin/users`, ghi chi tiết dạng response thực tế, thêm §0 tổng hợp khác biệt. |
| 2026-10-22 | branch `task/T02-flyway` | T02 | Thêm Flyway (`spring-boot-starter-flyway`, `flyway-mysql`) và V1 baseline; kiểm chứng trên MariaDB 11 với DB trống và DB có sẵn. Thêm F-31, F-32. Unit test 67/67 pass. |
| 2026-10-22 | branch `task/T02-flyway` | — | D-35: ghim MariaDB 11.8 LTS; CI đổi `mariadb:11` thành `mariadb:11.8`. Ghi nhận: MariaDB native của chủ dự án là 13.0.2 chưa chạy `mariadb-upgrade` (lỗi môi trường, không phải lỗi repo). |
| 2026-10-22 | branch `task/T01-test-infra` | T01 | F-06, F-07 (local) đã sửa; F-12 kiểm chứng bằng chạy thật (403). Thêm Testcontainers, profile `test`, `FakeEmailSender`, `AbstractIntegrationTest`; xoá `be/db/db.sql`. Test 72/72 pass. |
| 2026-10-22 | `62b7a85` | T01 | Merge vào `develop`; CI GitHub Actions xanh, F-07 xác nhận đã sửa. |
| 2026-10-22 | branch `task/T03-error-handling` | T03 | Prefix `/api/v1`, error body `{code, details}`, 401/403 JSON từ Security, map HTTP status theo spec. F-12, F-24, F-33 đã sửa; F-15 một phần. Test 89/89 pass. |
| 2026-10-22 | — | — | Rà soát checklist docs sau T01: sửa header file, F-25 (bằng chứng trỏ `V1__baseline.sql`), F-31 (Testcontainers đã ghim ở T01); header `DATA_MODEL.md` hết trỏ `db.sql`; plan T01 ghi đúng image `redis:7.4`. |
| 2026-10-22 | — | T03 | Ghi quy tắc "thành phần của `SecurityConfig`/`GlobalExceptionHandler` không là Spring bean" (T03 đã đưa vào `ARCHITECTURE.md` §5) thành P-23 trong `DECISIONS.md`, chủ dự án đã duyệt. F-03 ghi nhận mã lỗi mới sau T03; §1 (bảng Module, luồng auth) cập nhật path `/api/v1`. |
| 2026-10-22 | — | — | `ARCHITECTURE.md` §5 thêm 405 vào danh sách map chuẩn. Thêm `.env.example` (chỉ placeholder, không secret) và `.env` vào `.gitignore`, làm trước một phần phạm vi T05. |
| 2026-10-22 | `4111d8f` | T03 | CI GitHub Actions xanh sau khi push `develop` (run 39713873474, chủ dự án xác nhận). T03 hoàn tất. |
| 2026-10-22 | branch `task/T05-containers` | T05 | Thêm actuator (`health`, probes), `be/Dockerfile`, `be/.dockerignore`, `compose.yaml`, `application-dev.example.yaml`, README; `.env.example` đổi sang tên biến theo docs. F-31 compose ✓, F-32 đã sửa (local của chủ dự án chuyển sang compose). Kiểm chứng: repo sạch `cp .env.example .env && compose --profile app up` → health `UP` (app khởi động ~8 s sau khi DB healthy), Flyway migrate V1, image chạy uid 999, không có `application-dev*`; đổi source Java thì layer dependency dùng cache; compose hạ tầng + `./mvnw spring-boot:run` profile dev chạy được. Test 89/89 pass. |
| 2026-10-22 | branch `task/T06-ci` | T06 | `test.yaml` thay bằng `ci.yml`, `quality.yml`, `dependabot.yml` (PR vào `develop`); JaCoCo 0.8.13 chỉ report (surefire nối `@{argLine}` để giữ agent của JaCoCo cùng mockito agent); `docs/CONTRIBUTING.md`. Kiểm chứng local: actionlint sạch; `./mvnw verify` 89/89 pass, có `jacoco.csv` và bảng coverage; gitleaks phát hiện AWS key giả (2 finding), toàn bộ lịch sử repo không có finding. Acceptance trên GitHub (PR thử, PR chứa secret giả) chờ chủ dự án mở PR. |
| 2026-10-22 | `10337d1` | T06 | Merge PR #3 vào `develop`. Acceptance trên GitHub đạt (chủ dự án xác nhận): `ci` và `quality` xanh trên PR #3, có artifact `coverage-report`; PR thử chứa AWS key giả bị `gitleaks` chặn rồi đóng. Chủ dự án đã bật Ruleset cho `develop`/`main` (bắt buộc PR, `ci` xanh, cấm force push); bỏ hướng dẫn cấu hình khỏi `CONTRIBUTING.md`. |
| 2026-10-22 | branch `fix/deps-security` | — (chặn T07) | F-34: nâng Spring Boot 4.1.0 → 4.1.1 và override Tomcat 11.0.27 để sửa 6 CVE CRITICAL. `./mvnw verify` 89/89 pass; Trivy gate (CRITICAL có bản sửa) pass. |
| 2026-10-22 | branch `task/T07-image` | T07 | Thêm `image.yml` (tag `develop`/`develop-<sha>` và `x.y.z`/`x.y`/`latest`; Trivy báo cáo HIGH+CRITICAL vào job summary, chặn CRITICAL có bản sửa), `compose.prod.yaml` (DB user riêng, chỉ publish app ở `127.0.0.1`), profile `staging`/`prod`, `.env.prod.example`, `docs/DEPLOYMENT.md`. `.dockerignore` thu hẹp còn `application-dev*.yaml` để profile staging/prod vào image. Kiểm chứng local: actionlint sạch; `compose.prod.yaml` với image build local: profile `prod` health `UP`, Flyway migrate V1, Swagger 404; profile `staging` Swagger 200; backup/khôi phục bằng lệnh trong runbook chạy được. Trivy phát hiện F-34 (phải merge `fix/deps-security` trước). Acceptance trên GitHub (image `develop-<sha>`, tag `v0.1.0`) chờ merge. `dependency-review` trên PR chặn `aquasecurity/trivy-action@0.28.0` (GHSA-69fq-xp46-6x23: tag < 0.35.0 từng bị force-push thành malware); đổi sang v0.36.0 ghim theo commit SHA, Trivy `v0.70.0`. Workflow chưa từng chạy với bản cũ (chỉ chạy sau merge) nên không bị phơi nhiễm. |
| 2026-10-22 | `41b41c8`, `17f54cd` | T07 | Acceptance trên GitHub đạt: run `image` 39775998245 sinh `develop` và `develop-41b41c8`; PR #7 đưa `develop` vào `main`; tag `v0.1.0` (bản phát hành đầu) trên `17f54cd`, run 39779761683 sinh `0.1.0`, `0.1`, `latest`. Package GHCR public (pull không cần đăng nhập). T07 hoàn tất. |
| 2026-10-22 | `d254273` | — | Dependabot lần đầu (sau PR #7): merge 10 PR #10–#19 (mariadb-java-client 3.5.10, libphonenumber 9.0.41, OR-Tools 9.15, springdoc 3.1.1, Maven 3.10.0, `setup-java` v6, `gitleaks-action` v3, `docker/*` v4/v7); đóng #8, #9 (đổi Java của Docker image sang 25/26). Kiểm chứng local trước khi merge: #12 solver test trong Ubuntu JDK 21 + image chạy `UP`; #14 Swagger ở `staging` trả 200, đủ 11 path; #18 `verify` 89/89 và build image. Mọi run `ci`/`image` sau merge xanh. Thêm F-35. |
| 2026-10-22 | branch `chore/dependabot-config` | — | `dependabot.yml`: gộp PR (Maven minor/patch, toàn bộ GitHub Actions), bỏ qua bản major của `eclipse-temurin` và mọi cập nhật image `maven` (đổi Java là quyết định, không phải bump). Renormalize `be/mvnw.cmd` (Dependabot commit với CRLF). `CONTRIBUTING.md` thêm workflow `image`. |
