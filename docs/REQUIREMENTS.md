# REQUIREMENTS — Yêu cầu và phạm vi sản phẩm

> Nguồn: phỏng vấn chủ dự án 2026-10-21/22 (xem `DECISIONS.md`). Mỗi yêu cầu ghi nguồn quyết định.
> Ký hiệu trạng thái: ✅ đã có và đã kiểm chứng · 🟡 đã có nhưng lỗi/chưa kiểm chứng · ❌ chưa có.
> Tầng ưu tiên: **Must** (thiếu thì không phải MVP) · **Should** (trong MVP nếu kịp) · **Could** (sau MVP) · **Idea** (chưa phê duyệt).

## 1. Mục tiêu sản phẩm

PathWise giúp **cửa hàng có đội tài xế riêng** lập kế hoạch giao hàng trong ngày: dispatcher nhập danh sách điểm giao, hệ thống chia điểm cho các tài xế/xe và sắp thứ tự tối ưu có xét khung giờ nhận hàng, tải trọng xe và ca làm; tài xế xem tuyến trên điện thoại, dẫn đường tới từng điểm và báo trạng thái giao; cửa hàng theo dõi tiến độ và vị trí tài xế. (D-01, D-04, D-05, D-06)

Phạm vi repo này: **backend** (REST API) phục vụ web dashboard và app Flutter (D-09).

## 2. Actor

| Actor | Mô tả | Cách có tài khoản |
|---|---|---|
| ADMIN | Quản trị nền tảng PathWise. | Tạo thủ công (seed/SQL), không tự đăng ký. |
| OWNER | Chủ cửa hàng; toàn quyền trong store. | Tự đăng ký, store tạo cùng lúc (D-15). |
| DISPATCHER | Nhân viên điều phối của cửa hàng. | Được OWNER mời qua email (D-16). |
| DRIVER | Tài xế của cửa hàng. | Được OWNER mời qua email (D-16). |

Mỗi user thuộc tối đa 1 store và có đúng 1 role (D-02). OWNER hoặc DISPATCHER được bật cờ `canDrive` thì cũng có thể được gán route như tài xế (D-21).

## 3. Yêu cầu chức năng

### 3.1 Tài khoản & xác thực (D-11, D-14..D-17)

| ID | Yêu cầu | Tầng | Hiện trạng |
|---|---|---|---|
| FR-A01 | OWNER đăng ký bằng email, mật khẩu (8–72 ký tự), tên cửa hàng; tạo user + store trong 1 giao dịch; tài khoản ở trạng thái chờ xác thực. | Must | ✅ (T13) |
| FR-A02 | Xác thực email bằng OTP 6 số gửi qua email; gửi lại OTP có cooldown. Chưa xác thực thì không đăng nhập được. | Must | ✅ (T13) |
| FR-A03 | Đăng nhập bằng email + mật khẩu → access token (JWT, 14 phút) + refresh token (14 ngày). Web: refresh token trong HttpOnly cookie; mobile: trong body. | Must | ✅ (T10): web cookie `Path=/api/v1/auth`, mobile body |
| FR-A04 | Refresh: xoay vòng refresh token, phát hiện tái sử dụng (revoke cả family), từ chối token hết hạn, từ chối user bị vô hiệu hoá. | Must | ✅ (T10, T13) |
| FR-A05 | Đăng xuất thiết bị hiện tại. | Must | ✅ (T10): `/log-out` public, cookie hoặc body |
| FR-A06 | Xem danh sách session (thiết bị) đang hoạt động; đăng xuất từng session; đăng xuất mọi session khác. | Must | ✅ (T11) |
| FR-A07 | Quên mật khẩu: OTP qua email → đặt mật khẩu mới → thu hồi mọi session. Không tiết lộ email có tồn tại hay không. | Must | ✅ (T14) |
| FR-A08 | Đổi mật khẩu khi đã đăng nhập: mật khẩu cũ + OTP qua email; thu hồi mọi session khác. | Must | ✅ (T15) |
| FR-A09 | Xem/cập nhật hồ sơ của chính mình (số điện thoại VN). | Must | ✅ (`GET/PUT /users/me`) — cần bổ sung `fullName` |
| FR-A10 | ADMIN xem danh sách user (phân trang, sort whitelist). | Should | ✅ |

