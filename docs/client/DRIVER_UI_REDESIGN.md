# DRIVER_UI_REDESIGN — Thiết kế lại app tài xế theo Material 3 (gồm M06)

> **Trạng thái (2026-11-20):** kế hoạch đã chốt (chủ dự án trả lời Q1–Q6 ở §6); **D1 đã làm** (§7, PR #139); **D2 đã làm** (§8, PR #140); **D3 đã làm** (§9, branch `feat/driver-ui-d3`); **D4 là giai đoạn tiếp theo**. Nền tảng thiết kế theo P-77.
> Phiên sau làm tiếp từ đây: đọc §0, §4 (giai đoạn đang mở) và §5 (cách kiểm), rồi cập nhật trạng thái ở đầu file và
> mục kết quả của giai đoạn vừa làm.
>
> Nhãn nguồn: **[M3]** trích nguyên văn hướng dẫn Material 3 / Android của Google (kèm URL, tải 2026-11-20);
> **[WCAG]**; **[Dự án]** quy tắc đã có trong `DESIGN_SYSTEM.md`, `UI_GUIDELINES.md`, `SCREENS.md`, `UX_BRIEF.md`;
> **[Chủ dự án]** yêu cầu hoặc trả lời của chủ dự án. Không có nhãn "agent tự suy": chỗ chưa có nguồn ghi thành câu
> hỏi ở §6.

## 0. Phạm vi và nguyên tắc

**Phạm vi** (chủ dự án chọn "toàn bộ app", 2026-11-20): mọi màn của `mobile/`: Đăng nhập, Chưa có quyền, Tuyến của
tôi (đổi ngày), menu tài khoản, Chi tiết tuyến, Chia sẻ vị trí (hộp hỏi, băng trạng thái), Báo kết quả + lý do không
giao được, Dẫn đường, các trạng thái tải / trống / lỗi / cửa hàng bị khoá / kế hoạch đã huỷ. Gồm luôn **M06**
(`docs/client/PLAN.md` §2): màn tài xế tự sửa họ tên và số điện thoại, và web bỏ ô số điện thoại ở Nhận lời mời.
Backend không đổi.

**Nền tảng thiết kế (P-77):**

- P-71 (Apple trước) vẫn là nguyên tắc; app tài xế chọn "Apple như web" (chủ dự án, 2026-11-20). Nhưng Flutter
  không có API cho thiết kế Apple mới: issue chính thức flutter/flutter#170310 ghi *"we are not developing the new
  Apple'26 UI design features in the Cupertino library right now"* và việc này chuyển sang package `cupertino_ui`
  tách riêng, chưa ra bản nào; SDK đang dùng (Flutter 3.47.9 stable, 2026-10-29) không có class Liquid Glass nào
  [Verified-run: grep SDK]. Chủ dự án chốt: *"nếu flutter k có api cho apple hig liquid glass thì cứ chuẩn material 3
  mà phang"*.
- Vì vậy **component và hành vi theo Material 3** (widget Material có sẵn của Flutter, `useMaterial3: true`) và
  hướng dẫn Android của Google; app chỉ chạy Android (APK, không có `ios/`).
- **Vẫn áp** các quy tắc không phụ thuộc nền tảng: token màu/chữ/khoảng cách và logo (`brand/design-system`,
  `DESIGN_SYSTEM.md`); câu chữ và câu báo lỗi (`UX_BRIEF.md` §6); tương phản WCAG; danh sách kiểm trang
  `UI_GUIDELINES.md` §1b (chữ dài, trống/đang tải/lỗi, chuyển động, giảm chuyển động); các mục accessibility
  (A1–A3, nhãn cho trình đọc màn hình); theme theo hệ điều hành (P-73).
- **Không áp** cho app: các quy tắc riêng của Apple trong `UI_GUIDELINES.md` phần A về vật liệu kính (M1–M16) và hình
  theo cỡ control (S3: capsule/bo 8 theo chuột/cảm ứng); thay bằng hình của M3 (§2.11).

## 1. Hiện trạng (chụp 2026-11-20)

Máy ảo Pixel 8 (1080×2400, mật độ 420 ≈ 412×915 dp), Android 16 (API 36), app debug nối stack e2e; tài xế có một
tuyến hôm nay 6 điểm (1 đã giao, 1 không giao được, 1 điểm tên + địa chỉ rất dài). Ảnh không đưa vào git (§5).

| # | Màn | Thấy gì |
|---|---|---|
| H1 | Hộp "Chia sẻ vị trí cho tuyến này?" | Câu còn nói "kể cả khi bạn đang mở **Google Maps** để chỉ đường" — đã lỗi thời từ M05 (dẫn đường trong app). Hai nút ("Không chia sẻ" viền, "Chia sẻ vị trí" tô nền) xếp thành hai hàng lệch nhau vì không vừa một hàng. Nhãn ô tick "Ghi nhớ lựa chọn…" lùi xa ô tick. |
| H2 | Chi tiết tuyến, thanh trên | Nhãn trạng thái "ĐANG GIAO" sát mép phải (còn ~22 px), lệch lề 42 px (16 dp) của trang. |
| H3 | Mọi màn | Nút phụ đều là nút **viền** (Gọi, Chỉ đường, Bật chia sẻ vị trí, Đăng xuất, Quay lại); nút bo `radius-sm` vuông vức, không theo hình mặc định của M3. |
| H4 | Chi tiết tuyến | Mỗi điểm một thẻ có viền; điểm tiếp theo là thẻ nền nhạt viền xanh với nút "Gọi 0905…" bên trong, "Chỉ đường" và "Báo kết quả" bên dưới thẻ. Nhóm "Đã xong (2)" có "Thu gọn". |
| H5 | Chi tiết tuyến | Trình đọc màn hình đọc thanh tiến độ là **"33"** (thiếu đơn vị, không rõ nghĩa). |
| H6 | Tuyến của tôi, cỡ chữ 200% | Chữ phóng to đúng; mũi tên đổi ngày giữ nhỏ. Tiêu đề thẻ xuống dòng cạnh nhãn trạng thái — đọc được. |
| H7 | Báo kết quả | Ba nút kết quả 56 dp tô màu (xanh/đỏ/cam) xếp dọc ở đáy — đúng quy tắc [Dự án] DESIGN_SYSTEM (ngón cái); phần giữa màn trống. |
| H8 | Lý do không giao được | Hộp thoại có 4 gợi ý dạng viên thuốc viền **phía trên** ô lý do, ô 3 dòng, đếm 0/255; nút "Gửi kết quả" khi chưa có lý do là hồng nhạt (tắt). |
| H9 | Menu tài khoản | Bottom sheet có tay nắm: tên, email, chọn chia sẻ vị trí (radio 3 lựa chọn), "Đăng xuất" viền. Chưa có chỗ sửa họ tên / số điện thoại (M06). |
| H10 | Đăng nhập | Panel thương hiệu (bản đồ + logo) trên nửa đầu; ô nhập viền, nút "Đăng nhập" tô nền góc vuông; "Nhập mã mời" và "Quên mật khẩu?" mở trình duyệt (M03). |
| H11 | Dẫn đường | Mở màn là hỏi quyền vị trí của Android; sau đó hộp "Location Accuracy" của dịch vụ Google (thuộc hệ thống, không phải app). Bản đồ dẫn đường chưa chụp được ở lần này — đo ở D6. |
| H12 | Cả app | Chưa bật predictive back (`android:enableOnBackInvokedCallback` không có trong `AndroidManifest.xml`). |

## 2. Material 3 / Android nói gì, theo thành phần

Nguồn đọc được: developer.android.com (Compose Material 3 — cùng hệ thiết kế M3 với widget Material của Flutter).
m3.material.io và api.flutter.dev chưa đọc được trong phiên này (bị chặn quyền fetch); trang Lists và Text field
của developer.android.com trả 404 — hai mục đó chưa có nguồn, xem §6.

### 2.1 Thanh trên (top app bar)
[M3] https://developer.android.com/develop/ui/compose/components/app-bars — Small: *"For screens that don't require a
lot of navigation or actions."*; Center aligned: *"For screens that have a single, primary action."*; Medium: *"For
screens that require a moderate amount of navigation and actions."*; *"pinnedScrollBehavior: The app bar remains in
place and does not react to scrolling."*

### 2.2 Nút
[M3] https://developer.android.com/develop/ui/compose/components/button — Filled: *"High-emphasis buttons. These are
for primary actions in an application, such as "submit" and "save.""*; *"The filled style should be used sparingly,
ideally for only one action on a page."*; Filled tonal: *"Filled tonal buttons provide more visual weight and suit
functions such as "add to cart" and "Sign in.""*; Outlined: *"Medium-emphasis buttons, containing actions that are
important but not primary."*; Text: *"Low-emphasis buttons, ideal for less critical actions such as navigational
links"*.

### 2.3 Thẻ
[M3] https://developer.android.com/develop/ui/compose/components/card — *"Cards typically present a single coherent
piece of content."*

### 2.3a Danh sách
m3.material.io không đọc được (trang dựng bằng JavaScript, chỉ trả về tiêu đề). Nguồn đọc được:
[M3] https://material-web.dev/components/list/ (Material Web của Google) — *"Lists are continuous, vertical indexes of
text and images"*. [Flutter] https://api.flutter.dev/flutter/material/ListTile-class.html — *"A single fixed-height
row that typically contains some text as well as a leading or trailing icon."*; *"A list tile contains one to three
lines of text optionally flanked by icons or other widgets"*; *"To be accessible, tappable leading and trailing widgets
have to be at least 48x48 in size."*; *"List tiles are typically used in ListViews, or arranged in Columns in Drawers
and Cards."*; *"Card, which can be used with Column to show a few ListTiles."* Chưa có câu chính thức về khi nào dùng
thẻ, khi nào dùng danh sách ngoài §2.3.

### 2.4 Hộp thoại
[M3] https://developer.android.com/develop/ui/compose/components/dialog — dùng cho *"Confirming user action"*,
*"Requesting user input"*; *"If you only need to display a two-button dialog … you should use `AlertDialog`"*; *"if
you want to create a more complex dialog, perhaps with forms and multiple buttons, you should use `Dialog` with custom
content"*; *"Material dialogs align their actions to the end, 8.dp apart."*

### 2.5 Bottom sheet
[M3] https://developer.android.com/develop/ui/compose/components/bottom-sheets — modal bottom sheet (`ModalBottomSheet`).
Chưa có câu chính thức về chọn sheet hay hộp thoại; chủ dự án chọn bottom sheet cho lý do không giao được (§6 Q3).

### 2.6 Chip
[M3] https://developer.android.com/develop/ui/compose/components/chip — Suggestion: *"Typically appear beneath an
input field to prompt user actions."*; Filter: *"Allows users to refine content from a set of options."*; Assist:
*"Guides the user during a task."*

### 2.7 Thanh tiến độ
[M3] https://developer.android.com/develop/ui/compose/components/progress — *"Determinate: Displays exactly how much
progress has been made."*; *"Circular progress indicators should be centered directly on the container or page that's
loading"*.

### 2.8 Snackbar
[M3] https://developer.android.com/develop/ui/compose/components/snackbar — *"serves as a brief notification that
appears at the bottom of the screen"*; *"It provides feedback about an operation or action without interrupting the
user experience."*

### 2.9 Huy hiệu / trạng thái
[M3] https://developer.android.com/develop/ui/compose/components/badges — *"Show the status of a task, such as
"complete," "in progress," or "failed.""*; badge mặc định là chấm đỏ nhỏ trên một thành phần khác. Không có hướng
dẫn cho nhãn chữ kiểu "ĐANG GIAO" → giữ `StatusPill` của [Dự án].

### 2.10 Accessibility
[M3/Android] https://developer.android.com/guide/topics/ui/accessibility/apps — *"each interactive UI element have a
focusable area, or touch target size, of at least 48dp×48dp"*, *"Larger is even better."*; tương phản chữ nhỏ hơn
18sp (hoặc đậm nhỏ hơn 14sp) *"at least 4.5:1"*, chữ khác *"at least 3:1"* — trùng [WCAG] 1.4.3 và quy tắc 48 px của
[Dự án].

### 2.11 Hình (shape scale)
[M3] https://developer.android.com/develop/ui/compose/designsystems/material3 — *"The shape scale defines the style of
container corners, offering a range of roundedness from square to fully circular."*; ví dụ ghi đè: extraSmall 4,
small 8, medium 12, large 16, extraLarge 24 dp. Giá trị mặc định của từng thành phần (nút, thẻ, hộp thoại) chưa đọc
được nguyên văn → lấy theo mặc định của widget Material 3 trong Flutter (đo ở D1, ghi lại vào DESIGN_SYSTEM).

### 2.12 Điều hướng và nút back
[M3] https://developer.android.com/develop/ui/compose/components/navigation-bar — thanh điều hướng dùng cho *"Three
to five destinations of equal importance"* → app tài xế (một khu chính: tuyến) **không** dùng navigation bar.
[Android] https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture — *"Predictive Back, a
gesture navigation feature, lets users preview where the back swipe takes them."*; *"We strongly recommend that you
implement predictive back navigation as soon as possible."*

## 3. Đề xuất theo thành phần (agent, chờ duyệt ở §6)

| Thành phần | Hiện tại | Đề xuất | Nguồn |
|---|---|---|---|
| Theme | `ColorScheme` dựng tay từ token, font Be Vietnam Pro | Giữ token và font **Be Vietnam Pro** (Q5); điền đủ vai trò màu M3 (primary/onPrimary/primaryContainer/secondaryContainer/surfaceContainer…) từ token để widget Material mặc định đúng màu ở cả hai theme; **không dùng màu động** theo hình nền (Q2) | [M3] 2.11; [Dự án] token; [Chủ dự án] Q2, Q5 |
| Nút | `PwButton` 6 biến thể, bo `radius-sm` | Một nút **Filled** mỗi màn cho việc chính ("Đăng nhập", "Tiếp tục giao", "Báo kết quả", "Lưu"); **Filled tonal** cho việc quan trọng thứ hai ("Chỉ đường", "Gọi khách"); **Outlined** chỉ khi cần mức trung bình trên nền trơn; **Text** cho link/việc nhẹ ("Quên mật khẩu?", "Thu gọn", "Quay lại" trong hộp thoại); hình theo mặc định M3. Ba nút kết quả **giữ** màu (xanh/đỏ/cam) và 56 dp, xếp dọc ở đáy — lệch có chủ đích so với "one filled per page" (Q1) | [M3] 2.2; [Dự án] DESIGN_SYSTEM; [Chủ dự án] Q1 |
| Thanh trên | Thanh trắng có đường kẻ, tiêu đề + phụ đề; pill sát mép | Top app bar **small** ghim (pinned), nền theo `surface`, nút back chuẩn; nhãn trạng thái đưa xuống thân trang (cạnh tiến độ) để thanh không chật (sửa H2) | [M3] 2.1 |
| Danh sách điểm | Mỗi điểm một thẻ viền | Điểm tiếp theo: **một thẻ** (một nội dung trọn vẹn: khách, địa chỉ, giờ, ghi chú, nút); các điểm còn lại và đã xong: **hàng `ListTile`** (số thứ tự ở đầu, tên + địa chỉ + giờ, tối đa ba dòng) xếp trong một nhóm (Q6) | [M3] 2.3, 2.3a |
| Hộp thoại | `AlertDialog` bo `radius-lg`, nút xếp lệch | `AlertDialog` M3: nút canh cuối, cách 8; nhãn không vừa một hàng thì xếp dọc canh cuối (sửa H1); câu chữ bỏ "Google Maps" | [M3] 2.4 |
| Lý do không giao được | Hộp thoại, gợi ý ở trên ô | **Modal bottom sheet** (Q3) có tay nắm, tiêu đề, ô lý do, **suggestion chip** đặt **dưới** ô, nút "Gửi kết quả"; sheet nổi lên trên bàn phím | [M3] 2.5, 2.6; [Chủ dự án] Q3 |
| Menu tài khoản | Bottom sheet | Giữ modal bottom sheet; thêm hàng "Thông tin của tôi" (M06) mở **màn riêng** (Q4); "Đăng xuất" là nút Outlined (việc quan trọng nhưng không phải việc chính của sheet) | [M3] 2.2, 2.5; [Chủ dự án] Q4 |
| M06: Thông tin của tôi | Chưa có | Màn riêng (top app bar + form): Họ tên, Số điện thoại (`PATCH /users/me` merge patch, `null` xoá số), lỗi dưới ô theo `UX_BRIEF.md` §6; lưu xong snackbar "Đã lưu thông tin." và quay lại | [Dự án] PLAN M06; [M3] 2.8 |
| Kết quả việc | Chưa thống nhất | Snackbar ở đáy cho kết quả ngắn ("Đã giao điểm 3.") | [M3] 2.8 |
| Thanh tiến độ | `ProgressBar` riêng, đọc "33" | `LinearProgressIndicator` xác định, nhãn đọc "2 trên 6 điểm đã xong" (sửa H5) | [M3] 2.7; [WCAG] 1.1.1 |
| Trạng thái tuyến/điểm | `StatusPill` | Giữ (không có hướng dẫn M3 cho nhãn chữ); đặt trong thân trang, cách mép 16 dp | [M3] 2.9; [Dự án] |
| Back | Nút back trên thanh | Bật predictive back (manifest + `PopScope` chỗ cần chặn, vd form chưa lưu) | [Android] 2.12 |
| Vùng chạm | 48 dp | Giữ ≥ 48 dp mọi control, kể cả mũi tên đổi ngày; icon theo cỡ chữ hệ thống | [M3] 2.10; [Dự án] |

## 4. Giai đoạn triển khai (đề xuất)

Mỗi giai đoạn một PR vào `develop` (branch `feat/driver-ui-dN`), kèm test widget, ảnh kiểm theo §5 và cập nhật
`SCREENS.md` §2, `DESIGN_SYSTEM.md` (phần app tài xế), `CURRENT_STATE.md` §5 trong cùng PR. Kết quả từng giai đoạn
ghi vào mục "D*n* — kết quả" cuối file này.

| GĐ | Nội dung | Phụ thuộc |
|---|---|---|
| D1 | **Nền:** theme M3 đủ vai trò màu từ token (sáng/tối), shape mặc định M3; `PwButton` ánh xạ sang Filled / Filled tonal / Outlined / Text (+ ba nút kết quả); `AlertDialog`, bottom sheet, snackbar dùng chung; predictive back; script dựng dữ liệu cho máy ảo đưa vào repo (`scripts/`) | — |
| D2 | **Đăng nhập, Chưa có quyền, màn chờ** | D1 |
| D3 | **Tuyến của tôi** (đổi ngày, thẻ tuyến, trống/lỗi/khoá) + **menu tài khoản** + **M06 Thông tin của tôi** | D1 |
| D4 | **Chi tiết tuyến** + **chia sẻ vị trí** (hộp hỏi, băng trạng thái, câu chữ bỏ Google Maps) | D1 |
| D5 | **Báo kết quả** + **lý do không giao được** | D1 |
| D6 | **Dẫn đường** (đo lại trên máy ảo có vị trí giả) | D1 |
| D7 | **M06 phía web:** bỏ ô số điện thoại khỏi Nhận lời mời (`AcceptInvitePage`), sửa `SCREENS.md` và test; đóng M06 trong `PLAN.md` | D3 |

## 5. Cách kiểm (mọi giai đoạn)

- **Máy ảo:** AVD `pw_pixel8` (Pixel 8, API 36 `google_apis` x86_64, RAM khách 2 GB — chủ dự án chọn 2026-11-20 để máy
  không thiếu RAM; tổng emulator chiếm ~3 GB), chạy không cửa sổ:
  `emulator -avd pw_pixel8 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -memory 2048 -cores 2 -no-snapshot-save`.
  Chỉ bật khi chụp/đo, xong thì `adb emu kill`.
- **Dữ liệu:** stack e2e (`compose.e2e.yaml`, API `localhost:8091`, Mailpit `8035`); app build
  `flutter build apk --debug --dart-define=API_BASE=http://10.0.2.2:8091`; script dựng tài xế có tuyến hôm nay
  (OWNER tự bật "Đi giao", 6 điểm có toạ độ ở Đà Nẵng, một điểm tên/địa chỉ dài, 1 đã giao, 1 không giao được).
  Vị trí giả: `adb emu geo fix <lon> <lat>`.
- **Chụp:** `adb exec-out screencap -p`, mỗi màn: sáng, tối (`cmd uimode night yes`), cỡ chữ 200%
  (`settings put system font_scale 2.0`), bàn phím mở với màn có ô nhập. Ảnh để ở scratchpad, **không commit**.
- **Trình đọc màn hình:** `uiautomator dump` đọc nhãn (content-desc) từng control; nhãn phải nói đủ nghĩa (H5).
- **Test:** `flutter analyze`, `flutter test` xanh; test widget cho hành vi mới (`testWidgets('<hành vi> khi <điều kiện>')`).
- **Báo cáo cuối giai đoạn:** liệt kê từng mục ở §3 đã đụng và `UI_GUIDELINES.md` §1b: ✅ / ❌ (lý do) / —.

## 6. Câu hỏi cho chủ dự án và trả lời (2026-11-20)

| # | Câu hỏi | Trả lời |
|---|---|---|
| Q1 | Ba nút kết quả màu, 56 dp, xếp dọc ở đáy: giữ hay theo M3 (một nút Filled)? | **Giữ nguyên** (lệch có chủ đích, ghi ở §3) |
| Q2 | Màu động theo hình nền (Material You)? | **Không dùng** |
| Q3 | Lý do không giao được: hộp thoại, bottom sheet hay màn toàn trang? | **Bottom sheet** |
| Q4 | M06 "Thông tin của tôi": màn riêng hay sửa trong menu? | **Màn riêng** |
| Q5 | Font: Be Vietnam Pro hay Roboto? | **Giữ Be Vietnam Pro** |
| Q7 (D2) | Ô nhập: ô viền nhãn trên (như hiện tại), ô nền nhãn trong ô (mặc định Android), hay ô viền nhãn trong ô? | **Giữ ô viền, nhãn trên** |
| Q8 (D2) | Hai màn chờ lúc mở app: giữ màn chờ hệ thống bằng API của Flutter, vẽ màn chờ Flutter giống màn hệ thống, thêm thư viện `core-splashscreen`, hay giữ nguyên? | **Thêm `core-splashscreen`** (P-78) |
| Q6 | Danh sách điểm: đọc nguồn M3 trước | Đã đọc (§2.3a): m3.material.io không đọc được; Material Web và `ListTile` của Flutter ủng hộ thẻ cho điểm tiếp theo, hàng danh sách cho các điểm khác |

## 7. D1 — kết quả (2026-11-20)

- **`PwButton`** dựng trên `FilledButton`, `FilledButton.tonal`, `OutlinedButton`, `TextButton`; kiểu `filled`, `tonal`, `outlined`, `text`, `textDanger`, và ba nút kết quả `success`/`danger`/`signal` (Q1). Hình viên thuốc và đệm hai bên lấy mặc định M3 của Flutter (đọc trong SDK 3.47.9: `StadiumBorder`, đệm 24 dp, nút chữ 12 dp, thu lại theo cỡ chữ). Nút tắt dùng màu tắt M3 (trước đây nút đỏ "Gửi kết quả" tắt vẫn đỏ nhạt); `busy` giữ màu và bỏ qua lần bấm.
- Các chỗ gọi đổi tên giữ nguyên mức nhấn cũ (`primary`→`filled`, `secondary`→`outlined`, `quiet`→`text`); chọn lại mức nhấn theo §3 làm ở D2–D6 từng màn.
- **Theme:** điền vai trò màu M3 từ token (container `surface-tint` + `ink`; `surfaceContainer*` = `surface-raised`; snackbar trên `ink`/`surface`); bottom sheet có tay nắm mặc định; snackbar nổi.
- **Hộp xác nhận** (`showPwDialog`): hình/màu mặc định M3, hai nút chữ canh cuối.
- **Predictive back:** `android:enableOnBackInvokedCallback="true"`; Flutter 3.47.9 đã dùng `PredictiveBackPageTransitionsBuilder` làm chuyển trang mặc định trên Android.
- **`scripts/seed_driver_demo.py`:** dựng tài xế có tuyến hôm nay cho máy ảo (§5).
- **Kiểm:** `flutter analyze` sạch; `flutter test` 129/129 (thêm 4 test: kiểu nút và hình, nút tắt, nút đang chạy, hộp xác nhận). Máy ảo: Tuyến của tôi, menu tài khoản, hộp Đăng xuất, Chi tiết tuyến, Báo kết quả — sáng; Báo kết quả tối + chữ 200% (nhãn nút xuống dòng, không cắt).
- `UI_GUIDELINES.md` §1b: chữ dài ✅ (nhãn nút xuống dòng); trống/tải/lỗi — (không đổi màn); chuyển động ✅ (chuyển trang theo cử chỉ back của hệ thống); accessibility ✅ (nhãn và trạng thái tắt của nút có test).

## 8. D2 — kết quả (2026-11-20)

Hiện trạng đo trên máy ảo (Pixel 8, API 36): mở app thấy **hai màn chờ** (màn hệ thống nền xanh có logo, rồi màn Flutter nền trơn có vòng xoay); biểu tượng thanh trạng thái **màu tối trên phần đầu xanh** của màn đăng nhập; cỡ chữ 200%: hình ghim nằm dưới biểu tượng wifi (trắng trên trắng), đường nét đứt chạy qua logo và dòng "App giao hàng cho tài xế."; lưới đường phố vẽ tràn xuống form. Nút ("Đăng nhập" tô nền, "Quên mật khẩu?", "Nhập mã mời" nút chữ; "Đăng xuất" tô nền ở Chưa có quyền) đã đúng mức nhấn từ D1.

- **Màn chờ (Q8, P-78):** `MainActivity` cài `core-splashscreen` 1.2.0, giữ màn chờ hệ thống tới khi Flutter gọi `dismiss` qua kênh `pathwise/splash` (port `SystemSplash`); `app.dart` gọi sau khi phiên rõ và màn đầu đã vẽ (hai khung hình). Tối đa 3 giây rồi thả (tải qua mạng không rõ thời gian, Android khuyên hiện chỗ chờ). Đo: bản release trên máy ảo biết phiên sau ~1,9–2,2 s; bản debug ~3,9–4,9 s (debug chạy Dart chậm, nên trên máy ảo debug vẫn thấy vòng chờ sau 3 s).
- **Thanh trạng thái:** mặc định cả app theo theme (`statusBarFor`, `shared/ui/system_bars.dart`, chỉ đổi biểu tượng thanh trạng thái, không đụng thanh điều hướng); phần đầu màn đăng nhập luôn biểu tượng sáng. Nguồn: developer.android.com edge-to-edge *"manually update the system bar icon colors so they contrast with your app's background"*.
- **Phần đầu màn đăng nhập:** hình vẽ bắt đầu dưới thanh trạng thái (phần đầu cao thêm bằng thanh trạng thái so với prototype); logo và dòng giới thiệu bắt đầu ở chỗ của cỡ chữ 100% và to xuống dưới, phần đầu cao thêm thay vì chữ đè lên đường đi; cắt hình vẽ theo khung phần đầu.
- **Ô nhập (Q7):** giữ như cũ.
- **Kiểm:** `flutter analyze` sạch; `flutter test` 134/134 (thêm 5: giữ/thả màn chờ khi khôi phục phiên và khi chưa đăng nhập, biểu tượng sáng trên phần đầu ở hai theme, theo theme ở Chưa có quyền rồi sáng lại ở Đăng nhập, logo giữ chỗ ở chữ 200%). Máy ảo: Đăng nhập sáng/tối, tối + chữ 200%, bàn phím mở; Chưa có quyền sáng, tối + chữ 200%; chuỗi mở app khi chưa đăng nhập và khi khôi phục phiên.
- `UI_GUIDELINES.md` §1b: chữ dài ✅ (chữ 200% không đè hình, câu dài ở Chưa có quyền xuống dòng); trống/tải/lỗi ✅ (màn chờ, lỗi đăng nhập không đổi); chuyển động — ; accessibility ✅ (tương phản biểu tượng thanh trạng thái).
- Máy ảo RAM khách 2 GB bị OOM killer của máy chủ tắt hai lần khi Gradle đang build cùng lúc: build xong thì `./gradlew --stop` rồi mới bật máy ảo.

## 9. D3 — kết quả (2026-11-20)

Phạm vi: Tuyến của tôi, menu tài khoản, M06 "Thông tin của tôi" (phần app; phần web là D7).

- **Thanh trên:** Tuyến của tôi dùng top app bar small của M3 (`AppBar`, ghim, nền `surface`), chữ viết tắt tên ở cuối thanh mở menu tài khoản; bỏ thanh trắng có đường kẻ tự dựng. Màn "Thông tin của tôi" cũng là `AppBar` có nút back chuẩn.
- **Menu tài khoản:** thêm hàng `ListTile` "Thông tin của tôi" (icon người, mũi tên ở cuối) mở màn riêng (Q4); "Đăng xuất" là nút viền (Outlined); nhãn đọc của nút mở menu bỏ "và giao diện" (không còn lựa chọn giao diện từ P-73).
- **M06 Thông tin của tôi** (route `/profile`): đọc `GET /users/me` mỗi lần mở (đang tải, lỗi kèm "Thử lại"); ô Họ tên, Số điện thoại (gợi ý "Tuỳ chọn. Chỉ nhận số Việt Nam.", placeholder "0905 123 456", hiện số theo `formatPhone`); "Lưu" tô nền. Gửi `PATCH /users/me` merge patch: ô để trống gửi `null` để xoá, giống trang hồ sơ của web (gửi bằng map vì client sinh từ OpenAPI bỏ field `null`). Lỗi theo ô (`details[]`) hiện dưới ô theo UX_BRIEF §6.2 (`fieldMessage`, cùng câu với web); lỗi khác hiện băng lỗi. Lưu xong: phiên nhận tên mới (chữ viết tắt đổi theo), snackbar "Đã lưu thông tin." và quay về. Back (nút, cử chỉ, predictive back) khi còn thay đổi chưa lưu: hộp "Bỏ thay đổi chưa lưu?" ("Ở lại" / "Bỏ thay đổi").
- **Thanh tiến độ (H5):** `PwProgressBar` và thẻ tuyến đọc một câu "2 trên 6 điểm đã xong", không còn đọc "2/6" hay phần trăm.
- **Icon theo cỡ chữ:** mũi tên đổi ngày, icon hàng "Thông tin của tôi" và nút radio chia sẻ vị trí to theo cỡ chữ hệ thống (`scaledIconSize`; 48 dp ở 200%) — sửa phần còn lại của H6.
- **Lỗi tìm thấy khi làm:** đổi tên làm phiên đổi, router chạy lại redirect và giữ màn "Thông tin của tôi" lại sau khi bấm Lưu. Router giờ chỉ chạy lại khi nơi phiên dẫn tới đổi (đang tải / chưa đăng nhập / không có quyền / tài xế).
- **Không đổi:** thẻ tuyến giữ bo `radius-md` và viền (DESIGN_SYSTEM chưa có quy tắc thẻ M3 cho app; xem câu hỏi ở báo cáo D3); mỗi thẻ tuyến có một nút tô nền ("Tiếp tục giao") — thường một tuyến mỗi ngày; băng cửa hàng tạm khoá, trạng thái trống và lỗi giữ như cũ (đã có test).
- **Kiểm:** `flutter analyze` sạch; `flutter test` 142/142 (thêm 8: năm test màn Thông tin của tôi, câu lỗi theo ô, câu đọc thanh tiến độ, icon ở chữ 200%). Máy ảo (tài khoản tạo bằng `scripts/seed_driver_demo.py`): Tuyến của tôi sáng, tối + chữ 200%; menu tài khoản sáng, tối + chữ 200%; Thông tin của tôi sáng, bàn phím số mở, lỗi "Chỉ nhận số Việt Nam." từ server, lưu số `0905 123 456` → snackbar → mở lại đọc đúng số từ server; tối + chữ 200%.
- `UI_GUIDELINES.md` §1b: chữ dài ✅ (tên tuyến xuống dòng cạnh nhãn trạng thái, email dài xuống dòng trong menu); phân trang — ; bảng — ; trống/tải/lỗi ✅ (Thông tin của tôi có đang tải và lỗi kèm "Thử lại"); chuyển động — (chuyển màn mặc định của Android); accessibility ✅ (vùng chạm 48 dp, icon theo cỡ chữ, câu đọc thanh tiến độ).

