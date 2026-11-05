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
- **Phát sinh khi làm (2026-11-03):**
  - Chuẩn hoá và so sánh snapshot tách thành `scripts/openapi_snapshot.py` (`write`/`check`), dùng chung cho `sync_openapi.sh` và CI. Dạng chuẩn bỏ `servers` (springdoc ghi `http://localhost`, phụ thuộc môi trường) và sắp xếp key: hai lần chạy cho file giống hệt (đã kiểm), snapshot đầu tiên 38 path, 77 schema, có field của T45.
  - CI (`ci.yml`, job `ci`): bước "Script tests" trước build, "Check API contract snapshot" sau `./mvnw verify`. `.gitignore` thêm `__pycache__/`.
  - `build_tokens.py` có thêm `--only web|mobile`, `--tokens`, `--web-out`, `--mobile-out`; CSS có `color-scheme` theo theme và class `.pw-type-<style>`; Dart dùng tên `PwColorTokens`, `PwSpacing`, `PwRadius`, `PwGradients`, `PwTypeStyles`.
  - Flutter trên máy dev hỏng (`Wrong full snapshot version`, công cụ Flutter lệch Dart SDK 3.13.7), nên file Dart sinh ra được kiểm bằng `dart analyze` với stub các kiểu của `package:flutter/painting.dart` (không lỗi). M00 cần cài lại Flutter trước (xem M00).
  - Chưa commit file sinh (`tokens.css`, `tokens.g.dart`) và chưa thêm `--check` vào CI: việc của W01 (`--only web`) và M00 (`--only mobile`).

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
- **Từ W00:** chạy `python3 scripts/build_tokens.py --only web` rồi commit `tokens.css`; job `web` thêm `python3 scripts/build_tokens.py --check --only web`.
- **Phát sinh khi làm (2026-11-03):**
  - Phiên bản do pnpm chọn với `minimumReleaseAge` 2 tuần (`web/pnpm-workspace.yaml`): React 19.3, React Router **8.5** (P-43 không ghi số phiên bản), TanStack Query 5, Vite 8, Vitest 5, MSW 3, ESLint 10. **TypeScript ghim `~6.0.3`**: TS 7 (bản viết bằng Go) không có API JavaScript mà typescript-eslint cần (hỗ trợ < 6.1). `allowBuilds: unrs-resolver` (resolver import của ESLint).
  - Path trong OpenAPI đã gồm `/api/v1`, nên cấu hình là **origin** `VITE_API_ORIGIN` (rỗng = cùng origin), không phải base path; test đặt `http://localhost` vì `Request` của Node cần URL tuyệt đối.
  - `TextField` thêm prop tuỳ chọn `startIcon`, `endAction`, `size` (icon và nút hiện mật khẩu của màn Đăng nhập), ghi vào `brand/design-system/components.d.ts`; artifact design system chưa đồng bộ phần này.
  - Sau khi đăng nhập, `RedirectIfSignedIn` chuyển sang `?next=` (chỉ path trong app, chống open redirect) hoặc trang chính; màn Đăng nhập không tự điều hướng (tránh hai lần redirect giành nhau, test bắt được).
  - Tương phản theme tối: jsdom không tính được màu nên axe chạy với `color-contrast` tắt (kiểm cấu trúc: nhãn, role, thứ tự tiêu đề) trên trang component ở theme tối và màn Đăng nhập; tương phản kiểm bằng `shared/theme/contrast.test.ts` trên giá trị token (18 cặp chữ/nền × 2 theme = 36 lần kiểm, đều ≥ 4,5:1).
  - Màn của task sau (đăng ký, xe, plan…) là trang "Sắp có" để menu và link chạy được từ W01.
  - Luồng phiên với backend thật kiểm bằng `curl` qua proxy Vite (không có trình duyệt trong môi trường agent). Backend `dev` cần ghi đè mail sang Mailpit (`--spring.mail.host=localhost --spring.mail.port=1025 …`) vì `application-dev.yaml` trên máy chủ dự án dùng SMTP thật.
  - Phát hiện F-49 (response OpenAPI không có `required`).