### 3.2 Cửa hàng & thành viên (D-02, D-03, D-16)

| ID | Yêu cầu | Tầng | Hiện trạng |
|---|---|---|---|
| FR-S01 | OWNER xem/cập nhật thông tin store: tên, depot (địa chỉ + toạ độ), timezone. | Must | ✅ (T13, quyền theo role ở T17) |
| FR-S02 | OWNER mời thành viên (email + role DISPATCHER/DRIVER); email chứa mã mời; xem/huỷ lời mời đang chờ. | Must | ❌ |
| FR-S03 | Người được mời chấp nhận bằng mã mời + đặt mật khẩu + tên → tài khoản ACTIVE (email coi như đã xác thực). | Must | ❌ |
| FR-S04 | OWNER xem danh sách thành viên; vô hiệu hoá / kích hoạt lại thành viên (vô hiệu hoá → thu hồi mọi session). | Must | ❌ |
| FR-S05 | OWNER đổi role DISPATCHER ↔ DRIVER của thành viên. | Should | ❌ |
| FR-S06 | OWNER bật hoặc tắt `canDrive` cho chính mình và cho DISPATCHER; người có `canDrive` được gán route và dùng API tài xế (D-21). | Must | ❌ |

### 3.3 Xe (D-05)

| ID | Yêu cầu | Tầng | Hiện trạng |
|---|---|---|---|
| FR-V01 | OWNER/DISPATCHER tạo, sửa, vô hiệu hoá xe: tên/biển số, capacity (số nguyên > 0). | Must | ❌ |

### 3.4 Kế hoạch giao hàng (D-04, D-05, D-18, D-19, D-20, D-23, D-24, P-13, P-14)

| ID | Yêu cầu | Tầng | Hiện trạng |
|---|---|---|---|
| FR-P01 | Tìm địa chỉ → danh sách ứng viên có toạ độ (geocoding search, dùng cho autocomplete hoặc sửa stop lỗi). | Should | 🟡 có geocode nội bộ, chưa có endpoint |
| FR-P02 | Tạo plan nháp (DRAFT) cho một ngày: depot, danh sách stop (nhãn, tên/số điện thoại người nhận, địa chỉ hiển thị, **địa chỉ và/hoặc toạ độ** (phải có ít nhất một trong hai), demand, thời gian phục vụ, time window tuỳ chọn, ghi chú) và danh sách phân công (tài xế + xe + giờ bắt đầu/kết thúc ca + quãng đường tối đa tuỳ chọn). | Must | ❌ |
| FR-P03 | Sửa plan DRAFT (thêm/sửa/xoá stop, phân công). Mọi chỉnh sửa xoá kết quả tối ưu cũ; stop không đổi địa chỉ thì giữ kết quả geocode. | Must | ❌ |
| FR-P09 | Server geocode **chạy nền** các stop chỉ có địa chỉ ngay khi lưu plan. Dispatcher thấy tiến độ và từng stop thất bại kèm lý do, sửa được bằng địa chỉ mới hoặc toạ độ, và chạy lại được. Plan chỉ optimize khi mọi stop đã có toạ độ. | Must | ❌ (hiện geocode tuần tự trong request) |
| FR-P04 | Tối ưu plan: chia stop cho các route và sắp thứ tự, tôn trọng time window, capacity, ca làm, quãng đường tối đa; trả ETA từng stop, tổng quãng đường/thời gian mỗi route, và danh sách stop **không xếp được**. API dạng job, MVP chạy ngay trong request nhưng chuyển sang chạy nền được bằng cấu hình (D-19). | Must | 🟡 có TSP/VRP chỉ theo khoảng cách, không ràng buộc, không lưu |
| FR-P05 | Publish plan đã tối ưu → tài xế thấy route của mình; sau publish không sửa stop/phân công được. Từ chối publish nếu ca của một tài xế hoặc xe chồng giờ với plan đã publish khác cùng ngày (D-23). | Must | ❌ |
| FR-P06 | Huỷ plan (DRAFT hoặc PUBLISHED). | Must | ❌ |
| FR-P07 | Xem danh sách plan theo ngày/trạng thái và chi tiết plan với tiến độ từng stop. | Must | ❌ |
| FR-P08 | Chỉnh tay sau tối ưu: chuyển stop giữa route, đổi thứ tự; hệ thống tính lại ETA/quãng đường và cảnh báo vi phạm ràng buộc. Endpoint có sẵn trong API contract (D-24). | Should | ❌ |

