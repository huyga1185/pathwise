# DATA_MODEL — Mô hình dữ liệu mục tiêu

> Hiện trạng: có `users`, `refresh_tokens` (`V1__baseline.sql`), `stores` cùng các cột tenancy của `users` (`V2__stores_and_user_tenancy.sql`, T13), `store_invitations` (`V3__store_invitations.sql`, T16), `vehicles` (`V4__vehicles.sql`, T20) và `background_jobs`, `route_plans`, `route_plan_routes`, `route_plan_stops`, `route_plan_stop_demands` (`V5__route_plans.sql`, T24). Mọi bảng mục tiêu đã tồn tại.
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

### 2.1 `users` — Modify (V2, đã tạo ở T13)

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

Index: `idx_users_store_role (store_id, role)`. Tên ràng buộc trong V2: FK `fk_users_store_id`, CHECK `chk_users_store_by_role`.

### 2.2 `stores` — New (V2, đã tạo ở T13)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(150) | NOT NULL |
| depot_address | VARCHAR(500) | NULL |
| depot_lat, depot_lon | DOUBLE | NULL (cả hai cùng NULL hoặc cùng có: CHECK `chk_stores_depot_coordinates`) |
| timezone | VARCHAR(50) | NOT NULL DEFAULT `'Asia/Ho_Chi_Minh'` |
| created_at, updated_at | DATETIME(6) | NOT NULL |

OWNER của store = user có `role='OWNER' AND store_id=?` (đúng 1, BR-03, kiểm tra ở service).

### 2.3 `refresh_tokens` — Keep + index (V2, đã tạo ở T13)

Giữ nguyên cột (timestamp đổi sang `DATETIME(6)`). Thêm `idx_refresh_tokens_user_state (user_id, state)`. `family_id` = session id công khai (P-07). Trạng thái `EXPIRED` được gán khi phát hiện token hết hạn lúc rotate (T10). Không có job dọn dẹp trong MVP (Could).

### 2.4 `store_invitations` — New (V3, đã tạo ở T16)

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

Index `(store_id, status)`, `(email, status)`. Tên ràng buộc trong V3: `uk_store_invitations_code_hash`, FK `fk_store_invitations_store_id`/`_invited_by`/`_accepted_user_id`, CHECK `chk_store_invitations_role`. Trạng thái `EXPIRED` không được ghi vào DB: lời mời `PENDING` có `expires_at` đã qua được báo là `EXPIRED` và không chấp nhận/huỷ được nữa (T16). Quy tắc: mời lại cùng email trong cùng store → lời mời PENDING cũ chuyển `REVOKED`. Email đã thuộc một user bất kỳ → 409 `EMAIL_EXISTED` (D-02).

### 2.5 `vehicles` — New (V4, đã tạo ở T20)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| name | VARCHAR(100) | NOT NULL; UNIQUE `(store_id, name)` |
| plate_number | VARCHAR(20) | NULL |
| capacity | INT | NOT NULL, CHECK > 0 |
| capacity_unit | VARCHAR(20) | NOT NULL — `KG, BOX, PACKAGE, ORDER` (D-36); không có CHECK danh sách để thêm loại không cần migration |
| active | BOOLEAN | NOT NULL DEFAULT TRUE |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Không xoá cứng xe đã dùng trong plan; chỉ `active=false`. Tên ràng buộc trong V4: FK `fk_vehicles_store_id`, UNIQUE `uk_vehicles_store_name`, CHECK `chk_vehicles_capacity`. Unique theo collation của cột (mặc định không phân biệt hoa thường).

