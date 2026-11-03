# CLIENT PLAN — Kế hoạch triển khai web và app tài xế

> Quy tắc làm việc: `docs/client/README.md`. Kiến trúc: `docs/client/ARCHITECTURE.md`. Màn hình: `docs/client/SCREENS.md`. Quyết định: P-40 → P-47.
>
> Backend MVP đã xong (T00–T45). Kế hoạch này **không** sửa backend; thiếu gì ở API thì ghi vào báo cáo và tạo task backend riêng (đánh số T46 trở đi, ghi ở `docs/IMPLEMENTATION_PLAN.md`), như T45.
>
> Ước lượng tính cho 1 người kèm agent; tổng web khoảng 11–13 ngày, mobile khoảng 5–6 ngày. Mỗi task một branch (`task/W01-web-shell`, `task/M00-mobile-shell`) từ `develop`, một PR vào `develop`.

## 0. Thứ tự và phụ thuộc

```
W00 ─┬─ W01 ─┬─ W02
     │       ├─ W03
     │       └─ W04 ── W05 ── W06 ── W07 ── W08 (sau khi chốt deploy)
     └─ M00 ── M01 ── M02 ── M03
```

| Task | Phụ thuộc | Song song được với |
|---|---|---|
| W00 | — | — |
| W01 | W00 | M00 |
| W02 | W01 | W03, W04, M* |
| W03 | W01 | W02, W04, M* |
| W04 | W01 | W02, W03, M* |
| W05 | W04 | M* |
| W06 | W05 | M* |
| W07 | W02, W03, W06 | M* |
| W08 | W07, quyết định deploy | — |
| M00 | W00 | W01–W07 |
| M01 | M00 | W* |
| M02 | M01 | W* |
| M03 | M02 | W* |

File nóng của client (không hai task song song cùng sửa): `web/package.json`, `web/pnpm-lock.yaml`, `web/vite.config.ts`, `web/src/app/router.tsx`, `mobile/pubspec.yaml`, `mobile/pubspec.lock`, `mobile/lib/app/router.dart`, `api/openapi.json`, `brand/design-system/tokens.json`, cùng các file nóng của backend (`.github/**`, `compose*.yaml`…). Task song song mà phải thêm route/dependency thì bên merge sau rebase.

## 1. Nền tảng chung

### W00. Hợp đồng và token dùng chung
- **Ước lượng:** 0,5 ngày · **Quyết định:** P-41, P-42
- **Phạm vi:**
  - `api/openapi.json` (snapshot từ `be/target/openapi.json`) và `scripts/sync_openapi.sh` (chạy `ApiSpecContractIntegrationTest`, chép file, định dạng ổn định để diff gọn).
  - Bước kiểm drift trong job backend của CI (`be/target/openapi.json` khác `api/openapi.json` → fail kèm lệnh sửa).
  - `scripts/build_tokens.py` (thư viện chuẩn Python): `brand/design-system/tokens.json` → `web/src/shared/theme/tokens.css` và `mobile/lib/shared/theme/tokens.g.dart`; tuỳ chọn `--check` để CI so file sinh. Task này chỉ tạo script và test của nó (đầu ra có thể sinh vào thư mục tạm), file sinh được commit ở W01/M00.
  - README gốc: mục "Client" trỏ tới `docs/client/`.
- **Ngoài phạm vi:** tạo project web/mobile.
- **Acceptance:** chạy `sync_openapi.sh` hai lần liên tiếp không tạo diff; CI fail khi sửa tay `api/openapi.json`; `build_tokens.py` sinh CSS có đủ biến của mọi token màu (hai theme), khoảng cách, bo góc, bóng, gradient; test của script chạy bằng `python3 -m unittest`.

## 2. Web dispatcher

