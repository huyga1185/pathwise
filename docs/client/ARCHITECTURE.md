# CLIENT ARCHITECTURE — Kiến trúc web dispatcher và app tài xế

> Áp dụng cho `web/` (React) và `mobile/` (Flutter). Quyết định gốc: P-40 → P-47 (`docs/DECISIONS.md`). Kiến trúc backend: `docs/ARCHITECTURE.md`. Hợp đồng API: `docs/API_SPEC.md`. Thiết kế: `docs/client/DESIGN_SYSTEM.md`, `docs/client/SCREENS.md`.
>
> Mục tiêu: hai client (và client thứ ba sau này, ví dụ trang theo dõi đơn cho khách) **dùng chung một hợp đồng và một bộ quy tắc**, mỗi client tách lớp rõ để đổi thư viện, nhà cung cấp bản đồ, cách lưu token… mà không sửa màn hình.

## 1. Tổng thể repo (monorepo, P-40)

```
pathwise/
├── be/                     # Spring Boot (không đổi)
├── web/                    # React + Vite + TS (dispatcher, OWNER/DISPATCHER)
├── mobile/                 # Flutter (app tài xế, DRIVER hoặc canDrive)
├── api/
│   └── openapi.json        # snapshot hợp đồng sinh từ backend (P-41), nguồn để sinh code client
├── brand/
│   ├── logo/               # logo, favicon, app icon (bản gốc)
│   └── design-system/      # tokens.json + component tham chiếu (P-42)
├── scripts/
│   ├── sync_openapi.sh     # chạy test backend sinh openapi.json rồi chép vào api/
│   ├── openapi_snapshot.py # chuẩn hoá snapshot (write) và kiểm drift (check), dùng chung cho script và CI
│   └── build_tokens.py     # tokens.json → web CSS variables + mobile Dart theme
└── docs/client/            # kiến trúc, kế hoạch, màn hình, design system của client
```

Luồng phụ thuộc (một chiều):

```
be ──(sinh)──▶ api/openapi.json ──(sinh kiểu/client)──▶ web/src/api, mobile/lib/api
brand/design-system/tokens.json ──(build_tokens.py)──▶ web theme, mobile theme
docs/ux/samples/*.json ──(fixture test)──▶ web MSW handlers, mobile fake repositories
```

Không client nào import code của client khác hay của `be/`. Thứ dùng chung chỉ đi qua **file dữ liệu** (`api/openapi.json`, `tokens.json`, samples), nên thêm client thứ ba chỉ cần đọc cùng ba nguồn đó.

## 2. Lớp và quy tắc phụ thuộc (giống nhau ở hai client)

```
app/        khởi động, router, provider gốc, layout theo vai trò
  │
features/   mỗi nghiệp vụ một thư mục: auth, account, plans, fleet, members, store, driver-routes…
  │         gồm màn hình, form, hook/provider dữ liệu của nghiệp vụ đó
  ▼
shared/     ui (component từ design system), format, errors, status, hooks chung
  │
  ▼
data/       api client (bọc client sinh từ OpenAPI), query keys, mapping response → view model
  │
  ▼
platform/   cổng ra hệ thống bên ngoài: map (Basemap, MapView), storage (TokenStore), config, clock
```

Quy tắc cứng:
1. **Mũi tên chỉ đi xuống.** `shared` không import `features`; `data` không import `shared/ui`; `platform` không import gì phía trên.
2. **Feature không import ruột của feature khác.** Mỗi feature có file công khai (`index.ts` / `<feature>.dart`) xuất những gì feature khác được dùng (ví dụ route path, một component chọn tài xế). Cần dùng chung nhiều thì chuyển xuống `shared/`.
3. **Màn hình không gọi HTTP trực tiếp.** Màn hình → hook/provider của feature → `data/` → client sinh từ OpenAPI.
4. **Không chép kiểu DTO tay.** Kiểu request/response lấy từ code sinh (`api/openapi.json`). Kiểu riêng của màn hình (view model) đặt trong feature và map từ kiểu sinh.
5. **Không có logic nghiệp vụ trùng server.** Client không tự tính trạng thái route, thời gian dự kiến, kiểm tra trùng ca… Thiếu dữ liệu thì mở task backend (như T45), không tự suy ở client. Ngoại lệ được phép: định dạng hiển thị, validation tức thời trên form (bắt buộc, độ dài, định dạng) **trùng với** validation của server, và gợi ý không ràng buộc (ví dụ lý do không giao được).
6. **Mọi giá trị giao diện lấy từ token.** Không viết mã màu, khoảng cách, bo góc trong code màn hình; ngoại lệ duy nhất là màu cố định của bản đồ và logo đã ghi trong DESIGN_SYSTEM.

