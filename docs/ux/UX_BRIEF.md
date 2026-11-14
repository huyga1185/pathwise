# UX_BRIEF — Đầu vào thiết kế client

> Tài liệu bàn giao cho bước thiết kế (Claude Design) và code client (web, Flutter; D-09). **Hợp đồng API là `docs/API_SPEC.md`**; file này chỉ nói client cần thể hiện gì và gọi endpoint nào. Lệch với API_SPEC thì API_SPEC đúng.
> Dữ liệu mẫu thật: `docs/ux/samples/*.json`, sinh từ test E2E `MvpScenarioIntegrationTest` (plan 24 stop ở Đà Nẵng, 2 tài xế). Tài liệu OpenAPI để sinh code client: artifact `api-contract` của CI (`openapi.json`).
> Cập nhật: 2026-11-03.

## 1. Người dùng và nền tảng

| Vai trò | Nền tảng | Việc chính |
|---|---|---|
| OWNER | Web | Mọi việc của DISPATCHER, cộng: thông tin store và depot, mời và quản lý thành viên, bật `canDrive` cho mình. |
| DISPATCHER | Web | Xe; soạn plan, sửa stop geocode lỗi, optimize, publish, huỷ; theo dõi tiến độ giao. |
| DRIVER (hoặc OWNER/DISPATCHER có `canDrive`) | Mobile (Flutter) | Xem route trong ngày, mở bản đồ dẫn đường, báo kết quả từng stop. |
| ADMIN | Web, khu `/admin` (W11, M5, P-59) | Xem cửa hàng (số tổng, không nội dung), người dùng; sau T56–T58, T60: khoá chỉ đọc, hỗ trợ tài khoản, nhật ký, thông số. |

Một người có `canDrive` dùng cả hai: web để điều phối, mobile để đi giao. `GET /users/me` trả `role`, `canDrive` để client chọn menu.

## 2. Quy ước hiển thị

- **Ngôn ngữ:** tiếng Việt. API trả **mã** (`code`, `details[].code`), client dịch sang câu (§6). Không hiện mã thô cho người dùng, trừ khi không có bản dịch (hiện câu chung kèm mã nhỏ).
- **Thời gian:** sự kiện là ISO UTC (`plannedArrival`, `createdAt`…), hiển thị theo `timezone` của plan hoặc store (`Asia/Ho_Chi_Minh`). Giờ trong ngày (ca, time window) là `HH:mm` địa phương, hiển thị nguyên.
- **Đơn vị:** quãng đường mét → hiển thị km (1 chữ số thập phân); thời lượng giây → giờ/phút. Capacity và demand là số nguyên kèm loại (`KG`, `BOX`, `PACKAGE`, `ORDER`).
- **Số điện thoại:** API trả E.164 (`+84905123456`); client tự định dạng (`0905 123 456`) và dùng cho nút gọi.
- **Mỗi lỗi** có header `X-Request-Id`: hiện trong chi tiết lỗi ("Mã tra cứu") để người dùng báo lại.

## 3. Phiên đăng nhập

- **Web:** refresh token nằm trong cookie HttpOnly (`credentials: 'include'`); access token (14 phút) giữ trong bộ nhớ, không lưu `localStorage`. Gặp 401 thì gọi `POST /auth/refresh` một lần rồi gửi lại request; refresh cũng 401 thì về màn đăng nhập.
- **Mobile:** gửi header `X-Client-Platform: mobile`; refresh token đi trong body, lưu ở secure storage.
- Hai request refresh đồng thời có thể làm mất phiên (F-29): client chỉ cho một lần refresh chạy tại một thời điểm, các request khác chờ kết quả.
- Đăng xuất: `POST /auth/log-out` rồi xoá token. Màn "Thiết bị đăng nhập" dùng `GET /auth/sessions` (`current` đánh dấu máy hiện tại).

## 4. Màn hình

### 4.1 Web (OWNER, DISPATCHER)