### W01. Khung web, theme, component, đăng nhập
- **Ước lượng:** 2 ngày · **Quyết định:** P-43, P-46, P-47 · **Màn:** Đăng nhập, trang lỗi
- **Phạm vi:**
  - Vite + React + TypeScript strict, pnpm, ESLint (gồm `no-console`, cấm `dangerouslySetInnerHTML`, `import/no-cycle`, quy tắc ranh giới lớp ARCHITECTURE §2), Prettier, Vitest + Testing Library + MSW, base path `/app/`, proxy dev `/api` → `http://localhost:8080`.
  - Cấu trúc thư mục ARCHITECTURE §2; `app/` (router, providers, layout có header), `RequireAuth`, `RequireRole`.
  - `data/http`: client `openapi-fetch` + kiểu sinh từ `api/openapi.json`; bóc `{data}`; `ApiError`; single-flight refresh; header không lộ token vào log.
  - `shared/theme` (tokens.css sinh từ W00, chuyển theme, nhớ lựa chọn), font Be Vietnam Pro, favicon/logo từ `brand/logo`.
  - `shared/ui`: port 9 component tham chiếu (giữ tên/props `components.d.ts`), thêm `AppHeader`, `EmptyState`, `ErrorBanner`, `FieldError`, `OtpInput` (6 ô, cho W02).
  - `shared/errors` (bảng UX_BRIEF §6, `fieldErrors`), `shared/format` (km, thời lượng, giờ theo timezone, điện thoại), `shared/i18n/vi.ts`.
  - Màn Đăng nhập theo prototype (thẻ `radius-lg` + `shadow-raised`, panel `brand-gradient`), khôi phục phiên khi tải trang, đăng xuất; trang 404, không có quyền, "Dùng app tài xế".
  - CI: job `web` (install frozen, lint, typecheck, test, build) chạy khi đổi `web/**`, `api/**`, `brand/**`.
- **Acceptance:** đăng nhập/đăng xuất/khôi phục phiên chạy với MSW và với backend thật qua proxy; hai request 401 cùng lúc chỉ gọi refresh một lần (test); mỗi lỗi đăng nhập của SCREENS §1.1 có test; `statusInfo` và `formatPhone` có test bám bảng của DESIGN_SYSTEM; theme tối không có chữ dưới 4,5:1 trên component (test axe trên trang component).

### W02. Màn tài khoản
- **Ước lượng:** 1,5 ngày · **Màn:** Đăng ký, Xác thực email, Quên mật khẩu, Nhận lời mời, Hồ sơ
- **Phạm vi:** 5 màn theo SCREENS và prototype; `OtpInput` (tự nhảy ô, dán, Backspace, autofill `one-time-code`); đếm ngược gửi lại 60 s; 429 đếm ngược `Retry-After`; danh sách phiên (đăng xuất từng phiên / mọi phiên khác; đăng xuất phiên hiện tại = đăng xuất).
- **Acceptance:** mỗi màn có test luồng thành công và các lỗi của UX_BRIEF §6 liên quan; lỗi theo field hiện đúng ô; đổi mật khẩu xong thì danh sách phiên chỉ còn phiên hiện tại.

### W03. Xe và thành viên
- **Ước lượng:** 1,5 ngày · **Màn:** Xe, Thành viên và lời mời
- **Phạm vi:** danh sách + form thêm/sửa xe (đơn vị KG/thùng/kiện/đơn, tắt xe); thành viên (đổi vai trò, bật "Đi giao", vô hiệu hoá/kích hoạt lại có xác nhận), lời mời (gửi, danh sách, thu hồi); chế độ chỉ xem cho DISPATCHER.
- **Acceptance:** OWNER không có control đổi vai trò/trạng thái của chính mình; DISPATCHER không thấy control sửa và tab lời mời; lỗi từ `docs/ux/samples/error-validation.json` hiện đúng 3 ô; `EMAIL_EXISTED` khi mời hiện dưới ô email.

