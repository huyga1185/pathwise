# DESIGN_SYSTEM — Brand book và component của PathWise

> Bản trong repo của design system PathWise (Claude Design, artifact `https://claude.ai/artifact/CBQwFPSiBSH6JavBwHgutU`, chép ngày 2026-11-03, version artifact 15). **Repo là nguồn đúng cho code**; artifact là nơi chủ dự án xem và góp ý trực quan. Đổi token hay quy tắc thì sửa cả hai trong cùng PR (xem `docs/client/README.md` §4).
>
> File đi kèm trong `brand/design-system/`:
> - `tokens.json`: mọi token (màu theo theme sáng/tối, chữ, khoảng cách, bo góc, bóng, gradient). Code sinh CSS variables (web) và Dart theme (mobile) từ file này, không chép tay giá trị.
> - `components.d.ts`: props của 9 component tham chiếu (tên và props là hợp đồng; web giữ đúng tên/props, mobile giữ đúng tên/ý nghĩa).
> - `reference-components.js` / `.css`: implementation tham chiếu bằng React không build (dùng trong artifact). Web port sang TSX, không import trực tiếp.
>
> Prototype bấm được (19 màn, web + app tài xế): `https://claude.ai/artifact/JPW7uzXLFrpqfgFmQVjs5N`. Danh sách màn và artboard tương ứng: `docs/client/SCREENS.md`.

PathWise là phần mềm lập tuyến giao hàng cho cửa hàng nhỏ ở Việt Nam. Có hai bề mặt: **web dashboard** cho chủ cửa hàng và điều phối viên (bàn làm việc, nhiều bảng và bản đồ), và **app tài xế** (Flutter, dùng ngoài đường, một tay, dưới nắng). Hệ thống này dùng chung cho cả hai; khác biệt ghi rõ ở từng mục.

## Nội dung và giọng văn

- Viết tiếng Việt, câu ngắn, gọi việc bằng tên người dùng quen: "điểm giao", "tuyến", "ca làm", "giao việc". Thuật ngữ hệ thống chỉ dùng khi không có từ thường: viết "Tối ưu lộ trình", không viết "Optimize"; "Giao việc cho tài xế", không viết "Publish".
- Nút nói đúng việc sẽ xảy ra, động từ đứng đầu: "Tối ưu lộ trình", "Giao việc", "Huỷ kế hoạch", "Đã giao", "Không giao được", "Bỏ qua". Xong thì báo bằng câu ngắn cùng động từ: "Đã giao việc cho 2 tài xế."
- Viết hoa chữ cái đầu câu, không viết hoa mọi từ. Nhãn trạng thái (`label`) viết hoa toàn bộ.
- Không xưng "chúng tôi", không xin lỗi trong thông báo lỗi. Lỗi nói chuyện gì sai và sửa thế nào: "Còn 1 điểm giao chưa có toạ độ. Sửa địa chỉ hoặc đặt ghim trên bản đồ."
- Không dùng emoji. Số có đơn vị: "12,4 km", "2 giờ 35 phút", "4/6 thùng". Giờ theo 24 giờ: "09:00–11:00".
- Số điện thoại hiển thị dạng `0905 123 456`; dữ liệu từ API là E.164 (`+84905123456`).
- Danh sách thông điệp lỗi theo mã API: `docs/ux/UX_BRIEF.md` §6 trong repo PathWise.

## Màu

- Nền trang `surface`; panel, bảng, thẻ, khung bản đồ `surface-raised`. Phân tầng chính bằng nền. Bóng chỉ cho lớp nổi lên trên trang: `shadow-raised` cho thẻ đăng nhập và thông báo, `shadow-overlay` cho hộp thoại. Bảng, panel và thẻ dữ liệu không có bóng.
- Gradient: chỉ `brand-gradient` cho panel thương hiệu (màn tài khoản, đăng nhập app tài xế), và một lớp sáng rất nhẹ trên nút chính (có sẵn trong `Button`). Không dùng gradient cho nền trang, thẻ dữ liệu, chữ hay icon.
- Chữ `ink`; chữ phụ (địa chỉ, ETA, chú thích) `ink-muted`. Không dùng màu trạng thái cho chữ thường.
- `route` là màu duy nhất của thương hiệu: nút chính, link, tab đang chọn, vòng focus, và đường route mặc định trên bản đồ. Mỗi màn hình chỉ một nút chính `route`.
- Dòng đang chọn, stop tài xế sắp tới: nền `surface-tint`, chữ vẫn `ink`.
- Đường kẻ giữa các dòng `line`. Viền ô nhập, checkbox và control dùng `ink-muted`, vì `line` quá nhạt để làm viền duy nhất.

