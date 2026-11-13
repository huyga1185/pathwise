# IMPLEMENTATION_PLAN — Kế hoạch triển khai backend MVP

> Phạm vi: chỉ backend (D-09). Mốc: < 1 tháng (D-12). Tham chiếu: `REQUIREMENTS.md` (FR/BR), `ARCHITECTURE.md`, `DATA_MODEL.md`, `API_SPEC.md`, `DECISIONS.md`, `CURRENT_STATE.md` (F-xx).
>
> **Cảnh báo về thời gian:** Tổng ước lượng cho các task **Must** là khoảng **28 ngày công** cho 1 developer, chưa tính review. So với bản đầu đã tăng khoảng 2 ngày do geocode chạy nền (D-18, D-19) và khoảng 2 ngày do đưa CI/CD và container vào M0 (D-30). Mốc 1 tháng chỉ khả thi khi chạy song song nhiều agent theo các track dưới đây, và các task **Should** rất có thể bị trượt. Thứ tự cắt khi chậm tiến độ: T33 → T28 → T32 → T41 (phần không bảo mật).
>
> **Client (web, app tài xế):** kế hoạch riêng ở `docs/client/PLAN.md` (W00–W08, M00–M03), quy tắc ở `docs/client/README.md`, mở rộng D-09 theo P-40. Plan này vẫn chỉ gồm task backend; thiếu API cho client thì thêm task T46 trở đi ở đây.
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
M5 (vận hành, P-59)  A: T50─T51─T52─T53─T54   B: T59─T55─T56─T57─T58─T60 + W11 (web)
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
| T44 | T29 | — |
| T25 | T29, T22, T23, T44 | — |
| T26 | T25 | — |
| T27 | T25 | T26 |
| T30 | T26 | T40 |
| T31 | T30 | T40, T41 |
| T40 | T16 | T30, T31 |
| T41 | T10 | T30, T31 |
| T43 | toàn bộ Must | — |
| T45 | T31, T43 | T32, T33, T28 |
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
- **Phát sinh khi làm (2026-11-10):** chủ dự án cần self-host cho **prod**, không chỉ demo. Chủ dự án chọn file riêng `compose.routing.yaml` thay cho profile trong `compose.yaml` (P-55): chạy độc lập trên máy routing riêng (VPS hiện dưới 4 GB RAM) hoặc ghép bằng `-f` với `compose.prod.yaml`/`compose.yaml`. Đo thật: dựng OSRM cho cả Việt Nam cần **hơn 11 GB RAM** (OSRM v26.10 và v6.0 đều OOM ở 11 GB; OSM Việt Nam có 46 triệu node vì hẻm vẽ rất dày), nên chủ dự án chọn cắt dữ liệu theo vùng bằng biến `ROUTING_BBOX` (`osmconvert`), **mặc định Đà Nẵng và Hội An** cho demo; `none` giữ cả nước. Service: `routing-data` (tải file cả nước một lần, cắt vùng, làm lại khi vùng đổi), `osrm-prepare` (dựng đồ thị MLD, làm lại khi vùng đổi), `osrm`, `nominatim`; publish trên `ROUTING_BIND`. Hướng dẫn và số đo ở `DEPLOYMENT.md` §7, biến ở `.env.prod.example`/`.env.example`. Acceptance đổi lệnh thành `docker compose -f compose.routing.yaml up -d`.

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
  - Snapshot depot, timezone, capacity và `capacityUnit` của xe; stop nhận `demands` theo loại, lưu ở `route_plan_stop_demands` (D-36); optimistic lock bằng `version`; `processing_status` khác IDLE thì trả 409 `PLAN_BUSY`.
  - `PUT` thay toàn bộ draft, xoá kết quả tối ưu, giữ kết quả geocode của stop có `id` cũ không đổi địa chỉ/toạ độ. Thêm `PATCH /plans/{id}/stops/{stopId}`.
  - Kiểm tra giới hạn kích thước (D-19).
  - Task này **chưa** kích hoạt geocode (T29 làm). Stop `PENDING` cứ nằm đó.
  - Phát sinh khi làm (2026-10-28): chuyển tiện ích số điện thoại sang `core.phonenumber` (P-29) để lưu `recipientPhone` E.164; thêm `StoreQuery.findStore` (depot, timezone), `StoreMemberQuery.findMembers` và `VehicleQuery.findAll` (gồm cả thành viên bị vô hiệu hoá, xe đã tắt, để hiển thị `PlanDetail`); `DetailedAppException` để trả `INVALID_STOP_ID` trong `details`. Hợp đồng `PlanSummary`, ngữ nghĩa merge patch của `PATCH` stop và `INVALID_STOP_ID` do chủ dự án chọn (API_SPEC §7).
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
- **Phát sinh khi làm (2026-10-29):** 4 quyết định của chủ dự án ghi ở P-30 (job `QUEUED` cũng INTERRUPTED lúc khởi động; hàng đợi đầy → `JOB_QUEUE_FULL`; job xong tăng `version` 1; `POST /geocode` không có gì để geocode → 422 `NOTHING_TO_GEOCODE`). Mọi transaction phía job dùng `REQUIRES_NEW` (job inline chạy trong `afterCommit`). Job framework tách từ `core.job` thành module cấp 1 `job` (P-31, chủ dự án chọn), thêm mũi tên `planning → job`. Test: `FakeGeocodingProvider` `@Primary` cho mọi integration test (trước đó test plan có thể gọi Nominatim thật khi job chạy); profile test đặt `jobs.geocode-plan.mode=inline`, bộ test acceptance chạy lại ở async bằng subclass. Test T24 đọc `version` hiện tại thay vì hardcode, vì job inline đã tăng version.

### T44. Làm sạch API contract (phát sinh 2026-10-29)
- **Tầng:** Must · **Ước lượng:** 0,5–1 ngày · **Quyết định:** P-32 · **Phạm vi do chủ dự án duyệt sau khi rà soát `API_SPEC.md`**
- **Phạm vi (code):** `details[].field` của kiểm tra chéo là field thật (`core.validation`: `@SelfValidated`/`SelfValidating` thay `@AssertTrue`); `MergePatch` làm lớp cơ sở cho mọi PATCH (`NOT_NULLABLE`); response trả số điện thoại E.164; `PATCH /users/me` (merge patch, thêm `fullName`, trả 200 `User`) thay `PUT`; `PATCH /store/members/{userId}` theo merge patch; `/store` dùng `depot: {address, lat, lon} | null`; 404 tài nguyên trên path là `NOT_FOUND`; bỏ `JobStatus.CANCELLED`; `POST /store/invitations/{id}/revoke` thay `DELETE`.
- **Phạm vi (chỉ contract, task sau làm):** trạng thái stop không tăng `version` của plan; response của các endpoint trạng thái stop; format `details` của `SHIFT_OVERLAP`; `DriverRouteDetail.start/end`; bỏ `navigationUrl`. Ghi chú nằm ở T26, T30, T31.
- **Acceptance criteria:** các test hiện có cập nhật theo contract mới và xanh; test mới cho merge patch của `/users/me` và member, E.164 trong response, `depot` của store, tên field của lỗi kiểm tra chéo.