### 2.1 Điểm mở rộng (port/adapter phía client)

| Port (contract) | Web | Mobile | Đổi khi nào |
|---|---|---|---|
| `Basemap` (tile bản đồ) | `platform/map/basemap.ts` | `platform/map/basemap.dart` | Đổi nhà cung cấp tile (MapTiler → khác): thêm một factory, đổi biến cấu hình (P-45) |
| `MapView` component (`RouteMap`, `PinMap`, icon kho; `PinPicker` ghép ở `shared/ui`) | Leaflet bên trong `platform/map/*` | flutter_map bên trong `platform/map/*` | Đổi thư viện bản đồ (Leaflet → MapLibre): chỉ viết lại component trong `platform/map` |
| `TokenStore` (giữ token) | bộ nhớ (access) + cookie HttpOnly (refresh, do server đặt) | `flutter_secure_storage` (refresh) + bộ nhớ (access) | Đổi cách lưu (P-46) |
| `ApiClient` | bọc `openapi-fetch` | bọc client `dart-dio` sinh từ OpenAPI | Đổi thư viện HTTP: chỉ sửa `data/http` |
| `AppConfig` | `import.meta.env.VITE_*` | `--dart-define` | Thêm môi trường |
| `Clock` / định dạng giờ | `Intl` + timezone của store | `intl` + timezone của store | Hỗ trợ nhiều timezone |
| `Messages` (chuỗi giao diện) | `shared/i18n/vi.ts` | `shared/i18n/vi.dart` | Thêm ngôn ngữ: thêm file, không sửa màn hình |

Mỗi port là một `interface` (TS) hoặc `abstract interface class` (Dart). Code ngoài `platform/` chỉ phụ thuộc vào interface, implementation được chọn ở `app/` (composition root) theo cấu hình. Test thay implementation bằng fake.

## 3. Hợp đồng API (P-41)

- `api/openapi.json` là snapshot sinh từ backend (`ApiSpecContractIntegrationTest` ghi `be/target/openapi.json`; `scripts/sync_openapi.sh` chạy test đó rồi gọi `openapi_snapshot.py write`). Dạng chuẩn: bỏ `servers` (phụ thuộc môi trường), sắp xếp key, thụt 2 dấu cách, nên chạy lại không tạo diff. Commit cùng PR đổi API của backend.
- CI kiểm **drift**: job `ci` chạy `openapi_snapshot.py check` sau `./mvnw verify`; khác nhau thì fail, in diff và lệnh `sync_openapi.sh`.
- Web sinh kiểu bằng `openapi-typescript` → `web/src/data/schema.d.ts` (commit, không sửa tay); gọi qua `openapi-fetch`. Mobile sinh bằng `openapi-generator` (generator `dart-dio`) → package `mobile/packages/pathwise_api` (commit, không sửa tay).
- Từ T46 (P-48): field response luôn có mặt là bắt buộc trong kiểu sinh ra, field có thể `null` có kiểu `X | null` (client đổi sang `undefined` ở view model nếu cần), field vắng mặt được (`@JsonInclude(NON_NULL)`, ví dụ `LoginResponse.user.fullName`) là `X?`; request PATCH có kiểu `X | null` ở field xoá được. Mã thành công trong OpenAPI là mã thật (201/202/204).
- `API_SPEC.md` vẫn là hợp đồng nghiệp vụ; OpenAPI là bản máy đọc của nó. Client thấy hai bên lệch thì báo, không sửa client cho khớp chỗ sai.
- **Client không đổi API.** Thiếu field hay endpoint → ghi vào báo cáo task, chủ dự án tạo task backend (mẫu T45).

### 3.1 Bao response, lỗi, phân trang