### 2.6 `route_plans` — New (V5, đã tạo ở T24)

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| name | VARCHAR(150) | NOT NULL |
| plan_date | DATE | NOT NULL |
| status | VARCHAR(20) | NOT NULL — `DRAFT, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED`; chuyển theo `PlanLifecycle`, COMPLETED và CANCELLED là cuối cùng (P-34) |
| depot_address | VARCHAR(500) | NULL — snapshot |
| depot_lat, depot_lon | DOUBLE | NOT NULL — snapshot từ store hoặc ghi đè |
| timezone | VARCHAR(50) | NOT NULL — snapshot |
| processing_status | VARCHAR(20) | NOT NULL DEFAULT `IDLE`. Giá trị: `IDLE, GEOCODING, OPTIMIZING`. Khác `IDLE` thì plan không sửa, optimize hay publish được (409 `PLAN_BUSY`) (D-19). |
| active_job_id | BIGINT | NULL, FK background_jobs. Job đang chạy trên plan khi `processing_status` khác `IDLE`, NULL khi `IDLE` (T29). Loại khỏi optimistic lock: gắn job không tự tăng `version`. |
| optimized_at | DATETIME(6) | NULL. NULL nghĩa là chưa có kết quả hợp lệ. |
| total_distance_m | INT | NULL |
| total_duration_s | INT | NULL |
| created_by | BIGINT | NOT NULL, FK users |
| published_at, completed_at, cancelled_at | DATETIME(6) | NULL |
| version | BIGINT | NOT NULL DEFAULT 0 (`@Version`) |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(store_id, plan_date)`, `(store_id, status)`. Tên ràng buộc trong V5: FK `fk_route_plans_store_id`/`_active_job_id`/`_created_by`, index `idx_route_plans_store_date`/`_store_status`. Depot và timezone được snapshot mỗi lần lưu draft (`POST`, `PUT`): request không có `depot` thì lấy depot hiện tại của store (T24).

### 2.7 `route_plan_routes` — New (V5, đã tạo ở T24): phân công + kết quả

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| plan_id | BIGINT | NOT NULL, FK route_plans ON DELETE CASCADE |
| store_id | BIGINT | NOT NULL, FK |
| driver_id | BIGINT | NOT NULL, FK users; UNIQUE `(plan_id, driver_id)` |
| vehicle_id | BIGINT | NOT NULL, FK vehicles; UNIQUE `(plan_id, vehicle_id)` |
| vehicle_capacity | INT | NOT NULL — snapshot lúc tạo/sửa phân công |
| vehicle_capacity_unit | VARCHAR(20) | NOT NULL — snapshot cùng lúc (D-36) |
| shift_start, shift_end | TIME | NOT NULL; `shift_start < shift_end` |
| max_distance_m | INT | NULL |
| start_address, end_address | VARCHAR(500) | NULL (D-20) |
| start_lat, start_lon, end_lat, end_lon | DOUBLE | NOT NULL. Điểm đầu/cuối riêng của route; MVP luôn bằng depot của plan. Đây là điểm mở rộng cho nhiều kho/multi-depot (D-20). |
| planned_distance_m, planned_duration_s | INT | NULL — kết quả; 0 với route không được dùng (T25) |
| planned_start, planned_end | DATETIME(6) | NULL — kết quả; NULL với route không được dùng (T25) |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(driver_id)` cho truy vấn của tài xế. Tên ràng buộc trong V5: FK `fk_route_plan_routes_plan_id`/`_store_id`/`_driver_id`/`_vehicle_id`, UNIQUE `uk_route_plan_routes_plan_driver`/`_plan_vehicle`, CHECK `chk_route_plan_routes_shift` (`shift_start < shift_end`), `chk_route_plan_routes_max_distance` (`max_distance_m` NULL hoặc > 0, BR-09), `chk_route_plan_routes_capacity` (`vehicle_capacity > 0`). `PUT` plan xoá rồi tạo lại mọi route (id route đổi), vì giữ route rồi đổi xe giữa hai route sẽ vướng unique key giữa chừng (T24).

