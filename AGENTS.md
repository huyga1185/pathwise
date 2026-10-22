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
- Task chỉ xong khi `./mvnw test` xanh **và** đã làm đủ checklist bên dưới.

## Checklist bắt buộc trước khi kết thúc task / trước khi commit

Áp dụng cho **mọi** thay đổi code (kể cả fix nhỏ hoặc việc được yêu cầu ngoài plan). Docs phải được cập nhật **trong cùng commit/PR** với code. Đối chiếu bằng cách **đọc code thật** (grep mapping, xem DTO và migration), không dựa vào trí nhớ hay mô tả task.

1. **`docs/CURRENT_STATE.md`**
   - Đổi cột Trạng thái của mọi F-xx đã sửa hoặc thay đổi, kèm task ID. Không xoá dòng.
   - Thêm lỗi mới phát hiện thành F-xx mới, ghi mức bằng chứng (`[Verified-run]`, `[Verified-code]`, `[Risk]`).
   - Nếu build hoặc kết quả test thay đổi thì sửa §1 (dòng Build, Test, mô tả module bị ảnh hưởng).
   - Thêm một dòng vào §5 "Lịch sử cập nhật" (ngày, commit, task, thay đổi).
2. **`docs/API_SPEC.md`**: nếu đụng controller, DTO, `SecurityConfig`, `ErrorCode` hoặc handler lỗi.
   - Chạy `grep -rnE "@(Request|Get|Post|Put|Patch|Delete)Mapping" be/src/main/java` và so với spec.
   - Cập nhật cột Hiện trạng (✅ chỉ khi khớp spec **và** có test; còn lệch thì 🟡 kèm mô tả khác biệt cụ thể).
   - Cập nhật §0 "Tình trạng implementation" (prefix, format lỗi, mã HTTP, endpoint ngoài spec).
   - Khác spec có chủ đích thì sửa spec trong cùng PR và ghi lý do.
3. **`docs/DATA_MODEL.md`**: nếu có migration hoặc đổi entity/Redis key. Bảng hoặc cột phải khớp file migration; cập nhật bảng version ở §4.
4. **`docs/DECISIONS.md`**: nếu phát sinh quyết định mới. Ghi Proposed và hỏi chủ dự án; không tự đánh Approved.
5. **`docs/IMPLEMENTATION_PLAN.md`**: nếu phạm vi, dependency hoặc ước lượng của task thay đổi.
6. **Báo cáo cuối task** phải liệt kê từng file ở mục 1–5: đã cập nhật gì, hoặc "không cần vì …". Không được bỏ qua im lặng.

## Commit

- **Không** thêm attribution của AI vào commit hoặc PR (không có `Co-Authored-By: Claude…`, không có "Generated with…"). Author là chủ dự án.
- Index có thể đang chứa thay đổi do người khác stage sẵn. Commit bằng `git commit -- <danh sách file>` hoặc kiểm tra `git diff --cached --name-only` trước khi commit. Không commit file không thuộc task.
- Không push khi chủ dự án chưa yêu cầu.

## Quy ước code

- Code theo phong cách code xung quanh: Lombok (`@RequiredArgsConstructor`), record cho DTO, MapStruct cho mapping, Bean Validation với message là mã UPPER_SNAKE.
- Tên lớp: `XxxController`, `XxxService`, `XxxManager`, `XxxRepository`, `RedisXxxStore`, `XxxProvider`. Request/response dùng `CreateXxxRequest` / `XxxResponse`.
- Đơn vị: mét, giây, capacity là số nguyên. Timestamp dùng `Instant` UTC. Time window dùng `LocalTime` theo timezone của store.
- Git: mỗi task một branch `task/Txx-...` từ `develop`. Commit theo dạng `feat(module): ...`, `fix(module): ...`.
