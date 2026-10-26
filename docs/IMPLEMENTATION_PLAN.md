# IMPLEMENTATION_PLAN — Kế hoạch triển khai backend MVP

> Phạm vi: chỉ backend (D-09). Mốc: < 1 tháng (D-12). Tham chiếu: `REQUIREMENTS.md` (FR/BR), `ARCHITECTURE.md`, `DATA_MODEL.md`, `API_SPEC.md`, `DECISIONS.md`, `CURRENT_STATE.md` (F-xx).
>
> **Cảnh báo về thời gian:** Tổng ước lượng cho các task **Must** là khoảng **28 ngày công** cho 1 developer, chưa tính review. So với bản đầu đã tăng khoảng 2 ngày do geocode chạy nền (D-18, D-19) và khoảng 2 ngày do đưa CI/CD và container vào M0 (D-30). Mốc 1 tháng chỉ khả thi khi chạy song song nhiều agent theo các track dưới đây, và các task **Should** rất có thể bị trượt. Thứ tự cắt khi chậm tiến độ: T33 → T28 → T32 → T41 (phần không bảo mật).
>
> Mọi quyết định đã được duyệt ngày 2026-10-22. Nguyên tắc chung: làm đơn giản trước nhưng giữ sẵn điểm mở rộng. Reviewer phải kiểm tra điều này ở mọi PR.

## 0. Quy tắc thực hiện

- Mỗi task làm trên **một branch riêng** từ `develop` (`task/T13-store-tenancy`), kết thúc bằng PR vào `develop`. Merge xong mới bắt đầu task phụ thuộc.
- Definition of Done (áp dụng cho mọi task):
  1. `./mvnw test` xanh, kể cả test mới. Từ T01 trở đi, lệnh này chỉ cần Docker. Từ T06 trở đi, check `ci` trên PR phải xanh.
  2. Acceptance criteria của task đều được kiểm chứng bằng test hoặc ghi chú cách kiểm.
  3. Làm đủ **checklist docs trong `AGENTS.md`** (CURRENT_STATE, API_SPEC, DATA_MODEL, DECISIONS, IMPLEMENTATION_PLAN), cập nhật trong cùng PR với code, và liệt kê kết quả trong mô tả PR.
  4. Không thêm dependency, module hoặc mũi tên phụ thuộc ngoài những gì task cho phép.
- **File nóng**: các file dưới đây không cho hai task chạy song song cùng sửa. Task sửa file nóng phải đứng trong chuỗi tuần tự của track.
  `SecurityConfig`, `ErrorCode`, `GlobalExceptionHandler`, `AuthController`, `AuthService`, `pom.xml`, `application*.yaml`, `.github/**`, `compose*.yaml`, `be/Dockerfile`, `db/migration/*` (mỗi task chỉ tạo đúng file version đã được cấp).
  Riêng `ErrorCode`: chỉ được **thêm** giá trị. Nếu hai PR xung đột thì bên merge sau tự rebase.

## 1. Lộ trình và các track song song

```
Ngày     1    3    5    7    9    11   13   15   17   19   21   23   25   27   30
M0  T00─T04─┬─T01─┬─T03
        T02─┘     └─T05─T06─T07
                        └─T08 (Should, song song)
M1 Track A (auth)       T10─T11─T12─T13─T17─┬─T14─T15
                                            └─T16
   Track B (routing)    T21  T22  T23──────
M2 Track A                                       T20─T24─T29─T25─T26─T27
M3                                                          T30─T31  T40 T41
M4                                                                     T43  (T32 T33 T28 nếu kịp)
```

Bảng dependency (→ nghĩa là "phải xong trước"):

| Task | Phụ thuộc | Có thể song song với |
|---|---|---|
| T00 | — | — |
| T04 | T00 | T02 |
| T02 | T00 | T04 |
| T01 | T02, T04 | — |
| T03 | T01 | T05, T06, T07 |
| T05 | T01 | T03 |
| T06 | T05 | T03 |
| T07 | T06 | T03, T08, Track A/B |
| T08 (Should) | T05 | mọi task (không đụng code Java) |
| T10 | T03 | T21, T22, T23, T07 |
| T11 | T10 | Track B |
| T12 | T11 | Track B |
| T13 | T12, T02 | Track B |
| T17 | T13, T11 | Track B |
| T14 | T17 | T16 (khác file controller: xem ghi chú T16) |
| T15 | T14 | T16 |
| T16 | T17 | T14, T15 |
| T21 | T03 | Track A |
| T22 | T03 | Track A, T21 |
| T23 | T03 | Track A, T21, T22 |
| T20 | T17 | T14–T16 |
| T24 | T20, T13 | T14–T16 |
| T29 | T24, T21 | — |
| T25 | T29, T22, T23 | — |
| T26 | T25 | — |
| T27 | T25 | T26 |
| T30 | T26 | T40 |
| T31 | T30 | T40, T41 |
| T40 | T16 | T30, T31 |
| T41 | T10 | T30, T31 |
| T43 | toàn bộ Must | — |
| T28 (Should) | T26 | T30+ |
| T32 (Should) | T30 | T31 |
| T33 (Should) | T30, T22 | T31 |

Đã kiểm tra: không có vòng phụ thuộc.

## 2. Phase M0: Nền tảng (Must)