### T25. Optimize plan
- **Tầng:** Must · **Ước lượng:** 1,5 ngày · **Quyết định:** D-19, D-20, P-13 · **FR:** P04
- **Phạm vi:** `POST /plans/{id}/optimize` tạo job `OPTIMIZE_PLAN` qua framework T29 (mặc định `inline`), trả 202 `Job`. Handler theo mẫu 3 bước (tx đọc → matrix + solver ngoài transaction → tx ghi kèm kiểm tra version), như `ARCHITECTURE.md` §8.3. Dựng node start/end riêng từng route. Tính `plannedArrival` theo timezone. Stop bị drop có `dropReason = INFEASIBLE`.
- **Acceptance criteria (integration test với `MatrixProvider` giả):** response 202 với job SUCCEEDED, `GET /plans/{id}` có kết quả khớp; còn stop chưa có toạ độ → 422 `STOPS_NOT_GEOCODED`; plan bị sửa trong lúc optimize → job FAILED và plan giữ nguyên; plan không ở DRAFT → 422; provider lỗi → job FAILED `ROUTING_PROVIDER_ERROR`, plan IDLE, kết quả cũ giữ nguyên; đặt `jobs.optimize-plan.mode=async` thì test (có chờ) vẫn xanh.
- **Phát sinh khi làm (2026-10-29):** P-33 (chủ dự án chọn): plan không có stop hoặc phân công → 422 `PLAN_INCOMPLETE`; solver `NO_SOLUTION` → job `FAILED/NO_SOLUTION`, giữ kết quả cũ. BR-07 kiểm lại lúc optimize → 422 `INVALID_ASSIGNMENT` kèm `details` từng phân công. `JobFailedException` ở module `job` để handler fail job với mã riêng. `PlanVersionRequest` dùng chung cho `geocode`/`optimize` (thay `GeocodePlanRequest`), sẽ dùng cho `publish`/`cancel`. `PlanProcessingService.release` dùng chung cho mọi job của plan. Test: `FakeMatrixProvider` `@Primary`, `routing.solver.time-limit: 1s` ở profile test; bộ test chạy lại ở async bằng subclass; nhánh `NO_SOLUTION` kiểm bằng unit test handler (qua API khó tái hiện vì start = end = depot).
- **Ghi chú từ T29 (2026-10-29):** pool job hiện dùng chung cho mọi loại job (`jobs.executor.pool-size` 2, `queue-capacity` 50). Hai giá trị này hợp với `GEOCODE_PLAN`: nút thắt là rate limit Nominatim 1 req/s toàn hệ thống; pool lớn hơn `max-wait × per-second` (2) sẽ tự gây `GEOCODING_BUSY`; chúng được suy ra, chưa đo. Nếu chạy `OPTIMIZE_PLAN` ở `async` (kể cả chỉ trong test ở trên), optimize dùng chung pool đó: job geocode giữ luồng trong lúc chờ rate limit, nên hai plan lớn đang geocode có thể chiếm cả 2 luồng và optimize (khoảng 10 s CPU mỗi lần) phải xếp hàng. Trước khi đổi mặc định của optimize sang `async`, tách pool theo loại job (`pool-size`/`queue-capacity` riêng cho từng loại); đó là quyết định cần chủ dự án duyệt. Giữ `inline` như hiện tại thì không bị ảnh hưởng.

### T26. Publish, cancel và vòng đời trạng thái
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-23 · **FR:** P05, P06, BR-06, BR-10, BR-11, BR-12
- **Phạm vi:** `publish` có kiểm tra trùng ca cho người được gán và xe (D-23, BR-12, khoá theo `ARCHITECTURE.md` §8.1); `cancel`; service chuyển trạng thái plan dùng chung cho T31 (`PlanLifecycle`). Endpoint đặt lại stop về PENDING **bị bỏ** (P-34: không có đảo ngược trạng thái).
- **Phát sinh khi làm (2026-10-29):** P-34 (chủ dự án chọn): không đảo ngược trạng thái; route không có stop không tính trùng ca. Publish khoá mọi plan của store trong ngày (`ORDER BY id`) thay cho khoá route theo driver/vehicle, vì plan kia còn DRAFT lúc kiểm tra. Phát hiện F-44 (MariaDB snapshot isolation: câu khoá phải là lệnh đọc đầu tiên của transaction) khi viết test publish đồng thời, sửa ngay. BR-07 kiểm lại lúc publish cho route có stop. `DetailedAppException` nhận detail có thêm field (`ShiftOverlapDetail`).
- **Ghi chú từ T44 (P-32):** cập nhật trạng thái stop không tăng `version` của plan (chỉ `version` của stop); `SHIFT_OVERLAP` trả mỗi xung đột là một phần tử `details` `{field: "assignments[i].driverId|vehicleId", code, conflictPlanId}`, nên cần một kiểu detail có thêm field (`FieldErrorDetail` hiện chỉ có `field`, `code`).
- **Acceptance criteria:** publish khi chưa optimize → 422; tài xế có ca 08:00–12:00 ở plan A đã publish, plan B cùng ngày ca 11:00–15:00 → 422 `SHIFT_OVERLAP`, còn ca 12:00–16:00 thì publish được; xe trùng ca tương tự; sửa plan sau khi publish → 422; cancel từ IN_PROGRESS → 422; unit test bảng chuyển trạng thái; hai publish đồng thời trùng ca → đúng một thành công.

### T27. Xoá API route optimizer cũ
- **Tầng:** Must · **Ước lượng:** 0,25 ngày · **Gap:** Replace
- **Phạm vi:** xoá `/route/optimize*`, `OptimizationService`, `RouteOptimizationPipeline*`, DTO cũ và test cũ; dọn qualifier (F-18); hoàn tất đổi tên package thành `routing`.
- **Acceptance criteria:** không còn class nào trong `routeoptimizer`; test xanh.

## 7. Phase M3: Driver và hardening

### T30. API tài xế: xem route
- **Tầng:** Must · **Ước lượng:** 1 ngày · **Quyết định:** D-06, D-07 · **FR:** D01, D02
- **Phạm vi:** `GET /driver/routes`, `GET /driver/routes/{routeId}` (có `start`/`end` riêng của route, không có `navigationUrl`; P-32); chỉ trả route của chính người gọi trong plan PUBLISHED, IN_PROGRESS hoặc COMPLETED; mở cho `role=DRIVER` hoặc `canDrive` (D-21).
- **Acceptance criteria:** DRIVER không thấy route của driver khác (→ 404); OWNER có `canDrive` và được gán route thì thấy route của mình; DISPATCHER không có `canDrive` → 403; plan DRAFT không hiển thị; `start`/`end` lấy từ route (D-20); route không có stop không hiển thị cho tài xế (P-34).
- **Phát sinh khi làm (2026-10-29):** P-35 (chủ dự án chọn): `status` là trạng thái riêng của route (`RouteStatus`, tính từ stop, `PlanLifecycle.routeStatus`). Plan CANCELLED cũng ẩn. Không có ngày thì dùng hôm nay theo timezone hiện tại của store (`StoreQuery`). `PlanRouteRepository` cho truy vấn theo tài xế.

