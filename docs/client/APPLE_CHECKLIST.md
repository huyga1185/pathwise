# APPLE_CHECKLIST — Kiểm từng thành phần giao diện theo hướng dẫn Apple

> Dùng cho mọi thay đổi giao diện web (và app tài xế khi áp dụng được), theo P-71: hướng dẫn chính thức của Apple đứng trước, quy tắc cũ của dự án chỉ áp ở chỗ Apple không nói. Quy tắc chi tiết và mã quy tắc (M, S, T, A) ở `docs/client/LIQUID_GLASS.md`; tài liệu này là **danh sách để kiểm**, không thêm quy tắc mới.
>
> Nhãn: **[Apple]** trích nguyên văn tài liệu Apple; **[Web]** điều web buộc phải có (WCAG, trình duyệt), chọn bằng đo; **[Chủ dự án]** yêu cầu riêng. Mục nào không có nguồn thì không được ghi là "theo Apple".

## 0. Cách dùng

1. Chọn các mục ở §1 (mọi thành phần) và mục của từng thành phần bị đụng ở §3.
2. Kiểm bằng cách ghi ở cột "Cách kiểm": đo thật, chụp thật, có test; không kiểm bằng trí nhớ.
3. Báo cáo cuối task (và mô tả PR) liệt kê từng mục: ✅ đạt, ❌ chưa đạt (kèm lý do, hoặc ghi lệch có chủ đích vào `LIQUID_GLASS.md`), — không áp dụng.
4. Số đo mới (bo góc, thời lượng, độ đậm…) phải có nguồn hoặc ghi rõ [Web]/[Chủ dự án] kèm lý do; số tự chọn không nguồn thì sửa.

**Bộ kiểm chuẩn**

| Hạng mục | Cách làm |
|---|---|
| Khổ màn | Chụp Playwright ở 320, 390, 768, 1280, 1440, 1920px (trang dài thì `fullPage`) |
| Theme | Sáng và tối (`data-theme`) |
| Cài đặt trợ năng | `prefers-reduced-motion: reduce`, `prefers-contrast: more`, `prefers-reduced-transparency: reduce`, `forced-colors: active` |
| Tương phản | Chụp, ẩn chữ/biểu tượng, lấy màu nền thật phía sau, tính tỉ lệ WCAG ở chỗ **xấu nhất** |
| Hành vi | Test vitest (role, nhãn, focus, `aria-*`); thử bàn phím (Tab, Shift+Tab, Enter, Esc) |
| Trình duyệt | Chromium; thêm Firefox khi dùng CSS mới (`backdrop-filter`, `:has`, container query, view transitions) |

## 1. Mọi thành phần