### W02. Màn tài khoản
- **Ước lượng:** 1,5 ngày · **Màn:** Đăng ký, Xác thực email, Quên mật khẩu, Nhận lời mời, Hồ sơ
- **Phạm vi:** 5 màn theo SCREENS và prototype; `OtpInput` (tự nhảy ô, dán, Backspace, autofill `one-time-code`); đếm ngược gửi lại 60 s; 429 đếm ngược `Retry-After`; danh sách phiên (đăng xuất từng phiên / mọi phiên khác; đăng xuất phiên hiện tại = đăng xuất).
- **Acceptance:** mỗi màn có test luồng thành công và các lỗi của UX_BRIEF §6 liên quan; lỗi theo field hiện đúng ô; đổi mật khẩu xong thì danh sách phiên chỉ còn phiên hiện tại.

- **Phát sinh khi làm (2026-11-04):**
  - Bố cục hai cột của các màn tài khoản tách thành `features/auth/AuthLayout.tsx` (`AuthLayout`, `AuthCardHead`, hình `RouteArt`/`MainArt`/`CompactArt` vẽ đúng prototype); màn Đăng nhập dùng lại, giao diện không đổi. Màn Hồ sơ ở `features/account`.
  - Thêm dùng chung: `shared/hooks/useCountdown` (khoá 429 và 60 s gửi lại mã, Đăng nhập cũng dùng), `shared/forms/serverErrors` (`applyServerErrors` đặt `details[]` vào ô của react-hook-form), `TextField` làm prop `multiline` (đã có trong `components.d.ts`), class `.pw-field-code` cho ô mã một dòng, `formatDayTime`/`formatDate`/`formatClock`.
  - "Sai 3 lần phải xin mã mới": server chỉ trả `INVALID_OTP` cho mọi lần sai, nên client đếm số lần sai **của mã đang dùng** để báo trước và khoá nút; đếm lại khi xin mã mới. Server vẫn là nơi quyết định (lần thứ 4 vẫn nhận `INVALID_OTP`).
  - Khác prototype có chủ đích: Xác thực email hỏi thêm email khi URL không có `?email=`; Quên mật khẩu bước 2 có nút "Gửi lại mã" (khoá 60 s) vì sai 3 lần phải xin mã mới; Nhận lời mời của DRIVER không có nút "Mở app tài xế" (chưa có link tải app, M03/W08) và không có "Đăng nhập trên web" (DRIVER không dùng web, P-46); hạn lời mời hiện theo giờ của máy vì `GET /invitations/{code}` không trả timezone của cửa hàng.
  - Tên thiết bị trong danh sách phiên đọc từ `userAgent` ("Chrome trên Linux", "App PathWise trên Android"); chỉ để hiển thị.
  - Không có mẫu `SessionResponse` trong `docs/ux/samples`, test dùng dữ liệu theo đúng dạng API_SPEC §2. Chưa chạy lại luồng W02 với backend thật (đã chạy ở W01 cho đăng ký/OTP/đăng nhập); E2E ở W07.
  - Phát hiện F-50 (OpenAPI: mã 201/204 và `nullable` của PATCH).

### W03. Xe và thành viên
- **Ước lượng:** 1,5 ngày · **Màn:** Xe, Thành viên và lời mời
- **Phạm vi:** danh sách + form thêm/sửa xe (đơn vị KG/thùng/kiện/đơn, tắt xe); thành viên (đổi vai trò, bật "Đi giao", vô hiệu hoá/kích hoạt lại có xác nhận), lời mời (gửi, danh sách, thu hồi); chế độ chỉ xem cho DISPATCHER.
- **Acceptance:** OWNER không có control đổi vai trò/trạng thái của chính mình; DISPATCHER không thấy control sửa và tab lời mời; lỗi từ `docs/ux/samples/error-validation.json` hiện đúng 3 ô; `EMAIL_EXISTED` khi mời hiện dưới ô email.

