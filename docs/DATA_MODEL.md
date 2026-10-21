# DATA_MODEL — Mô hình dữ liệu mục tiêu

> Hiện trạng: chỉ có `users`, `refresh_tokens` (`be/db/db.sql`). Mọi bảng khác là **mục tiêu**, chưa tồn tại.
> Dựa trên D-01..D-05, D-15, D-16, D-18..D-21, P-01, P-07, P-14 (đã Approved). Dữ liệu cũ không được giữ (D-29).
> Quy ước: `BIGINT AUTO_INCREMENT` id; timestamp sự kiện `DATETIME(6)` lưu UTC (`hibernate.jdbc.time_zone: UTC`, Java `Instant`); toạ độ `DOUBLE`; khoảng cách mét `INT`, thời lượng giây `INT`; enum lưu `VARCHAR` (`@Enumerated(STRING)`); bảng thuộc store luôn có `store_id NOT NULL` + FK + index.

## 1. Sơ đồ quan hệ

```
stores 1───* users (role ≠ ADMIN)
stores 1───* store_invitations
stores 1───* vehicles
stores 1───* route_plans 1───* route_plan_routes *───1 users(DRIVER)
                        │                     *───1 vehicles
                        └───* route_plan_stops *───0..1 route_plan_routes
users  1───* refresh_tokens
```

## 2. Bảng

### 2.1 `users` — Modify (V2)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| id | BIGINT | PK | có sẵn |
| email | VARCHAR(255) | NOT NULL, UNIQUE | lưu lowercase (P-19) |
| password | VARCHAR(255) | NOT NULL | BCrypt |
| full_name | VARCHAR(100) | NULL | **mới** |
| phone_number | VARCHAR(20) | NULL | E.164 |
| role | VARCHAR(25) | NOT NULL | `ADMIN, OWNER, DISPATCHER, DRIVER` (bỏ `USER`) |
| status | VARCHAR(30) | NOT NULL | **mới**: `PENDING_VERIFICATION, ACTIVE, DISABLED` |
| store_id | BIGINT | NULL, FK stores(id) | **mới**; CHECK `(role = 'ADMIN' AND store_id IS NULL) OR (role <> 'ADMIN' AND store_id IS NOT NULL)` |
| email_verified_at | DATETIME(6) | NULL | **mới** |
| can_drive | BOOLEAN | NOT NULL DEFAULT FALSE | **mới** (D-21). Với role DRIVER luôn được coi là TRUE; OWNER/DISPATCHER có cờ này thì được gán route. |
| created_at, updated_at | DATETIME(6) | NOT NULL | có sẵn (đổi sang DATETIME(6)) |

Index: `idx_users_store_role (store_id, role)`.

### 2.2 `stores` — New (V2)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(150) | NOT NULL |
| depot_address | VARCHAR(500) | NULL |
| depot_lat, depot_lon | DOUBLE | NULL (cả hai cùng NULL hoặc cùng có) |
| timezone | VARCHAR(50) | NOT NULL DEFAULT `'Asia/Ho_Chi_Minh'` |
| created_at, updated_at | DATETIME(6) | NOT NULL |

OWNER của store = user có `role='OWNER' AND store_id=?` (đúng 1, BR-03, kiểm tra ở service).

### 2.3 `refresh_tokens` — Keep + index (V2)

Giữ nguyên cột. Thêm `idx_refresh_tokens_user_state (user_id, state)`. `family_id` = session id công khai (P-07). Trạng thái `EXPIRED` được gán khi phát hiện token hết hạn lúc rotate (T10). Không có job dọn dẹp trong MVP (Could).