### 2.8 `route_plan_stops` — New (V5, đã tạo ở T24)

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
| ~~demand~~ | — | Bỏ theo D-36: demand theo loại nằm ở `route_plan_stop_demands` (§2.8a). |
| service_time_s | INT | NOT NULL DEFAULT 0, CHECK ≥ 0 |
| tw_start, tw_end | TIME | NULL; cả hai cùng NULL hoặc `tw_start < tw_end` |
| note | VARCHAR(500) | NULL |
| planned_arrival | DATETIME(6) | NULL — kết quả |
| status | VARCHAR(20) | NOT NULL DEFAULT `PENDING` — `PENDING, DELIVERED, FAILED, SKIPPED`; chỉ đi từ `PENDING`, không đảo ngược (P-34) |
| failure_reason | VARCHAR(255) | NULL — bắt buộc khi FAILED |
| status_updated_at | DATETIME(6) | NULL |
| status_updated_by | BIGINT | NULL, FK users |
| version | BIGINT | NOT NULL DEFAULT 0 |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(plan_id)`, `(route_id, sequence)`. Tên ràng buộc trong V5: FK `fk_route_plan_stops_plan_id`/`_store_id`/`_route_id`/`_status_updated_by`, CHECK `chk_route_plan_stops_location` (có địa chỉ hoặc toạ độ), `chk_route_plan_stops_coordinates` (`lat`, `lon` cùng NULL hoặc cùng có), `chk_route_plan_stops_service_time`, `chk_route_plan_stops_time_window`. `geocode_error` lưu tên enum `GeocodeError` (`NOT_FOUND`, `PROVIDER_ERROR`).

### 2.8a `route_plan_stop_demands` — New (V5, đã tạo ở T24) (D-36)

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| stop_id | BIGINT | NOT NULL, FK route_plan_stops ON DELETE CASCADE |
| unit | VARCHAR(20) | NOT NULL — `KG, BOX, PACKAGE` (`ORDER` không lưu: mỗi stop là 1) |
| quantity | INT | NOT NULL, CHECK ≥ 0 |

PK `(stop_id, unit)`. Không có dòng cho một loại nghĩa là demand loại đó bằng 0; service không ghi dòng có `quantity = 0`. Tên ràng buộc trong V5: `pk_route_plan_stop_demands`, FK `fk_route_plan_stop_demands_stop_id`, CHECK `chk_route_plan_stop_demands_quantity`.

### 2.9 `background_jobs` — New (V5, đã tạo ở T24) (D-19)

Bảng job dùng chung. MVP có 2 loại job: `GEOCODE_PLAN` (chạy nền) và `OPTIMIZE_PLAN` (chạy ngay trong request, nhưng vẫn ghi job).

| Cột | Kiểu | Ràng buộc / ghi chú |
|---|---|---|
| id | BIGINT | PK |
| store_id | BIGINT | NOT NULL, FK |
| type | VARCHAR(30) | NOT NULL — `GEOCODE_PLAN, OPTIMIZE_PLAN` |
| target_id | BIGINT | NOT NULL. Là plan id. Không có FK để bảng dùng chung cho mọi loại job. |
| status | VARCHAR(20) | NOT NULL — `QUEUED, RUNNING, SUCCEEDED, FAILED` (`CANCELLED` bỏ ở T44, P-32: không có gì huỷ job) |
| error_code | VARCHAR(50) | NULL. Khi `FAILED`: `INTERRUPTED`, `JOB_QUEUE_FULL`, `INTERNAL_ERROR` (hằng `job.model.JobErrors`), mã riêng của handler (`NO_SOLUTION`, T25, P-33) hoặc tên `ErrorCode`, ví dụ `GEOCODING_PROVIDER_ERROR`, `ROUTING_PROVIDER_ERROR`, `PLAN_MODIFIED` (T29, P-30). |
| attempts | INT | NOT NULL DEFAULT 0. Tăng mỗi lần job chuyển sang `RUNNING`. |
| created_by | BIGINT | NOT NULL, FK users |
| started_at, finished_at | DATETIME(6) | NULL |
| created_at, updated_at | DATETIME(6) | NOT NULL |

Index `(status, created_at)`, `(store_id, target_id)`. Tên ràng buộc trong V5: FK `fk_background_jobs_store_id`/`_created_by`, index `idx_background_jobs_status_created`/`_store_target`. Entity `job.model.BackgroundJob` và repository (module `job`; T24 đặt ở `core.job`, T29 tách ra, P-31); executor, `JobService`, khôi phục lúc khởi động (T29). Khi khởi động, job `QUEUED` cũng chuyển sang `FAILED/INTERRUPTED` như `RUNNING` (P-30). Khi ứng dụng khởi động, job đang `RUNNING` hoặc `QUEUED` được chuyển sang `FAILED` (`error_code = INTERRUPTED`) và plan tương ứng trả về `processing_status = IDLE`. MVP giả định chỉ chạy **một instance**; muốn chạy nhiều instance thì phải có cơ chế claim job (ghi vào Could).

## 3. Redis keys (mọi key có TTL)

| Key | Giá trị | TTL | Module |
|---|---|---|---|
| `otp:{purpose}:{userId}` | `{sha256}{số lần sai}`; `purpose` là tên `OtpPurpose` (`VERIFY_EMAIL`, `RESET_PASSWORD`, `CHANGE_PASSWORD`) | `otp.ttl` (mặc định 5 phút) | auth |
| `otp:cooldown:{purpose}:{userId}` | `1`; bị xoá cùng OTP khi email gửi lỗi | `otp.cooldown` (mặc định 60 s) | auth |
| `otpToken:{purpose}:{ticket}` | `{userId}:{sha256(secret)}`; `purpose` là `RESET_PASSWORD` (T14; `updateUserPassword` của luồng đổi mật khẩu cũ đã bỏ ở T15) | 10 phút | auth |
| `ratelimit:{scope}:{key}:{window}` | counter | = window | core |
| `geocode:nominatim:{sha256(normalizedQuery + "|" + limit)}` | JSON danh sách `{displayName, lat, lon}` (cả danh sách rỗng); query chuẩn hoá = trim, gộp khoảng trắng, chữ thường | `routing.geocoding.cache-ttl` (24 h) | routing |
| `geocode:nominatim:next-slot` | thời điểm (ms, đồng hồ Redis) slot trống kế tiếp; Lua `ReserveThrottleSlot.lua` đặt slot theo thứ tự đến (P-11) | hết khi slot cuối đã đặt kết thúc (`chờ + 1000 / per-second` ms) | routing |
| `driver:location:{storeId}:{driverId}` | JSON vị trí | 10 phút | planning |
| `directions:route:{routeId}:{planVersion}` (Should) | JSON geometry/steps | 24 h | planning |

Key OTP dùng **userId** (T12), không dùng email. OTP bị xoá khi đúng (dùng một lần) hoặc khi sai đủ `otp.max-attempts` (mặc định 3) lần.

## 4. Migration (Flyway, P-01)

Version được cấp trước — agent **không** tự chọn số khác:

| Version | File | Task | Nội dung |
|---|---|---|---|
| V1 | `V1__baseline.sql` | T02 ✓ | Nội dung `be/db/db.sql` (bỏ `CREATE DATABASE`/`USE`). `baseline-on-migrate` cho DB tạo trước từ `db.sql`. **Đã tạo.** |
| V2 | `V2__stores_and_user_tenancy.sql` | T13 ✓ | Bảng `stores`; cột mới của `users`; không chuyển dữ liệu (§5, D-29); index refresh_tokens; đổi timestamp sang `DATETIME(6)`. **Đã tạo.** |
| V3 | `V3__store_invitations.sql` | T16 ✓ | Bảng `store_invitations`. **Đã tạo.** |
| V4 | `V4__vehicles.sql` | T20 ✓ | Bảng `vehicles`. **Đã tạo.** |
| V5 | `V5__route_plans.sql` | T24 ✓ | `background_jobs`, `route_plans`, `route_plan_routes`, `route_plan_stops`, `route_plan_stop_demands` (D-36). **Đã tạo.** |

Task phát sinh cần migration mới lấy số tiếp theo **sau khi** cập nhật bảng này trong cùng PR.

## 5. Dữ liệu cũ (D-29)

- **Không chuyển dữ liệu.** Trước khi chạy V2, dev xoá DB local (`DROP DATABASE pathwise; CREATE DATABASE pathwise;`) để Flyway chạy lại từ V1.
- V2 thêm các cột NOT NULL (`status`...) mà không đặt giá trị mặc định cho dữ liệu cũ. Nếu bảng `users` còn dữ liệu thì migration fail, coi như tín hiệu nhắc phải reset DB.
- Testcontainers và CI luôn bắt đầu từ DB trống nên không bị ảnh hưởng.
- ADMIN đầu tiên được tạo bằng script seed thủ công. T13 cung cấp file `be/db/seed-admin.example.sql`, không chạy tự động.