### Theme sáng và tối

- Hai theme dùng cùng tên token; client chỉ đổi giá trị. Mặc định theo cài đặt hệ điều hành (`prefers-color-scheme` trên web, `ThemeMode.system` trên Flutter), người dùng chọn lại được và lựa chọn được nhớ trên máy.
- Ở theme tối, màu nhấn (`route`, trạng thái) sáng lên để đọc được trên nền tối; vì thế chữ đặt trên nền màu nhấn luôn dùng `on-accent`, không viết cứng màu trắng.
- Phân tầng ở theme tối vẫn bằng nền: `surface` tối nhất, `surface-raised` sáng hơn một bậc. Bóng chỉ cho lớp nổi như ở theme sáng; token bóng đậm hơn để còn thấy trên nền tối.
- Bản đồ: tile OSM chuẩn chỉ có bản sáng. Ở theme tối, khung bản đồ vẫn dùng tile sáng cho đến khi chọn nguồn tile tối (cần quyết định riêng vì là dịch vụ ngoài); đường route và marker vẫn dùng token theo theme sáng để nổi trên tile sáng.
- Ảnh và logo: theme tối dùng mark trắng.

### Màu theo trạng thái

Mỗi trạng thái hiện bằng **pill**: nhãn `label` viết hoa, chữ `on-accent` trên nền màu trạng thái, bo `radius-full`. Màu không bao giờ đứng một mình: luôn có chữ.

| Trạng thái (API) | Nhãn | Token |
|---|---|---|
| Plan `DRAFT` | NHÁP | `ink-muted` |
| Plan `PUBLISHED` | ĐÃ GIAO VIỆC | `info` |
| Plan `IN_PROGRESS`, route `IN_PROGRESS` | ĐANG GIAO | `signal` |
| Plan `COMPLETED`, route `COMPLETED` | HOÀN THÀNH | `success` |
| Plan `CANCELLED` | ĐÃ HUỶ | `danger` |
| Stop `PENDING`, route `PENDING` | CHỜ GIAO | `ink-muted` |
| Stop `DELIVERED` | ĐÃ GIAO | `success` |
| Stop `FAILED` | KHÔNG GIAO ĐƯỢC | `danger`, kèm lý do bên dưới bằng `small` |
| Stop `SKIPPED` | BỎ QUA | `signal` |
| Stop chưa có tuyến, `dropReason` `NOT_OPTIMIZED` | CHƯA XẾP TUYẾN | `ink-muted` |
| Stop chưa có tuyến, `dropReason` `INFEASIBLE` | KHÔNG XẾP ĐƯỢC | `signal` |
| Plan `processingStatus` `GEOCODING` / `OPTIMIZING` | ĐANG TÌM TOẠ ĐỘ / ĐANG TỐI ƯU | `info`, kèm `ProgressBar` |
| Địa chỉ `geocodeStatus` `PENDING` | ĐANG TÌM TOẠ ĐỘ | `info` |
| Địa chỉ `geocodeStatus` `FAILED` | KHÔNG TÌM THẤY | `danger` |
| Lời mời `PENDING` / `ACCEPTED` / `REVOKED` / `EXPIRED` | CHỜ NHẬN / ĐÃ NHẬN / ĐÃ THU HỒI / HẾT HẠN | `info` / `success` / `ink-muted` / `ink-muted` |
| Thành viên `PENDING_VERIFICATION` / `ACTIVE` / `DISABLED` | CHƯA XÁC THỰC / ĐANG HOẠT ĐỘNG / ĐÃ VÔ HIỆU HOÁ | `ink-muted` / `success` / `danger` |
| Job `QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` | ĐANG CHỜ / ĐANG CHẠY / XONG / LỖI | `ink-muted` / `info` / `success` / `danger` |

