# SCREENS — Danh sách màn hình, route, API và trạng thái

> Prototype: `https://claude.ai/artifact/JPW7uzXLFrpqfgFmQVjs5N` (tên artboard ở cột "Artboard"; các trạng thái đặc biệt nằm trong nút Tweaks của artboard). Endpoint và thông điệp lỗi chi tiết: `docs/ux/UX_BRIEF.md` §4–§6, `docs/API_SPEC.md`. Quy tắc giao diện: `docs/client/DESIGN_SYSTEM.md`.
>
> Prototype là **tham chiếu bố cục và hành vi**, không phải dữ liệu thật: toạ độ mẫu là lưới giả, kết quả tìm địa chỉ và danh sách thiết bị là minh hoạ. Khi prototype và docs (API_SPEC, UX_BRIEF, DESIGN_SYSTEM) khác nhau thì docs đúng.

## 1. Web dispatcher (`web/`, base path `/app/`)

| Route | Màn hình | Artboard | Vai trò | API chính | Task |
|---|---|---|---|---|---|
| `/login` | Đăng nhập | Đăng nhập | công khai | `POST /auth/log-in`, `POST /auth/refresh` | W01 |
| `/register` | Đăng ký cửa hàng | Đăng ký cửa hàng | công khai | `POST /auth/register` | W02 |
| `/verify-email?email=` | Xác thực email (6 ô OTP) | Xác thực email | công khai | `POST /auth/email/verify`, `/email/verify/resend` | W02 |
| `/forgot-password` | Quên mật khẩu (3 bước) | Quên mật khẩu | công khai | `/password/forgot` → `/password/reset/verify` → `/password/reset` | W02 |
| `/invite` | Nhận lời mời | Nhận lời mời | công khai | `GET /invitations/{code}`, `POST /invitations/accept` | W02 |
| `/profile` | Hồ sơ, đổi mật khẩu, thiết bị | Hồ sơ và bảo mật | O, D | `GET/PATCH /users/me`, `/password/change/otp`, `PUT /password`, `/auth/sessions*` | W02 |
| `/vehicles` | Xe | Xe | O, D | `GET/POST /vehicles`, `PUT /vehicles/{id}` | W03 |
| `/members` | Thành viên và lời mời (2 tab) | Thành viên và lời mời | O sửa, D xem | `/store/members*`, `/store/invitations*` | W03 |
| `/store` | Cửa hàng, kho xuất phát | Cửa hàng | O sửa, D xem | `GET/PUT /store`, `GET /geocoding/search` | W04 |
| `/plans` | Danh sách kế hoạch | Danh sách kế hoạch | O, D | `GET /plans?date=&status=&page=` | W05 |
| `/plans/new`, `/plans/:id/edit` | Tạo/sửa kế hoạch | Tạo kế hoạch | O, D | `POST /plans`, `PUT /plans/{id}`, `GET /store/members?assignable=true`, `GET /vehicles?active=true` | W05 |
| `/plans/:id` (DRAFT, chưa tối ưu) | Tìm toạ độ, sửa điểm lỗi | Soạn plan — tìm toạ độ | O, D | `GET /plans/{id}` (polling), `PATCH /plans/{id}/stops/{stopId}`, `POST /plans/{id}/geocode`, `GET /geocoding/search` | W05 |
| `/plans/:id` (DRAFT, đã tối ưu) | Kết quả tối ưu, bản đồ | Soạn plan — sau khi tối ưu | O, D | `POST /plans/{id}/optimize`, `GET /plans/{id}` | W06 |
| (hộp thoại trên `/plans/:id`) | Xác nhận giao việc, trùng ca, huỷ | Xác nhận giao việc | O, D | `POST /plans/{id}/publish`, `POST /plans/{id}/cancel` | W06 |
| `/plans/:id` (PUBLISHED trở đi) | Theo dõi tiến độ | Theo dõi tiến độ | O, D | `GET /plans/{id}` (30 s) | W06 |
| `*` | Không tìm thấy / không có quyền / tài xế dùng web | (không có artboard) | — | — | W01 |

Từ W01, route của màn chưa làm hiện trang "Sắp có" (ghi task phụ trách) để menu và link đã chạy được; task của màn đó thay trang này.

`/plans/:id` là **một route**: màn hiển thị chọn theo `status`, `optimizedAt`, `processingStatus` của plan (DRAFT chưa tối ưu → tìm toạ độ; DRAFT đã tối ưu → kết quả; PUBLISHED/IN_PROGRESS/COMPLETED/CANCELLED → theo dõi). Không làm route riêng cho từng trạng thái để link chia sẻ luôn đúng.

### 1.1 Hành vi bắt buộc theo màn (ngoài layout trong prototype)