| # | Kiểm | Nguồn | Cách kiểm |
|---|---|---|---|
| C1 | Vùng bấm ≥ 44×44 (px trên web) | [Apple] HIG Buttons: "a button needs a hit region of at least 44x44 pt"; HIG Accessibility (bảng kích thước control) | Đo `boundingBox` của phần tử bấm được |
| C2 | Đủ khoảng cách giữa control: khoảng 12pt quanh phần tử có viền/nền, 24pt quanh phần tử không viền | [Apple] HIG Accessibility: "about 12 points of padding around elements that include a bezel… about 24 points… without a bezel" | Đo khoảng cách trong ảnh chụp |
| C3 | Control tự làm có **trạng thái nhấn** | [Apple] HIG Buttons: "Always include a press state for a custom button" | Chụp lúc `mouse.down()` |
| C4 | Hover chỉ khi có chuột thật (`@media (hover: hover)`); không kẹt hover sau khi chạm | [Web] | Kiểm CSS; thử trên điện thoại |
| C5 | Không có ô vuông tô sáng của trình duyệt khi chạm (`-webkit-tap-highlight-color: transparent`), trạng thái nhấn tự vẽ theo hình của control | [Web] (C3 thay thế) | Thử trên điện thoại thật |
| C6 | Focus bàn phím nhìn thấy được, ôm theo hình control | [Web] WCAG 2.4.7 | Tab qua trang, chụp |
| C7 | Chữ thường ≥ 4,5:1; chữ ≥ 18pt hoặc đậm ≥ 3:1; viền/biểu tượng cần để nhận ra control ≥ 3:1 | [Apple] HIG Accessibility (bảng tương phản theo cỡ chữ); [Web] WCAG 1.4.11 cho phần không phải chữ | Đo theo bộ kiểm §0 |
| C8 | Mọi control có nhãn đọc được (chữ thấy hoặc `aria-label`); phần chỉ để trang trí thì `aria-hidden` | [Apple] HIG VoiceOver: "Provide alternative labels for all key interface elements" | Test `getByRole(…, { name })` |
| C9 | Nội dung đổi tại chỗ (đổi bước, thay thẻ) thì báo cho trình đọc màn hình (focus về tiêu đề mới hoặc `role="status"`) | [Apple] HIG VoiceOver: "Inform VoiceOver when visible content or layout changes occur" | Test `toHaveFocus()` sau khi đổi |
| C10 | Không dùng chuyển động làm cách **duy nhất** để báo thông tin | [Apple] HIG Motion: "avoid using it as the only way to communicate important information" | Tắt chuyển động, thông tin vẫn còn |
| C11 | Giảm chuyển động: tắt hoặc giảm hoạt ảnh tự chạy và lặp | [Apple] HIG Accessibility: "reducing automatic and repetitive animations"; WWDC25 219 | Chụp với `reducedMotion: 'reduce'` |
| C12 | Tăng tương phản / giảm trong suốt / forced colors có cách hiển thị riêng (nền đặc, viền rõ, màu hệ thống) | [Apple] WWDC25 219; [Web] `forced-colors` | Chụp từng chế độ |
| C13 | Cùng loại thành phần thì cùng hình, cỡ, trạng thái ở mọi màn | [Chủ dự án] "ưu tiên tính nhất quán" | So ảnh các màn |
| C14 | Không có chữ/hint đè lên nhau ở mọi khổ (đặc biệt 320–390px) | [Web] | Đo giao cắt `getBoundingClientRect` |

## 2. Hình dạng, vật liệu, bóng

| # | Kiểm | Nguồn | Cách kiểm |
|---|---|---|---|
| H1 | Mỗi hình thuộc đúng một trong ba loại: cố định, capsule (bán kính = nửa chiều cao), đồng tâm (bán kính cha − padding) | [Apple] WWDC25 356 (S1) | Ghi loại và phép tính cạnh mỗi `border-radius` |
| H2 | Hình lồng trong khối khác (ô trong thẻ, khối trong thẻ, nút trong ô) dùng **đồng tâm**; đứng riêng thì có bán kính dự phòng | [Apple] WWDC25 356: "nested containers—like artwork in a card"; "a concentric shape with a fallback radius" (S2) | Tính lại: thẻ màn tài khoản 46 = 22 + padding 24 → phần tử lồng 22; nút 40px trong ô 44px → 22 − 2 = 20 |
| H3 | Không có góc "too pinched— or flared" | [Apple] WWDC25 356 | Nhìn ảnh phóng to |
| H4 | Control lớn, cảm ứng: capsule; giao diện desktop dày đặc: capsule chỉ cho hành động nổi bật | [Apple] WWDC25 356 (S3) | Kiểm theo màn |
| H5 | Liquid Glass chỉ ở lớp chức năng (thanh, control nổi, menu, popover, hộp thoại); lớp nội dung (thẻ, danh sách, ảnh) dùng bề mặt thường | [Apple] HIG Materials (M1, M2) | Phân loại bề mặt theo `LIQUID_GLASS.md` §2 |
| H6 | Không kính chồng kính; thứ đặt trên kính dùng fill | [Apple] WWDC25 219 (M6) | Kiểm cây phần tử |
| H7 | Dùng kính ít | [Apple] HIG Materials: "Use Liquid Glass effects sparingly" (M5) | Đếm bề mặt kính trên màn |
| H8 | Bóng: chỉ cho lớp nổi (thẻ trên ảnh, control kính); ô nhập, khối lồng không có bóng đổ | [Apple] WWDC25 219 (M14 nói bóng của kính); [Web] token `shadow-raised` | Kiểm CSS |
| H9 | Khúc xạ (lensing) không dùng trên web | [Chủ dự án] §7.3 `LIQUID_GLASS.md` (chỉ Chromium, thử ở nút 44px không thấy khác) | — |