### T31. Tài xế cập nhật trạng thái stop
- **Tầng:** Must · **Ước lượng:** 1 ngày · **FR:** D03, BR-10, BR-11
- **Phạm vi:** `PATCH /driver/routes/{routeId}/stops/{stopId}/status`; chuyển plan tự động PUBLISHED → IN_PROGRESS → COMPLETED; xử lý idempotent; concurrency theo `ARCHITECTURE.md` §7.
- **Ghi chú từ T44 (P-32):** response là `{stop, routeStatus, planStatus}`; cập nhật không tăng `version` của plan, để dispatcher vẫn `cancel {version}` được trong lúc tài xế đang giao.
- **Ghi chú từ T26:** dùng `PlanLifecycle.canMove(StopStatus, StopStatus)` và `PlanLifecycle.progress(...)`; một chiều, không đảo ngược (P-34), nên stop đã kết thúc mà gửi trạng thái khác → 422 `INVALID_STOP_TRANSITION`, gửi lại đúng trạng thái đang có → 200 idempotent. Tự chuyển `status` của plan (IN_PROGRESS, COMPLETED) không được tăng `version`: hoặc loại `status`/`completed_at` khỏi optimistic lock, hoặc cập nhật bằng câu `UPDATE` riêng; cần test. Nếu dùng `SELECT … FOR UPDATE`, đặt nó là lệnh đọc đầu tiên của transaction (F-44).
- **Acceptance criteria:** FAILED không có lý do → 422; cập nhật lặp lại cùng trạng thái → 200 và không đổi gì; `version` của plan không đổi; stop cuối cùng xong → plan COMPLETED; plan CANCELLED → 422 `PLAN_NOT_ACTIVE`.
- **Phát sinh khi làm (2026-11-02):** P-36 (chủ dự án duyệt): route của plan chưa từng publish → 404, đã publish rồi huỷ → `PLAN_NOT_ACTIVE`; kiểm idempotent trước `PLAN_NOT_ACTIVE` để gửi lại lần cập nhật cuối (plan đã COMPLETED) vẫn 200. Concurrency: đọc `plan_id` của route trong transaction riêng, transaction cập nhật bắt đầu bằng khoá dòng plan (F-44), nên hai tài xế xong stop cuối cùng lúc vẫn đưa plan sang COMPLETED (test đồng thời; bỏ khoá thì test fail). Trạng thái plan đổi bằng câu `UPDATE` JPQL riêng (`RoutePlanRepository.updateStatus`), không qua entity, nên `version` không tăng. Thêm `ErrorCode` `PLAN_NOT_ACTIVE`, `INVALID_STOP_TRANSITION`, `FAILURE_REASON_REQUIRED` (đã có trong API_SPEC §9). Ghi chú của T44 "để dispatcher vẫn `cancel` được trong lúc tài xế đang giao" không xảy ra: plan IN_PROGRESS không huỷ được (T26); `version` không đổi chỉ giúp dispatcher không phải tải lại plan.

### T40. Rate limiting
- **Tầng:** Must · **Ước lượng:** 0,5 ngày · **Quyết định:** P-16 · **NFR:** 01
- **Phạm vi:** `RateLimiter` trong `core` (Redis fixed window) áp dụng cho log-in, register, resend verify, forgot, change/otp, invitations (POST); ngưỡng cấu hình được.
- **Acceptance criteria:** vượt ngưỡng → 429 `RATE_LIMITED`; Redis lỗi → fail-closed (503) cho endpoint auth.
- **Phát sinh khi làm (2026-11-03):** P-37 (chủ dự án chọn): ngưỡng mặc định; `change/otp` đếm theo user, invitations theo store; 503 là mã mới `SERVICE_UNAVAILABLE`; IP chung với `ClientDetail` (F-21, T41 sửa). Thêm `RATE_LIMITED(429)` vào `ErrorCode` (API_SPEC §9 đã có nhưng enum chưa có). Contract `RateLimiter` (`core.ratelimit`) trả `RateLimitDecision` (`Allowed`/`Limited(retryAfter)`, P-24), implementation `RedisRateLimiter` chạy Lua `FixedWindowRateLimit.lua` (tăng và đặt TTL một bước), Redis lỗi thì ném `StateStoreException` như mọi Redis store. `AuthRateLimitPolicy` (`auth.infrastructure.ratelimit`) gom tên rule và trả decision; service tự đổi `Limited` thành `RateLimitedException` (429, mang thời gian còn lại cho `Retry-After`) và `StateStoreException` thành `SERVICE_UNAVAILABLE`, vì `AppException` chỉ ném từ service. `register`, `resendVerification`, `forgotPassword` nhận thêm `ClientDetail`. Profile test nâng mọi ngưỡng lên 1.000.000 để test khác không chạm giới hạn; `RateLimitIntegrationTest` tự đặt ngưỡng thấp. Redis lỗi → 503 kiểm bằng unit test (`RedisRateLimiterTest`, `AuthServiceTest`) và slice test (không tắt được Redis dùng chung của Testcontainers).

### T41. Hardening cấu hình
- **Tầng:** Must (CORS, XFF) / Should (phần còn lại) · **Ước lượng:** 0,5 ngày
- **Phạm vi:** CORS whitelist từ env; `forward-headers-strategy` để chỉ tin XFF sau proxy (F-21); security headers; log MDC requestId/userId/storeId (NFR-06); kiểm tra không log secret.
- **Acceptance criteria:** preflight từ origin lạ bị từ chối; origin hợp lệ nhận đúng header; test XFF bị bỏ qua khi tắt forward headers.
- **Phát sinh khi làm (2026-11-03):** P-38 (chủ dự án chọn): `native` ở staging/prod; danh sách origin chính xác; thêm `Referrer-Policy`, không CSP; requestId luôn tự sinh. `RateLimitIntegrationTest` (T40) đổi từ giả `X-Forwarded-For` sang đặt remote address, vì header không còn được đọc. Thêm `ForwardedHeadersIntegrationTest` chạy server thật (`RANDOM_PORT`) để chứng minh `native` áp XFF từ proxy tin cậy (MockMvc không chạy valve của Tomcat). Rà soát log: không log mật khẩu, OTP, token, secret; ghi F-46 (message lỗi ràng buộc DB có thể chứa email).

### T43. Kiểm thử nghiệm thu MVP
- **Tầng:** Must · **Ước lượng:** 1 ngày · **REQ:** §6
- **Phạm vi:** integration test kịch bản E2E trong `REQUIREMENTS.md` §6.2 (Testcontainers, matrix giả); checklist demo thủ công với Nominatim/OSRM thật; đối chiếu OpenAPI với `API_SPEC.md`; cập nhật `CURRENT_STATE.md`.
- **Acceptance criteria:** toàn bộ tiêu chí MVP trong `REQUIREMENTS.md` §6 được đánh dấu kèm bằng chứng.
- **Phát sinh khi làm (2026-11-03), chủ dự án chọn:** bằng chứng và checklist ở file mới `docs/ACCEPTANCE.md`; đối chiếu OpenAPI bằng test tự động (method + path, `ApiSpecContractIntegrationTest`, đọc `docs/API_SPEC.md`) cộng một lần so field thủ công; agent chạy demo local, chủ dự án làm phần VPS. Thêm `scripts/acceptance_demo.py` (thư viện chuẩn Python) chạy kịch bản trên stack thật. Máy chạy demo không vào được Nominatim nên geocode thành công với Nominatim thật còn chờ chủ dự án (ACCEPTANCE §2.2). Phát hiện F-47 (tài liệu OpenAPI), F-48 (log geocode thiếu nguyên nhân).