### T00. Hoàn thiện refactor đang dở và khôi phục build xanh
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-13
- **Mục tiêu:** Working tree compile được, vá lỗ hổng F-01, sửa F-04, dọn F-19, commit bản refactor.
- **Phạm vi:**
  - F-00: thay mọi chỗ dùng `ErrorCode.SERVER_ERROR` bằng `INTERNAL_ERROR(500)` (thêm mới).
  - F-01: `OtpTokenManager.generateToken(purpose, userId)` lưu value `{userId}:{hash}`. `verifyOtpToken(ticket, token, purpose)` trả `Optional<Long>` userId. `AuthService.updateUserPassword` từ chối nếu userId khác `identity.id()`.
  - F-04: `expiresAfter = Instant.now().plus(ttl)`, TTL lấy từ một hằng/config duy nhất. Sửa `OtpTokenManagerTest` cho đúng giá trị.
  - F-19: xoá bean `hmacOtpTokenGenerator`, DTO `GeneratedOtpToken`, property `token.jwt.otp-token-*`.
  - F-26: thêm unit test round-trip `AccessTokenManager` (generate → verify ra đúng `role`).
- **Không thuộc phạm vi:** đổi luồng đổi mật khẩu (T15), gộp `OtpStore`/`OtpManager` (T12), sửa package (T04).
- **File dự kiến:** `auth/token/opaque/otptoken/**`, `auth/service/AuthService.java`, `auth/token/jwt/**`, `core/exception/ErrorCode.java`, `routeoptimizer/.../NominatimGeocodingProvider.java`, test tương ứng.
- **Acceptance criteria:**
  - `./mvnw -o compile` thành công.
  - Unit test pass (trừ `ToApplicationTests` và `OptimizationControllerTest`, sẽ sửa ở T01).
  - Test mới: token/ticket sinh cho user A, đem dùng với identity B → `INVALID_TOKEN`, mật khẩu B không đổi.
  - Test: `expiresAfter` nằm trong khoảng `now + ttl ± 5s`.
- **Deliverable:** PR "complete OTP/token refactor".

### T04. Đổi tên package `auth.infastructure` thành `auth.infrastructure`
- **Tầng:** Must · **Ước lượng:** 0,25 ngày · **Quyết định:** P-17
- **Phạm vi:** đổi tên package và import thuần cơ học. Không thay đổi logic.
- **Rủi ro:** đụng nhiều file. Phải làm **một mình**, không task nào khác sửa `auth/**` cùng lúc.
- **Acceptance criteria:** `grep -r infastructure be/src` không còn kết quả; test của T00 vẫn pass.

### T02. Thêm Flyway và baseline migration
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **Quyết định:** P-01
- **Phạm vi:** thêm dependency `spring-boot-starter-flyway` và `flyway-mysql` (Spring Boot 4 tách auto-configuration ra module riêng, nên chỉ thêm `flyway-core` thì Flyway không tự chạy); tạo `db/migration/V1__baseline.sql` (lấy từ `be/db/db.sql`, bỏ `CREATE DATABASE`/`USE`); bật `baseline-on-migrate`; đặt `ddl-auto: validate` cho cả profile dev (ghi chú cho chủ dự án: sửa file local `application-dev.yaml`, vì file này gitignored).
- **Không thuộc phạm vi:** bảng mới; xoá `be/db/db.sql` (T01 xoá sau khi CI không còn dùng).
- **Acceptance criteria:** chạy với DB trống tạo đúng 2 bảng; chạy với DB dev hiện có không lỗi (baseline); Hibernate validate pass.

### T01. Hạ tầng test và CI
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** P-20, P-21
- **Phạm vi:**
  - F-06: bỏ `@Configuration` trên `JwtAuthFilter`/`ClientDetailFilter`. Filter **không** là bean; `SecurityConfig` tự khởi tạo (`new JwtAuthFilter(accessTokenManager)`). Không dùng `@Component` + `FilterRegistrationBean` như dự tính ban đầu, vì `@WebMvcTest` vẫn nạp mọi bean kiểu `Filter`. Thêm `spring-boot-starter-security-test` (Spring Boot 4 cần module này để slice test có `HttpSecurity`).
  - Thêm `spring-boot-testcontainers`, `testcontainers` (`mariadb`, `junit-jupiter`); container MariaDB dùng image `mariadb:11.8` (D-35); Redis dùng `GenericContainer` (`redis:7.4`, ghim tag cụ thể giống tinh thần D-35) với `@ServiceConnection`.
  - Tạo `src/test/resources/application-test.yaml` với secret giả. Tạo bean test `FakeEmailSender` lưu email để test đọc OTP.
  - Base class `AbstractIntegrationTest`.
  - Sửa `ToApplicationTests` (context load bằng Testcontainers) và `OptimizationControllerTest` (chạy cùng security).
  - CI tối thiểu để giữ xanh: sửa `test.yaml` hiện có thành chỉ chạy `./mvnw -B test`, bỏ service MariaDB và bước `mysql < db.sql`; xoá `be/db/db.sql`. Workflow đầy đủ do T06 thay thế.
  - Giữ cấu hình surefire và mockito agent đang có trong `pom.xml`.
- **Acceptance criteria:** `./mvnw test` xanh trên máy chỉ có Docker; mọi test hiện có pass; CI trên GitHub xanh (kèm link run trong PR).

### T03. Chuẩn hoá xử lý lỗi
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** P-03
- **Phạm vi:**
  - `ErrorResponse` đổi thành `{code, details, exception?, stackTrace?}`.
  - Map HTTP status theo `ARCHITECTURE.md` §5 và bổ sung các `ErrorCode` có trong `API_SPEC.md` §9 mà luồng hiện tại cần (F-24).
  - Thêm handler cho `DataIntegrityViolationException` (→ 409, F-15), `HttpMessageNotReadableException` (→ 400 `MALFORMED_REQUEST`), `MissingRequestCookieException`, `ExternalServiceException` (mới, → 502).
  - Thêm `AuthenticationEntryPoint` và `AccessDeniedHandler` ghi JSON (F-12).
  - Đổi message của các constraint đang dùng câu văn sang mã UPPER_SNAKE.
  - Thêm `permitAll` cho `/actuator/health/**` trong `SecurityConfig` (phục vụ T05).