- **Phát sinh khi làm (2026-11-04):**
  - Thêm `SelectField`, `Checkbox`, `.pw-card` ở `shared/ui` (design system chưa có ô chọn và checkbox; theo class của prototype); trạng thái xe qua `statusInfo('vehicle', ACTIVE|INACTIVE)` (thêm vào `brand/design-system` và DESIGN_SYSTEM).
  - Query cửa hàng chuyển sang `data/storeQuery.ts` (header, hồ sơ, thành viên dùng chung); rule form chung chuyển sang `shared/forms/rules.ts`; `src/test/samples.ts` đọc fixture từ `docs/ux/samples`.
  - Màn Xe bỏ nút "+ Thêm xe" ở đầu trang của prototype (chủ dự án yêu cầu, trùng với form "Thêm xe" luôn hiện bên phải; đang sửa thì "Huỷ" quay về thêm mới).
  - Bộ lọc xe và tab thành viên nằm trên URL (`?active=true`, `?tab=invitations`).
  - Câu "Vui lòng chọn …" cho ô chọn (`CAPACITY_UNIT_REQUIRED`, `ROLE_REQUIRED`), ghi vào UX_BRIEF §6.2.
  - Kiểm giao diện bằng Chrome headless điều khiển qua DevTools protocol (Node, đăng nhập bằng tài khoản test trên backend dev, chỉ đọc): màn Xe, Thành viên, Lời mời khớp bố cục prototype; sửa ô "Sức chứa" lệch với "Đơn vị" (lưới `align-items: start`). Cửa hàng test chưa có xe và chỉ có chủ cửa hàng, nên dòng dữ liệu chỉ kiểm bằng test.

### W04. Nền tảng bản đồ và màn cửa hàng
- **Ước lượng:** 1,5 ngày · **Quyết định:** P-45 · **Màn:** Cửa hàng
- **Phạm vi:** `platform/map`: contract `Basemap` + factory `maptiler(key, style)`, `osm()` (chỉ dev), chọn theo env; `RouteMap` (marker theo `routeIndex`/tone, kho, đường nối, chọn điểm, + / −, mở rộng khung, chú giải), `PinPicker` (ghim, tìm địa chỉ qua `/geocoding/search` có debounce, hộp thoại `Dialog size="full"`), attribution bắt buộc; react-leaflet chỉ được import trong `platform/map`. Màn Cửa hàng (tên, timezone, kho).
- **Acceptance:** đổi `VITE_BASEMAP` không đổi code màn nào (test render với `Basemap` giả); lint chặn import `leaflet`/`react-leaflet` ngoài `platform/map`; đặt kho bằng ghim gửi đúng `depot {lat, lon, address?}`; không có kho → cảnh báo.

- **Phát sinh khi làm (2026-11-04):**
  - Dependency: `leaflet` 1.9.4, `react-leaflet` 5.0.0, `@types/leaflet` (P-43).
  - `PinPicker` không nằm trong `platform/map` như ARCHITECTURE §7 viết lúc đầu: nó cần `TextField`/`Dialog` của `shared/ui`, mà `platform` không được import lớp trên (§2). Tách: `platform/map` có `PinMap`, `RouteMap`, `DepotIcon`, `Basemap`; `shared/ui/PinPicker` + `PinPickerDialog` ghép phần form. ARCHITECTURE §2.1, §7 đã sửa.
  - Thêm so với prototype: nút "Đặt ghim ở giữa bản đồ" (đặt ghim bằng bàn phím, WCAG 2.1.1) và chọn marker bằng Enter/Space (Leaflet chỉ dùng Enter để mở popup).
  - react-leaflet chỉ đọc `className` của đường lúc tạo (`pathOptions` chỉ restyle): truyền `className` làm prop, không thì đường mang màu xanh mặc định của Leaflet (test bắt được).
  - Chưa có MapTiler key: dev chạy `osm()` (máy chủ dự án đang bị OSM chặn tile, nên bản đồ dev có thể không có nền tới khi đặt `VITE_MAPTILER_KEY` trong `web/.env.local`). Production thiếu key thì không có tile (marker vẫn hiện).
  - Test: thời gian chờ mặc định của Testing Library nâng lên 3 s (`src/test/setup.ts`), vì bộ test chạy song song với bản đồ có lúc vượt 1 s (trước khi nâng: 2/6 lần chạy cả bộ có một test đỏ, ở màn Xe hoặc Thành viên; sau khi nâng: 5/5 lần xanh). Setup bỏ qua phần DOM khi test chạy môi trường node (test ESLint) và thêm `createSVGRect` cho jsdom để Leaflet vẽ đường bằng SVG như trình duyệt.
  - Bundle lớn hơn 500 kB sau khi thêm Leaflet (Vite cảnh báo): tách chunk theo route ở W07.
  - Chưa chụp được màn Cửa hàng với backend thật: backend dev đang chạy trên máy chủ dự án (IntelliJ) có `POST /auth/login` thay vì `/auth/log-in` của `develop`, nên web không đăng nhập được qua nó.