### T45. Bổ sung field cho client (phát sinh 2026-11-03)
- **Tầng:** Must (trước khi dựng `web/` và `mobile/`) · **Ước lượng:** 0,25 ngày · **Quyết định:** P-39, P-35 · **FR:** P07, D01
- **Nguồn:** thiết kế màn hình client (Claude Design, sau bước chuẩn bị client) cho thấy hai chỗ client phải tự bù dữ liệu mà server đã có.
- **Phạm vi:**
  1. `PlanDetail.routes[].status` (`RouteStatus`: `PENDING`, `IN_PROGRESS`, `COMPLETED`), tính bằng `PlanLifecycle.routeStatus` từ các stop của route, cùng nghĩa với `status` của route phía tài xế (P-35) và `routeStatus` của T31. Màn theo dõi của dispatcher hiện phải tự đếm stop để hiện trạng thái từng tuyến, tức là lặp lại quy tắc của server ở client.
  2. `DriverRouteDetail.planName`: tên plan, như `GET /driver/routes` đã có. Hiện app phải mang tên từ màn danh sách sang, nên mở thẳng chi tiết tuyến (thông báo, khởi động lại app, deep link) thì không có tên để hiện.
  3. Docs: `API_SPEC` §7, §8 (ví dụ JSON); `docs/ux/samples` sinh lại từ `MvpScenarioIntegrationTest`; `UX_BRIEF` nếu có chỗ nói client tự tính.
- **Ngoài phạm vi:** không đổi schema, không lưu trạng thái route vào DB (P-35 giữ nguyên: tính lúc đọc); không thêm field nào khác; không đổi `DriverRouteSummary` hay response cập nhật stop.
- **Acceptance criteria:**
  - `GET /plans/{id}` của plan DRAFT (kể cả sau optimize) có `routes[].status = PENDING`; route không có stop cũng `PENDING`.
  - Tài xế giao một stop → `GET /plans/{id}` của dispatcher có route đó `IN_PROGRESS`; xong mọi stop của route → `COMPLETED`, route của tài xế khác vẫn theo stop của riêng nó; giá trị luôn bằng `status` mà tài xế thấy ở `GET /driver/routes/{routeId}`.
  - `GET /driver/routes/{routeId}` có `planName` bằng tên plan.
  - Chỉ thêm field: test hiện có không phải sửa assertion cũ; `./mvnw test` xanh.

### T46. OpenAPI mô tả đúng response, mã thành công và PATCH (phát sinh 2026-11-04)
- **Tầng:** Must (client đang sinh kiểu từ `api/openapi.json`) · **Ước lượng:** 0,5 ngày · **Quyết định:** P-41, P-48 · **Lỗi:** F-49, F-50
- **Nguồn:** làm W01, W02 thấy kiểu TypeScript sinh từ OpenAPI coi mọi field response là tuỳ chọn, `AuthUserResponse.role` là chuỗi, mã thành công luôn là 200 và request PATCH không cho gửi `null`; client phải kiểm thừa (`session.tsx` `toUser`) và ép kiểu (màn Hồ sơ).
- **Phạm vi:**
  1. Quy ước P-48: component của record response (`*Response` và record lồng trong nó) luôn có mặt và không `null`, trừ khi đánh `@Nullable` (JSpecify, đã có qua Spring); record `@JsonInclude(NON_NULL)` thì field `@Nullable` vắng mặt thay vì `null`. Đánh `@Nullable` cho mọi field response có thể `null` (đối chiếu entity, mapper và `docs/ux/samples`).
  2. `core.web`: model converter của springdoc theo quy ước trên: `required` cho field luôn có mặt, kiểu kèm `null` cho field `@Nullable`; field `@Nullable` của class merge patch (`PATCH`, API_SPEC §1) là kiểu kèm `null` (gửi `null` để xoá).
  3. `AuthUserResponse.role` dùng enum `UserRole` (JSON không đổi).
  4. Mã thành công đúng API_SPEC trong OpenAPI: `@ResponseStatus` 201 (`/auth/register`, `POST /vehicles`, `POST /plans`, `POST /store/invitations`), 202 (`/plans/{id}/geocode`, `/optimize`), 204 (endpoint không có body).
  5. `scripts/sync_openapi.sh` → `api/openapi.json`; web sinh lại `schema.d.ts`, bỏ phần kiểm thừa trong `session.tsx`.
- **Ngoài phạm vi:** không đổi JSON trả về hay hành vi API; không mô tả response lỗi trong OpenAPI; không đổi request POST/PUT.
- **Acceptance criteria:**
  - OpenAPI: `GetUserResponse.required` gồm mọi field, `fullName`/`phoneNumber`/`storeId` có kiểu kèm `null`; `AuthUserResponse.fullName`, `storeId` không `required`; `AuthUserResponse.role` là enum; `UpdateProfileRequest.fullName` có kiểu kèm `null`; `POST /auth/register` 201, `POST /auth/email/verify` 204, `POST /plans/{id}/optimize` 202.
  - Mỗi field `@Nullable` đều có thể `null` thật (có test hoặc mẫu chứng minh hoặc entity cho phép); field không đánh dấu không bao giờ `null` trong các test hiện có (kiểm bằng test quét response của `MvpScenarioIntegrationTest` hoặc tương đương).
  - Response thật không đổi: test hiện có không phải sửa assertion; `./mvnw test` xanh; `pnpm lint && pnpm typecheck && pnpm test` của web xanh sau khi sinh lại kiểu.