## 3. Từng thành phần

### 3.1 Thẻ / bề mặt nội dung

| # | Kiểm | Nguồn |
|---|---|---|
| K1 | Bề mặt đặc (`surface-raised`, viền `line`, `shadow-raised`), không Liquid Glass | [Apple] M2; [Chủ dự án] P-72 |
| K2 | Bo góc thẻ và padding cho ra bán kính đồng tâm của phần tử bên trong (46 = 22 + 24) | [Apple] H1, H2 |
| K3 | Cùng chỗ và cùng cỡ giữa các màn chuyển qua lại bằng View Transitions | [Chủ dự án] P-72 |
| K4 | Phân nhóm bằng khoảng cách (tiêu đề và nút chính cách nhóm ô 24, nhãn cách ô 8, ô cách ô 16) | [Apple] WWDC25 356: "hierarchy should be expressed through layout and grouping" |

### 3.2 Ô nhập chữ (`TextField`)

| # | Kiểm | Nguồn |
|---|---|---|
| T-1 | Ô cho thông tin ngắn là text field một dòng; nhiều chữ mới dùng ô nhiều dòng | [Apple] HIG Text fields: "Use a text field to request a small amount of information" |
| T-2 | Nhãn riêng phía trên ô; placeholder chỉ là ví dụ/gợi ý, không thay nhãn | [Apple] HIG Text fields: "it can also be useful to include a separate label describing the field"; P-72 |
| T-3 | Kích thước ô hợp với lượng chữ | [Apple] HIG Text fields: "match the size of a text field to the quantity of anticipated text" |
| T-4 | Bàn phím đúng loại (`type`/`inputMode`: email, tel, numeric) và `autocomplete` đúng | [Apple] HIG Text fields: "show the appropriate keyboard type" |
| T-5 | Mật khẩu dùng ô ẩn chữ | [Apple] HIG Text fields: "Use secure text fields to hide private data" |
| T-6 | Thời điểm kiểm: email khi rời ô; tên người dùng/mật khẩu mới **trước khi** rời ô (khi gõ); ô nhập lại khi rời ô | [Apple] HIG Text fields: "Validate fields when it makes sense…" |
| T-7 | Nhiều ô xếp dọc, cách đều; Tab đi đúng thứ tự | [Apple] HIG Text fields: "Evenly space multiple text fields", "tabbing… flows as people expect" |
| T-8 | Biểu tượng đầu ô cho biết ô dùng làm gì; cuối ô cho chức năng thêm (hiện mật khẩu, xoá) | [Apple] HIG Text fields: "use the leading end… to indicate a field's purpose and the trailing end to offer additional features" |
| T-9 | Ô chữ một dòng có nút xoá ⓧ khi có chữ (`clearable`), trừ ô mật khẩu | [Apple] HIG Text fields (iOS): "Display a Clear button in the trailing end of a text field" |
| T-10 | Lỗi nằm dưới ô, ô có `aria-invalid` và `aria-describedby` trỏ tới lỗi; gợi ý trên hàng nhãn không đè nhãn | [Web]; F-67 |
| T-11 | Ô cao 44, capsule (bo 22) trong thẻ tài khoản; focus sáng viền | [Apple] C1, H1; WWDC25 219 "illuminates from within"; P-72 |