| Màn hình | Endpoint | Ghi chú thiết kế |
|---|---|---|
| Đăng ký | `POST /auth/register` | Sau đó chuyển sang nhập OTP. |
| Xác thực email | `POST /auth/email/verify`, `/email/verify/resend` | OTP 6 số, hạn 5 phút; nút gửi lại khoá 60 s; sai 3 lần phải xin mã mới. |
| Đăng nhập | `POST /auth/log-in` | `EMAIL_NOT_VERIFIED` → dẫn sang xác thực; `ACCOUNT_DISABLED` → báo liên hệ chủ cửa hàng. |
| Quên mật khẩu (3 bước) | `POST /auth/password/forgot` → `/password/reset/verify` → `/password/reset` | Bước 1 luôn báo "đã gửi nếu email tồn tại". Xong thì mọi thiết bị bị đăng xuất. |
| Hồ sơ, đổi mật khẩu, thiết bị | `GET/PATCH /users/me`, `POST /auth/password/change/otp`, `PUT /auth/password`, `/auth/sessions` | Đổi mật khẩu: mật khẩu hiện tại → OTP qua email → mật khẩu mới. |
| Cửa hàng (OWNER) | `GET/PUT /store` | Depot chọn trên bản đồ (bắt buộc `lat`/`lon`, địa chỉ tuỳ chọn); có thể tìm bằng `GET /geocoding/search`. Chưa có depot thì không tạo được plan (`STORE_DEPOT_REQUIRED`). |
| Thành viên (OWNER sửa, DISPATCHER xem) | `GET /store/members`, `PATCH /store/members/{userId}`, `PATCH /store/members/me/can-drive` | Vô hiệu hoá thì người đó bị đăng xuất ngay. Không sửa được role/status của OWNER. |
| Lời mời (OWNER) | `POST/GET /store/invitations`, `POST /store/invitations/{id}/revoke` | Trạng thái `PENDING`, `ACCEPTED`, `REVOKED`, `EXPIRED` (7 ngày). |
| Nhận lời mời (công khai, mã trong email) | `GET /invitations/{code}`, `POST /invitations/accept` | Email hiện chỉ chứa mã mời 43 ký tự (chưa có link), nên cần ô dán mã; sau đó hiện tên cửa hàng, email, vai trò; người được mời đặt mật khẩu và tên. |
| Xe | `GET/POST /vehicles`, `GET/PUT /vehicles/{id}` | Tắt xe thay vì xoá. |
| Danh sách plan | `GET /plans?date=&status=` | Lọc theo ngày và trạng thái; phân trang. |
| Soạn plan | `POST /plans`, `PUT /plans/{id}`, `PATCH /plans/{id}/stops/{stopId}` | Bảng stop + bản đồ; phân công tài xế/xe/ca (`GET /store/members?assignable=true`, `GET /vehicles?active=true`). Tối đa 100 stop, 20 phân công. Mọi request sửa gửi `version` đang có. |
| Geocode | tự chạy khi lưu; `POST /plans/{id}/geocode` | §5.1. Stop `FAILED` cần nổi bật kèm lý do; sửa bằng địa chỉ mới hoặc kéo pin trên bản đồ. |
| Tối ưu và kết quả | `POST /plans/{id}/optimize`, `GET /plans/{id}` | Mỗi route một màu trên bản đồ, danh sách stop theo `sequence` kèm ETA; tổng km/thời gian, tải/capacity mỗi xe; `unassignedStops` có `dropReason = INFEASIBLE` là cảnh báo, vẫn publish được. Optimize mất khoảng 10 s: hiện trạng thái đang tính. |
| Publish, huỷ | `POST /plans/{id}/publish`, `/cancel` | `SHIFT_OVERLAP` chỉ ra phân công nào trùng ca với plan nào (`details[].conflictPlanId`, dẫn link sang plan đó). Huỷ được ở DRAFT và PUBLISHED. |
| Theo dõi tiến độ | `GET /plans/{id}`, `GET /plans/{id}/driver-locations` (tải lại định kỳ, T32) | Trạng thái từng stop và từng route; lý do thất bại; vị trí cuối của tài xế (10 phút) hoặc lý do không có (tài xế tắt chia sẻ, chưa cấp quyền, GPS tắt, mất sóng) kèm số điện thoại `driver.phoneNumber` để gọi nhắc. Không có realtime trong MVP. |