### W04. Nền tảng bản đồ và màn cửa hàng
- **Ước lượng:** 1,5 ngày · **Quyết định:** P-45 · **Màn:** Cửa hàng
- **Phạm vi:** `platform/map`: contract `Basemap` + factory `maptiler(key, style)`, `osm()` (chỉ dev), chọn theo env; `RouteMap` (marker theo `routeIndex`/tone, kho, đường nối, chọn điểm, + / −, mở rộng khung, chú giải), `PinPicker` (ghim, tìm địa chỉ qua `/geocoding/search` có debounce, hộp thoại `Dialog size="full"`), attribution bắt buộc; react-leaflet chỉ được import trong `platform/map`. Màn Cửa hàng (tên, timezone, kho).
- **Acceptance:** đổi `VITE_BASEMAP` không đổi code màn nào (test render với `Basemap` giả); lint chặn import `leaflet`/`react-leaflet` ngoài `platform/map`; đặt kho bằng ghim gửi đúng `depot {lat, lon, address?}`; không có kho → cảnh báo.

### W05. Kế hoạch: danh sách, tạo/sửa, tìm toạ độ
- **Ước lượng:** 2 ngày · **Màn:** Danh sách kế hoạch, Tạo kế hoạch, Soạn plan — tìm toạ độ
- **Phạm vi:** danh sách (lọc trên URL, phân trang, trạng thái trống); form plan (thông tin, phân công, bảng điểm giao nhập được — react-hook-form field array; thêm/xoá dòng; giới hạn 100/20); lưu → `/plans/:id`; polling khi `GEOCODING`; sửa điểm lỗi bằng địa chỉ (PATCH stop) hoặc `PinPicker`; "Tìm lại toạ độ"; khoá thao tác khi đang xử lý; 409 `PLAN_MODIFIED`.
- **Acceptance:** lỗi `stops[3].timeWindow.end` hiện ở đúng ô dòng 4; `STORE_DEPOT_REQUIRED` dẫn sang `/store`; polling dừng khi `IDLE` và dùng `version` mới (test với MSW đổi dần `processingStatus`); "Tối ưu lộ trình" khoá khi còn điểm chưa có toạ độ.

### W06. Tối ưu, kết quả, giao việc, theo dõi
- **Ước lượng:** 2 ngày · **Màn:** Soạn plan — sau khi tối ưu, Xác nhận giao việc, Theo dõi tiến độ
- **Phạm vi:** optimize + polling `OPTIMIZING`; màn kết quả (thẻ tổng quan, khối tuyến thu gọn được, bảng ↔ bản đồ, cảnh báo `INFEASIBLE`); giao việc (hộp xác nhận, `SHIFT_OVERLAP` với link plan trùng, toast); huỷ (DRAFT/PUBLISHED); theo dõi (đếm theo kết quả, `routes[].status`, thanh tiến độ, lý do, polling 30 s chỉ khi tab hiện, dừng khi kết thúc). Job lỗi (`NO_SOLUTION`, `ROUTING_PROVIDER_ERROR`…) hiện câu theo UX_BRIEF và giữ kết quả cũ.
- **Acceptance:** mỗi trạng thái plan mở đúng màn trên cùng route; không còn chỗ nào tự tính trạng thái route; giao việc thất bại vì trùng ca không đổi trạng thái giao diện; test với `plan-detail-optimized.json` và `plan-detail-completed.json`.

### W07. Hoàn thiện, accessibility, E2E
- **Ước lượng:** 1,5 ngày
- **Phạm vi:** rà responsive (≥ 360px, bảng cuộn ngang, bản đồ xuống dưới bảng < 1024px); axe trên mọi màn chính; bàn phím (Tab, Esc, Enter) ở bảng, hộp thoại, OTP; theme tối toàn bộ; Playwright E2E trên stack compose: đăng ký → xác thực (Mailpit) → kho → xe → mời → plan → tối ưu → giao việc → (gọi API tài xế báo kết quả) → theo dõi COMPLETED.
- **Acceptance:** E2E xanh trong CI (job riêng, chạy khi đổi `web/**` hoặc `be/**`, có thể chỉ trên `develop`); không lỗi axe mức serious/critical.