- **Phát sinh khi làm (2026-11-04):**
  - W02 (PR #63) đã vào `develop` trước khi tạo branch, nên T46 bỏ luôn ép kiểu của màn Hồ sơ và các giá trị dự phòng thừa (`resetToken`, `role`, `sessionId`, `lastUsedAt`).
  - springdoc trả về `$ref` cho model dùng làm property; converter sửa định nghĩa trong `ModelConverterContext.getDefinedModels()`.
  - Field `@Nullable` đã đối chiếu: `null` thật trong `docs/ux/samples` (stop, route, job, member, vehicle, user) hoặc cột/entity cho phép (`SessionResponse.userAgent`/`ipAddress`, `DriverRouteSummary.plannedStart`, `PlanRouteResponse.Driver.fullName`, `GetUserResponse.storeId` của ADMIN). Field không đánh dấu (ví dụ `PlanRoute.vehicle`, `start`/`end`, `TimeWindow.start`/`end`, `PlanDetail.depot`) là cột `NOT NULL` hoặc bắt buộc ở request.

### T47. Timezone cho màn của tài xế (phát sinh 2026-11-05)
- **Tầng:** Must (trước M01) · **Ước lượng:** 0,25 ngày · **Quyết định:** P-14, P-50 · **FR:** D01
- **Nguồn:** M01 phải hiện giờ xuất phát và giờ đến dự kiến theo timezone của cửa hàng (UX_BRIEF, client ARCHITECTURE §6), nhưng tài xế không đọc được timezone: `GET /store` chỉ cho OWNER/DISPATCHER, response của `/driver/*` và `/users/me` không có. Chủ dự án chọn làm task backend trước M01.
- **Phạm vi:**
  1. `timezone` (IANA, bản chụp của plan như `PlanDetail.timezone`, P-14) trong `DriverRouteSummary` (`GET /driver/routes`) và `DriverRouteDetail` (`GET /driver/routes/{routeId}`), ngay sau `planDate`.
  2. `scripts/sync_openapi.sh` → `api/openapi.json`; sinh lại `web/src/data/schema.d.ts` và `mobile/packages/pathwise_api`.
  3. Docs: `API_SPEC` §8; `docs/ux/samples/driver-route-list.json`, `driver-route-detail.json` thêm field (sửa đúng field, không thay cả bộ mẫu vì id trong mẫu sinh lại sẽ đổi và test web đang dùng).
- **Ngoài phạm vi:** không đổi schema; không đổi cách chọn "hôm nay" khi không gửi `date` (vẫn theo timezone hiện tại của cửa hàng); không thêm timezone vào `/users/me` hay response cập nhật stop.
- **Acceptance criteria:**
  - `GET /driver/routes` và `GET /driver/routes/{routeId}` có `timezone` bằng timezone của plan; cửa hàng đổi timezone sau khi lưu plan thì tài xế vẫn thấy timezone của plan (giống dispatcher).
  - OpenAPI có `timezone` là `string` bắt buộc ở hai schema; `OpenApiNullabilityIntegrationTest` xanh với mẫu đã sửa.
  - Chỉ thêm field: test hiện có không phải sửa assertion cũ; `./mvnw test` xanh.

### T48. Tài xế đọc tuyến của plan đã huỷ nhận PLAN_NOT_ACTIVE (phát sinh 2026-11-08)
- **Tầng:** Must (trước M03) · **Ước lượng:** 0,25 ngày · **Quyết định:** P-36, P-51, P-52 · **FR:** D01 · **Lỗi:** F-53
- **Nguồn:** M02 (P-51) bắt tài xế chờ server và tự tải lại tuyến mỗi 30 giây để biết sớm plan đã bị huỷ. Nhưng `GET /driver/routes/{routeId}` của plan đã publish rồi bị huỷ trả 404 `NOT_FOUND`, giống tuyến của người khác, nên app chỉ nói được "Kế hoạch có thể đã bị huỷ". `PATCH …/status` đã trả 422 `PLAN_NOT_ACTIVE` cho đúng trường hợp này (P-36).
- **Phạm vi:**
  1. `GET /driver/routes/{routeId}`: route của tài xế (cùng store, cùng `driverId`), có ít nhất một stop, thuộc plan đã publish rồi bị huỷ → 422 `PLAN_NOT_ACTIVE`, cùng cách nhận biết "đã publish" với `PATCH` (P-36). Các trường hợp khác giữ 404 như cũ (plan chưa từng publish, tài xế khác, store khác, route không có stop).
  2. Test: route của plan đã huỷ sau publish → 422 `PLAN_NOT_ACTIVE`; plan DRAFT bị huỷ → 404; tài xế khác/store khác → 404 (tenant isolation giữ nguyên).
  3. OpenAPI (`scripts/sync_openapi.sh`) nếu mô tả lỗi thay đổi; sinh lại client web/mobile nếu snapshot đổi.
  4. Docs: `API_SPEC` §8 (dòng `GET /driver/routes/{routeId}` thêm 422 `PLAN_NOT_ACTIVE`, đoạn P-36 nói rõ áp dụng cho cả `GET`); F-53 trong `CURRENT_STATE`.
- **Ngoài phạm vi:** `GET /driver/routes` (danh sách) vẫn không có route của plan đã huỷ; không thêm thông báo đẩy; app đổi câu báo lỗi là việc của client (sau T48).
- **Acceptance criteria:**
  - Tài xế mở route của plan đã publish rồi bị huỷ → 422 `{code: PLAN_NOT_ACTIVE}`; route của plan chưa từng publish hay của người khác vẫn 404.
  - Test tenant isolation hiện có không phải sửa; `./mvnw test` xanh.
- **Phát sinh khi làm (2026-11-08):** P-36 (Approved) ghi "`GET` vẫn ẩn plan CANCELLED"; T48 thay phần đó nên ghi thành P-52. Test `routes_whenPlanDraftOrCancelled_hidden` kiểm đúng hành vi cũ (plan publish rồi huỷ → 404) nên được tách: phần plan huỷ khi còn nháp giữ 404, phần plan publish rồi huỷ thành test mới trả 422. Không đổi OpenAPI vì controller của tài xế không khai báo mã lỗi.

## 8. Should (làm nếu kịp, theo thứ tự)

### T32. Vị trí tài xế bằng polling
- **Ước lượng:** 1 ngày · **Quyết định:** D-08, D-26, P-15 · **FR:** D04
- **Phạm vi:** `POST /driver/location` nhận lô `points` (D-26); `DriverLocationService` đẩy vào danh sách `LocationSink`, MVP có `LastKnownLocationSink` (Redis TTL 10 phút, bỏ qua điểm cũ hơn); `GET /plans/{id}/driver-locations`; chỉ DRIVER đang có route trong plan PUBLISHED hoặc IN_PROGRESS mới hiển thị.
- **Acceptance criteria:** gửi lô 3 điểm lộn thứ tự → lưu điểm `recordedAt` mới nhất; lô sau có điểm cũ hơn điểm đang lưu → không ghi đè; vị trí cũ hơn 10 phút không trả về; DISPATCHER store khác → 404; validate lat/lon.
- **Phát sinh khi chuẩn bị (2026-11-10):** chủ dự án yêu cầu làm T32 cùng phần client (web W09, app M04 trong `docs/client/PLAN.md`) trên một branch `task/T32-driver-location`, mỗi phần một commit, một PR. Thiết kế đã duyệt trên prototype (artboard "Theo dõi — vị trí tài xế (T32)" và "Tài xế — chia sẻ vị trí (T32)"); quyết định P-53 (API) và P-54 (app). Phạm vi backend thêm:
  1. `POST /driver/location` (D-26) như trên, thêm: bỏ qua điểm có `recordedAt` muộn hơn giờ server quá 2 phút hoặc sớm hơn 10 phút (vẫn 204); TTL key = 10 phút tính từ `recordedAt` của điểm lưu; ghi "chỉ khi mới hơn" bằng Lua (atomic, như `ReserveThrottleSlot.lua`).
  2. `PUT /driver/location/sharing {routeId, state}` (P-53): 204; route không thấy được → 404; plan COMPLETED/CANCELLED → 422 `PLAN_NOT_ACTIVE`; Redis `driver:sharing:{storeId}:{driverId}` TTL 12 giờ.
  3. `GET /plans/{id}/driver-locations` (O, D): mỗi route có stop một phần tử `{routeId, driverId, location, sharing}` theo thứ tự `PlanDetail.routes`; plan không PUBLISHED/IN_PROGRESS → `[]`; plan store khác → 404.
  4. `PlanDetail.routes[].driver.phoneNumber` (E.164, `@Nullable`) lấy từ `StoreMemberQuery.findMembers`.
  5. Lớp: `DriverLocationController` (`/driver/location`), `DriverLocationService` → danh sách `LocationSink` (`LastKnownLocationSink`), Redis store `DriverLocationStore`/`LocationSharingStore` ở `planning/store/` (interface + `RedisXxx`, ARCHITECTURE §9). Không thêm `ErrorCode`, không migration.
  6. OpenAPI (`scripts/sync_openapi.sh`), sinh lại client web/mobile, mẫu `docs/ux/samples/plan-driver-locations.json` (+ `phoneNumber` trong mẫu plan detail).
- **Acceptance criteria bổ sung:** điểm ở tương lai quá 2 phút không lưu và không chặn điểm thật sau đó; `PUT sharing` route của tài xế khác hay store khác → 404, plan COMPLETED → 422; `driver-locations` trả `sharing` chỉ cho đúng route, `location: null` khi hết hạn; tenant isolation cho cả ba endpoint; `./mvnw test` xanh.

### T33. Turn-by-turn directions
- **Ước lượng:** 1,5 ngày · **Quyết định:** D-07 · **FR:** D05
- **Phạm vi:** `DirectionsProvider` dùng OSRM route (`steps=true`, `polyline6`); `GET /driver/routes/{routeId}/directions`; cache theo `routeId` và version của plan.
- **Acceptance criteria:** parse response mẫu; route có n stop → n+1 leg (về depot); provider lỗi → 502 (app fallback sang deep link).
- **Ghi chú từ T44:** `DirectionsResponse` ở API_SPEC §8 đang theo OSRM (`geometry` polyline6, `maneuver`/`modifier` theo từ vựng OSRM). Trước khi làm, chốt một dạng trung tính với provider (ghi rõ độ chính xác polyline, enum maneuver riêng), để đổi provider sau này không phải đổi contract; đây là quyết định cần chủ dự án duyệt.
- **Phát sinh khi làm (2026-11-13), P-57:** chủ dự án chọn contract trung tính (enum riêng + polyline6), tính đường **từ vị trí hiện tại** và app **dẫn đường trực tiếp** (client ở `docs/client/PLAN.md` M05). Phạm vi mới thay phạm vi cũ:
  - `routing.directions`: `DirectionsProvider.directions(from, to)` → `Directions` (khoảng cách, thời gian, polyline6, bước với `Maneuver` của PathWise); `OsrmDirectionsProvider` (`/route/v1/driving`, `overview=full`, `geometries=polyline6`, `steps=true`, timeout như matrix, đổi `type`/`modifier` của OSRM sang enum; `code` khác `Ok`, `NoRoute`, HTTP lỗi → `ROUTING_PROVIDER_ERROR`); chọn bằng `routing.directions.provider`, base URL `routing.directions.osrm.base-url` (mặc định bằng của matrix, cùng server tự host ở `compose.routing.yaml`).
  - `GET /driver/routes/{routeId}/directions?lat&lon[&toStopId]` trong `DriverRouteController`: tìm route như `GET /driver/routes/{id}` trong transaction chỉ đọc, gọi provider **ngoài** transaction; plan `COMPLETED`/huỷ sau publish → 422 `PLAN_NOT_ACTIVE`; rate limit `directions-user` 30/5 phút. **Không cache** (điểm đầu là vị trí hiện tại), bỏ phần "cache theo `routeId` và version".
  - Acceptance mới: parse response OSRM mẫu (cả vòng xoay, rẽ nhánh, đường không tên) ra đúng enum; đích mặc định là stop `PENDING` đầu tiên, `toStopId` lạ → 404, hết stop → điểm kết thúc; tenant isolation; 422 khi plan đã xong; 429 khi vượt giới hạn; provider lỗi → 502; không gọi provider bên trong transaction.
  - Ước lượng: 1,5 ngày (không đổi).

### T28. Chỉnh tay route sau optimize
- **Ước lượng:** 1,5 ngày · **FR:** P08 · **Quyết định:** D-24
- **Phạm vi:** `PUT /plans/{id}/routes`; tính lại ETA và quãng đường bằng matrix (không giải lại); trả `violations` (vượt capacity, trễ time window, quá ca).
- **Acceptance criteria:** chuyển stop gây vượt capacity → lưu được nhưng có violation `CAPACITY_EXCEEDED`; stopIds không thuộc plan → 422.
- **Phát sinh khi làm (2026-11-13), P-58:** chủ dự án chọn chỉ sửa plan DRAFT đã tối ưu, lưu cả khi vi phạm và cảnh báo (`PlanDetail.violations` luôn có, tính mỗi lần đọc; publish vẫn được, web hỏi xác nhận), web kéo thả bằng `@dnd-kit` sau port (`docs/client/PLAN.md` W10).
  - Phạm vi chốt: `PUT /plans/{id}/routes` (API_SPEC §7): kiểm tra như sửa draft (404 → `PLAN_NOT_EDITABLE` → `PLAN_BUSY` → `PLAN_MODIFIED` → `PLAN_NOT_OPTIMIZED`), rồi 400 validation (đủ mọi route, mỗi stop đúng một lần, stop chưa có toạ độ không vào route); transaction đọc → matrix ngoài transaction → transaction ghi có kiểm `version`; lịch mỗi route: rời điểm đầu lúc `shiftStart` hoặc muộn hơn để không chờ ở stop đầu, chờ tới đầu time window khi tới sớm. `violations` tính trong `PlanDetailAssembler` từ ETA, tải, giờ về, quãng đường đã lưu. Cột `manually_unassigned` (V6, DATA_MODEL §4) cho `dropReason = MANUAL`.
  - Acceptance chốt: chuyển stop làm vượt tải → 200, `violations` có `CAPACITY_EXCEEDED` với `excess`; đổi thứ tự làm trễ time window → `TIME_WINDOW_MISSED`; route về muộn hơn ca → `SHIFT_EXCEEDED`; ETA/quãng đường tính lại đúng với matrix giả; stop không thuộc plan, lặp hay thiếu → 400 với đúng field (thay "422" của bản cũ); plan PUBLISHED → 422; version cũ → 409; matrix lỗi → 502 và không đổi gì; tenant isolation; optimize lại xoá `MANUAL`; matrix không được gọi trong transaction.
- **Ghi chú từ T44:** hình dạng `violations[]` chưa có trong API_SPEC, chốt khi làm task này (đề xuất mỗi phần tử có `code`, `routeId`, `stopId?` và dữ liệu đo được như vượt bao nhiêu). `PUT /plans/{id}` tạo lại route nên `routeId` đổi sau mỗi lần sửa draft; request của task này dùng `routeId` lấy từ `PlanDetail` mới nhất.

## 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59)

Làm **A trước B**. A giữ an toàn dữ liệu và VPS (2 GB, chạy cả routing); B cho ADMIN thực quyền và trang thông số. Giai đoạn C (nhập điểm giao từ CSV/Excel, sao chép plan hôm trước, báo cáo ngày) để mốc sau. Mỗi task backend có ước lượng riêng; web của B là W11 (`docs/client/PLAN.md`).

| Task | Phụ thuộc | Song song được với |
|---|---|---|
| T50 | — | T51, T52 |
| T51, T52, T53, T54 | — | nhau |
| T59 | — | A |
| T55 | T59 | T53, T54 |
| T56, T57, T58 | T55 | nhau |
| T60 | T55 | T56–T58 |
| W11 | T55 (danh sách), T56–T58, T60 theo từng màn | — |

### Giai đoạn A: an toàn vận hành

#### T50. Giờ trong ngày không đổi theo múi giờ JVM (F-54)
- **Ước lượng:** 0,5–1 ngày · **Phụ thuộc:** —
- **Phạm vi:** `LocalTime` (ca `shift_start`/`shift_end` của route, time window `tw_start`/`tw_end` của stop) đọc/ghi không qua đổi múi giờ (kiểu JDBC/Hibernate không đổi zone cho `TIME`, hoặc converter); Flyway migration chuyển dữ liệu đã lưu lệch về giờ thật (lệch phụ thuộc múi giờ JVM lúc ghi: ghi lại cách xác định và giả định trong DATA_MODEL); test chạy với JVM ở hai múi giờ khác nhau (`-Duser.timezone`).
- **Acceptance:** ca 08:00 nằm trong DB là `08:00:00` (đọc bằng SQL); app chạy JVM UTC hay +07 đều đọc 08:00; dữ liệu cũ sau migration đúng giờ; mọi test cũ xanh.
- **Phát sinh khi làm (2026-11-14):** không làm migration chuyển dữ liệu: image app không đặt múi giờ nên JVM trong container chạy UTC, dữ liệu staging/prod vốn đúng và một migration dời giờ sẽ làm hỏng nó; chỉ DB dev ghi bởi app chạy từ IDE (JVM +07) bị lệch 8 giờ, README hướng dẫn tạo lại hoặc chạy câu SQL sửa (dời 8 giờ, quay vòng trong ngày). Cách sửa: `@JdbcType(LocalTimeJdbcType.class)` trên bốn cột giờ (bind `LocalTime` thẳng qua driver), giữ `hibernate.jdbc.time_zone: UTC` cho `Instant`. Test ép JVM `Asia/Ho_Chi_Minh` để CI (UTC) cũng bắt lỗi.

#### T51. Giới hạn log Docker
- **Ước lượng:** 0,25 ngày · **Phụ thuộc:** —
- **Phạm vi:** mọi service trong `compose.yaml`, `compose.prod.yaml`, `compose.routing.yaml`, `compose.web.yaml` có `logging: {driver: json-file, options: {max-size, max-file}}` (mặc định 10 MB × 3, chỉnh bằng biến); DEPLOYMENT ghi cách xem log cũ và dung lượng tối đa.
- **Acceptance:** `docker compose config` của mọi tổ hợp có `logging`; CI xanh.

#### T52. Backup MariaDB tự động, đẩy ra ngoài VPS
- **Ước lượng:** 1 ngày · **Phụ thuộc:** —
- **Phạm vi:** service `backup` trong file compose riêng (`compose.backup.yaml`, ghép bằng `-f`): mỗi đêm `mariadb-dump` nén, giữ 7 bản ngày + 4 bản tuần trên VPS, đẩy lên dịch vụ tương thích S3 (Cloudflare R2, Backblaze B2…) khi có biến `BACKUP_S3_*` (trống thì chỉ giữ trên VPS), mã hoá file bằng khoá trong `.env` (tuỳ chọn); script khôi phục và hướng dẫn tập khôi phục định kỳ vào DB tạm; log thành công/thất bại để T54 cảnh báo.
- **Acceptance:** chạy tay một lần: file xuất hiện trên VPS và trên S3 giả (MinIO trong test thủ công); khôi phục vào DB tạm và đếm bảng khớp; xoay vòng giữ đúng số bản.
- **Phát sinh khi làm (2026-11-14):** không build image riêng (VPS chỉ kéo image): `backup` dùng `mariadb:11.8` (có `mariadb-dump`, `openssl`, tzdata), `backup-offsite` dùng `rclone/rclone:1.71.2` với remote khai báo bằng biến môi trường; script trong `command` như `compose.routing.yaml`. Chép lên bucket bằng `rclone copy` (không xoá; luật hết hạn đặt trên bucket) để kẻ chiếm VPS không xoá được bản ngoài. Thêm `BACKUP_WEEKLY_DAY`, `.sha256` cho mỗi file. S3 giả dùng `rclone serve s3` vì image `minio/minio` không còn kéo được từ Docker Hub. Khôi phục là các lệnh trong DEPLOYMENT §5.3–5.4, không phải script riêng.

#### T53. Dọn dữ liệu cũ
- **Ước lượng:** 0,5 ngày · **Phụ thuộc:** —
- **Phạm vi:** job theo lịch (Spring `@Scheduled`, khoá Redis có TTL để chỉ một instance chạy): xoá refresh token hết hạn hoặc đã thu hồi quá 30 ngày, job nền đã kết thúc quá 30 ngày, lời mời hết hạn quá 90 ngày; số ngày cấu hình được; log số dòng đã xoá. Thay mục "job dọn refresh token hết hạn" ở §10.
- **Acceptance:** integration test: dữ liệu quá hạn bị xoá, dữ liệu còn hiệu lực giữ nguyên; hai instance chạy cùng lúc chỉ một lần xoá.
- **Phát sinh khi làm (2026-11-14):** khung chung ở `core.housekeeping` (khoá Redis, lịch 03:30 sau backup), mỗi module tự xoá bảng của mình qua interface `RetentionTask` (ARCHITECTURE §7.1) để `core` không phụ thuộc module nghiệp vụ. Lời mời `ACCEPTED` không xoá (lịch sử ai mời ai); lời mời `REVOKED` cũng xoá sau 90 ngày; job còn là `active_job_id` của plan được giữ (FK). Không thêm endpoint chạy tay: test gọi `HousekeepingService.run()`. Quy tắc giữ ở DATA_MODEL §6.

#### T54. Giám sát tối thiểu và cảnh báo
- **Ước lượng:** 1 ngày · **Phụ thuộc:** T52 (trạng thái backup)
- **Phạm vi:** không thêm Prometheus/Grafana (VPS 2 GB). (1) Health công khai đủ cho dịch vụ uptime bên ngoài (UptimeRobot/BetterStack, gói miễn phí) qua Caddy; hướng dẫn đăng ký. (2) Script cron trên host (trong repo, `deploy/monitor/`): đĩa > 85 %, RAM khả dụng thấp, container restart hoặc unhealthy, backup đêm qua thất bại → gửi email qua SMTP của app. (3) App log có cấu trúc khi gửi SMTP lỗi, OSRM/Nominatim lỗi, job lỗi, để T60 đếm.
- **Acceptance:** chạy script với ngưỡng thấp gửi được email (Mailpit ở dev); dừng container app thì script báo; DEPLOYMENT có mục giám sát.
- **Phát sinh khi làm (2026-11-14):** health công khai là `https://<domain>/actuator/health` (Caddy mở đúng path này). Script `deploy/monitor/pathwise-monitor.sh` (bash + curl + docker, không cài thêm gì) đọc cấu hình từ `.env` (biến môi trường thắng), gửi email bằng `curl` qua SMTP của app; mỗi sự cố báo một lần khi bắt đầu, nhắc lại sau `MONITOR_REMIND_HOURS` (6 giờ), báo khi hết (trạng thái ở `.monitor/`); thêm kiểm tra health của app và container tự khởi động lại; `--dry-run`, `--test-email`. Log lỗi qua interface `core.observability.FailureLog` (ARCHITECTURE §10) để T60 gắn bộ đếm vào cùng chỗ. SMTP chết thì script không gửi được: uptime bên ngoài là lớp báo thứ hai.

### Giai đoạn B: ADMIN có thực quyền

ADMIN **không xem nội dung** của cửa hàng (plan, xe, điểm giao, người nhận): chỉ số tổng và nhật ký (BR-01, P-59). Mọi endpoint ở `/api/v1/admin/**`, quyền **AD**; mọi hành động ghi audit log (T58).

#### T59. Tạo ADMIN không qua SQL
- **Ước lượng:** 0,5 ngày · **Phụ thuộc:** —
- **Phạm vi:** khi khởi động, nếu chưa có ADMIN nào và có biến `BOOTSTRAP_ADMIN_EMAIL` (+ mật khẩu lấy từ biến hoặc sinh ngẫu nhiên gửi qua email đặt lại mật khẩu) thì tạo một ADMIN đã xác thực; đã có ADMIN thì bỏ qua và log cảnh báo nếu biến còn đặt; không log mật khẩu. DEPLOYMENT hướng dẫn.
- **Acceptance:** lần đầu tạo đúng một ADMIN; khởi động lại không tạo thêm; ADMIN đăng nhập được.

#### T55. Danh sách và chi tiết cửa hàng; sửa danh sách user
- **Ước lượng:** 1 ngày · **Phụ thuộc:** T59
- **Phạm vi:** `GET /admin/stores` (phân trang, tìm theo tên/email chủ, lọc trạng thái, sort whitelist): tên, chủ (email), ngày tạo, trạng thái (ACTIVE/READ_ONLY), số thành viên theo vai trò, số xe, số plan, lần hoạt động gần nhất; `GET /admin/stores/{id}`: thêm số liệu 30 ngày (plan, điểm giao, tỉ lệ giao được), thành viên (email, vai trò, trạng thái; không số điện thoại); `GET /admin/users` trả đủ `storeId`, `status` (đóng lệch ở API_SPEC §0), lọc theo store/vai trò/trạng thái. Truy vấn tổng hợp, không trả nội dung plan.
- **Acceptance:** số liệu đúng trên dữ liệu test; OWNER/DISPATCHER/DRIVER → 403; không field nội dung plan/điểm giao nào trong response.

#### T56. Khoá cửa hàng (chỉ đọc), khoá user, buộc đăng xuất
- **Ước lượng:** 1–1,5 ngày · **Phụ thuộc:** T55
- **Phạm vi:** `POST /admin/stores/{id}/lock {reason}` / `unlock`: store **READ_ONLY** (P-59): thành viên vẫn đăng nhập và đọc; mọi request ghi của store (tạo/sửa/xoá xe, thành viên, lời mời, plan; geocode, optimize, publish, huỷ) → mã mới `STORE_READ_ONLY` (chủ dự án duyệt tên mã 2026-11-14; HTTP status chốt ở task, ghi API_SPEC); tài xế **vẫn báo kết quả và gửi vị trí** để giao nốt plan đang chạy; web/app hiện băng "Cửa hàng đang tạm khoá: {lý do}". `POST /admin/users/{id}/disable` / `enable` (ADMIN không khoá được chính mình hay ADMIN khác cuối cùng); `POST /admin/users/{id}/sessions/revoke` (thu hồi mọi refresh token). Kiểm tra chặn ghi đặt ở một chỗ (filter hoặc aspect theo endpoint), có test cho từng nhóm endpoint ghi.
- **Acceptance:** store khoá: đọc 200, ghi → mã khoá, tài xế báo kết quả 200; mở khoá ghi lại được; user bị khoá không refresh được, lần đăng nhập sau 403 `ACCOUNT_DISABLED`; audit log có đủ.

#### T57. Hỗ trợ tài khoản
- **Ước lượng:** 1 ngày · **Phụ thuộc:** T55
- **Phạm vi:** `POST /admin/users/{id}/verification-email` (gửi lại OTP xác thực, theo rate limit), `POST /admin/users/{id}/password-reset-email` (gửi email đặt lại mật khẩu như quên mật khẩu; ADMIN không thấy, không đặt mật khẩu), `POST /admin/stores/{id}/owner {userId}` (chuyển OWNER cho một thành viên ACTIVE đã xác thực; OWNER cũ thành DISPATCHER; khi chủ cũ mất email). Email báo cho người liên quan.
- **Acceptance:** email đúng người nhận (FakeEmailSender); chuyển chủ: đúng vai trò hai người, token cũ của họ hết hiệu lực vai trò (refresh lại ra vai trò mới); không chuyển cho user của store khác (404).

#### T58. Nhật ký thao tác (audit log)
- **Ước lượng:** 1–1,5 ngày · **Phụ thuộc:** T55 (dùng ở T56, T57; làm trước hoặc cùng T56)
- **Phạm vi:** bảng `audit_events` (migration mới, DATA_MODEL §4): thời điểm, người làm (userId, vai trò), storeId, hành động (enum), đối tượng (loại, id), chi tiết nhỏ (JSON, không dữ liệu cá nhân ngoài email), requestId, IP. Ghi: mọi hành động ADMIN; trong store: đổi vai trò/khoá thành viên, thu hồi lời mời, publish, huỷ plan, chỉnh tay tuyến. `GET /admin/audit` (lọc store, người, hành động, khoảng ngày); `GET /store/audit` cho OWNER (chỉ store mình, không thấy IP của ADMIN). Giữ 1 năm (T53 dọn).
- **Acceptance:** mỗi hành động trên ghi đúng một dòng trong cùng transaction với thay đổi; OWNER không thấy store khác; ghi không làm hỏng thao tác khi chi tiết quá lớn (cắt).

#### T60. Thông số nền tảng cho ADMIN
- **Ước lượng:** 1,5 ngày · **Phụ thuộc:** T55
- **Phạm vi:** `GET /admin/metrics/business?from&to` (theo ngày, múi giờ Asia/Ho_Chi_Minh): cửa hàng mới, cửa hàng hoạt động (có plan publish) 7/30 ngày, cửa hàng bị khoá, user theo vai trò, plan tạo/publish/hoàn thành, điểm giao và tỉ lệ giao được/không được/bỏ qua, số lần tối ưu và tỉ lệ lỗi. `GET /admin/metrics/technical`: từ Micrometer trong app (không cần Prometheus): request/phút, tỉ lệ 4xx/5xx, p95 thời gian phản hồi theo nhóm endpoint, job đang chạy/lỗi 24 giờ, lỗi OSRM/Nominatim/SMTP 24 giờ, số lần bị rate limit, heap JVM, số kết nối DB, dung lượng DB (bảng lớn nhất), tuổi bản backup gần nhất (T52). Số kỹ thuật là của instance đang chạy (reset khi khởi động lại, ghi rõ); số nghiệp vụ tính từ DB, cache Redis ngắn (có TTL).
- **Acceptance:** số nghiệp vụ đúng trên dữ liệu test; số kỹ thuật tăng sau khi gọi API và gây lỗi giả; chỉ ADMIN; response < 1 s với 100 store.

## 10. Sau MVP (Could, chưa lên lịch)

Giai đoạn C của M5: nhập điểm giao từ CSV/Excel, sao chép plan hôm trước, báo cáo ngày và xuất CSV (P-59) · Google provider (I-03) · realtime push (I-04) · optimize bất đồng bộ (FR-X03) · lịch sử vị trí · gợi ý địa điểm (I-01, cần phê duyệt) · thanh toán QR (I-02, cần phê duyệt).

## 11. Task đầu tiên: T00

T00 phải làm trước vì:

1. Working tree hiện **không compile** (F-00). Mọi agent khác bắt đầu sẽ gặp build đỏ.
2. Working tree đang chứa **lỗ hổng bảo mật** (F-01) do refactor dở. Nếu commit nguyên trạng, lỗ hổng sẽ vào lịch sử `develop`.
3. Chủ dự án đã quyết định tiếp tục hướng refactor này (D-13). T00 đóng nó lại thành một commit sạch, để T04 (đổi tên package, đụng mọi file auth) chạy trên nền ổn định.
4. Phạm vi nhỏ (1 ngày), không cần thêm quyết định kiến trúc, và acceptance criteria rõ ràng.

Lưu ý khi giao T00: các thay đổi chưa commit hiện chỉ có trên máy chủ dự án. Agent làm T00 phải làm việc **trên working tree hiện tại**, không checkout lại từ `develop`.