### 4.2 Mobile (tài xế)

| Màn hình | Endpoint | Ghi chú thiết kế |
|---|---|---|
| Đăng nhập, quên mật khẩu, nhận lời mời | như web, kèm `X-Client-Platform: mobile` | |
| Route trong ngày | `GET /driver/routes?date=` | Mặc định hôm nay; mỗi route: tên plan, số stop đã xong/tổng, giờ xuất phát, `status` riêng của route. |
| Chi tiết route | `GET /driver/routes/{routeId}` | Tiêu đề lấy `planName` của response (mở thẳng màn này vẫn có tên). Danh sách stop theo `sequence`: nhãn, người nhận (nút gọi), địa chỉ, ETA, time window, ghi chú, trạng thái. Stop kế tiếp nổi bật. |
| Dẫn đường | không có endpoint | Nút mở Google Maps (`https://www.google.com/maps/dir/?api=1&destination={lat},{lon}`) hoặc Apple Maps (`https://maps.apple.com/?daddr={lat},{lon}`) theo nền tảng (D-07, FR-D02). |
| Báo kết quả stop | `PATCH /driver/routes/{routeId}/stops/{stopId}/status` | Ba nút: Đã giao, Không giao được (bắt buộc lý do, ≤ 255 ký tự), Bỏ qua. **Không hoàn tác được** (P-34): cần bước xác nhận. Response có `routeStatus`, `planStatus` để báo "đã xong route". Mất mạng thì gửi lại cùng trạng thái: server trả 200 (idempotent). |
| Chia sẻ vị trí (T32, P-53, P-54) | `PUT /driver/location/sharing`, `POST /driver/location` | Mặc định hỏi khi mở một tuyến chưa xong ("Chia sẻ vị trí cho tuyến này?", ô ghi nhớ); menu tài khoản chọn Hỏi / Luôn / Không (chỉ lưu trên máy). App báo trạng thái `SHARING`, `DECLINED`, `NO_PERMISSION`, `GPS_OFF` để dispatcher biết lý do; gửi lô điểm mỗi 30 giây kể cả khi chạy nền (thông báo cố định trên Android), dừng khi tuyến xong, plan hết chạy hoặc đăng xuất. |

## 5. Trạng thái cần thiết kế

### 5.1 Xử lý nền của plan

`processingStatus`: `IDLE` · `GEOCODING` · `OPTIMIZING`. Khác `IDLE` thì khoá mọi nút sửa (server trả 409 `PLAN_BUSY`) và tải lại `GET /plans/{id}` mỗi 2–3 s tới khi `IDLE`. `geocodingSummary` (`resolved`, `provided`, `pending`, `failed` / `total`) đổi dần trong lúc geocode: hiện thanh tiến độ. Geocode xong thì `version` tăng: dùng `version` mới cho request sửa tiếp theo.

### 5.2 Vòng đời