`geocodeStatus` `PROVIDED` / `RESOLVED` và `processingStatus` `IDLE` không hiện pill. Component `StatusPill` giữ đúng bảng này (`kind` + `status` của API); client không tự viết nhãn.

### Màu route trên bản đồ

Plan có tối đa 20 route. Route thứ *n* (theo thứ tự trong `routes` của API) dùng `map-route-((n-1) % 7 + 1)`: teal (trùng `route`), tím, nâu, xanh biển, mận, chì, ô liu. Bảng này cố ý tránh đỏ, cam và xanh lá vì đó là màu trạng thái; bảy màu cùng độ đậm nên phân biệt chủ yếu bằng sắc, vì vậy **luôn ghi tên tài xế** cạnh đường và trong chú giải, không để người dùng nhận route chỉ bằng màu. Từ route thứ 8 màu lặp lại.

Stop trên bản đồ là `MapMarker` mang số `sequence`, màu theo route (`routeIndex`); stop không xếp được dùng `signal`, chưa xếp tuyến dùng `ink-muted`, cả hai không có số. Kho (depot) là icon nhà kho tô đặc màu thương hiệu `#0b6a5c` (giữ nguyên ở hai theme) có viền trắng và bóng nhẹ để nổi trên tile, cửa kho trắng, cỡ 30px, không có nền vuông; chú giải dưới bản đồ luôn có mục "Kho xuất phát" với cùng icon. Sau khi plan chạy, marker đổi theo kết quả: đã giao `success`, không giao được `danger`.

## Component tham chiếu

Bộ component React (`window.PathWise`) là bản tham chiếu cho web dashboard: Button, TextField, StatusPill, ProgressBar, DataTable, StopCard, MapMarker, Dialog, Toast, kèm hàm `formatPhone` và `statusInfo`. Web (React + Vite) dựng lại theo cùng tên, props và class; app tài xế (Flutter) dựng lại theo cùng token và quy tắc, lấy `StopCard` và nút kết quả làm mẫu. Mọi màu trong component lấy từ token, nên theme tối không cần CSS riêng.

## Chữ

- Một họ chữ: **Be Vietnam Pro** (Google Fonts), vẽ riêng cho tiếng Việt nên dấu thanh không chạm dòng trên. Tải các weight 400, 600, 700 kèm subset `vietnamese`.
- Web: chữ thường `body` (14px); app tài xế: `body-touch` (16px).
- Mỗi màn hình một `display` (tên plan hoặc tên cửa hàng). Panel dùng `title`, thẻ và nhãn stop dùng `heading`.
- Cột số (ETA, km, tải) bật `font-variant-numeric: tabular-nums` để thẳng hàng.
- `label` chỉ cho pill trạng thái và tiêu đề cột.

## Khoảng cách, bo góc, bố cục

- Khoảng cách theo bậc `space-1` (4), `space-2` (8), `space-4` (16), `space-6` (24); không dùng giá trị lẻ.
- Panel và thẻ padding `space-4`, cách nhau `space-6`. Ô bảng padding dọc `space-2`.
- Bo góc: ô nhập, nút, khung bảng, khung bản đồ `radius-sm`; thẻ, panel, hộp thoại `radius-md`; thẻ nổi đứng riêng (thẻ đăng nhập) `radius-lg`; pill và marker `radius-full`.
- Web dashboard: màn hình soạn plan chia đôi, bảng stop bên trái, bản đồ bên phải; dưới 1024px bản đồ xuống dưới bảng.
- App tài xế: vùng chạm tối thiểu 48px; ba nút kết quả của stop (Đã giao, Không giao được, Bỏ qua) xếp dọc, cao 56px, ở nửa dưới màn hình để bấm bằng ngón cái.

## Trạng thái và tương tác