### W05. Kế hoạch: danh sách, tạo/sửa, tìm toạ độ
- **Ước lượng:** 2 ngày · **Màn:** Danh sách kế hoạch, Tạo kế hoạch, Soạn plan — tìm toạ độ
- **Phạm vi:** danh sách (lọc trên URL, phân trang, trạng thái trống); form plan (thông tin, phân công, bảng điểm giao nhập được — react-hook-form field array; thêm/xoá dòng; giới hạn 100/20); lưu → `/plans/:id`; polling khi `GEOCODING`; sửa điểm lỗi bằng địa chỉ (PATCH stop) hoặc `PinPicker`; "Tìm lại toạ độ"; khoá thao tác khi đang xử lý; 409 `PLAN_MODIFIED`.
- **Acceptance:** lỗi `stops[3].timeWindow.end` hiện ở đúng ô dòng 4; `STORE_DEPOT_REQUIRED` dẫn sang `/store`; polling dừng khi `IDLE` và dùng `version` mới (test với MSW đổi dần `processingStatus`); "Tối ưu lộ trình" khoá khi còn điểm chưa có toạ độ.

- **Phát sinh khi làm (2026-11-05):**
  - Màn `/plans/:id` ở W05 vẽ plan DRAFT chưa tối ưu (tìm toạ độ); plan đã tối ưu hoặc đã giao việc tạm hiện "đang xây dựng (W06)". Nút "Tối ưu lộ trình" đã gọi `POST /optimize` và theo dõi bằng polling chung; màn kết quả là việc của W06.
  - Prototype ghi "Để trống địa chỉ và đặt ghim sau khi lưu", nhưng API trả 400 `ADDRESS_OR_COORDINATES_REQUIRED` khi điểm giao không có cả địa chỉ lẫn toạ độ (API_SPEC §7): câu gợi ý đổi thành "Ghi địa chỉ gần đúng, sau khi lưu đặt ghim đúng chỗ" (docs đúng hơn prototype).
  - Cột số lượng của bảng điểm giao theo đơn vị của các xe đã chọn (prototype chỉ có "Thùng" vì xe mẫu tính thùng); `ORDER` không có cột (D-36).
  - Form chờ danh sách tài xế/xe/cửa hàng tải xong mới dựng (ô chọn nhận giá trị lúc mount); khi sửa, tài xế/xe trong plan mà đã bị vô hiệu hoá/tắt vẫn có trong ô chọn để không mất giá trị (server sẽ báo `INVALID_ASSIGNMENT` nếu cần).
  - Thêm `todayIn(timeZone)` (`shared/format`), `TrashIcon`, `data/queryKeys` cho plan; câu lỗi `INVALID_DEMAND`, `INVALID_MAX_DISTANCE`, `INVALID_SERVICE_TIME`, `DRIVER_REQUIRED`, `VEHICLE_REQUIRED` ghi vào UX_BRIEF §6.2.
  - Phát hiện F-51 (OpenAPI ghi `PlanDraftRequest.version` bắt buộc cho cả `POST`; client không gửi `version` khi tạo).