### W08. Đóng gói và deploy web
- **Ước lượng:** 0,5–1 ngày · **Điều kiện:** chủ dự án chốt reverse proxy và domain (đang để sau, xem P-47)
- **Phạm vi:** build tĩnh phục vụ ở `/app/` cùng origin với `/api/`; cấu hình proxy (Caddy hoặc nginx); cache header cho asset có hash; `CORS_ALLOWED_ORIGINS` không cần khi cùng origin; cập nhật `docs/DEPLOYMENT.md`.

## 3. App tài xế

### M00. Khung Flutter, theme, đăng nhập
- **Ước lượng:** 1,5 ngày · **Quyết định:** P-44, P-46 · **Màn:** Tài xế — đăng nhập
- **Phạm vi:** project `mobile/` (Android + iOS), `flutter_lints` nghiêm; cấu trúc ARCHITECTURE §2; client sinh `dart-dio` từ `api/openapi.json` vào `mobile/packages/pathwise_api`; dio interceptor (`X-Client-Platform: mobile`, single-flight refresh, refresh token trong `flutter_secure_storage`); theme từ `tokens.g.dart` (sáng/tối), font bundle; widget `PwButton`, `TextField`, `StatusPill`, `StopCard`, `ProgressBar`, `PwDialog`; go_router với redirect theo phiên; màn đăng nhập; app icon/splash từ `brand/logo`; CI job `mobile` (analyze, test).
- **Acceptance:** đăng nhập, khôi phục phiên sau khi tắt mở app, refresh khi access hết hạn (test interceptor với server giả); người không phải tài xế → màn không có quyền.

### M01. Tuyến và chi tiết tuyến
- **Ước lượng:** 1,5 ngày · **Màn:** Tài xế — tuyến hôm nay, Tài xế — chi tiết tuyến
- **Phạm vi:** danh sách theo ngày (đổi ngày, kéo tải lại, trạng thái trống); chi tiết (tiêu đề `planName`, điểm tiếp theo, các điểm còn lại, nhóm đã xong); "Gọi" (`tel:`), "Chỉ đường" (Google/Apple Maps theo nền tảng, `url_launcher`).
- **Acceptance:** widget test với `driver-route-list.json`, `driver-route-detail.json`; deep link chỉ đường đúng định dạng D-07 cho từng nền tảng.

### M02. Báo kết quả điểm giao
- **Ước lượng:** 1,5 ngày · **Màn:** Tài xế — báo kết quả
- **Phạm vi:** ba nút kết quả, hộp xác nhận, lý do bắt buộc khi FAILED (≤ 255, gợi ý nhanh), sang điểm tiếp theo, xong tuyến; hàng đợi gửi lại khi mất mạng (persist, gửi lại cùng nội dung, chặn báo kết quả khác cho stop đang chờ); `PLAN_NOT_ACTIVE`, `INVALID_STOP_TRANSITION`.
- **Acceptance:** test hàng đợi: tắt mạng → báo kết quả → bật mạng → gửi đúng một nội dung, server trả 200 idempotent khi gửi lại; `routeStatus = COMPLETED` hiện màn xong tuyến.

### M03. Hoàn thiện app
- **Ước lượng:** 1 ngày
- **Phạm vi:** quên mật khẩu/nhận lời mời (mở web trong trình duyệt, hoặc native nếu chủ dự án muốn — hỏi trước), đăng xuất, cài đặt theme; semantics (TalkBack/VoiceOver) cho nút chính; kiểm cỡ chữ hệ thống lớn; build release Android (ký app là việc của chủ dự án).
- **Acceptance:** không cắt chữ khi cỡ chữ hệ thống 130%; mọi nút có nhãn đọc được.

## 4. Sau MVP (chưa lên lịch)

Vị trí tài xế (cần T32) · dẫn đường trong app (cần T33) · chỉnh tay tuyến (cần T28) · thông báo đẩy · trang theo dõi đơn cho khách (client thứ ba) · đa ngôn ngữ.