### 2.4 `store_invitations` — New (V3)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| email | VARCHAR(255) | NOT NULL (lowercase) |
| role | VARCHAR(25) | NOT NULL — `DISPATCHER` hoặc `DRIVER` |
| code_hash | CHAR(64) | NOT NULL, UNIQUE (SHA-256 của mã mời) |
| status | VARCHAR(20) | NOT NULL — `PENDING, ACCEPTED, REVOKED, EXPIRED` |
| invited_by | BIGINT | NOT NULL, FK users |
| accepted_user_id | BIGINT | NULL, FK users |
| expires_at | DATETIME(6) | NOT NULL |
| accepted_at | DATETIME(6) | NULL |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(store_id, status)`, `(email, status)`. Quy tắc: mời lại cùng email trong cùng store → lời mời PENDING cũ chuyển `REVOKED`. Email đã thuộc một user bất kỳ → 409 `EMAIL_EXISTED` (D-02).

### 2.5 `vehicles` — New (V4)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| name | VARCHAR(100) | NOT NULL; UNIQUE `(store_id, name)` |
| plate_number | VARCHAR(20) | NULL |
| capacity | INT | NOT NULL, CHECK > 0 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Không xoá cứng xe đã dùng trong plan; chỉ `active=false`.

### 2.6 `route_plans` — New (V5)

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| name | VARCHAR(150) | NOT NULL |
| plan_date | DATE | NOT NULL |
| status | VARCHAR(20) | NOT NULL — `DRAFT, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED` |
| depot_address | VARCHAR(500) | NULL — snapshot |
| depot_lat, depot_lon | DOUBLE | NOT NULL — snapshot từ store hoặc ghi đè |
| timezone | VARCHAR(50) | NOT NULL — snapshot |
| processing_status | VARCHAR(20) | NOT NULL DEFAULT `IDLE`. Giá trị: `IDLE, GEOCODING, OPTIMIZING`. Khác `IDLE` thì plan không sửa, optimize hay publish được (409 `PLAN_BUSY`) (D-19). |
| active_job_id | BIGINT | NULL, FK background_jobs |
| optimized_at | DATETIME(6) | NULL. NULL nghĩa là chưa có kết quả hợp lệ. |
| total_distance_m | INT | NULL |
| total_duration_s | INT | NULL |
| created_by | BIGINT | NOT NULL, FK users |
| published_at, completed_at, cancelled_at | DATETIME(6) | NULL |
| version | BIGINT | NOT NULL DEFAULT 0 (`@Version`) |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(store_id, plan_date)`, `(store_id, status)`.

### 2.7 `route_plan_routes` — New (V5): phân công + kết quả

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| plan_id | BIGINT | NOT NULL, FK route_plans ON DELETE CASCADE |
| store_id | BIGINT | NOT NULL, FK |
| driver_id | BIGINT | NOT NULL, FK users; UNIQUE `(plan_id, driver_id)` |
| vehicle_id | BIGINT | NOT NULL, FK vehicles; UNIQUE `(plan_id, vehicle_id)` |
| vehicle_capacity | INT | NOT NULL — snapshot lúc tạo/sửa phân công |
| shift_start, shift_end | TIME | NOT NULL; `shift_start < shift_end` |
| max_distance_m | INT | NULL |
| start_address, end_address | VARCHAR(500) | NULL (D-20) |
| start_lat, start_lon, end_lat, end_lon | DOUBLE | NOT NULL. Điểm đầu/cuối riêng của route; MVP luôn bằng depot của plan. Đây là điểm mở rộng cho nhiều kho/multi-depot (D-20). |
| planned_distance_m, planned_duration_s | INT | NULL — kết quả |
| planned_start, planned_end | DATETIME(6) | NULL — kết quả |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(driver_id)` cho truy vấn của tài xế.

### 2.8 `route_plan_stops` — New (V5)

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| plan_id | BIGINT | NOT NULL, FK route_plans ON DELETE CASCADE |
| store_id | BIGINT | NOT NULL, FK |
| route_id | BIGINT | NULL, FK route_plan_routes ON DELETE SET NULL — NULL = chưa gán/bị drop |
| sequence | INT | NULL — thứ tự trong route (1..n) |
| label | VARCHAR(150) | NOT NULL — tên điểm/mã đơn do dispatcher nhập |
| recipient_name | VARCHAR(100) | NULL |
| recipient_phone | VARCHAR(20) | NULL — E.164 |
| address_text | VARCHAR(500) | NULL. CHECK: `address_text` và (`lat`, `lon`) không được cùng NULL. |
| lat, lon | DOUBLE | NULL cho tới khi geocode xong hoặc dispatcher nhập (D-18) |
| geocode_status | VARCHAR(20) | NOT NULL. Giá trị: `PROVIDED` (client gửi toạ độ), `PENDING`, `RESOLVED`, `FAILED`. |
| geocode_error | VARCHAR(50) | NULL. Ví dụ `NOT_FOUND`, `PROVIDER_ERROR`. |
| geocoded_display_name | VARCHAR(500) | NULL. Tên địa chỉ provider trả về, để dispatcher kiểm tra độ chính xác. |
| demand | INT | NOT NULL DEFAULT 0, CHECK ≥ 0 |
| service_time_s | INT | NOT NULL DEFAULT 0, CHECK ≥ 0 |
| tw_start, tw_end | TIME | NULL; cả hai cùng NULL hoặc `tw_start < tw_end` |
| note | VARCHAR(500) | NULL |
| planned_arrival | DATETIME(6) | NULL — kết quả |
| status | VARCHAR(20) | NOT NULL DEFAULT `PENDING` — `PENDING, DELIVERED, FAILED, SKIPPED` |
| failure_reason | VARCHAR(255) | NULL — bắt buộc khi FAILED |
| status_updated_at | DATETIME(6) | NULL |
| status_updated_by | BIGINT | NULL, FK users |
| version | BIGINT | NOT NULL DEFAULT 0 |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(plan_id)`, `(route_id, sequence)`.