### W06. Tối ưu, kết quả, giao việc, theo dõi
- **Ước lượng:** 2 ngày · **Màn:** Soạn plan — sau khi tối ưu, Xác nhận giao việc, Theo dõi tiến độ
- **Phạm vi:** optimize + polling `OPTIMIZING`; màn kết quả (thẻ tổng quan, khối tuyến thu gọn được, bảng ↔ bản đồ, cảnh báo `INFEASIBLE`); giao việc (hộp xác nhận, `SHIFT_OVERLAP` với link plan trùng, toast); huỷ (DRAFT/PUBLISHED); theo dõi (đếm theo kết quả, `routes[].status`, thanh tiến độ, lý do, polling 30 s chỉ khi tab hiện, dừng khi kết thúc). Job lỗi (`NO_SOLUTION`, `ROUTING_PROVIDER_ERROR`…) hiện câu theo UX_BRIEF và giữ kết quả cũ.
- **Acceptance:** mỗi trạng thái plan mở đúng màn trên cùng route; không còn chỗ nào tự tính trạng thái route; giao việc thất bại vì trùng ca không đổi trạng thái giao diện; test với `plan-detail-optimized.json` và `plan-detail-completed.json`.

- **Phát sinh khi làm (2026-11-05):**
  - Prototype giải thích lý do điểm không xếp được ("Khung giờ nhận 20:00–21:00 nằm ngoài ca…"); API chỉ trả `dropReason = INFEASIBLE`, nên màn dùng câu chung (ARCHITECTURE §2 quy tắc 5: không tự suy logic server). Muốn có lý do cụ thể thì cần task backend (solver trả lý do).
  - Job optimize lỗi chỉ biết qua `GET /jobs/{id}` (plan về `IDLE` không kèm kết quả job): màn kết quả giữ job của `POST /optimize` và theo dõi tới khi xong (`useJob`).
  - Plan CANCELLED dùng màn theo dõi kèm thông báo "Kế hoạch đã huỷ." (prototype đặt thông báo trên màn kết quả; một route cho mọi trạng thái nên plan đã huỷ có tuyến hay không đều xem được).
  - "N tài xế · N xe" và "Giao việc cho N tài xế" đếm tuyến có điểm (route không có điểm không giao cho ai, P-34).
  - Hộp trùng ca hỗ trợ cả `assignments[i].vehicleId` (trùng xe), prototype chỉ vẽ trùng tài xế; bỏ câu "Tài xế A không bị ảnh hưởng" của prototype.
  - Chưa chụp màn với backend thật: cửa hàng test chưa có plan nào; không tạo dữ liệu trong DB dev của chủ dự án chỉ để chụp. Kiểm bằng test với `plan-detail-optimized.json`, `plan-detail-completed.json` và axe; kiểm tay ở W07 (E2E).

### W07. Hoàn thiện, accessibility, E2E
- **Ước lượng:** 1,5 ngày
- **Phạm vi:** rà responsive (≥ 360px, bảng cuộn ngang, bản đồ xuống dưới bảng < 1024px); axe trên mọi màn chính; bàn phím (Tab, Esc, Enter) ở bảng, hộp thoại, OTP; theme tối toàn bộ; Playwright E2E trên stack compose: đăng ký → xác thực (Mailpit) → kho → xe → mời → plan → tối ưu → giao việc → (gọi API tài xế báo kết quả) → theo dõi COMPLETED.
- **Acceptance:** E2E xanh trong CI (job riêng, chạy khi đổi `web/**` hoặc `be/**`, có thể chỉ trên `develop`); không lỗi axe mức serious/critical.