- Focus: viền 2px `route`, cách control 2px, rõ trên mọi nền.
- Lỗi form: viền ô `danger`, câu lỗi `small` màu `danger` ngay dưới ô.
- Plan đang xử lý nền (geocode, tối ưu): khoá các nút sửa, hiện thanh tiến độ `info` và tải lại định kỳ; không dùng spinner che cả trang.
- Hành động không hoàn tác được (giao việc, huỷ plan, báo kết quả stop) có bước xác nhận ngay trong trang, nêu rõ hậu quả.

## Biểu tượng

Chưa có bộ icon. Khi chọn, dùng một bộ nét đơn (outline, nét 1,5–2px) cho cả web và app; icon luôn đi kèm nhãn chữ, trừ nút gọi điện và nút mở bản đồ có nhãn ẩn cho trình đọc màn hình.

## Logo

Mark là chữ P kèm con đường và ghim vị trí: con đường là tuyến giao, ghim là điểm giao. File gốc nằm ở `brand/logo/` trong repo PathWise; `web/` và `mobile/` chép bản cần dùng, không sửa trực tiếp.

- **Mark màu** (`pathwise-mark.svg`, nền chuyển `#1fa189` → `#0b6a5c` → `#054a40`): dùng trên nền sáng (`surface`, `surface-raised`).
- **Mark trắng** (`pathwise-mark-white.svg`): dùng trên nền màu thương hiệu `#0b6a5c`, ảnh tối, và mọi nền ở theme tối (phần tối của mark màu chìm vào nền tối). Không đặt mark màu lên nền màu. Ở theme tối, `route` sáng hơn nên không làm nền cho mark trắng.
- **Lockup**: mark đứng trước, cách chữ `space-2`, chữ **PathWise** Be Vietnam Pro 700 màu `ink`; chiều cao mark bằng khoảng 1,4 lần cỡ chữ. Header web dùng lockup; chỗ hẹp chỉ dùng mark.
- **Cỡ nhỏ**: dưới 48px dùng bản không có vạch đường, vì vạch đứt thành nhiễu ở cỡ đó. `favicon.svg` (icon tab trình duyệt), `favicon-32.png` và `favicon.ico` (16, 32, 48px, dự phòng cho trình duyệt và công cụ không đọc SVG) là bản không vạch; `favicon-192.png`, `favicon-512.png`, `apple-touch-icon.png` giữ vạch. Mark không nhỏ hơn 16px.
- **App icon** (`app-icon.svg`, `app-icon-foreground.svg` cho icon Android thích ứng): chỉ dùng cho icon cài đặt, không dùng trong giao diện.
- Không đổi màu, xoay, kéo méo, thêm bóng hay viền cho mark. Chừa khoảng trống quanh mark tối thiểu bằng 1/4 chiều cao mark.
- Màu `route` của hệ thống trùng với màu chủ đạo của mark (`#0b6a5c`), nên nút chính và logo luôn cùng một màu.

## Hướng dẫn từng component

### Button

Nút bấm cho mọi hành động; mỗi màn hình chỉ một nút `primary`, cho việc chính của màn đó.

- **primary** (`route`): việc chính — "Tối ưu lộ trình", "Giao việc", "Lưu".
- **secondary** (mặc định): việc phụ, nhiều nút cùng lúc.
- **quiet**: hành động nhẹ trong bảng hoặc thanh công cụ ("Xem bản đồ").
- **danger**: hành động phá huỷ ("Huỷ kế hoạch"); luôn đi qua `Dialog` xác nhận.
- **success / danger / signal** cỡ `xl`: chỉ cho ba nút kết quả của stop trong app tài xế (Đã giao, Không giao được, Bỏ qua), xếp dọc, nửa dưới màn hình.
- Cỡ: `md` 36px cho web; `lg` 48px là tối thiểu trong app tài xế; `xl` 56px cho nút kết quả.
- Nhãn: động từ đứng đầu, viết hoa chữ cái đầu câu. Khi đang gửi request dùng `busy` và đổi nhãn sang dạng tiếp diễn ("Đang giao việc…"), không thay bằng spinner.
- Màu chữ trên nút màu dùng `on-accent`, đổi theo theme.

### TextField