- Thành công: `{ "data": …, "message"?: … }` (API_SPEC §1). `data/http` bóc `data` một lần; feature không thấy lớp bọc. Job dài trả `202 { data: Job }`, client luôn xử lý như bất đồng bộ (theo `processingStatus` của resource).
- Lỗi: `{ "code", "details"?: [{ "field", "code", …field thêm theo mã }] }` (không có `message`; câu hiển thị do client chọn theo `code`). `data/http` chuyển thành `ApiError { status, code, details, retryAfter?, requestId? }` (đọc header `Retry-After`, `X-Request-Id`).
- Phân trang: `{ items, page, size, totalPages, totalElements }`.

### 3.2 Lỗi → giao diện (một chỗ duy nhất)

`shared/errors/` có:
- `messageFor(code)`: bảng mã → câu tiếng Việt, **chép nguyên từ `docs/ux/UX_BRIEF.md` §6** (đổi câu thì sửa UX_BRIEF trước). Mã không có trong bảng → "Đã có lỗi xảy ra. Mã tra cứu: {requestId}".
- `fieldErrors(details)`: map `details[].field` (đường dẫn kiểu `stops[3].timeWindow.end`) vào đường dẫn field của form (react-hook-form `setError` / Flutter form state) với câu theo `details[].code` (UX_BRIEF §6.2).
- Web: `shared/forms/serverErrors.ts` có `applyServerErrors(error, setError, fields, labels)` đặt kết quả của `fieldErrors` vào ô của react-hook-form (W02); trả `false` khi không đặt được ô nào để màn hình hiện hộp lỗi đầu form.
- Quy tắc hiển thị: lỗi theo field → dưới ô (không toast); lỗi toàn form → hộp lỗi đầu form (`role="alert"`); lỗi của hành động trong hộp thoại → trong hộp thoại; lỗi nền (polling) → toast `danger` có "Thử lại".
- Mã cần xử lý riêng (không chỉ hiện câu): `UNAUTHORIZED`/`INVALID_TOKEN` (§4), `PLAN_MODIFIED` (409: hỏi tải lại, không ghi đè), `PLAN_BUSY` (khoá nút, polling), `RATE_LIMITED` (đếm ngược theo `Retry-After`), `EMAIL_NOT_VERIFIED` (dẫn sang xác thực), `SHIFT_OVERLAP` (liệt kê `details[].conflictPlanId` kèm link), `PLAN_NOT_ACTIVE` (app tài xế: báo plan đã huỷ/kết thúc).

## 4. Phiên đăng nhập (P-46)

Theo API_SPEC §2 và UX_BRIEF §3.

**Web**
- Đăng nhập: `POST /auth/log-in` (không gửi `X-Client-Platform`) → `accessToken` giữ **trong bộ nhớ** (không localStorage/sessionStorage); refresh token do server đặt cookie HttpOnly `Path=/api/v1/auth`.
- Khởi động app: gọi `POST /auth/refresh` (cookie tự gửi) để khôi phục phiên; 401 → màn đăng nhập.
- 401 `INVALID_TOKEN`/`UNAUTHORIZED` khi gọi API: **single-flight refresh** (mọi request đang chờ dùng chung một lần refresh), thành công thì gửi lại request đúng một lần, thất bại thì xoá phiên và về đăng nhập kèm câu "Phiên đăng nhập đã hết…".
- Đăng xuất: `POST /auth/log-out`, xoá token trong bộ nhớ, xoá cache dữ liệu (TanStack Query `clear()`).
- Web và API cùng origin (P-47) nên cookie không cần cấu hình cross-site; dev dùng proxy của Vite.

**Mobile**
- Gửi `X-Client-Platform: mobile` ở mọi request auth; `refreshToken` trong body.
- `refreshToken` lưu `flutter_secure_storage`; `accessToken` chỉ trong bộ nhớ.
- Interceptor của dio làm single-flight refresh như web; refresh thất bại → xoá secure storage, về đăng nhập.

**Phân quyền giao diện** (không thay cho kiểm tra ở server): lấy `user.role`, `canDrive` từ `LoginResponse.user` / `GET /users/me`.
- Web: OWNER thấy mọi mục; DISPATCHER không thấy thao tác chỉ OWNER làm được (mời, đổi vai trò, sửa cửa hàng) mà thấy chế độ chỉ xem; DRIVER đăng nhập web → trang báo "Dùng app tài xế" kèm link tải app (không có màn dispatcher).
- Mobile: chỉ DRIVER hoặc `canDrive`; người khác → màn báo không có quyền.

## 5. Dữ liệu, cache, polling