- **Không thuộc phạm vi:** đổi logic nghiệp vụ của bất kỳ endpoint nào.
- **Acceptance criteria (web slice test):** gọi endpoint bảo vệ không có token → 401 `UNAUTHORIZED`; sai role → 403 `FORBIDDEN`; body sai JSON → 400 `MALFORMED_REQUEST`; validation → 400 kèm `details`; profile không phải dev không có `stackTrace`.

> **Prefix `/api/v1` (P-02):** đặt `server.servlet.context-path` không phù hợp vì swagger cũng bị ảnh hưởng. Mỗi controller khai báo `@RequestMapping("/api/v1/...")`. Việc đổi path làm trong T03 cho toàn bộ controller hiện có (`auth`, `users`, `admin/users`, `route/optimize`) và cập nhật `SecurityConfig`.

### T05. Container hoá: Dockerfile multi-stage và compose
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-30, D-32 · **NFR:** 05
- **Phạm vi:**
  - `be/Dockerfile` multi-stage `build` → `extract` → `runtime` theo `ARCHITECTURE.md` §12.1: user không phải root, `HEALTHCHECK`, `.dockerignore`.
  - Thêm `spring-boot-starter-actuator`, chỉ expose `health`, có liveness/readiness (cấu hình trong `application.yaml`). **Không sửa `SecurityConfig`**: `permitAll` cho `/actuator/health/**` do T03 thêm, để tránh xung đột khi T03 và T05 chạy song song.
  - `compose.yaml` ở root theo §12.2 với service mặc định (mariadb, redis, mailpit) và profile `app`. Chưa làm profile `routing` (T08).
  - `.env.example`, `be/src/main/resources/application-dev.example.yaml` (không chứa secret, `ddl-auto: validate`, SMTP trỏ tới mailpit, DB trỏ tới `localhost:3307`, Redis trỏ tới compose). Image `mariadb:11.8` (D-35).
  - README có mục Troubleshooting: dùng MariaDB native thay compose mà Flyway báo "Unable to check whether schema is empty" sau khi nâng cấp MariaDB thì chạy `mariadb-upgrade`; khuyến nghị dùng DB trong compose. Thêm `.env` vào `.gitignore` (đã làm trước trên `develop` ở `4111d8f`; T05 cập nhật `.env.example` cho khớp `compose.yaml`).
  - Cấu hình qua biến môi trường cho mọi secret (`SPRING_DATASOURCE_*`, `SPRING_MAIL_*`, `TOKEN_JWT_ACCESS_TOKEN_SECRET`, ...).
  - `README.md` ở root: yêu cầu môi trường, 2 cách chạy (IDE + hạ tầng, hoặc A–Z), chạy test, xem email trên mailpit.