Ô nhập có nhãn, chữ hướng dẫn và câu lỗi; dùng cho mọi form trên web và app.

- Nhãn luôn hiện phía trên ô, không dùng placeholder thay nhãn. Placeholder chỉ để gợi định dạng ("0905 123 456").
- `error`: câu lỗi lấy từ bảng thông điệp theo mã lỗi API (`docs/ux/UX_BRIEF.md` §6); nói chuyện gì sai và sửa thế nào. Ô lỗi có viền `danger` 2px, câu lỗi `small` màu `danger` ngay dưới ô, liên kết qua `aria-describedby`.
- Với lỗi validation từ API (`fields`), gắn từng câu vào đúng ô theo tên field.
- `multiline` cho ghi chú stop.
- Web (W01) thêm `startIcon` (icon trong ô, ví dụ thư, ổ khoá ở màn tài khoản), `endAction` (nút trong ô, ví dụ hiện mật khẩu, có `aria-label` và `aria-pressed`) và `size="lg"` (48px, chữ 16px cho màn tài khoản).
- Trong app tài xế, form hiếm khi xuất hiện (chỉ lý do không giao được); khi có, dùng cỡ chữ `body-touch` qua CSS của client.

### StatusPill

Nhãn trạng thái viết hoa trên nền màu trạng thái; truyền thẳng giá trị enum của API qua `kind` + `status`.

- Ánh xạ nhãn và màu nằm trong component (`statusInfo`), khớp bảng trạng thái ở README: client không tự viết nhãn.
- `kind`: `plan` (PlanStatus), `route` (RouteStatus), `stop` (StopStatus), `dropReason` (lý do stop nằm trong `unassignedStops`: `NOT_OPTIMIZED` → CHƯA XẾP TUYẾN, `INFEASIBLE` → KHÔNG XẾP ĐƯỢC), `geocode` (GeocodeStatus), `processing` (ProcessingStatus của plan), `invitation` (InvitationStatus), `member` (UserStatus của thành viên), `job` (JobStatus).
- Trạng thái không cần pill thì không hiện gì: geocode `PROVIDED`/`RESOLVED`, processing `IDLE`.
- Màu không bao giờ đứng một mình: pill luôn có chữ. Stop `FAILED` đi kèm lý do ngay dưới, chữ `small`.
- Chữ pill dùng `on-accent` (trắng ở theme sáng, gần đen ở theme tối).

### ProgressBar

Thanh tiến độ mảnh màu `info` cho việc chạy nền của plan (geocode, tối ưu); không che trang.

- Plan có `processingStatus` khác `IDLE`: hiện thanh này ở đầu màn hình plan, khoá các nút sửa, tải lại plan định kỳ cho tới khi về `IDLE`.
- Biết số lượng (đã geocode 7/12 stop) thì truyền `value`/`max`; không biết (tối ưu) thì bỏ trống để chạy vô định.
- Tôn trọng `prefers-reduced-motion`: thanh vô định đứng yên.
- Không dùng spinner che cả trang.

### DataTable

Bảng dữ liệu của web dashboard: danh sách stop, plan, xe, thành viên; dòng chọn được nối với bản đồ.

- Nền `surface-raised`, đường kẻ `line`, tiêu đề cột `label` viết hoa màu `ink-muted`, tiêu đề dính khi cuộn.
- Cột số (`numeric`): ETA, khung giờ, km, tải — căn phải, `tabular-nums`.
- `onRowClick` + `selectedKey`: dòng đang chọn nền `surface-tint`; chọn được bằng bàn phím (Tab, Enter). Trên màn soạn plan, chọn dòng thì marker tương ứng trên bản đồ `selected`, và ngược lại.
- `empty`: câu hướng dẫn việc tiếp theo, ví dụ "Chưa có điểm giao. Thêm điểm giao để tối ưu lộ trình."
- Phân trang theo API (`page`, `size`) đặt dưới bảng; component không tự phân trang.
- Ô chứa trạng thái dùng `StatusPill`.

### StopCard

Thẻ một điểm giao trong app tài xế: số thứ tự, đơn và khách, địa chỉ, giờ dự kiến, ghi chú, nút gọi và các nút kết quả.