- Web: TanStack Query. Query key theo tài nguyên: `['plans', {date,status,page}]`, `['plan', id]`, `['vehicles', {active}]`… đặt trong `data/queryKeys.ts`.
- Mobile: Riverpod (`AsyncNotifier`/`FutureProvider`) theo cùng cách đặt khoá.
- **Polling** (UX_BRIEF §5.1):
  - Plan có `processingStatus != IDLE`: tải lại `GET /plans/{id}` mỗi 2 s tới khi `IDLE`, khoá nút sửa; xong thì dùng `version` mới.
  - Màn theo dõi plan `PUBLISHED`/`IN_PROGRESS`: mỗi 30 s khi tab đang hiện; dừng khi plan `COMPLETED`/`CANCELLED`.
  - Web (W06): một hàm `planRefetchInterval(plan)` (`features/plans/planApi.ts`) quyết định cả ba trường hợp; `refetchIntervalInBackground: false` để không tải khi tab ẩn.
  - Job (optimize): plan về `IDLE` mà không nói job kết thúc thế nào, nên màn kết quả giữ `Job` của `POST /optimize` và đọc `GET /jobs/{id}` mỗi 2 s tới khi `SUCCEEDED`/`FAILED` (API_SPEC §6); `FAILED` → câu theo `errorCode`.
  - App tài xế: kéo xuống để tải lại; tự tải lại khi app trở lại foreground.
- **Optimistic concurrency:** mọi request sửa plan gửi `version` đang có; 409 `PLAN_MODIFIED` → hộp thoại "Plan vừa được người khác thay đổi. Tải lại để xem bản mới." (không tự ghi đè).
- **Báo kết quả stop (mobile):** idempotent ở server (P-36) nên khi mất mạng app giữ yêu cầu trong hàng đợi trên máy (persist), gửi lại đúng nội dung khi có mạng; không cho báo kết quả khác cho stop đang chờ gửi.
- Không cập nhật lạc quan (optimistic update) cho thao tác không hoàn tác được (giao việc, huỷ, báo kết quả): chờ server trả về rồi mới đổi giao diện.

## 6. Định dạng và đơn vị

- Thời điểm (`Instant` UTC) hiển thị theo `timezone` của plan/store, không theo máy người dùng. Giờ `HH:mm` 24 giờ.
- Khoảng cách mét → "12,4 km" (dấu phẩy thập phân, `vi-VN`); thời lượng giây → "2 giờ 35 phút".
- Số điện thoại: server trả E.164; hiển thị `0905 123 456` (`formatPhone` của design system); gửi lên đúng dạng người dùng nhập (server chuẩn hoá).
- Nhãn trạng thái: chỉ qua `statusInfo(kind, status)` (design system), không viết nhãn ở màn hình.
- Chuỗi giao diện tiếng Việt tập trung trong `shared/i18n/vi.*` theo khoá; màn hình không viết chuỗi dài trực tiếp (chuỗi ngắn một lần trong JSX/widget được phép ở giai đoạn MVP, nhưng thông điệp lỗi, nhãn trạng thái, nút hành động chính phải qua `vi.*`).

## 7. Bản đồ (P-45)

- Nhà cung cấp tile: **MapTiler** qua contract `Basemap { id, tileUrl, attribution, maxZoom, tileSize }`; chọn bằng `VITE_BASEMAP` / `--dart-define=BASEMAP`, key `VITE_MAPTILER_KEY` / `MAPTILER_KEY`. Có sẵn factory `osm()` cho dev khi không có key (chỉ dev, theo chính sách của OSM).
- Key nằm phía client (bắt buộc với tile); giới hạn key theo domain/bundle id trong trang quản lý MapTiler. Không commit key; `.env.example` có tên biến.
- Màn hình chỉ dùng component bản đồ đã bọc: `platform/map` có `RouteMap` (tuyến, marker theo màu tuyến/kết quả, kho, đường nối, chọn điểm bằng chuột hoặc Enter/Space, + / −, mở rộng khung, chú giải), `PinMap` (một ghim: bấm bản đồ, kéo ghim, nút "Đặt ghim ở giữa bản đồ" cho bàn phím), `DepotIcon`; `shared/ui/PinPicker` ghép ô tìm địa chỉ (debounce 400 ms, từ 3 ký tự) + kết quả + `PinMap` + chú giải, `PinPickerDialog` đặt nó trong `Dialog size="full"`. Lý do tách (W04): `platform` là lớp dưới cùng, không được import `shared/ui` (`TextField`, `Dialog`) theo §2; Leaflet vẫn chỉ nằm trong `platform/map`. `PinPicker` nhận hàm `search` từ feature (`data/geocoding.ts` → `GET /geocoding/search`), không tự gọi HTTP.
- Basemap chọn ở composition root (`basemapFor(config, import.meta.env.DEV)`, `BasemapProvider`); thiếu `VITE_MAPTILER_KEY` thì dev dùng `osm()`, production không có tile. Test luôn dùng `blankBasemap()` (không tải tile). Attribution của basemap luôn hiện, kể cả khi không có tile.
- Quy tắc hiển thị (màu tuyến, marker, icon kho, attribution, đường nối thẳng vì API chưa có hình học đường): DESIGN_SYSTEM "Màu route trên bản đồ".
- Tìm địa chỉ luôn qua backend (`GeocodingProvider` phía server), client không gọi API geocoding của bên thứ ba.