### 3.5 Tài xế (D-06, D-07, D-08)

| ID | Yêu cầu | Tầng | Hiện trạng |
|---|---|---|---|
| FR-D01 | Tài xế xem các route được giao (theo ngày) và chi tiết route: stop theo thứ tự, toạ độ, ETA, time window, liên hệ, ghi chú. | Must | ❌ |
| FR-D02 | Dữ liệu đủ để app mở Google/Apple Maps dẫn đường tới stop kế tiếp (deep link). | Must | ❌ |
| FR-D03 | Cập nhật trạng thái stop: DELIVERED / FAILED (bắt buộc lý do) / SKIPPED; dispatcher thấy ngay khi tải lại. | Must | ❌ |
| FR-D04 | Gửi vị trí định kỳ (15–30 s) theo **lô điểm** (gom được các điểm bị mất mạng); dispatcher xem vị trí cuối của các tài xế đang chạy plan. Thiết kế mở rộng được sang lưu lịch sử (D-26). | Should¹ | ❌ |
| FR-D05 | Turn-by-turn trong app: backend trả geometry + chỉ dẫn từng bước cho route. | Should (fallback FR-D02) | ❌ |

¹ Chủ dự án muốn làm polling ngay sau core (D-08); xếp Should vì phụ thuộc toàn bộ phần plan/driver.

### 3.6 Ngoài MVP

| ID | Nội dung | Tầng |
|---|---|---|
| FR-X01 | Provider Google (geocoding + matrix) để demo thật. | Could (I-03) |
| FR-X02 | Realtime push vị trí. | Could (I-04) |
| FR-X03 | Tối ưu bất đồng bộ (job) cho plan lớn. | Could |
| FR-X04 | Gợi ý địa điểm đã dùng. | Idea (I-01) |
| FR-X05 | Thanh toán QR tại điểm giao + xác thực thanh toán. | Idea (I-02) |
| FR-X06 | Danh mục đơn hàng lâu dài, import CSV. | Không có kế hoạch (trái D-04) |

## 4. Business rules & invariants

- BR-01 Mọi dữ liệu thuộc store (xe, plan, stop, lời mời, thành viên) chỉ được đọc/ghi bởi user cùng store (hoặc ADMIN nếu có endpoint admin riêng). `storeId` **luôn lấy từ token**, không bao giờ từ request của user thường.
- BR-02 Email là duy nhất toàn hệ thống (không phân biệt hoa thường, P-19).
- BR-03 Một store có đúng 1 OWNER trong MVP.
- BR-04 User DISABLED hoặc PENDING_VERIFICATION không đăng nhập/refresh được.
- BR-05 OTP: 6 chữ số, TTL cấu hình (mặc định 5 phút), tối đa 3 lần nhập sai rồi huỷ, cooldown gửi lại 60 s, mỗi OTP gắn với (purpose, userId) và dùng một lần.
- BR-06 Plan chỉ tối ưu được ở DRAFT; chỉ publish được khi đã có kết quả tối ưu còn hiệu lực; sau PUBLISHED chỉ trạng thái stop thay đổi.
- BR-07 Trong một plan: mỗi người được gán xuất hiện tối đa 1 lần, mỗi xe tối đa 1 lần. Người được gán phải là thành viên ACTIVE của store và có `role=DRIVER` hoặc `canDrive=true`. Xe phải ACTIVE và thuộc store.
- BR-08 Stop: phải có địa chỉ hoặc toạ độ. Toạ độ (nếu có) hợp lệ: lat ∈ [-90,90], lon ∈ [-180,180]. Demand ≥ 0; service time ≥ 0; nếu có time window thì `start < end`.
- BR-09 Phân công: `shiftStart < shiftEnd`; `maxDistance` nếu có > 0.
- BR-10 Tài xế chỉ cập nhật stop thuộc route của mình, trong plan PUBLISHED/IN_PROGRESS. Chuyển trạng thái hợp lệ: `PENDING → DELIVERED | FAILED | SKIPPED`; DISPATCHER/OWNER có thể đặt lại về `PENDING`.
- BR-12 Không publish được nếu, với cùng `plan_date`, khoảng ca của một người được gán hoặc một xe giao với khoảng ca của họ trong một plan PUBLISHED/IN_PROGRESS khác (D-23).
- BR-13 Plan đang có job chạy (`processingStatus` khác IDLE) thì không sửa, optimize hay publish được. Optimize yêu cầu mọi stop đã có toạ độ.
- BR-11 Plan tự chuyển `PUBLISHED → IN_PROGRESS` khi có stop đầu tiên rời PENDING, và `→ COMPLETED` khi mọi stop đã gán đều ở trạng thái kết thúc.

