# CLIENT PLAN — Kế hoạch triển khai web và app tài xế

> Quy tắc làm việc: `docs/client/README.md`. Kiến trúc: `docs/client/ARCHITECTURE.md`. Màn hình: `docs/client/SCREENS.md`. Quyết định: `docs/DECISIONS.md` (P-40 trở đi).
>
> Kế hoạch này **không** sửa backend; thiếu gì ở API thì tạo task backend ở `docs/IMPLEMENTATION_PLAN.md`. Mỗi task một branch từ `develop` (`task/W01-...`, `task/M00-...`; việc ngoài plan dùng `feat/...`, `fix/...`, `docs/CONTRIBUTING.md` §1), một PR vào `develop`.
>
> Các task dưới đây **đã xong**; đặc tả đầy đủ (thứ tự, phụ thuộc, phạm vi, acceptance) ở `docs/archive/client/PLAN.md`, cùng số mục. Thiết kế lại khu đã đăng nhập theo Apple (G1–G7, P-74): `docs/archive/client/OWNER_UI_REDESIGN.md`; quy tắc rút ra đã ở `DESIGN_SYSTEM.md`, hành vi ở `SCREENS.md`. Task mới viết đầy đủ ở §2; xong thì chuyển sang file lưu trữ và thêm một dòng vào §1.

## 1. Task đã xong

| Task | Tên | Mục trong `docs/archive/client/PLAN.md` |
|---|---|---|
| W00 | Hợp đồng và token dùng chung | 1. Nền tảng chung |
| W01 | Khung web, theme, component, đăng nhập | 2. Web dispatcher |
| W02 | Màn tài khoản | 2. Web dispatcher |
| W03 | Xe và thành viên | 2. Web dispatcher |
| W04 | Nền tảng bản đồ và màn cửa hàng | 2. Web dispatcher |
| W05 | Kế hoạch: danh sách, tạo/sửa, tìm toạ độ | 2. Web dispatcher |
| W06 | Tối ưu, kết quả, giao việc, theo dõi | 2. Web dispatcher |
| W07 | Hoàn thiện, accessibility, E2E | 2. Web dispatcher |
| W09 | Vị trí tài xế trên màn theo dõi (cùng T32) | 2. Web dispatcher |
| W10 | Chỉnh tay tuyến sau tối ưu (cùng T28) | 2. Web dispatcher |
| W11 | Khu quản trị cho ADMIN (M5 giai đoạn B, P-59) | 2. Web dispatcher |
| W12 | Nhập file, sao chép kế hoạch, báo cáo (M5 giai đoạn C, P-67, P-68) | 2. Web dispatcher |
| W08 | Đóng gói và deploy web | 2. Web dispatcher |
| M00 | Khung Flutter, theme, đăng nhập | 3. App tài xế |
| M01 | Tuyến và chi tiết tuyến | 3. App tài xế |
| M02 | Báo kết quả điểm giao | 3. App tài xế |
| M03 | Hoàn thiện app | 3. App tài xế |
| M04 | Chia sẻ vị trí khi giao (cùng T32) | 3. App tài xế |
| M05 | Dẫn đường trong app (cùng T33) | 3. App tài xế |

## 2. Task đang mở

### M06. Tài xế tự sửa thông tin trong app, bỏ ô số điện thoại khỏi Nhận lời mời (để cuối, chủ dự án ghi 2026-11-17)

> **Gộp vào thiết kế lại app tài xế (2026-11-20, chủ dự án):** làm ở `docs/client/DRIVER_UI_REDESIGN.md` D3 (màn "Thông tin của tôi") và D7 (web bỏ ô số điện thoại); phạm vi và acceptance dưới đây giữ nguyên.

- **Lý do:** ô số điện thoại (tuỳ chọn) ở form Nhận lời mời làm form dài; chủ dự án muốn bỏ. Hiện chưa bỏ được vì **tài xế không có chỗ nào khác để nhập số**: web chặn DRIVER (trang "Dùng app tài xế"), app tài xế chưa có màn sửa hồ sơ, chủ cửa hàng không sửa được số của thành viên (`PATCH /store/members/{userId}` không có `phoneNumber`). Số này dùng ở màn theo dõi để điều phối viên gọi nhắc tài xế (P-53, `driver.phoneNumber`).
- **Phạm vi:**
  - App tài xế (mobile): màn xem và sửa thông tin của mình: họ tên, số điện thoại (`PATCH /users/me` merge patch `{fullName?, phoneNumber?}`, `null` xoá số; lỗi `FULL_NAME_REQUIRED`, `INVALID_FULL_NAME_LENGTH`, `PHONE_NUMBER_*` dưới ô theo UX_BRIEF). Vào từ bảng tài khoản đang có.
  - Web: bỏ ô số điện thoại khỏi bước "Tạo tài khoản" của Nhận lời mời (`AcceptInvitePage`); không gửi `phoneNumber` (API vẫn nhận, tuỳ chọn). Sửa `SCREENS.md` (bỏ `INVALID_PHONE_NUMBER` dưới ô số điện thoại) và test.
- **Ngoài phạm vi:** chủ cửa hàng sửa số của thành viên (cần API mới, hỏi lại khi làm).
- **Phụ thuộc:** M03 (bảng tài khoản trong app). Backend không cần đổi.
- **Acceptance:** tài xế đổi được họ tên và số trong app, màn theo dõi trên web thấy số mới sau khi tải lại; form Nhận lời mời chỉ còn họ tên và mật khẩu; test mobile và web cho cả hai.

## 3. Sau MVP (chưa lên lịch)

Thông báo đẩy · trang theo dõi đơn cho khách (client thứ ba) · đa ngôn ngữ.