## 8. Giao diện và theme (P-42)

- `scripts/build_tokens.py` sinh từ `brand/design-system/tokens.json`:
  - web: `web/src/shared/theme/tokens.css`: `:root,[data-theme="light"]` và `[data-theme="dark"]` (màu, bóng, `color-scheme`), `:root` (khoảng cách, bo góc, gradient, `--font-sans`), class `.pw-type-<style>` cho mỗi kiểu chữ;
  - mobile: `mobile/lib/shared/theme/tokens.g.dart`: `PwColorTokens.light/dark` (màu + bóng, tên camelCase: `surfaceRaised`, `mapRoute1`), `PwSpacing`, `PwRadius`, `PwGradients`, `PwTypeStyles` (letter-spacing em đổi ra logical px).
  Script đọc giá trị chặt: màu tên (`red`), `var()`, bóng `inset` hay đơn vị lạ làm build dừng thay vì sinh theme sai. `--only web|mobile` để mỗi client sinh/kiểm phần của mình.
  File sinh có dòng đầu "generated, do not edit"; CI kiểm file sinh khớp tokens.json.
- Component: web port 9 component tham chiếu sang TSX trong `shared/ui/` giữ **đúng tên và props** của `components.d.ts`; mobile làm widget cùng tên (`PwButton`, `StatusPill`, `StopCard`…) cùng ý nghĩa props.
- Theme: mặc định theo hệ điều hành, người dùng đổi được, nhớ trên máy (`localStorage` web / `shared_preferences` mobile). Bản đồ luôn theme sáng.
- Font: Be Vietnam Pro (Google Fonts trên web với subset `vietnamese`; bundle file font trong app mobile, không tải mạng lúc chạy).
- Logo/favicon/app icon chép từ `brand/logo/` bằng script hoặc bước build, không sửa trực tiếp trong `web/`/`mobile/`. Web: `brand/logo` là `publicDir` của Vite (phục vụ ở `/app/favicon.svg`… khi dev, chép vào `dist/` khi build).

## 9. Điều hướng

- Web: React Router, mỗi màn một route (danh sách ở `SCREENS.md`); base path `/app/` (P-47); route cần đăng nhập bọc `RequireAuth` + `RequireRole`. Trạng thái có thể chia sẻ (bộ lọc, trang, plan đang xem) nằm trên URL.
- Mobile: go_router; redirect theo trạng thái phiên; deep link `pathwise://routes/{routeId}` (để dành cho thông báo sau MVP) mở thẳng chi tiết tuyến (đã có `planName`, T45).

## 10. Kiểm thử

| Loại | Web | Mobile | Bắt buộc từ |
|---|---|---|---|
| Unit (format, errors, statusInfo, mapping) | Vitest | `flutter_test` | W01 / M00 |
| Component/widget | Vitest + Testing Library + MSW (fixture từ `docs/ux/samples`) | widget test + fake repository | W01 / M00 |
| Luồng chính có API giả | Testing Library + MSW (đăng nhập, tạo plan, giao việc…) | widget test nhiều màn | từng task |
| E2E với backend thật | Playwright trên stack compose (`be` profile app + `compose.e2e.yaml`, P-49) | `integration_test` (sau MVP) | W07 |
| Accessibility | `axe-core` trong test (tắt `color-contrast` vì jsdom không vẽ) + test tương phản trên giá trị token | semantics test cho nút chính | W01 / M03 |