## 5. Yêu cầu phi chức năng

| ID | Yêu cầu | Mục tiêu MVP |
|---|---|---|
| NFR-01 Bảo mật | Mật khẩu BCrypt; token/OTP lưu dạng hash; fail-closed khi Redis lỗi; không lộ stack trace ngoài profile dev; rate limit endpoint auth (P-16); tenant isolation có test. | Bắt buộc |
| NFR-02 Hiệu năng | Optimize ≤ 100 stops / ≤ 20 routes trong ≤ 15 s (đã có toạ độ). API CRUD p95 < 300 ms ở tải demo. | Mục tiêu, đo bằng test thủ công |
| NFR-03 Độ tin cậy | Lỗi provider ngoài (OSRM/Nominatim) trả lỗi rõ ràng (`502/503` + code), không 500 chung chung; có timeout cho HTTP client. | Bắt buộc |
| NFR-04 Kiểm thử | Build xanh; unit test cho solver/state machine/token; integration test (Testcontainers) cho luồng auth, tenant isolation, plan lifecycle. | Bắt buộc |
| NFR-05 Vận hành | Chạy toàn bộ stack bằng `docker compose --profile app up` (image multi-stage); dev chạy hạ tầng bằng compose và app từ IDE; cấu hình qua biến môi trường; CI build, test, quét chất lượng và push image lên GHCR; deploy thủ công theo runbook (D-30 → D-34). | Bắt buộc |
| NFR-06 Quan sát | Log có cấu trúc tối thiểu (userId, storeId, requestId); không log secret/OTP/token. | Should |
| NFR-07 Ngôn ngữ | Email gửi người dùng bằng tiếng Việt. | Should (đề xuất, chưa phê duyệt) |

## 6. Tiêu chí MVP hoàn thành

MVP được coi là xong khi **tất cả** điều sau được kiểm chứng bằng test tự động hoặc kịch bản demo ghi lại:

1. Build + toàn bộ test xanh trên CI.
2. Kịch bản E2E: OWNER đăng ký → xác thực email → đăng nhập → cập nhật depot → mời 1 DISPATCHER và 2 DRIVER → họ chấp nhận lời mời → DISPATCHER tạo 2 xe → tạo plan 20–30 stop (phần lớn chỉ có địa chỉ, vài stop có toạ độ) có time window và demand → geocode chạy nền xong, sửa stop geocode lỗi → optimize (job) → kết quả tôn trọng capacity/time window/ca, liệt kê stop không xếp được nếu có → publish → mỗi DRIVER thấy đúng route của mình, mở được deep link, cập nhật trạng thái → plan chuyển IN_PROGRESS → COMPLETED.
3. Các luồng FR-A01..A08 có integration test cho nhánh thành công và các nhánh lỗi chính.
4. Test tenant isolation: user store A không đọc/sửa được xe/plan/stop/thành viên của store B (trả 404).
5. Không còn lỗi nào thuộc nhóm "Critical/High" trong `CURRENT_STATE.md` ở trạng thái mở.
6. `docker compose --profile app up` chạy được từ repo sạch theo README; image `develop-<sha>` được publish lên GHCR và deploy được theo `docs/DEPLOYMENT.md`.
7. `API_SPEC.md` khớp với implementation (kiểm bằng OpenAPI sinh ra).