- **Phát sinh khi làm (2026-11-05):**
  - Backend chỉ có provider Nominatim/OSRM thật nên E2E dùng server giả trong compose (P-49, chủ dự án chọn): `compose.e2e.yaml`, `web/e2e/stub/server.mjs`, `web/e2e/e2e.env` (cổng 3317/6390/1035/8035/8091, project `pathwise-e2e` để không đụng DB và Mailpit dev). Solver chạy 2 s trong E2E.
  - Bước "tài xế báo kết quả" gọi API của app (app Flutter chưa có, M00–M03).
  - Khoá JWT của stack E2E không nằm trong repo: sinh mỗi lần chạy (`E2E_JWT_SECRET=$(openssl rand -base64 32)`, CI sinh và che trong log); `compose.e2e.yaml` báo lỗi nếu thiếu. Commit đầu của PR #69 (`b024c64`) có một khoá test bịa trong `web/e2e/e2e.env` (gitleaks bắt được), chưa từng dùng ở môi trường nào; commit sau gỡ nó và `.gitleaksignore` ghi đúng finding đó kèm lý do (không viết lại lịch sử nhánh).
  - Dependency `@playwright/test` 1.64.1 (P-43). Vitest chỉ nhận `src/**/*.test.*`; ESLint có cấu hình Node cho server giả.
  - Tách chunk theo route (`lazy` của React Router; composition root chỉ import phần basemap; `PinPicker` import theo đường dẫn riêng): bundle chính 707 kB → 258 kB (gzip 213 → 79 kB), Leaflet nằm trong chunk của màn có bản đồ; hết cảnh báo chunk lớn của W04.
  - `Dialog` giữ focus bên trong khi Tab/Shift+Tab (trước đây Tab ra được trang phía sau); có test bàn phím.
  - Rà responsive ở 360px trên backend dev (Kế hoạch, Tạo kế hoạch, Xe, Thành viên, Cửa hàng, Hồ sơ): không cuộn ngang trang, bảng cuộn trong khung, bản đồ xuống dưới bảng khi hẹp; không cần sửa. Axe thêm trang 404, không có quyền, "Dùng app tài xế" (mọi màn của SCREENS đều có kiểm axe).
  - Màn kết quả và theo dõi được kiểm với backend thật qua E2E (không chụp ảnh riêng).

### W08. Đóng gói và deploy web
- **Ước lượng:** 0,5–1 ngày · **Điều kiện:** chủ dự án chốt reverse proxy và domain (đang để sau, xem P-47)
- **Phạm vi:** build tĩnh phục vụ ở `/app/` cùng origin với `/api/`; cấu hình proxy (Caddy hoặc nginx); cache header cho asset có hash; `CORS_ALLOWED_ORIGINS` không cần khi cùng origin; cập nhật `docs/DEPLOYMENT.md`.

## 3. App tài xế

### M00. Khung Flutter, theme, đăng nhập
- **Ước lượng:** 1,5 ngày · **Quyết định:** P-44, P-46 · **Màn:** Tài xế — đăng nhập
- **Phạm vi:** project `mobile/` (Android + iOS), `flutter_lints` nghiêm; cấu trúc ARCHITECTURE §2; client sinh `dart-dio` từ `api/openapi.json` vào `mobile/packages/pathwise_api`; dio interceptor (`X-Client-Platform: mobile`, single-flight refresh, refresh token trong `flutter_secure_storage`); theme từ `tokens.g.dart` (sáng/tối), font bundle; widget `PwButton`, `TextField`, `StatusPill`, `StopCard`, `ProgressBar`, `PwDialog`; go_router với redirect theo phiên; màn đăng nhập; app icon/splash từ `brand/logo`; CI job `mobile` (analyze, test).
- **Từ W00:** Flutter trên máy dev đang hỏng (`flutter create` báo `Wrong full snapshot version`; Dart SDK 3.13.7 chạy được): chủ dự án cài lại Flutter stable (khuyến nghị dùng FVM, ghi phiên bản vào `mobile/.fvmrc`) trước khi làm. Chạy `python3 scripts/build_tokens.py --only mobile` rồi commit `tokens.g.dart`; job `mobile` thêm `--check --only mobile`.
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