- Không test nào gọi MapTiler hay dịch vụ ngoài thật; bản đồ trong test dùng `Basemap` giả (tile trống).
- E2E web (W07, `web/e2e/`): Playwright chạy bản build production qua `vite preview` (proxy `/api` tới stack E2E, biến `PATHWISE_API_PROXY`), stack `pathwise-e2e` có server giả Nominatim/OSRM (P-49); đọc OTP và mã mời từ Mailpit của stack (`PATHWISE_MAILPIT`); tài xế báo kết quả qua API của app (`X-Client-Platform: mobile`) vì app Flutter làm ở M00–M03; chặn mọi request ra ngoài `localhost` (font); build không có key MapTiler nên không tải tile. Vitest chỉ chạy `src/**/*.test.{ts,tsx}`.
- Fixture lấy từ `docs/ux/samples/*.json` (dữ liệu thật từ test E2E backend); thêm fixture mới thì sinh từ backend, không viết tay JSON lệch hợp đồng. Web đọc bằng `sample(name)` của `src/test/samples.ts` (W03).
- Web (W03): query dùng chung nhiều feature đặt ở `data/` (`data/storeQuery.ts` cho header, hồ sơ, thành viên); rule kiểm form dùng chung ở `shared/forms/rules.ts` (`requiredRule`, `emailRules`, `passwordRules`).

## 11. Build, CI, môi trường

- Web: pnpm (P-43), Node LTS ghi trong `web/.nvmrc` và `packageManager` của `package.json`; lệnh: `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`.
- Mobile: Flutter stable ghi trong `mobile/.fvmrc` hoặc README; `flutter analyze`, `flutter test`.
- CI (`.github/workflows/ci.yml`, file nóng): job `web` (W01) chạy khi output `web` của `detect-code-changes` là true (đổi `web/`, `api/`, `brand/`, `scripts/build_tokens.py`, `.github/`): install frozen, kiểm `tokens.css` và `schema.d.ts` khớp nguồn, lint, typecheck, test, build. Job `e2e` (W07) chạy khi đổi `web/`, `be/`, `api/`, `compose*.yaml` hoặc `.github/`: dựng stack E2E, cài Chromium, `pnpm e2e`; lỗi thì tải report, trace và log của stack. Job `mobile` thêm ở M00 theo cùng cách. Job backend kiểm drift `api/openapi.json`.
- Biến môi trường: `web/.env.example` (`VITE_API_ORIGIN`: origin của API, rỗng = cùng origin, vì path OpenAPI đã gồm `/api/v1`; `VITE_BASEMAP`, `VITE_MAPTILER_KEY`), mobile `--dart-define` (`API_BASE`, `BASEMAP`, `MAPTILER_KEY`). Không commit `.env`.
- Phiên bản thư viện: chọn bản ổn định đã phát hành ít nhất 2 tuần lúc làm W01/M00, ghi lockfile; Dependabot thêm ecosystem `npm` (web) và `pub` (mobile).

## 12. Bảo mật phía client

- Không lưu access token ngoài bộ nhớ; không log token, OTP, mật khẩu (kể cả `console.log` khi dev: ESLint `no-console` ở mức error trừ `warn`/`error`).
- Không render HTML từ dữ liệu server (`dangerouslySetInnerHTML` cấm bằng lint).
- Link ngoài (Google Maps, tel:) dựng từ dữ liệu đã kiểm (toạ độ là số, số điện thoại E.164).
- Key MapTiler giới hạn theo domain; key khác (nếu có sau này) không bao giờ vào client.

## 13. Mở rộng sau MVP (đã chừa chỗ)

- Vị trí tài xế (T32): `mobile/platform/location` (port `LocationSource`), web thêm lớp marker tài xế trong `RouteMap`.
- Dẫn đường trong app (T33): port `DirectionsSource` ở mobile, màn riêng; khi chưa có thì giữ deep link Google/Apple Maps.
- Chỉnh tay tuyến (T28): `RouteMap` nhận chế độ kéo thả; logic nằm ở feature `plans`.
- Client thứ ba: tạo thư mục mới cùng cấp, dùng lại `api/openapi.json`, `tokens.json`, quy tắc trong file này.