### 3.3 Ô mã một lần (`CodeField`)

| # | Kiểm | Nguồn |
|---|---|---|
| O-1 | Là **một** `<input>` thật `autocomplete="one-time-code"`, `inputMode="numeric"` | [Apple] *Securing logins with iCloud Keychain verification codes*; `UITextContentType.oneTimeCode` |
| O-2 | Không tự chế bàn phím/ô nhập thay hệ thống | [Apple] *Enabling Password AutoFill on a Text Input View*: "If you use a custom input view… iOS can't display the necessary AutoFill UI" |
| O-3 | Dán mã có khoảng trắng/gạch vẫn đủ số (không đặt `maxLength` cắt trước khi lọc) | [Web]; test `CodeField.test.tsx` |
| O-4 | Khung kế tiếp sáng viền thay con trỏ; khung `aria-hidden`, trình đọc màn hình thấy một ô có nhãn | [Apple] C8; [Chủ dự án] 6 khung |
| O-5 | Khung trong thẻ bo đồng tâm 22, không bóng đổ | [Apple] H2, H8 |

### 3.4 Nút

| # | Kiểm | Nguồn |
|---|---|---|
| B-1 | Hành động chính nổi bật bằng **kiểu** (màu nền), không bằng cỡ khác | [Apple] HIG Buttons: "Use style — not size — to visually distinguish the preferred choice" |
| B-2 | Chữ trên nút ngắn, bắt đầu bằng động từ | [Apple] HIG Buttons: "consider starting the label with a verb" |
| B-3 | Có nền/hình rõ ràng (trừ nút trong thanh, menu) | [Apple] HIG Buttons, phần visionOS: "Prefer buttons that have a discernible background shape and fill" (áp sang web vì không có câu tương đương chung) |
| B-4 | Thao tác chưa xong ngay thì nút hiện vòng xoay (`busy`) | [Apple] HIG Buttons: "display an activity indicator" |
| B-5 | Nút phụ cạnh nhau: capsule, cao ≥ 44, tự xuống dòng khi hẹp | [Apple] HIG Buttons, phần visionOS: "prefer the capsule shape in a horizontal row of buttons"; C1 |
| B-6 | Màu chữ/biểu tượng nút không trùng tông nền nội dung sặc sỡ (đơn sắc trên ảnh) | [Apple] HIG Buttons: "Avoid applying a similar color to button labels and content layer backgrounds" |

### 3.5 Nút biểu tượng (hiện mật khẩu, xoá, tạm dừng hoạt ảnh)

| # | Kiểm | Nguồn |
|---|---|---|
| I-1 | Biểu tượng quen thuộc với hành động | [Apple] HIG Buttons: "associate familiar actions with familiar icons" |
| I-2 | Có `aria-label` (và `title` khi có chuột) | [Apple] C8; HIG Buttons (tooltip) |
| I-3 | Hình tròn, bán kính đồng tâm với ô chứa nó | [Apple] H2 |
| I-4 | Nút nổi trên ảnh/nội dung: lớp chức năng → kính (nhoè, viền sáng), đổi đậm/nhạt theo nền để biểu tượng ≥ 4,5:1 và hình nút tách khỏi nền | [Apple] M1, M8; [Web] đo |
| I-5 | Nút chỉ cho chuột/cảm ứng (như nút xoá) thì ngoài thứ tự Tab và bấm không lấy focus khỏi ô | [Web] |

### 3.6 Link, logo

| # | Kiểm | Nguồn |
|---|---|---|
| L-1 | Logo làm link: vùng bấm vừa logo, có hover/nhấn/focus theo hình capsule, chữ không đổi màu khi hover | [Apple] C3; F-66 |
| L-2 | Logo chỉ xuất hiện ở chỗ cần ngữ cảnh | [Apple] HIG Branding: "Resist the temptation to display your logo throughout your app" |
| L-3 | Chữ logo hai màu đúng biến thể nền (bạc hà trên nền tối, `route` trên nền sáng) | [Chủ dự án] P-72 |