- **Mọi màn đã đăng nhập:** header (logo → `/plans`, menu Kế hoạch/Xe/Thành viên/Cửa hàng, tên cửa hàng, tên người dùng → `/profile`); `aria-current` cho mục đang mở; theme sáng/tối theo hệ thống.
- **Đăng nhập:** lỗi `INVALID_EMAIL_OR_PASSWORD` (hộp lỗi đầu form), `EMAIL_NOT_VERIFIED` (link sang `/verify-email`), `ACCOUNT_DISABLED`, `RATE_LIMITED` (khoá nút, đếm ngược `Retry-After`). DRIVER không có `canDrive` đăng nhập web → trang "Dùng app tài xế".
- **Xác thực email:** 6 ô, tự nhảy ô, Backspace lùi, dán/tự điền mã 6 số vào ô bất kỳ; nút gửi lại khoá 60 s sau khi mã vừa được gửi (từ Đăng ký hoặc sau "Gửi lại mã"); sai 3 lần phải xin mã mới (client đếm lần sai của mã đang dùng, khoá nút Xác thực); URL không có `?email=` (mở từ chỗ khác) thì hỏi thêm email; `EMAIL_ALREADY_VERIFIED` → báo đã xác thực, nút Đăng nhập.
- **Quên mật khẩu:** bước 1 luôn báo "nếu email có tài khoản, mã đã được gửi"; bước 2 có "Gửi lại mã" (khoá 60 s) và "Đổi email khác"; bước 3 trong 10 phút, `INVALID_RESET_TOKEN` → làm lại từ bước 1; mật khẩu nhập lại phải khớp; xong thì mọi thiết bị bị đăng xuất.
- **Nhận lời mời:** ô dán mã 43 ký tự (bỏ khoảng trắng/xuống dòng khi dán); `INVITATION_INVALID` dưới ô (cả khi mã hết hiệu lực lúc bấm "Tham gia"); `INVALID_PHONE_NUMBER` dưới ô số điện thoại; `EMAIL_EXISTED` → hộp lỗi kèm link đăng nhập; sau khi nhận: DRIVER → hướng dẫn mở app (chưa có nút tải app), khác → "Đăng nhập trên web".
- **Hồ sơ:** sửa họ tên, số điện thoại (ô để trống gửi `null` để xoá, F-43); đổi mật khẩu 2 bước (mật khẩu hiện tại → OTP + mật khẩu mới; `INVALID_CURRENT_PASSWORD` dưới ô, `OTP_COOLDOWN`/`RATE_LIMITED` báo trong thẻ); danh sách phiên: thiết bị này đứng đầu, tên thiết bị đọc từ `userAgent`, giờ theo timezone cửa hàng; đăng xuất từng phiên, "Đăng xuất mọi thiết bị khác" (khoá khi không còn phiên khác), đăng xuất ở dòng thiết bị này = đăng xuất; đổi mật khẩu xong thì tải lại danh sách (chỉ còn thiết bị này).
- **Xe:** không xoá, chỉ tắt (ô "Đang dùng" khi sửa); "Hiện cả xe đã tắt" (bỏ chọn → `?active=true` trên URL, gọi `GET /vehicles?active=true`); sức chứa hiện kèm đơn vị ("30 thùng"); lỗi theo field (`VEHICLE_NAME_REQUIRED`, `INVALID_CAPACITY`, `CAPACITY_UNIT_REQUIRED`, `VEHICLE_NAME_EXISTED` dưới ô tên); OWNER và DISPATCHER đều thêm/sửa được (API_SPEC §5).
- **Thành viên:** tab trên URL (`?tab=invitations`, chỉ OWNER; người khác luôn ở tab thành viên); dòng của người đang xem có "(bạn)", người chưa đặt tên hiện "Chưa đặt tên"; OWNER không đổi vai trò/trạng thái được của chính mình (chỉ "Đi giao", qua `PATCH /store/members/me/can-drive`); DRIVER luôn đi giao (ô khoá); vô hiệu hoá có hộp xác nhận `danger` nêu "bị đăng xuất ngay"; kích hoạt lại không cần xác nhận; thay đổi chờ server trả về rồi mới đổi dòng, lỗi hiện ở đầu bảng; lời mời: gửi (vai trò mặc định Tài xế), `EMAIL_EXISTED` dưới ô email, 429 khoá nút theo `Retry-After`, PENDING có "Thu hồi", hạn theo timezone cửa hàng; DISPATCHER xem chỉ đọc, không có tab lời mời (không gọi `GET /store/invitations`).
- **Cửa hàng:** kho đặt bằng ghim (bắt buộc toạ độ): bấm bản đồ, kéo ghim, "Đặt ghim ở giữa bản đồ" hoặc chọn kết quả tìm (`GET /geocoding/search`; chọn kết quả thì điền địa chỉ nếu ô địa chỉ đang trống); địa chỉ tuỳ chọn; toạ độ hiện 5 chữ số thập phân; chưa có kho → cảnh báo vì không tạo được plan, nút "Lưu thay đổi" khoá tới khi có ghim; lỗi theo field dưới ô (`depot.address` → ô địa chỉ, `INVALID_TIMEZONE` → ô múi giờ); lưu xong header đổi tên cửa hàng; DISPATCHER chỉ xem (ô khoá, không có tìm, ghim, nút lưu).
- **Danh sách kế hoạch:** lọc ngày + trạng thái + trang trên URL (`?date=&status=&page=`), 20 kế hoạch một trang, ngày mới nhất trước; bấm dòng hoặc tên mở `/plans/:id`; "Tối ưu lúc" theo timezone cửa hàng; trạng thái trống có nút "Tạo kế hoạch" (tiêu đề nêu ngày khi đang lọc theo ngày).
- **Tạo/sửa kế hoạch:** tối đa 100 điểm, 20 phân công (nút thêm khoá khi đủ); tài xế chọn từ `assignable=true`, xe từ `active=true` (khi sửa, tài xế/xe đang có trong plan vẫn chọn được dù đã bị vô hiệu hoá/tắt); quãng đường tối đa nhập km, gửi mét; cột số lượng theo đơn vị của các xe đã chọn (không có cột cho `ORDER`); ngày mặc định là hôm nay theo timezone cửa hàng; kiểm tức thời trước khi gửi (mã đơn, địa chỉ, khung giờ, ca); lỗi theo field map vào đúng ô của dòng (`stops[i].…`, `assignments[i].…`) kèm hộp "Còn N ô cần sửa"; `STORE_DEPOT_REQUIRED` (hoặc cửa hàng chưa có kho) → link sang `/store`, nút lưu khoá; khi sửa gửi `version` và `id` của điểm giao, toạ độ nhập sẵn chỉ gửi lại khi địa chỉ không đổi; `PLAN_MODIFIED` → hộp thoại "Tải lại"; plan không còn DRAFT → báo không sửa được; lưu xong sang `/plans/:id`.
- **Tìm toạ độ:** polling 2 s khi `processingStatus` khác `IDLE` (thanh tiến độ theo `geocodingSummary`, khoá nút sửa), dừng khi `IDLE` và dùng `version` mới; điểm `FAILED` nổi bật kèm lý do (`geocodeError`), mở sẵn điểm lỗi đầu tiên; sửa bằng địa chỉ mới (`PATCH` địa chỉ; địa chỉ không đổi thì `POST /geocode`) hoặc ghim (`PATCH` toạ độ; bản đồ nhỏ + hộp thoại bản đồ toàn màn hình có tìm địa chỉ, + / −); "Tìm lại toạ độ" khi còn điểm lỗi; "Tối ưu lộ trình" khoá tới khi mọi điểm có toạ độ và có phân công, lý do ghi dưới nút; `PLAN_BUSY`/`PLAN_MODIFIED` → báo lỗi và tải lại plan. Plan đã tối ưu hoặc đã giao việc: W06.
- **Kết quả tối ưu:** thẻ tổng quan (điểm đã xếp tuyến/tổng, quãng đường, thời gian, số tài xế); mỗi tuyến một khối thu gọn được, màu `map-route-n`, tóm tắt "N điểm · km · thời gian · giờ đi–về · tải x/y", bảng điểm theo `sequence` với giờ dự kiến (timezone của plan); chọn dòng ↔ marker, dòng trạng thái dưới bản đồ; `unassignedStops` có `dropReason = INFEASIBLE` là cảnh báo với câu chung (không tự suy lý do; API không trả lý do cụ thể), vẫn giao việc được; "Tối ưu lại" (polling `OPTIMIZING`, khoá các nút, theo dõi `GET /jobs/{id}`; job `FAILED` → câu theo `errorCode` của UX_BRIEF, kết quả cũ giữ nguyên); "Sửa điểm giao" → `/plans/:id/edit`; bản đồ có zoom và nút mở rộng khung; chú giải có kho và tên tài xế.
- **Giao việc:** nút "Giao việc cho N tài xế" (N = tuyến có điểm); hộp xác nhận nêu từng tài xế (xe, số điểm, giờ đi–về), điểm chưa có tài xế, hậu quả; `SHIFT_OVERLAP` mở hộp thứ hai liệt kê phân công trùng (tài xế hoặc xe, ca, ngày) và link `/plans/{conflictPlanId}`, "Sửa phân công" → `/plans/:id/edit`, plan không đổi; lỗi khác hiện trong hộp; thành công → toast "Đã giao việc cho N tài xế." + chuyển sang theo dõi.
- **Huỷ:** được ở DRAFT và PUBLISHED; hộp xác nhận `danger` nêu hậu quả (DRAFT: tài xế chưa thấy tuyến; PUBLISHED: tài xế không còn thấy tuyến); xong → thông báo "Kế hoạch đã huỷ." trên màn theo dõi, nút "Về danh sách kế hoạch".
- **Theo dõi:** trạng thái plan, "cập nhật lúc" khi đang giao, câu kết thúc khi COMPLETED; thẻ đếm theo kết quả (đã giao/tổng điểm có tuyến, không giao được, bỏ qua, không xếp được); mỗi tuyến dùng `routes[].status` của server (T45) và thanh tiến độ chia theo kết quả (đếm để hiển thị, không suy trạng thái); lý do `FAILED`; marker theo kết quả (đã giao `success`, không giao được `danger`, bỏ qua `signal`, chờ giao màu tuyến); tự tải lại 30 s khi tab hiện, dừng khi COMPLETED/CANCELLED; PUBLISHED có "Huỷ kế hoạch".

