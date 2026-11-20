# IMPLEMENTATION_PLAN — Kế hoạch triển khai backend

> Phạm vi: backend (D-09). Tham chiếu: `REQUIREMENTS.md` (FR/BR), `ARCHITECTURE.md`, `DATA_MODEL.md`, `API_SPEC.md`, `DECISIONS.md`, `CURRENT_STATE.md` (F-xx).
>
> **Client (web, app tài xế):** kế hoạch riêng ở `docs/client/PLAN.md`, quy tắc ở `docs/client/README.md` (P-40). Thiếu API cho client thì thêm task backend (số T tiếp theo) ở đây.
>
> Các task T00–T63 **đã xong**; đặc tả đầy đủ (phạm vi, acceptance, "Phát sinh khi làm", lộ trình và phụ thuộc) ở `docs/archive/IMPLEMENTATION_PLAN.md`, cùng số mục. Task mới viết đầy đủ ở §2 file này; xong thì chuyển nguyên mục sang file lưu trữ và thêm một dòng vào §1.

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

## 1. Task đã xong

| Task | Tên | Mục trong `docs/archive/IMPLEMENTATION_PLAN.md` |
|---|---|---|
| T00 | Hoàn thiện refactor đang dở và khôi phục build xanh | 2. Phase M0: Nền tảng (Must) |
| T04 | Đổi tên package `auth.infastructure` thành `auth.infrastructure` | 2. Phase M0: Nền tảng (Must) |
| T02 | Thêm Flyway và baseline migration | 2. Phase M0: Nền tảng (Must) |
| T01 | Hạ tầng test và CI | 2. Phase M0: Nền tảng (Must) |
| T03 | Chuẩn hoá xử lý lỗi | 2. Phase M0: Nền tảng (Must) |
| T05 | Container hoá: Dockerfile multi-stage và compose | 2. Phase M0: Nền tảng (Must) |
| T06 | CI và kiểm tra chất lượng | 2. Phase M0: Nền tảng (Must) |
| T07 | Publish image và runbook deploy thủ công | 2. Phase M0: Nền tảng (Must) |
| T08 | Profile `routing`: OSRM và Nominatim self-host | 2. Phase M0: Nền tảng (Must) |
| T10 | Refresh token, cookie và logout (web/mobile) | 3. Phase M1: Auth và tenancy |
| T11 | Quản lý session | 3. Phase M1: Auth và tenancy |
| T12 | Gộp OTP và tách module notification | 3. Phase M1: Auth và tenancy |
| T13 | Store, tenancy, đăng ký và xác thực email | 3. Phase M1: Auth và tenancy |
| T17 | Authorization và tenant isolation | 3. Phase M1: Auth và tenancy |
| T14 | Quên mật khẩu | 3. Phase M1: Auth và tenancy |
| T15 | Đổi mật khẩu bằng mật khẩu cũ + OTP | 3. Phase M1: Auth và tenancy |
| T16 | Lời mời và quản lý thành viên | 3. Phase M1: Auth và tenancy |
| T21 | Endpoint geocoding search | 4. Track B: Routing adapters (Must, chạy song song với M1) |
| T22 | Matrix provider trả cả distance và duration | 4. Track B: Routing adapters (Must, chạy song song với M1) |
| T23 | Solver VRPTW | 4. Track B: Routing adapters (Must, chạy song song với M1) |
| T20 | Xe | 6. Phase M2: Planning |
| T24 | Plan draft: schema và CRUD | 6. Phase M2: Planning |
| T29 | Job framework và geocode chạy nền | 6. Phase M2: Planning |
| T44 | Làm sạch API contract (phát sinh 2026-10-29) | 6. Phase M2: Planning |
| T25 | Optimize plan | 6. Phase M2: Planning |
| T26 | Publish, cancel và vòng đời trạng thái | 6. Phase M2: Planning |
| T27 | Xoá API route optimizer cũ | 6. Phase M2: Planning |
| T30 | API tài xế: xem route | 7. Phase M3: Driver và hardening |
| T31 | Tài xế cập nhật trạng thái stop | 7. Phase M3: Driver và hardening |
| T40 | Rate limiting | 7. Phase M3: Driver và hardening |
| T41 | Hardening cấu hình | 7. Phase M3: Driver và hardening |
| T43 | Kiểm thử nghiệm thu MVP | 7. Phase M3: Driver và hardening |
| T45 | Bổ sung field cho client (phát sinh 2026-11-03) | 7. Phase M3: Driver và hardening |
| T46 | OpenAPI mô tả đúng response, mã thành công và PATCH (phát sinh 2026-11-04) | 7. Phase M3: Driver và hardening |
| T47 | Timezone cho màn của tài xế (phát sinh 2026-11-05) | 7. Phase M3: Driver và hardening |
| T48 | Tài xế đọc tuyến của plan đã huỷ nhận PLAN_NOT_ACTIVE (phát sinh 2026-11-08) | 7. Phase M3: Driver và hardening |
| T32 | Vị trí tài xế bằng polling | 8. Should (làm nếu kịp, theo thứ tự) |
| T33 | Turn-by-turn directions | 8. Should (làm nếu kịp, theo thứ tự) |
| T28 | Chỉnh tay route sau optimize | 8. Should (làm nếu kịp, theo thứ tự) |
| T50 | Giờ trong ngày không đổi theo múi giờ JVM (F-54) | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T51 | Giới hạn log Docker | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T52 | Backup MariaDB tự động, đẩy ra ngoài VPS | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T53 | Dọn dữ liệu cũ | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T54 | Giám sát tối thiểu và cảnh báo | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T59 | Tạo ADMIN không qua SQL | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T55 | Danh sách và chi tiết cửa hàng; sửa danh sách user | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T56 | Khoá cửa hàng (chỉ đọc), khoá user, buộc đăng xuất | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T57 | Hỗ trợ tài khoản | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T58 | Nhật ký thao tác (audit log) | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T60 | Thông số nền tảng cho ADMIN | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T61 | Nhập điểm giao từ file CSV/Excel | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T62 | Sao chép plan | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |
| T63 | Báo cáo và xuất CSV | 9. Phase M5: Vận hành và quản trị (chủ dự án duyệt 2026-11-14, P-59) |

## 2. Task đang mở

Không có.

## 3. Sau MVP (Could, chưa lên lịch)

Google provider (I-03) · realtime push (I-04) · optimize bất đồng bộ (FR-X03) · lịch sử vị trí · gợi ý địa điểm (I-01, cần phê duyệt) · thanh toán QR (I-02, cần phê duyệt).