### 3.7 Khối lồng (báo lỗi, thông tin, thông báo, xong)

| # | Kiểm | Nguồn |
|---|---|---|
| G-1 | Bo đồng tâm với thẻ (22) | [Apple] H2 |
| G-2 | Khối thông tin/thông báo dùng fill nhạt, không thêm bề mặt thứ hai có bóng | [Apple] M6 (fill cho phần tử lồng) |
| G-3 | Khối báo kết quả/trạng thái có `role="status"` hoặc `role="alert"` | [Apple] C9 |

### 3.8 Chỉ báo tiến trình

| # | Kiểm | Nguồn |
|---|---|---|
| P-1 | Thanh xác định (determinate), đầy từ bên trái, có chữ mô tả ngắn | [Apple] HIG Progress indicators: "fills from the leading side", "display a description that provides additional context" |
| P-2 | Báo đúng: đầy theo phần **đã xong** | [Apple] HIG Progress indicators: "Be as accurate as possible" |
| P-3 | Cùng vị trí ở mọi bước; ẩn khi xong | [Apple] "Display a progress indicator in a consistent location"; "transient… disappearing after it completes" |
| P-4 | Không có chuyển động lặp khi không có gì đang chạy (lặp = "đang xử lý") | [Apple] HIG Progress indicators ("Keep progress indicators moving" là cho tác vụ đang chạy); [Web] WCAG 2.2.2 |
| P-5 | `role="progressbar"` có nhãn, `aria-valuenow/min/max/valuetext` | [Web]; C8 |
| P-6 | Ghi rõ lệch: Apple không có component "bước X trên Y" cho form | `LIQUID_GLASS.md` §9 |

### 3.9 Chuyển động, hoạt ảnh, ảnh nền

| # | Kiểm | Nguồn |
|---|---|---|
| M-1 | Có mục đích, không thêm cho có | [Apple] HIG Motion: "Add motion purposefully… Don't add motion for the sake of adding motion" |
| M-2 | Phản hồi ngắn, gắn với hành động | [Apple] HIG Motion: "Aim for brevity and precision in feedback animations" |
| M-3 | Không gắn chuyển động vào thao tác lặp lại thường xuyên | [Apple] HIG Motion: "avoid adding motion to UI interactions that occur frequently" |
| M-4 | Không bắt người dùng chờ hoạt ảnh xong | [Apple] HIG Motion: "Let people cancel motion" |
| M-5 | Hoạt ảnh tự chạy > 5 giây cạnh nội dung có nút tạm dừng (hoặc dừng trước 5 giây) | [Web] WCAG 2.2.2; F-63 |
| M-6 | Giảm chuyển động: bản tĩnh/không hoạt ảnh | [Apple] C11 |
| M-7 | Chuyển trang bằng View Transitions: bề mặt chung đứng yên, nội dung mờ ra/vào; trình duyệt không hỗ trợ thì chuyển thẳng | [Apple] WWDC25 219 (biến hình giữa trạng thái); P-72 |
| M-8 | Số thời lượng/easing ghi [Web] kèm lý do (Apple không cho số) | [Web] A3 |

### 3.10 Chữ, màu

| # | Kiểm | Nguồn |
|---|---|---|
| X-1 | Tiêu đề ở khoảnh khắc quan trọng đậm, canh trái | [Apple] WWDC25 356 (T1) |
| X-2 | Màu nhấn chỉ cho phần tử/hành động chính | [Apple] WWDC25 219 (M16) |
| X-3 | Chữ trên ảnh/kính đạt tương phản ở chỗ xấu nhất (thêm lớp tối nếu cần) | [Web] T4, C7 |
| X-4 | Token chữ/màu/khoảng cách của DESIGN_SYSTEM; không thêm token khi Apple không yêu cầu | [Web] T2 |