| Đối tượng | Trạng thái | Gợi ý màu/biểu tượng |
|---|---|---|
| Plan | `DRAFT` → `PUBLISHED` → `IN_PROGRESS` → `COMPLETED`; `CANCELLED` | nháp xám, đã giao việc xanh dương, đang giao cam, xong xanh lá, huỷ đỏ nhạt |
| Stop (giao hàng) | `PENDING` → `DELIVERED` / `FAILED` / `SKIPPED` | chờ xám, đã giao xanh lá, thất bại đỏ (kèm lý do), bỏ qua vàng |
| Stop (vị trí) | `geocodeStatus`: `PROVIDED`, `PENDING`, `RESOLVED`, `FAILED` (+ `geocodeError`: `NOT_FOUND`, `PROVIDER_ERROR`) | `RESOLVED` hiện `geocodedDisplayName` để dispatcher đối chiếu |
| Stop chưa xếp | `dropReason`: `NOT_OPTIMIZED` (chưa tối ưu), `INFEASIBLE` (solver không xếp được) | |
| Route | `PENDING`, `IN_PROGRESS`, `COMPLETED` | tính riêng cho từng route (P-35); tài xế thấy ở `/driver/routes`, dispatcher thấy ở `PlanDetail.routes[].status` (P-39); client không tự tính |
| Job | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` (+ `errorCode`) | |
| Cửa hàng (khu ADMIN, T55) | `ACTIVE` "Đang hoạt động", `READ_ONLY` "Tạm khoá (chỉ đọc)" kèm `lockReason` | khoá: cam/vàng, không dùng đỏ (vẫn đọc được, tài xế vẫn giao) |

### 5.3 Xung đột và giới hạn

- **409 `PLAN_MODIFIED`:** người khác vừa sửa plan. Báo "Plan đã thay đổi, tải lại?" và tải lại; không tự ghi đè.
- **429 `RATE_LIMITED`:** đọc header `Retry-After` (giây), khoá nút và đếm ngược.
- **503 `SERVICE_UNAVAILABLE`, `GEOCODING_BUSY`:** tạm thời; cho thử lại.
- **502 `*_PROVIDER_ERROR`:** dịch vụ bản đồ lỗi; cho thử lại sau.
- **Dữ liệu cũ bị dọn (T53, DATA_MODEL §6):** job đã xong quá 30 ngày bị xoá, `GET /jobs/{id}` trả 404 (client không còn theo dõi job cũ như vậy); lời mời chưa nhận đã hết hạn hoặc bị huỷ quá 90 ngày biến khỏi `GET /store/invitations` (lời mời đã nhận giữ lại).

### 5.4 Nhật ký thao tác (T58)

| `action` | Câu hiển thị (gợi ý) | Từ `details` |
|---|---|---|
| `MEMBER_UPDATED` | "{actorEmail} đổi thành viên {details.email}: {thay đổi}" | `changes.role` "vai trò Điều phối → Tài xế", `changes.status` "khoá" / "mở khoá", `changes.canDrive` "bật/tắt giao hàng" |
| `INVITATION_REVOKED` | "{actorEmail} huỷ lời mời {details.email} ({vai trò})" | `email`, `role` |
| `PLAN_PUBLISHED` | "{actorEmail} giao việc kế hoạch #{targetId}" | — |
| `PLAN_CANCELLED` | "{actorEmail} huỷ kế hoạch #{targetId}" | `from`: trạng thái trước khi huỷ |
| `PLAN_ROUTES_EDITED` | "{actorEmail} chỉnh tay tuyến kế hoạch #{targetId}" (+ "còn {n} vi phạm" khi `violations` > 0) | `violations` |
| `STORE_VIEWED` | "Quản trị viên nền tảng xem thông tin cửa hàng" | — (với OWNER không có IP) |

Giá trị `action` mới có thể xuất hiện (T56, T57): chưa có câu thì hiện mã. `details` có `truncated` nghĩa là chi tiết quá dài đã bị cắt: hiện "(chi tiết quá dài)".

## 6. Thông điệp lỗi

### 6.1 Mã lỗi (`code`)

| Mã | Câu gợi ý | Nơi gặp |
|---|---|---|
| `VALIDATION_ERROR` | (dùng `details`, §6.2) | form |
| `MALFORMED_REQUEST` | Dữ liệu gửi lên không hợp lệ. | bug client |
| `UNAUTHORIZED`, `INVALID_TOKEN` | Phiên đăng nhập đã hết, vui lòng đăng nhập lại. | mọi màn (sau khi refresh thất bại) |
| `INVALID_EMAIL_OR_PASSWORD` | Email hoặc mật khẩu không đúng. | đăng nhập |
| `EMAIL_NOT_VERIFIED` | Email chưa được xác thực. | đăng nhập |
| `ACCOUNT_DISABLED` | Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng. | đăng nhập |
| `EMAIL_EXISTED` | Email này đã được dùng. | đăng ký, mời |
| `EMAIL_ALREADY_VERIFIED` | Email đã được xác thực, bạn có thể đăng nhập. | xác thực |
| `INVALID_OTP` | Mã OTP không đúng hoặc đã hết hạn. | OTP |
| `OTP_COOLDOWN` | Vui lòng chờ trước khi xin mã mới. | đổi mật khẩu |
| `INVALID_RESET_TOKEN` | Phiên đặt lại mật khẩu đã hết hạn, vui lòng làm lại. | quên mật khẩu |
| `INVALID_CURRENT_PASSWORD` | Mật khẩu hiện tại không đúng. | đổi mật khẩu |
| `INVALID_PHONE_NUMBER` | Số điện thoại không hợp lệ. | nhận lời mời |
| `INVALID_TIMEZONE` | Múi giờ không hợp lệ. | cửa hàng |
| `INVALID_SORT_FIELD` | (bug client) | danh sách |
| `FORBIDDEN` | Bạn không có quyền thực hiện thao tác này. | mọi màn |
| `NOT_FOUND` | Không tìm thấy (có thể đã bị xoá). | mọi màn |
| `INVITATION_INVALID` | Lời mời không còn hiệu lực. (Màn Nhận lời mời nói thêm lý do và cách làm: "Lời mời không còn hiệu lực: mã sai, đã dùng, đã bị thu hồi hoặc đã hết hạn. Nhờ chủ cửa hàng mời lại.") | nhận lời mời |
| `INVITATION_NOT_PENDING` | Lời mời này đã được dùng hoặc đã huỷ. | lời mời |
| `CANNOT_MODIFY_OWNER` | Không thể thay đổi chủ cửa hàng. | thành viên |
| `VEHICLE_NAME_EXISTED` | Tên xe đã tồn tại. | xe |
| `STORE_DEPOT_REQUIRED` | Cửa hàng chưa có kho xuất phát. Cập nhật trong phần Cửa hàng. | tạo plan |
| `INVALID_ASSIGNMENT` | Tài xế hoặc xe không còn dùng được (`details` chỉ ra phân công nào). | plan |
| `PLAN_TOO_LARGE` | Plan vượt giới hạn 100 điểm giao hoặc 20 phân công. | plan |
| `PLAN_NOT_EDITABLE` | Plan đã giao việc, không sửa được nữa. | plan |
| `PLAN_BUSY` | Plan đang được xử lý, vui lòng chờ. | plan |
| `PLAN_MODIFIED` | Plan vừa được người khác thay đổi. Tải lại để xem bản mới. | plan |
| `NOTHING_TO_GEOCODE` | Không còn địa chỉ nào cần tìm toạ độ. | geocode |
| `PLAN_INCOMPLETE` | Plan cần ít nhất một điểm giao và một phân công. | optimize |
| `STOPS_NOT_GEOCODED` | Còn điểm giao chưa có toạ độ. | optimize |
| `PLAN_NOT_OPTIMIZED` | Cần tối ưu lộ trình trước khi giao việc. | publish |
| `SHIFT_OVERLAP` | Ca làm trùng với plan khác cùng ngày (`details` chỉ ra phân công và plan). | publish |
| `PLAN_NOT_CANCELLABLE` | Plan đang giao hoặc đã kết thúc, không huỷ được. | huỷ |
| `PLAN_NOT_ACTIVE` | Plan đã kết thúc hoặc đã bị huỷ. | tài xế |
| `INVALID_STOP_TRANSITION` | Điểm giao đã được báo kết quả, không đổi được. | tài xế |
| `FAILURE_REASON_REQUIRED` | Vui lòng nhập lý do không giao được. | tài xế |
| `RATE_LIMITED` | Bạn thao tác quá nhiều lần. Thử lại sau {Retry-After}. | đăng nhập, đăng ký, OTP, lời mời |
| `CONFLICT` | Dữ liệu vừa thay đổi, vui lòng thử lại. | hiếm |
| `GEOCODING_BUSY` | Dịch vụ tìm địa chỉ đang bận, thử lại sau ít phút. | tìm địa chỉ |
| `GEOCODING_PROVIDER_ERROR`, `ROUTING_PROVIDER_ERROR` | Dịch vụ bản đồ đang gặp sự cố, thử lại sau. | tìm địa chỉ, optimize |
| `SERVICE_UNAVAILABLE` | Hệ thống tạm thời gián đoạn, thử lại sau. | auth |
| `INTERNAL_ERROR` | Đã có lỗi xảy ra. Mã tra cứu: {X-Request-Id}. | mọi màn |

Job thất bại có `errorCode` riêng: `NO_SOLUTION` ("Không tìm được lộ trình, ca làm hoặc quãng đường tối đa có thể quá chặt"), `INTERRUPTED`, `JOB_QUEUE_FULL` ("Hệ thống bị gián đoạn, chạy lại"), cộng các mã provider ở trên. Các mã `@Deprecated` trong `ErrorCode` không còn được trả.

### 6.2 Lỗi theo field (`details[].code`)

`details[].field` là đường dẫn tới field của form (`stops[3].timeWindow.end`, `assignments[0].driverId`): hiện lỗi ngay dưới ô đó. Nhóm mã:

- **Bắt buộc** (`*_REQUIRED`): "Vui lòng nhập {tên trường}."; ô chọn thì "Vui lòng chọn {tên trường}." (`CAPACITY_UNIT_REQUIRED` → "Vui lòng chọn đơn vị.", `ROLE_REQUIRED` → "Vui lòng chọn vai trò."). Ví dụ `EMAIL_REQUIRED`, `PLAN_NAME_REQUIRED`, `STOP_LABEL_REQUIRED`, `DRIVER_REQUIRED`, `SHIFT_START_REQUIRED`, `VERSION_REQUIRED` (bug client).
- **Độ dài** (`INVALID_*_LENGTH`): "{Tên trường} quá dài/ngắn." Giới hạn ở API_SPEC (mật khẩu 8–72, tên plan ≤ 150, địa chỉ ≤ 500, ghi chú ≤ 500…).
- **Định dạng và miền giá trị:** `INVALID_EMAIL_FORMAT`, `INVALID_LATITUDE`, `INVALID_LONGITUDE`, `INVALID_CAPACITY` (> 0), `INVALID_DEMAND` ("Số lượng phải là số nguyên, không âm."), `INVALID_SERVICE_TIME` ("Thời gian phục vụ từ 0 đến 24 giờ."), `INVALID_MAX_DISTANCE` ("Quãng đường tối đa phải lớn hơn 0."), `INVALID_LIMIT`. Ô chọn tài xế/xe: `DRIVER_REQUIRED` → "Vui lòng chọn tài xế.", `VEHICLE_REQUIRED` → "Vui lòng chọn xe." (web W05).
- **Số điện thoại:** `PHONE_NUMBER_INVALID`, `PHONE_NUMBER_WRONG_REGION` ("Chỉ nhận số Việt Nam"), `PHONE_NUMBER_TOO_SHORT`, `PHONE_NUMBER_TOO_LONG`, `PHONE_NUMBER_TYPE_NOT_ALLOWED`.
- **Kiểm tra chéo:** `INVALID_TIME_WINDOW` (giờ kết thúc phải sau giờ bắt đầu, trên `timeWindow.end`), `INVALID_SHIFT` (trên `shiftEnd`), `COORDINATES_INCOMPLETE` (thiếu `lat` hoặc `lon`), `ADDRESS_OR_COORDINATES_REQUIRED` (trên `address`), `ORDER_DEMAND_NOT_ALLOWED`, `INVALID_STOP_ID`, `NOT_NULLABLE` (bug client).
- **Trong `PLAN_INCOMPLETE`:** `STOPS_REQUIRED`, `ASSIGNMENTS_REQUIRED`. **Trong `SHIFT_OVERLAP`:** mỗi phần tử có `conflictPlanId`.
- **Chỉnh tay tuyến (`PUT /plans/{id}/routes`, T28):** `INVALID_ROUTE_ID`, `ROUTES_INCOMPLETE`, `INVALID_STOP_ID`, `STOPS_INCOMPLETE`, `STOP_NOT_GEOCODED` là lỗi của client (nháp lệch với plan): hiện "Danh sách tuyến đã thay đổi, tải lại rồi chỉnh lại." và tải lại plan.

### 6.3 Vi phạm sau khi chỉnh tay (`PlanDetail.violations`, T28)

Không phải lỗi: plan vẫn lưu và giao việc được (P-58). Câu theo `code`, `excess` đổi đơn vị:
- `CAPACITY_EXCEEDED`: "{Tuyến}: vượt tải {excess} {đơn vị của xe}" (ví dụ "vượt tải 3 thùng"; `ORDER` là "đơn").
- `TIME_WINDOW_MISSED`: "{Điểm}: tới muộn {phút} phút so với khung giờ" (làm tròn lên).
- `SHIFT_EXCEEDED`: "{Tuyến}: về muộn {phút} phút so với ca".
- `MAX_DISTANCE_EXCEEDED`: "{Tuyến}: vượt {km} km quãng đường tối đa".
Giao việc khi còn vi phạm: hộp xác nhận liệt kê các dòng trên, nút "Vẫn giao việc".

Danh sách đầy đủ theo endpoint: API_SPEC §2–§8. Mã không có trong bảng: hiện "Giá trị không hợp lệ".

## 7. Ngoài phạm vi MVP (thiết kế chỗ trống, chưa có API)

Không còn mục nào: dẫn đường trong app (T33, M05), chỉnh tay tuyến (T28, W10) và vị trí tài xế (T32) đã có.

## 8. Dữ liệu mẫu (`docs/ux/samples`)

| File | Nội dung |
|---|---|
| `plan-detail-geocoded.json` | Plan DRAFT sau geocode: 1 stop `FAILED/NOT_FOUND`, chưa tối ưu (mọi stop trong `unassignedStops`) |
| `job-optimize.json` | Job optimize `SUCCEEDED` (mode inline) |
| `plan-detail-optimized.json` | Kết quả tối ưu: 2 route kèm ETA, tải, quãng đường; 1 stop `INFEASIBLE` |
| `plan-detail-completed.json` | Plan COMPLETED: stop `DELIVERED`, `FAILED` (kèm lý do), `SKIPPED` |
| `plan-list.json` | Trang `PlanSummary` |
| `driver-route-list.json`, `driver-route-detail.json` | Màn hình tài xế lúc vừa publish |
| `driver-stop-status-update.json` | Response sau khi báo stop đầu tiên |
| `vehicle-list.json`, `member-list.json`, `invitation-list.json`, `store.json`, `user-me.json` | Dữ liệu quản trị |
| `error-validation.json`, `error-plan-not-editable.json` | Hai dạng body lỗi |

Sinh lại: chạy `MvpScenarioIntegrationTest` (ghi vào `be/target/ux-samples/`) rồi chép sang đây (định dạng lại bằng `python3 -m json.tool`). Email trong mẫu là địa chỉ test ngẫu nhiên; id và toạ độ không có ý nghĩa ngoài mẫu.