- Stop tài xế sắp tới: `current` (nền `surface-tint`, viền `route`); chỉ một thẻ `current` mỗi lúc.
- `phone` nhận E.164 từ API, hiện dạng `0905 123 456` và là nút gọi `tel:` cao 48px.
- `actions`: ba `Button` cỡ `xl`, `block`, xếp dọc — Đã giao (`success`), Không giao được (`danger`), Bỏ qua (`signal`). Chỉ hiện trên stop `PENDING` của plan đang chạy; bấm xong mới gọi API, có bước xác nhận ngay trong trang.
- Không giao được: hỏi lý do (bắt buộc, tối đa 255 ký tự) trước khi gửi; lý do hiện trên thẻ bằng `danger`.
- Chữ cỡ `body-touch` (16px); vùng chạm tối thiểu 48px.
- Đây là bản tham chiếu HTML cho thiết kế; app tài xế (Flutter) dựng lại theo cùng token và quy tắc.

### MapMarker

Chấm tròn đánh số `sequence` đánh dấu một điểm giao trên bản đồ (Leaflet trên web, flutter_map trong app).

- Mặc định `route`; điểm đã giao `success`, không giao được `danger`, không xếp được (`INFEASIBLE`) `signal` không số, chưa xếp tuyến `muted` không số.
- `selected`: to hơn, viền `ink` — đồng bộ với dòng đang chọn trong `DataTable`.
- Viền trắng 2px và bóng nhẹ để nổi trên tile bản đồ.
- **Luôn dùng màu theme sáng** (component tự đặt `data-theme="light"`): tile OSM chỉ có bản sáng, nên marker giữ màu sáng cả khi app ở theme tối.
- Trên web, dùng làm `L.divIcon` (render ra HTML); kích thước 28px, 36px khi chọn.
- `routeIndex` (0 = route đầu tiên): tô theo `map-route-1…7`, ghi đè `tone`. Dùng khi plan còn nháp hoặc stop còn `PENDING`; stop đã có kết quả thì dùng `tone`.
- Luôn kèm chú giải tên tài xế theo màu tuyến.

### Dialog

Hộp xác nhận cho hành động không hoàn tác được: giao việc, huỷ plan, xoá; nêu rõ hậu quả trước khi làm.

- Tiêu đề là câu hỏi có tên đối tượng: "Huỷ kế hoạch "Giao sáng 01/12"?".
- Thân: hậu quả cụ thể bằng số ("2 tài xế đang có tuyến…", "không khôi phục được").
- Nút xác nhận lặp lại động từ của hành động ("Huỷ kế hoạch", "Giao việc"), không dùng "OK"/"Có". Nút phụ "Quay lại".
- `tone="danger"` cho hành động phá huỷ (nút `danger`, `role="alertdialog"`).
- Mở thì focus vào nút đầu tiên; `Esc` đóng; đóng thì trả focus về chỗ cũ.
- `size`: `md` (mặc định, xác nhận), `lg` (form ngắn), `full` (gần toàn màn hình, dùng cho chọn vị trí trên bản đồ: ô tìm địa chỉ + bản đồ lớn + ghim kéo được). `confirmDisabled` khoá nút xác nhận tới khi nội dung hợp lệ.
- `busy` trong lúc request chạy; lỗi API hiện trong thân hộp, không đóng hộp.

### Toast

Thông báo ngắn sau một hành động, ở góc dưới màn hình; không thay cho lỗi gắn với ô nhập.

- Câu ngắn, cùng động từ với hành động vừa làm: "Đã giao việc cho 2 tài xế."
- `success` / `info` tự ẩn sau 5 giây; `danger` ở lại tới khi người dùng đóng hoặc bấm hành động.
- Một hành động tối đa (`actionLabel`): "Thử lại", "Xem".
- Lỗi theo field (validation) hiện dưới ô, không dùng toast. Lỗi 429 dùng `Retry-After` để nói thời gian chờ.
- `role="status"`, lỗi `role="alert"` để trình đọc màn hình đọc.