## 2. App tài xế (`mobile/`)

| Màn hình | Artboard | API | Task |
|---|---|---|---|
| Đăng nhập | Tài xế — đăng nhập | `POST /auth/log-in` (`X-Client-Platform: mobile`), `POST /auth/refresh` | M00 |
| Tuyến hôm nay (đổi ngày) | Tài xế — tuyến hôm nay | `GET /driver/routes?date=` | M01 |
| Chi tiết tuyến | Tài xế — chi tiết tuyến | `GET /driver/routes/{routeId}` | M01 |
| Báo kết quả điểm giao | Tài xế — báo kết quả | `PATCH /driver/routes/{routeId}/stops/{stopId}/status` | M02 |
| Quên mật khẩu, nhận lời mời | (dùng màn web tương ứng) | như web | M03 |

Hành vi bắt buộc:
- Chữ `body-touch` 16px, vùng chạm ≥ 48px, nút kết quả 56px xếp dọc ở nửa dưới (DESIGN_SYSTEM).
- **Tuyến hôm nay:** mặc định hôm nay theo timezone store (server tự xử khi không gửi `date`); thẻ tuyến có `status` của route, `completedCount/stopCount`, giờ xuất phát (theo `timezone` của plan, T47); kéo xuống tải lại; ngày không có tuyến → trạng thái trống. Route `/routes`. Đổi ngày gửi `date` tính từ ngày đang xem; nhãn "Hôm nay/Hôm qua/Ngày mai" so với hôm nay theo timezone của route đã tải, chưa có route nào thì theo ngày trên máy. Chữ viết tắt tên ở đầu màn mở menu tài khoản (M03): tên, email, giao diện Theo máy / Sáng / Tối (nhớ trên máy), "Đăng xuất" có hộp xác nhận.
- **Chi tiết tuyến:** route `/routes/:routeId` (mở thẳng được, id sai → về danh sách); tiêu đề `planName` (T45); điểm tiếp theo nổi bật (`StopCard current`) với "Chỉ đường" (Google Maps trên Android, Apple Maps trên iOS, dựng từ `lat/lon`, D-07) và "Báo kết quả"; các điểm còn lại; nhóm đã xong (thu gọn được) có lý do thất bại.
- **Báo kết quả:** route `/routes/:routeId/stops/:stopId` (mở bằng "Báo kết quả" của điểm tiếp theo; tiêu đề "Điểm N/tổng"; "Gọi khách" và "Chỉ đường"; đọc lại tuyến khi mở). Đã giao / Không giao được (bắt buộc lý do ≤ 255, gợi ý nhanh) / Bỏ qua, mỗi nút có xác nhận vì không hoàn tác (P-34); xong → điểm tiếp theo; `routeStatus = COMPLETED` → "Bạn đã xong tuyến này"; mất mạng → "Chưa gửi được: mất kết nối." kèm "Gửi lại" gửi cùng nội dung (idempotent, P-36), không có hàng đợi trên máy, tài xế chờ (P-51); `PLAN_NOT_ACTIVE` → báo plan đã huỷ/kết thúc, khoá nút. Chi tiết tuyến và màn báo kết quả tự tải lại mỗi 30 giây; tuyến trả `PLAN_NOT_ACTIVE` (plan bị huỷ, T48) hoặc 404 → câu ở mục dưới kèm "Về danh sách tuyến".
- Quên mật khẩu và nhận lời mời trên mobile: mở trình duyệt tới màn web tương ứng (`WEB_BASE/forgot-password`, `WEB_BASE/invite`; chủ dự án chọn ở M03, không làm native).
- Tuyến của plan đã publish rồi bị huỷ (`PLAN_NOT_ACTIVE`, T48) ở chi tiết tuyến và màn báo kết quả: "Plan đã kết thúc hoặc đã bị huỷ. Dừng giao và liên hệ điều phối viên." kèm "Về danh sách tuyến"; 404: "Tuyến này không còn. Liên hệ điều phối viên nếu cần."