- **Không thuộc phạm vi:** workflow (T06, T07), OSRM/Nominatim local (T08), `compose.prod.yaml` (T07).
- **Acceptance criteria:**
  - Repo sạch: `cp .env.example .env && docker compose --profile app up -d --build` → `GET /actuator/health` trả `UP` trong 2 phút; Flyway migrate thành công.
  - `docker compose up -d` cộng `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (dùng file example đã copy) chạy được.
  - Image runtime chạy bằng user không phải root (`docker run --rm <img> id -u` khác 0); không có file `application-dev.yaml` trong image.
  - Build lại khi chỉ đổi source Java không tải lại dependency Maven (layer cache hoạt động).
- **Rủi ro:** OR-Tools cần native lib và glibc, nên phải kiểm tra solver chạy được trong container. Kèm theo một bước smoke test: gọi `ORToolsSolver` lúc khởi động bằng bài toán nhỏ, hoặc chạy test solver trong container.

### T06. CI và kiểm tra chất lượng
- **Tầng:** Must · **Ước lượng:** 0,75 ngày · **Quyết định:** D-31, D-34
- **Phạm vi:**
  - Thay `test.yaml` bằng `ci.yml`, `quality.yml` và `.github/dependabot.yml` theo `ARCHITECTURE.md` §12.3.
  - Thêm plugin JaCoCo vào `pom.xml` (chỉ report, không có ngưỡng); report được upload làm artifact và ghi tóm tắt vào job summary.
  - Cache Maven; concurrency group để huỷ run cũ của cùng PR.
  - Tạo `docs/CONTRIBUTING.md` (hoặc một mục trong README) hướng dẫn chủ dự án cấu hình branch protection: `develop`/`main` cần PR, check `ci` xanh, cấm force push.
- **Acceptance criteria:** mở PR thử thì `ci` và `quality` chạy và xanh; một PR thử chứa secret giả (chuỗi kiểu AWS key) bị gitleaks chặn (ghi lại kết quả rồi đóng PR); artifact coverage có trong run.

### T07. Publish image và runbook deploy thủ công
- **Tầng:** Must · **Ước lượng:** 0,75 ngày · **Quyết định:** D-31, D-34
- **Phạm vi:**
  - `image.yml` theo `ARCHITECTURE.md` §12.3: Buildx kèm cache, Trivy, push `ghcr.io`, quy tắc tag cho `develop` và `v*`.
  - `compose.prod.yaml` dùng image từ registry, không có mailpit.
  - Profile Spring `staging` và `prod` (bật/tắt swagger, mức log).
  - `docs/DEPLOYMENT.md`: chuẩn bị VPS (Docker, `.env`, đăng nhập `ghcr.io` bằng PAT `read:packages`), lần deploy đầu, cập nhật phiên bản, rollback (pull tag cũ), backup MariaDB (`mariadb-dump` thủ công), ghi chú reverse proxy TLS.
- **Không thuộc phạm vi:** workflow deploy tự động (D-31), cấu hình reverse proxy.
- **Phụ thuộc phát sinh:** Trivy phát hiện 6 CVE CRITICAL trong dependency của Spring Boot 4.1.0 (F-34). Đã xử lý bằng PR #5 `fix/deps-security` (Boot 4.1.1 + Tomcat 11.0.27), merge trước T07; nếu không, gate của `image.yml` fail ở lần merge đầu.
- **Acceptance criteria:** merge vào `develop` sinh image `develop-<sha>` trên GHCR; push tag `v0.1.0` thử sinh tag `0.1.0` và `latest`; Trivy chạy và ghi kết quả; làm theo `DEPLOYMENT.md` trên một máy sạch (hoặc VM local) với `compose.prod.yaml` thì app health `UP`.

### T08. Profile `routing`: OSRM và Nominatim self-host
- **Tầng:** Should (trong M0, chạy song song, không chặn task nào) · **Ước lượng:** 1 ngày · **Quyết định:** D-25, D-33
- **Phạm vi:** thêm service `osrm-prepare`, `osrm`, `nominatim` vào `compose.yaml` dưới profile `routing` (dữ liệu Việt Nam từ Geofabrik, volume riêng); mục README hướng dẫn bật profile, thời gian import và yêu cầu RAM/disk, cùng các biến env để trỏ app vào provider local và tắt rate limit.
- **Phụ thuộc mềm:** biến `ROUTING_*` chỉ có tác dụng sau khi T21/T22 đổi provider sang đọc cấu hình. Trước đó vẫn kiểm tra được bằng curl trực tiếp tới service.
- **Acceptance criteria:** `docker compose --profile routing up -d` → sau khi import xong, `curl` tới OSRM `/table/v1/driving/...` với 3 toạ độ ở Đà Nẵng trả ma trận; Nominatim `/search?q=Đà Nẵng` trả kết quả.

## 3. Phase M1: Auth và tenancy

### T10. Refresh token, cookie và logout (web/mobile)
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-14, P-04 · **FR:** A03, A04, A05
- **Phạm vi:**
  - F-02: rotate từ chối token có `expiresAt < now` (đặt state `EXPIRED`); nhánh grace cũng kiểm tra hạn.
  - F-03, F-14: cookie dùng `Path=/api/v1/auth` và `SameSite=Strict`; `/log-out` chuyển sang public, nhận refresh token từ cookie (web) hoặc body (mobile), luôn trả 204.
  - Header `X-Client-Platform: mobile` → refresh token đi qua body cho cả log-in, refresh và log-out.
  - Response log-in/refresh theo `API_SPEC.md` §2 (`accessTokenExpiresAt`, `user`).
  - Refresh từ chối user không còn ACTIVE. Bước này chỉ chuẩn bị chỗ kiểm tra; trạng thái user có từ T13, nên trước đó kiểm tra "user tồn tại" như hiện tại.
  - F-30: đổi `@JoinColumn` thành `@Column`.
- **Acceptance criteria (integration test):** token hết hạn → 401; web log-in có `Set-Cookie` với path và SameSite đúng, body không có `refreshToken`; mobile có `refreshToken` trong body và không có `Set-Cookie`; log-out web chỉ dùng cookie (không cần access token) → family bị revoke; reuse token đã USED sau grace → revoke family.

### T11. Quản lý session
- **Tầng:** Must · **Ước lượng:** 0,75 ngày · **Quyết định:** P-07, P-24 · **FR:** A06
- **Phạm vi:**
  - Access token thêm claim `sid`.
  - `GET /sessions` trả mỗi family một dòng, chỉ family có token ACTIVE chưa hết hạn, kèm `current`.
  - Thêm `DELETE /sessions/{sessionId}` và `POST /sessions/revoke-others`.
  - Thêm `RefreshTokenManager.revokeAllForUser(userId, exceptFamilyId?)`, sẽ dùng lại ở T14, T15, T16.
  - F-37 (P-24): `rotateToken` trả `RotationResult` (`Rotated`/`Rejected(RejectReason)`) thay cho `Optional`; `AuthService` log lý do và ném `INVALID_TOKEN`. `verifyJwt`/`verifyAccessToken` trả kiểu kết quả kèm lý do; `JOSEException` thành lỗi 500 thay vì "token không hợp lệ".
  - F-38: `auth.cookie.same-site` chuyển sang `@ConfigurationProperties`.
- **Acceptance criteria:** đăng nhập 2 lần thì thấy 2 session; xoá session kia thì refresh bằng token của nó → 401; không xoá được session của user khác (→ 404); token hết hạn không xuất hiện trong danh sách; unit test `RefreshTokenManager` assert đúng `RejectReason` cho từng nhánh (thiếu token, không tìm thấy, hết hạn, revoked, dùng lại sau grace).

### T12. Gộp OTP và tách module notification
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-13, ARCHITECTURE §2, §6.1
- **Phạm vi:**
  - Purpose chuyển thành enum `OtpPurpose`.
  - `OtpStore` chỉ giữ thao tác Redis (save kèm cooldown, verify bằng Lua). `OtpManager` (implementation thật, bỏ stub) lo sinh OTP, gọi store, gửi email theo purpose. Key dùng `userId`.
  - TTL, cooldown và số lần thử lấy từ `@ConfigurationProperties` (F-09).
  - Email gửi lỗi thì xoá cooldown (F-10).
  - Chuyển `auth.email.infrastructure` thành module `notification` với API `EmailNotificationService.send(templateName, to, variables)`. Template tiếng Việt cho `verify-email`, `reset-password`, `change-password` (F-23).
  - Bỏ `Security.setProperty` toàn cục (F-22).
  - Luồng đổi mật khẩu cũ phải tiếp tục chạy trên API mới.
- **Acceptance criteria (integration test với Redis thật):** sai 3 lần → OTP bị huỷ; gửi lại trong lúc cooldown → `OTP_COOLDOWN`; TTL theo config; email lỗi → không còn key cooldown; OTP purpose A không verify được cho purpose B.

### T13. Store, tenancy, đăng ký và xác thực email
- **Tầng:** Must · **Ước lượng:** 1,5 ngày · **Quyết định:** D-01, D-02, D-03, D-15, D-21, D-29, P-19 · **FR:** A01, A02, A09, S01 · **Migration:** V2
- **Phạm vi:**
  - Migration V2 theo `DATA_MODEL.md` §2.1, §2.2, §2.3, bao gồm cột `can_drive`. Không chuyển dữ liệu (D-29, §5). Cung cấp `be/db/seed-admin.example.sql`; ghi trong PR hướng dẫn reset DB dev.
  - Module `store`: entity `Store`, `StoreService` gồm `GET/PUT /store` (phân quyền chi tiết làm ở T17, tạm thời yêu cầu đăng nhập).
  - `UserRole` mới; `UserStatus`; chuẩn hoá email.
  - Register tạo OWNER và store trong một transaction, sau commit mới gửi OTP `VERIFY_EMAIL`.
  - Thêm `POST /email/verify` và `/email/verify/resend`.
  - Log-in từ chối `PENDING_VERIFICATION`/`DISABLED`; refresh từ chối user không ACTIVE. `findUserByCredentials` thay bằng `authenticate` trả `AuthenticationResult` (`Authenticated`/`InvalidCredentials`/`EmailNotVerified`/`Disabled`) theo P-24 (F-37).
  - `/users/me` thêm `fullName`, `role`, `storeId`, `status`.
  - `UserIdentity` thêm `storeId`, JWT có thêm claim `storeId` (P-09). Phần này chuyển từ T17 sang (chủ dự án chọn 2026-10-26), vì `GET/PUT /store` phải lấy `storeId` từ token. T17 vẫn lo `canDrive` và phân quyền theo role.
  - Interface công khai `StoreMemberQuery` (ví dụ `findActiveMember(storeId, userId, role)`) để `planning` kiểm tra tài xế mà không đụng repository của `user`.
- **Acceptance criteria:** register → user PENDING kèm store, FakeEmailSender nhận OTP; log-in trước khi verify → 403 `EMAIL_NOT_VERIFIED`; verify xong log-in được; register trùng email (khác hoa thường) → 409; Flyway chạy V1→V2 trên DB trống thành công; Hibernate validate pass.

### T17. Authorization và tenant isolation
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-03, P-08, P-09
- **Phạm vi:**
  - `UserIdentity` thêm `canDrive`; JWT có thêm claim `canDrive` (P-09). (`storeId` đã chuyển sang T13; `sessionId` nằm trong principal `AuthenticatedUser` từ T11, P-25.)
  - Quy tắc "người được gán route" (`role=DRIVER` hoặc `canDrive`) dùng chung, có sẵn để T24 và T30 sử dụng (ví dụ bean `@authz.canDrive(principal)`).
  - `@PreAuthorize` theo ma trận `ARCHITECTURE.md` §6.3 cho các endpoint hiện có (`/store`, `/admin/users`, ...).
  - Thêm helper test `TestUsers` (tạo store, OWNER, DISPATCHER, DRIVER và access token) cùng mẫu test tenant isolation để các task sau dùng lại. T13 đã có `support/TestAccounts` (đăng ký và verify OWNER qua API), có thể mở rộng từ đó.
  - ADMIN không truy cập được `/store`.
- **Acceptance criteria:** ma trận quyền của endpoint hiện có được test (một test cho mỗi ô ✓/— quan trọng); OWNER store A gọi `/store` chỉ thấy store A.

### T14. Quên mật khẩu
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **Quyết định:** P-06 · **FR:** A07
- **Phạm vi:** 3 endpoint `/password/forgot`, `/password/reset/verify`, `/password/reset`; reset token bind userId (dùng lại `OtpTokenManager` của T00); reset thành công thì revoke mọi session. `verifyOtpToken` đổi từ `Optional<Long>` sang kiểu kết quả; giá trị Redis hỏng là lỗi kỹ thuật, không coi là token sai (P-24, F-37).
- **Acceptance criteria:** email không tồn tại → vẫn 204 và không gửi email; luồng đúng thì đổi được mật khẩu và mọi refresh token cũ → 401; dùng lại reset token → 400; reset token hết hạn → 400 (mã `INVALID_RESET_TOKEN`, P-26).

### T15. Đổi mật khẩu bằng mật khẩu cũ + OTP
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **Quyết định:** D-17, P-05 · **FR:** A08
- **Phạm vi:**
  - Thêm `POST /password/change/otp {currentPassword}` và `PUT /password {otp, newPassword}`.
  - Xoá luồng cũ: `GET/POST /auth/update-password*`, cookie `updateUserPassword*`, và `OtpTokenManager` nếu không còn chỗ dùng (T14 vẫn dùng nên **giữ**). Xử lý F-05, F-13.
- **Acceptance criteria:** sai mật khẩu cũ → 400 và không gửi email; đúng mật khẩu cũ và OTP → đổi thành công, session hiện tại vẫn refresh được, các session khác → 401; OTP của user A không dùng được cho user B.

### T16. Lời mời và quản lý thành viên
- **Tầng:** Must · **Ước lượng:** 1,5 ngày · **Quyết định:** D-16, D-21, D-27 · **FR:** S02, S03, S04, S06, (S05 Should) · **Migration:** V3
- **Phạm vi:** bảng `store_invitations`; các endpoint trong `API_SPEC.md` §4, gồm cả `canDrive` trong `PATCH /store/members/{userId}` và `PATCH /store/members/me/can-drive` (FR-S06, D-21); template email `invitation`; chấp nhận lời mời tạo user ACTIVE với `email_verified_at` được set; vô hiệu hoá thành viên thì revoke session.
- **Ghi chú song song:** controller mới nằm trong module `store`, không sửa `AuthController`. Nếu phải sửa `SecurityConfig` (permitAll cho `/invitations/**`) thì rebase sau T14/T15.
- **Acceptance criteria:** mời → email chứa mã; chấp nhận → log-in được với role đúng và `storeId` đúng; mã đã dùng, đã revoke hoặc hết hạn → `INVITATION_INVALID`; mời email đã có tài khoản → 409; DISPATCHER gọi API mời → 403; OWNER store B huỷ lời mời của store A → 404.

## 4. Track B: Routing adapters (Must, chạy song song với M1)

### T21. Endpoint geocoding search
- **Tầng:** Must (provider, cache, rate limit) / Should (endpoint search) · **Ước lượng:** 1 ngày · **Quyết định:** D-18, D-25, P-11 · **FR:** P01, P09
- **Phạm vi:**
  - Đổi tên package `routeoptimizer` thành `routing` (chỉ phần geocoding trong task này; phần còn lại xử lý ở T22, T23, T27).
  - `GeocodingProvider.search(query, limit)` gọi Nominatim (`countrycodes=vn`); rate limit toàn cục bằng Redis (cấu hình được: `enabled`, `per-second`; tắt được khi tự host Nominatim, D-25), chờ tối đa 2 s rồi trả 503 `GEOCODING_BUSY`; `CachingGeocodingProvider` cache 24 h, cache hit không tốn slot rate limit; base URL cấu hình được.
  - Timeout HTTP client; bỏ `System.out` (F-16).
  - Thêm `GET /api/v1/geocoding/search`.
  - Xoá `geocodeListAddress` và `Thread.sleep`.
- **Acceptance criteria:** tắt rate limit bằng config thì gọi liên tiếp không bị chờ; `MockRestServiceServer` kiểm tra query param; lần gọi thứ 2 cùng query lấy từ cache (không có HTTP call); provider lỗi hoặc timeout → 502; validation `q`.

### T22. Matrix provider trả cả distance và duration
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **FR:** P04
- **Phạm vi:** `MatrixProvider.matrix(List<Coordinate>)` trả record `TravelMatrix(distanceMeters, durationSeconds)`; OSRM gọi với `annotations=distance,duration`; xử lý giá trị `null` (cặp điểm không có đường) thành lỗi rõ ràng; timeout; chọn provider bằng property.
- **Acceptance criteria:** test parse response mẫu; response thiếu trường hoặc có `null` → `ExternalServiceException`.

### T23. Solver VRPTW
- **Tầng:** Must · **Ước lượng:** 2 ngày · **Quyết định:** D-05, D-19, D-20, P-13, P-14 · **FR:** P04
- **Phạm vi:** model `VrpProblem`/`VrpSolution` trong `routing`, trong đó mỗi xe có `startNode`/`endNode` **riêng** (D-20); triển khai OR-Tools theo `ARCHITECTURE.md` §8.3 (capacity, time windows, ca, max distance, service time, drop penalty, time limit cấu hình). Không phụ thuộc Spring Data hay entity.
- **Không thuộc phạm vi:** gọi matrix, lưu DB (T25).
- **Acceptance criteria (unit test với ma trận cố định):**
  - Tổng demand trên mỗi route ≤ capacity.
  - Mọi ETA nằm trong time window.
  - Route bắt đầu và kết thúc trong ca.
  - Quãng đường mỗi route ≤ maxDistance.
  - Bài toán không khả thi (một stop có demand > mọi capacity) → stop đó nằm trong `dropped`, các stop còn lại vẫn được xếp.
  - Thời gian chạy ≤ time limit + 1 s với 100 stop và ma trận ngẫu nhiên.
  - Hai xe có start/end khác nhau → mỗi route bắt đầu và kết thúc đúng node của xe đó.
- **Rủi ro:** tuning penalty/slack; số học `long`/`int` trong OR-Tools.

## 5. Track C: Vận hành

T42 cũ (Docker, compose, README) đã được gộp vào T05 và T07 trong M0 (D-30).

## 6. Phase M2: Planning

### T20. Xe
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **FR:** V01 · **Migration:** V4
- **Phạm vi:** module `fleet`; các endpoint `API_SPEC.md` §5; interface công khai `VehicleQuery` để `planning` lấy xe ACTIVE theo `storeId`.
- **Acceptance criteria:** CRUD; trùng tên trong cùng store → 409; tên trùng giữa 2 store khác nhau thì được; test tenant isolation; DRIVER → 403.

### T24. Plan draft: schema và CRUD
- **Tầng:** Must · **Ước lượng:** 1,5 ngày · **Quyết định:** D-04, D-18, D-19, D-20, D-21, P-14 · **FR:** P02, P03, P06 (phần DRAFT), P07, BR-06..BR-09, BR-13 · **Migration:** V5
- **Phạm vi:**
  - Module `planning`: entity, repository, `POST/GET/PUT /plans`, `GET /plans/{id}`.
  - Validate BR-07..BR-09, dùng `StoreMemberQuery` (lấy DRIVER ACTIVE) và `VehicleQuery`.
  - Migration V5 gồm cả `background_jobs` (entity và repository nằm ở `core.job`, chưa có executor, executor làm ở T29).
  - Stop nhận địa chỉ và/hoặc toạ độ, set `geocode_status` (`PROVIDED`/`PENDING`). Route lưu start/end = depot (D-20).
  - Người được gán phải là DRIVER hoặc `canDrive` (BR-07).
  - Snapshot depot, timezone, capacity; optimistic lock bằng `version`; `processing_status` khác IDLE thì trả 409 `PLAN_BUSY`.
  - `PUT` thay toàn bộ draft, xoá kết quả tối ưu, giữ kết quả geocode của stop có `id` cũ không đổi địa chỉ/toạ độ. Thêm `PATCH /plans/{id}/stops/{stopId}`.
  - Kiểm tra giới hạn kích thước (D-19).
  - Task này **chưa** kích hoạt geocode (T29 làm). Stop `PENDING` cứ nằm đó.
- **Acceptance criteria:** tạo plan với driver hoặc vehicle của store khác → 422 `INVALID_ASSIGNMENT`; `version` cũ → 409; time window `start >= end` → 400; store chưa có depot và request không truyền depot → 422 `STORE_DEPOT_REQUIRED`; stop không có cả địa chỉ lẫn toạ độ → 400; OWNER có `canDrive` gán được, DISPATCHER không có `canDrive` → 422 `INVALID_ASSIGNMENT`; PUT giữ geocode của stop không đổi; test tenant isolation.

### T29. Job framework và geocode chạy nền
- **Tầng:** Must · **Ước lượng:** 1,5–2 ngày · **Quyết định:** D-18, D-19 · **FR:** P09, BR-13
- **Phạm vi:**
  - `core.job`: `JobExecutor` với 2 chế độ `inline`/`async` chọn theo loại job bằng cấu hình; interface `JobHandler`; submit sau commit (`TransactionSynchronization` hoặc `@TransactionalEventListener(AFTER_COMMIT)`); khi khởi động chuyển job `RUNNING` sang `FAILED/INTERRUPTED` và plan về IDLE; `GET /api/v1/jobs/{id}` (store-scoped).
  - `planning`: `GeocodePlanJobHandler` theo `ARCHITECTURE.md` §8.2 (ghi theo lô, stop lỗi không làm hỏng job). Lưu plan có stop `PENDING` thì tự tạo job. Thêm `POST /plans/{id}/geocode`; `PlanDetail` có `processingStatus`, `activeJob`, `geocodingSummary`.
- **Không thuộc phạm vi:** optimize (T25 dùng lại framework); chạy nhiều instance.
- **Acceptance criteria (integration test, `GeocodingProvider` giả):**
  - Lưu plan 5 stop (3 địa chỉ, 2 toạ độ) → job QUEUED, sau đó SUCCEEDED; 3 stop RESOLVED; plan về IDLE.
  - Provider trả rỗng cho 1 địa chỉ → stop đó FAILED/NOT_FOUND, các stop khác vẫn RESOLVED.
  - Trong lúc GEOCODING, `PUT`/`optimize` → 409 `PLAN_BUSY`.
  - Giả lập khởi động lại khi job đang RUNNING → job FAILED/INTERRUPTED, plan IDLE.
  - Đổi cấu hình `jobs.geocode-plan.mode=inline` thì cùng test vẫn xanh (chứng minh chuyển chế độ không đổi hành vi).
  - Tenant isolation cho `GET /jobs/{id}`.
- **Rủi ro:** quản lý transaction trong thread nền (handler phải tự mở transaction ngắn); test async cần chờ có điều kiện (Awaitility; dependency test được phép thêm).

### T25. Optimize plan
- **Tầng:** Must · **Ước lượng:** 1,5 ngày · **Quyết định:** D-19, D-20, P-13 · **FR:** P04
- **Phạm vi:** `POST /plans/{id}/optimize` tạo job `OPTIMIZE_PLAN` qua framework T29 (mặc định `inline`), trả 202 `Job`. Handler theo mẫu 3 bước (tx đọc → matrix + solver ngoài transaction → tx ghi kèm kiểm tra version), như `ARCHITECTURE.md` §8.3. Dựng node start/end riêng từng route. Tính `plannedArrival` theo timezone. Stop bị drop có `dropReason = INFEASIBLE`.
- **Acceptance criteria (integration test với `MatrixProvider` giả):** response 202 với job SUCCEEDED, `GET /plans/{id}` có kết quả khớp; còn stop chưa có toạ độ → 422 `STOPS_NOT_GEOCODED`; plan bị sửa trong lúc optimize → job FAILED và plan giữ nguyên; plan không ở DRAFT → 422; provider lỗi → job FAILED `ROUTING_PROVIDER_ERROR`, plan IDLE, kết quả cũ giữ nguyên; đặt `jobs.optimize-plan.mode=async` thì test (có chờ) vẫn xanh.

### T26. Publish, cancel và vòng đời trạng thái
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-23 · **FR:** P05, P06, BR-06, BR-10, BR-11, BR-12
- **Phạm vi:** `publish` có kiểm tra trùng ca cho người được gán và xe (D-23, BR-12, khoá theo `ARCHITECTURE.md` §8.1); `cancel`; `PATCH /plans/{id}/stops/{stopId}/status` (dispatcher reset về PENDING); service chuyển trạng thái plan dùng chung cho T31.
- **Acceptance criteria:** publish khi chưa optimize → 422; tài xế có ca 08:00–12:00 ở plan A đã publish, plan B cùng ngày ca 11:00–15:00 → 422 `SHIFT_OVERLAP`, còn ca 12:00–16:00 thì publish được; xe trùng ca tương tự; sửa plan sau khi publish → 422; cancel từ IN_PROGRESS → 422; unit test bảng chuyển trạng thái.

### T27. Xoá API route optimizer cũ
- **Tầng:** Must · **Ước lượng:** 0,25 ngày · **Gap:** Replace
- **Phạm vi:** xoá `/route/optimize*`, `OptimizationService`, `RouteOptimizationPipeline*`, DTO cũ và test cũ; dọn qualifier (F-18); hoàn tất đổi tên package thành `routing`.
- **Acceptance criteria:** không còn class nào trong `routeoptimizer`; test xanh.

## 7. Phase M3: Driver và hardening

### T30. API tài xế: xem route
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-06, D-07 · **FR:** D01, D02
- **Phạm vi:** `GET /driver/routes`, `GET /driver/routes/{routeId}` với `navigationUrl`; chỉ trả route của chính người gọi trong plan PUBLISHED, IN_PROGRESS hoặc COMPLETED; mở cho `role=DRIVER` hoặc `canDrive` (D-21).
- **Acceptance criteria:** DRIVER không thấy route của driver khác (→ 404); OWNER có `canDrive` và được gán route thì thấy route của mình; DISPATCHER không có `canDrive` → 403; plan DRAFT không hiển thị; `navigationUrl` đúng định dạng.

### T31. Tài xế cập nhật trạng thái stop
- **Tầng:** Must · **Ước lượng:** 1 ngày · **FR:** D03, BR-10, BR-11
- **Phạm vi:** `PATCH /driver/routes/{routeId}/stops/{stopId}/status`; chuyển plan tự động PUBLISHED → IN_PROGRESS → COMPLETED; xử lý idempotent; concurrency theo `ARCHITECTURE.md` §7.
- **Acceptance criteria:** FAILED không có lý do → 422; cập nhật lặp lại cùng trạng thái → 200 và không đổi gì; stop cuối cùng xong → plan COMPLETED; plan CANCELLED → 422 `PLAN_NOT_ACTIVE`.

### T40. Rate limiting
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **Quyết định:** P-16 · **NFR:** 01
- **Phạm vi:** `RateLimiter` trong `core` (Redis fixed window) áp dụng cho log-in, register, resend verify, forgot, change/otp, invitations (POST); ngưỡng cấu hình được.
- **Acceptance criteria:** vượt ngưỡng → 429 `RATE_LIMITED`; Redis lỗi → fail-closed (503) cho endpoint auth.

### T41. Hardening cấu hình
- **Tầng:** Must (CORS, XFF) / Should (phần còn lại) · **Ước lượng:** 0,5 ngày
- **Phạm vi:** CORS whitelist từ env; `forward-headers-strategy` để chỉ tin XFF sau proxy (F-21); security headers; log MDC requestId/userId/storeId (NFR-06); kiểm tra không log secret.
- **Acceptance criteria:** preflight từ origin lạ bị từ chối; origin hợp lệ nhận đúng header; test XFF bị bỏ qua khi tắt forward headers.

### T43. Kiểm thử nghiệm thu MVP
- **Tầng:** Must · **Ước lượng:** 1 ngày · **REQ:** §6
- **Phạm vi:** integration test kịch bản E2E trong `REQUIREMENTS.md` §6.2 (Testcontainers, matrix giả); checklist demo thủ công với Nominatim/OSRM thật; đối chiếu OpenAPI với `API_SPEC.md`; cập nhật `CURRENT_STATE.md`.
- **Acceptance criteria:** toàn bộ tiêu chí MVP trong `REQUIREMENTS.md` §6 được đánh dấu kèm bằng chứng.

## 8. Should (làm nếu kịp, theo thứ tự)

### T32. Vị trí tài xế bằng polling
- **Ước lượng:** 1 ngày · **Quyết định:** D-08, D-26, P-15 · **FR:** D04
- **Phạm vi:** `POST /driver/location` nhận lô `points` (D-26); `DriverLocationService` đẩy vào danh sách `LocationSink`, MVP có `LastKnownLocationSink` (Redis TTL 10 phút, bỏ qua điểm cũ hơn); `GET /plans/{id}/driver-locations`; chỉ DRIVER đang có route trong plan PUBLISHED hoặc IN_PROGRESS mới hiển thị.
- **Acceptance criteria:** gửi lô 3 điểm lộn thứ tự → lưu điểm `recordedAt` mới nhất; lô sau có điểm cũ hơn điểm đang lưu → không ghi đè; vị trí cũ hơn 10 phút không trả về; DISPATCHER store khác → 404; validate lat/lon.

### T33. Turn-by-turn directions
- **Ước lượng:** 1,5 ngày · **Quyết định:** D-07 · **FR:** D05
- **Phạm vi:** `DirectionsProvider` dùng OSRM route (`steps=true`, `polyline6`); `GET /driver/routes/{routeId}/directions`; cache theo `routeId` và version của plan.
- **Acceptance criteria:** parse response mẫu; route có n stop → n+1 leg (về depot); provider lỗi → 502 (app fallback sang deep link).

### T28. Chỉnh tay route sau optimize
- **Ước lượng:** 1,5 ngày · **FR:** P08 · **Quyết định:** D-24
- **Phạm vi:** `PUT /plans/{id}/routes`; tính lại ETA và quãng đường bằng matrix (không giải lại); trả `violations` (vượt capacity, trễ time window, quá ca).
- **Acceptance criteria:** chuyển stop gây vượt capacity → lưu được nhưng có violation `CAPACITY_EXCEEDED`; stopIds không thuộc plan → 422.

## 9. Sau MVP (Could, chưa lên lịch)

Google provider (I-03) · realtime push (I-04) · optimize bất đồng bộ (FR-X03) · job dọn refresh token hết hạn · lịch sử vị trí · gợi ý địa điểm (I-01, cần phê duyệt) · thanh toán QR (I-02, cần phê duyệt).

## 10. Task đầu tiên: T00

T00 phải làm trước vì:

1. Working tree hiện **không compile** (F-00). Mọi agent khác bắt đầu sẽ gặp build đỏ.
2. Working tree đang chứa **lỗ hổng bảo mật** (F-01) do refactor dở. Nếu commit nguyên trạng, lỗ hổng sẽ vào lịch sử `develop`.
3. Chủ dự án đã quyết định tiếp tục hướng refactor này (D-13). T00 đóng nó lại thành một commit sạch, để T04 (đổi tên package, đụng mọi file auth) chạy trên nền ổn định.
4. Phạm vi nhỏ (1 ngày), không cần thêm quyết định kiến trúc, và acceptance criteria rõ ràng.

Lưu ý khi giao T00: các thay đổi chưa commit hiện chỉ có trên máy chủ dự án. Agent làm T00 phải làm việc **trên working tree hiện tại**, không checkout lại từ `develop`.