### 2.9 `background_jobs` — New (V5) (D-19)

Bảng job dùng chung. MVP có 2 loại job: `GEOCODE_PLAN` (chạy nền) và `OPTIMIZE_PLAN` (chạy ngay trong request, nhưng vẫn ghi job).

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| type | VARCHAR(30) | NOT NULL — `GEOCODE_PLAN, OPTIMIZE_PLAN` |
| target_id | BIGINT | NOT NULL. Là plan id. |
| status | VARCHAR(20) | NOT NULL — `QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED` |
| error_code | VARCHAR(50) | NULL |
| attempts | INT | NOT NULL DEFAULT 0 |
| created_by | BIGINT | NOT NULL, FK users |
| started_at, finished_at | DATETIME(6) | NULL |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(status, created_at)`, `(store_id, target_id)`. Khi ứng dụng khởi động, job đang `RUNNING` được chuyển sang `FAILED` (`error_code = INTERRUPTED`) và plan tương ứng trả về `processing_status = IDLE`. MVP giả định chỉ chạy **một instance**; muốn chạy nhiều instance thì phải có cơ chế claim job (ghi vào Could).

## 3. Redis keys (mọi key có TTL)

| Key | Giá trị | TTL | Module |
|---|---|---|---|
| `otp:{purpose}:{userId}` | `{sha256}{attempts}` (định dạng hiện tại của Lua) | `otp.ttl` (300 s) | auth |
| `otp:cooldown:{purpose}:{userId}` | `1` | `otp.cooldown` (60 s) | auth |
| `otpToken:{purpose}:{ticket}` | `{userId}:{sha256(secret)}` | 10 phút | auth |
| `ratelimit:{scope}:{key}:{window}` | counter | = window | core |
| `geocode:nominatim:{sha256(normalizedQuery)}` | JSON kết quả | 24 h | routing |
| `geocode:nominatim:lock` | token rate limit 1 req/s | 1 s | routing |
| `driver:location:{storeId}:{driverId}` | JSON vị trí | 10 phút | planning |
| `directions:route:{routeId}:{planVersion}` (Should) | JSON geometry/steps | 24 h | planning |

Lưu ý: key OTP hiện tại dùng **email** làm identifier (bản refactor); mục tiêu dùng **userId** (T12).

## 4. Migration (Flyway, P-01)

Version được cấp trước — agent **không** tự chọn số khác:

| Version | File | Task | Nội dung |
|---|---|---|---|
| V1 | `V1__baseline.sql` | T02 | Nội dung `be/db/db.sql` hiện tại (bỏ `CREATE DATABASE`/`USE`). `baseline-on-migrate` cho DB dev đã có. |
| V2 | `V2__stores_and_user_tenancy.sql` | T13 | Bảng `stores`; cột mới của `users`; chuyển dữ liệu (§5); index refresh_tokens; đổi timestamp sang `DATETIME(6)`. |
| V3 | `V3__store_invitations.sql` | T16 | Bảng `store_invitations`. |
| V4 | `V4__vehicles.sql` | T20 | Bảng `vehicles`. |
| V5 | `V5__route_plans.sql` | T24 | `background_jobs`, `route_plans`, `route_plan_routes`, `route_plan_stops`. |

Task phát sinh cần migration mới lấy số tiếp theo **sau khi** cập nhật bảng này trong cùng PR.

## 5. Dữ liệu cũ (D-29)

- **Không chuyển dữ liệu.** Trước khi chạy V2, dev xoá DB local (`DROP DATABASE pathwise; CREATE DATABASE pathwise;`) để Flyway chạy lại từ V1.
- V2 thêm các cột NOT NULL (`status`...) mà không đặt giá trị mặc định cho dữ liệu cũ. Nếu bảng `users` còn dữ liệu thì migration fail, coi như tín hiệu nhắc phải reset DB.
- Testcontainers và CI luôn bắt đầu từ DB trống nên không bị ảnh hưởng.
- ADMIN đầu tiên được tạo bằng script seed thủ công. T13 cung cấp file `be/db/seed-admin.example.sql`, không chạy tự động.
