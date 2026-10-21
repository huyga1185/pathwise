# AGENTS.md — Quy tắc cho coding agent

PathWise là backend Spring Boot 4.1 / Java 21 (`be/`) cho SaaS lập tuyến giao hàng đa tenant (store, OWNER/DISPATCHER/DRIVER).

## Đọc trước khi làm

1. Task của bạn trong `docs/IMPLEMENTATION_PLAN.md`: phạm vi, ngoài phạm vi, acceptance criteria.
2. `docs/ARCHITECTURE.md`: quy chuẩn bắt buộc.
3. Phần liên quan trong `docs/API_SPEC.md`, `docs/DATA_MODEL.md`, `docs/REQUIREMENTS.md`.
4. `docs/DECISIONS.md`: chỉ coi mục **Approved** là chốt. **Proposed** chỉ áp dụng khi task tham chiếu tới nó. **Idea** (I-xx) thì không triển khai.
5. `docs/CURRENT_STATE.md`: lỗi đã biết (F-xx). Đừng "sửa tiện tay" một F-xx thuộc task khác.

## Quy tắc cứng

- Chỉ làm trong phạm vi task. Gặp quyết định kiến trúc hoặc nghiệp vụ chưa có trong docs thì **dừng lại và hỏi**, không tự chọn.
- Phụ thuộc giữa module theo đúng sơ đồ ở `ARCHITECTURE.md` §2.1. Không inject repository, entity hay `infrastructure` của module khác.
- Controller không chứa business logic. `@Transactional` chỉ đặt ở service. Không gọi SMTP, OSRM hay Nominatim bên trong transaction DB.
- **Tenant isolation:** mọi truy vấn dữ liệu thuộc store phải lọc theo `actor.storeId()` lấy từ token, không bao giờ lấy từ request. Thuộc store khác thì trả 404. Mỗi endpoint store-scoped mới cần ít nhất một test tenant isolation.
- Lỗi nghiệp vụ dùng `throw new AppException(ErrorCode.X)`. `ErrorCode` chỉ được **thêm** giá trị. Body lỗi theo `API_SPEC.md` §1.
- Schema chỉ thay đổi qua Flyway, dùng đúng số version đã được cấp trong `DATA_MODEL.md` §4. Không sửa migration đã merge.
- Mọi key Redis phải có TTL.
- Không log mật khẩu, OTP, token, ticket, secret. Không dùng `System.out`.
- Không commit secret. `application-dev.yaml` đã được gitignore.
- Không thêm dependency khi task không cho phép.
- File nóng (`SecurityConfig`, `ErrorCode`, `GlobalExceptionHandler`, `AuthController`, `AuthService`, `pom.xml`, `application*.yaml`, `.github/**`, `compose*.yaml`, `be/Dockerfile`) chỉ sửa khi task yêu cầu. Sửa thì rebase trước khi mở PR.

## Build và test

```bash
docker compose up -d                          # hạ tầng: MariaDB, Redis, Mailpit (UI http://localhost:8025)
cd be && ./mvnw test                          # từ T01 trở đi chỉ cần Docker (Testcontainers)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
docker compose --profile app up -d --build    # chạy toàn bộ stack trong container
```

- Đổi `Dockerfile`, `compose*.yaml` hoặc `.github/**` thì phải giữ cho cả hai cách chạy hoạt động, và CI (`ci.yml`) phải xanh.

- Không test nào được gọi dịch vụ ngoài thật. Dùng `MockRestServiceServer` hoặc fake provider, và `FakeEmailSender`.
- Tên test theo dạng `method_whenCondition_expectedResult`. Sửa bug thì kèm test tái hiện bug đó.
- Task chỉ xong khi `./mvnw test` xanh **và** đã cập nhật docs theo Definition of Done (`IMPLEMENTATION_PLAN.md` §0).

## Quy ước code

- Code theo phong cách code xung quanh: Lombok (`@RequiredArgsConstructor`), record cho DTO, MapStruct cho mapping, Bean Validation với message là mã UPPER_SNAKE.
- Tên lớp: `XxxController`, `XxxService`, `XxxManager`, `XxxRepository`, `RedisXxxStore`, `XxxProvider`. Request/response dùng `CreateXxxRequest` / `XxxResponse`.
- Đơn vị: mét, giây, capacity là số nguyên. Timestamp dùng `Instant` UTC. Time window dùng `LocalTime` theo timezone của store.
- Git: mỗi task một branch `task/Txx-...` từ `develop`. Commit theo dạng `feat(module): ...`, `fix(module): ...`.
