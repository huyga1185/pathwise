# LIQUID_GLASS — Đặc tả vật liệu kính cho web PathWise

> **Trạng thái:** nguyên tắc **đã duyệt** (P-71, 2026-11-16): hướng dẫn chính thức của Apple đứng trước, quy tắc cũ của dự án chỉ áp ở chỗ Apple không nói. Đã áp cho màn **Đăng nhập** và **Đăng ký cửa hàng** (§9, P-72). Các bề mặt khác ở §2 (header, menu, hộp thoại, điều khiển trên bản đồ) **chưa làm**; khi làm thì theo tài liệu này. Phạm vi: web dispatcher (`web/`); app tài xế không thuộc tài liệu này, trừ chữ logo (§9). Bổ sung cho `docs/client/DESIGN_SYSTEM.md`, không thay nó.

## 0. Quy ước

Mỗi quy tắc mang **một** nhãn:

- **[Apple]**: có câu trong nguồn chính thức, trích nguyên văn (tiếng Anh) kèm mã nguồn.
- **[Web]**: Apple không quy định vì trên nền tảng của Apple hệ thống tự làm (API native), nhưng web buộc phải có một giá trị hoặc kỹ thuật. Ghi cách chọn; giá trị nào chọn được bằng đo thì đo, không chọn theo cảm giác.
- **[Chủ dự án]**: chủ dự án yêu cầu, không phải từ Apple.

Không có nhãn "agent tự suy": thứ gì không thuộc ba nhãn trên thì không đưa vào đặc tả (xem §8).

**Thứ tự ưu tiên (chủ dự án chốt 2026-11-16):** hướng dẫn chính thức của Apple đứng trên các quyết định giao diện cũ của dự án (DESIGN_SYSTEM, các P-xx về giao diện). Quy tắc cũ chỉ áp ở chỗ Apple không nói gì; khi hai bên lệch nhau thì theo Apple và ghi rõ quy tắc cũ nào bị thay. Yêu cầu cụ thể của chủ dự án ghi nhãn **[Chủ dự án]**.

| Mã | Nguồn |
|---|---|
| [HIG] | Human Interface Guidelines, *Materials* — https://developer.apple.com/design/human-interface-guidelines/materials |
| [ADOPT] | *Adopting Liquid Glass* — https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass |
| [RES] | *Apple Design Resources* — https://developer.apple.com/design/resources/ |
| [219] | WWDC25 *Meet Liquid Glass* — https://developer.apple.com/videos/play/wwdc2025/219/ (transcript chính thức) |
| [356] | WWDC25 *Get to know the new design system* — https://developer.apple.com/videos/play/wwdc2025/356/ (transcript chính thức) |
| HIG Text fields | https://developer.apple.com/design/human-interface-guidelines/text-fields |
| HIG Branding | https://developer.apple.com/design/human-interface-guidelines/branding |
| HIG Buttons | https://developer.apple.com/design/human-interface-guidelines/buttons |
| HIG Accessibility | https://developer.apple.com/design/human-interface-guidelines/accessibility |

[RES] chỉ có UI kit, Icon Composer, SF Symbols và font SF cho nền tảng Apple, không ghi điều khoản dùng ngoài nền tảng Apple: không dùng tài sản nào từ đó; giữ font Be Vietnam Pro của DESIGN_SYSTEM.

## 1. Vật liệu: Liquid Glass và vật liệu tiêu chuẩn

| # | Quy tắc | Nguồn |
|---|---|---|
| M1 | Liquid Glass là lớp chức năng cho điều khiển và điều hướng, nổi trên lớp nội dung. | [Apple] "Liquid Glass forms a distinct functional layer for controls and navigation elements — like tab bars and sidebars — that floats above the content layer" [HIG] |
| M2 | Không dùng Liquid Glass trong lớp nội dung; lớp nội dung dùng vật liệu tiêu chuẩn. | [Apple] "Don’t use Liquid Glass in the content layer." … "Instead, use Standard materials for elements in the content layer, such as app backgrounds." [HIG] |
| M3 | Ngoại lệ: control trong lớp nội dung có phần tử tương tác tạm thời (slider, toggle) mang dáng Liquid Glass **khi đang được kích hoạt**. | [Apple] [HIG] |
| M4 | Thành phần chuẩn tự nhận Liquid Glass: thanh (bar), sheet, popover, control; menu, toolbar; sheet và action sheet dạng modal. | [Apple] "standard components like bars, sheets, popovers, and controls automatically adopt this material" [ADOPT §Visual refresh]; "Menus … adopt Liquid Glass"; "Toolbars take on a Liquid Glass appearance" [ADOPT §Menus and toolbars]; "Modal views like sheets and action sheets adopt Liquid Glass." [ADOPT §Windows and modals] |
| M5 | Dùng ít, chỉ cho phần tử chức năng quan trọng nhất. | [Apple] "Use Liquid Glass effects sparingly." … "Limit these effects to the most important functional elements in your app." [HIG] |
| M6 | Không chồng kính lên kính; thứ đặt trên kính dùng fill, độ trong, màu vibrant. | [Apple] "always avoid glass on glass" … "use fills, transparency, and vibrancy for the top elements" [219] |
| M7 | Hai biến thể Regular và Clear, không trộn. Regular cho phần tử có nhiều chữ (alert, sidebar, popover). Clear chỉ khi: trên nội dung giàu hình ảnh, lớp nội dung chịu được lớp làm tối, nội dung đặt lên kính đậm và sáng. | [Apple] [HIG]; "They should never be mixed" và ba điều kiện [219] |
| M8 | Regular làm nhoè và chỉnh độ sáng nền; tint và dải sáng tự đổi để chữ đọc được mà nội dung lộ ra nhiều nhất có thể. | [Apple] "The regular variant blurs and adjusts the luminosity of background content" [HIG]; "The amount of tint and the dynamic range shift to always ensure buttons remain legible, while letting as much of the content through as possible." [219] |
| M9 | Clear trên nền sáng: cân nhắc lớp làm tối 35%. | [Apple] [HIG] |
| M10 | Bốn mức ultra-thin / thin / regular / thick là **vật liệu tiêu chuẩn** cho lớp nội dung, không phải độ dày của Liquid Glass. Vật liệu dày hơn cho chữ nét nhỏ, mỏng hơn để giữ ngữ cảnh. | [Apple] [HIG §Platform considerations, §Standard materials] |
| M11 | Chữ trên vật liệu dùng màu vibrant. | [Apple] "Help ensure legibility by using vibrant colors on top of materials." [HIG] |
| M12 | Lensing (bẻ ánh sáng) là cách chính Liquid Glass tự định hình. | [Apple] "The primary way Liquid Glass visually defines itself is through something called Lensing." [219] |
| M13 | Có lớp highlights: nguồn sáng chiếu lên vật liệu tạo vệt sáng theo hình khối. | [Apple] "Light sources inside of this environment shine on the material producing highlights that respond to geometry" [219] |
| M14 | Bóng đậm hơn khi nằm trên chữ, nhạt hơn trên nền sáng trơn; phần tử lớn hơn có bóng sâu hơn. | [Apple] [219] |
| M15 | Ở trạng thái nghỉ, tránh giao cắt giữa nội dung và Liquid Glass; dời hoặc thu nhỏ nội dung. | [Apple] "In steady states … avoid intersections between content and Liquid Glass. Instead, reposition or scale the content to maintain separation." [219] |
| M16 | Màu nhấn (tint) chỉ cho phần tử và hành động chính; muốn có màu thì đặt ở lớp nội dung. | [Apple] [219] |

## 2. Phân loại bề mặt của PathWise (áp M1–M4 nguyên văn)

| Bề mặt | Thuộc loại nào theo Apple | Vật liệu |
|---|---|---|
| Header ứng dụng (logo, menu, người dùng) | Thanh điều hướng (bar) — M1, M4 | Liquid Glass Regular |
| Điều khiển nổi trên bản đồ (phóng to, chọn tuyến, chú giải) | Control nổi trên nội dung — M1 | Liquid Glass Regular |
| Menu, dropdown, popover | M4 | Liquid Glass Regular |
| Hộp thoại xác nhận (giao việc, huỷ, nhập file, sao chép) | Sheet/alert modal — M4; có nhiều chữ — M7 | Liquid Glass Regular + lớp làm tối khi chen ngang tác vụ ([Apple] "When a task interrupts the main flow, pair Liquid Glass with a dimming layer" [356]) |
| Bảng, panel, thẻ dữ liệu, biểu đồ, form trong trang, danh sách quản trị | Lớp nội dung — M2 | Không Liquid Glass; giữ bề mặt đặc như hiện tại |
| Toast | Nguồn không nói tới | Giữ như hiện tại (không áp dụng) |
| **Thẻ đăng nhập / đăng ký trên ảnh giới thiệu** | **Không phải thanh, control, menu hay modal: là nội dung chính của trang — M2** | **Theo Apple: vật liệu tiêu chuẩn, không Liquid Glass** (xem §7, cần chủ dự án quyết) |
| Ảnh giới thiệu, nền trang | Lớp nội dung (app background) — M2 | Ảnh / nền |

## 3. Hình dạng

| # | Quy tắc | Nguồn |
|---|---|---|
| S1 | Ba loại hình: cố định (bán kính không đổi), capsule (bán kính = nửa chiều cao), đồng tâm (bán kính = bán kính cha − padding). | [Apple] [356] |
| S2 | Hình đồng tâm có bán kính dự phòng khi đứng riêng hoặc khi phép trừ không còn dương. | [Apple] "use a concentric shape with a fallback radius" [356] |
| S3 | Giao diện desktop dày đặc: capsule cho hành động nổi bật; control nhỏ, vừa dùng hình chữ nhật bo góc. | [Apple] [356] |
| S4 | Giá trị bán kính: giữ token DESIGN_SYSTEM (`radius-sm` 4, `radius-md` 8, `radius-lg` 16, `radius-full`). Thẻ đăng nhập `radius-lg`; ô nhập trong thẻ: 16 − padding ≤ 0 nên dùng dự phòng `radius-sm` (S2). | [Web] token có sẵn, không thêm |

## 4. Chữ, màu, khoảng cách

| # | Quy tắc | Nhãn |
|---|---|---|
| T1 | Ở khoảnh khắc quan trọng (alert, giới thiệu), chữ đậm hơn và canh trái. | [Apple] [356] |
| T2 | Thang chữ, màu, khoảng cách: giữ DESIGN_SYSTEM. | [Web] không thêm token khi Apple không yêu cầu |
| T3 | Tiêu đề giới thiệu to hơn `display`, logo to hơn trên màn đăng nhập desktop. | [Chủ dự án] |
| T4 | Chữ trên kính: Apple dùng màu vibrant của hệ thống (M11), web không có. Thay bằng màu chữ đạt 4,5:1 (chữ thường) trên nền **xấu nhất** đo được ở chỗ đặt; chỉ đổi màu chữ khi token hiện có không đạt. | [Web] |
| T5 | Tint của kính: giá trị **nhỏ nhất** đạt T4 ở chỗ đặt (theo M8 "letting as much of the content through as possible"). | [Web] đo, không chọn tay |

## 5. Chuyển động và tương tác

| # | Quy tắc | Nhãn |
|---|---|---|
| A1 | Trạng thái nghỉ yên tĩnh; phần tử sáng lên và co giãn khi tương tác. | [Apple] [219] |
| A2 | Menu "bật mở" từ nút gọi; action sheet xuất phát từ phần tử gọi. | [Apple] [219], [ADOPT §Windows and modals] |
| A3 | Không đặt thời lượng, easing hay hiệu ứng tự chế cho đến khi có thành phần cụ thể cần; khi cần thì ghi vào đây cùng lý do. | [Web] Apple không cho số |

## 6. Accessibility

| Cài đặt (Apple) | Apple mô tả | Web |
|---|---|---|
| Reduced Transparency | "makes Liquid Glass frostier and obscures more of the content behind it" [219] | `prefers-reduced-transparency: reduce` → tăng tint và độ nhoè (vẫn là kính, mờ hơn) |
| Increased Contrast | "makes elements predominantly black or white and highlights them with a contrasting border" [219] | `prefers-contrast: more` → nền đặc trắng/đen theo theme, viền 2px `ink` |
| Reduced Motion | "decreases the intensity of some effects and disables any elastic properties" [219] | `prefers-reduced-motion: reduce` → tắt hoạt ảnh (ảnh nền dùng bản tĩnh) |
| Kiểm thử với các cài đặt | "Test your interface with a variety of display and accessibility settings." [ADOPT §Visual refresh] | Chụp và kiểm từng chế độ |
| — | — | [Web] `forced-colors: active` (Windows) → bỏ kính, viền `CanvasText`; trình duyệt không có `backdrop-filter` → nền đặc |

Tương phản đo theo T4: chụp ảnh thật, ẩn chữ, lấy nền sau chữ, theme sáng và tối, 1280×800, 1366×768, 1920×1080.

## 7. Câu hỏi đã được chủ dự án quyết (2026-11-16)

1. **Thẻ đăng nhập/đăng ký:** theo M1, M2, M4 thẻ là nội dung chính của trang (không phải thanh, control, menu hay modal) → **bề mặt đặc của lớp nội dung**, không Liquid Glass, không lensing hay lớp highlights. Chủ dự án đã thử Clear, Regular với nhiều độ phủ rồi chọn bề mặt đặc.
2. **Ảnh `map-route.svg`:** dùng làm nền màn đăng nhập/đăng ký (lớp nội dung), giữ tông xanh lá của ảnh; đường tuyến trong ảnh uốn theo nét vẽ của chủ dự án (`docs/ux/samples/fb_4.png`).
3. **Lensing:** không dùng (không còn bề mặt Liquid Glass nào trên web).
4. **Thứ tự triển khai các bề mặt ở §2:** chưa chốt; làm khi có task.

## 8. Đã bỏ khỏi bản trước vì là tự suy (không có nguồn, không bắt buộc cho web)

- Token "glass-thick" và mọi khái niệm "Liquid Glass dày": Apple không có (M10).
- Bán kính mới `radius-xl` 24, padding 20 của thẻ, thang chữ `hero`/`lead` với số cụ thể, khoảng cách `space-8`/`space-12`: không có nguồn; thay bằng token có sẵn hoặc chuyển thành yêu cầu của chủ dự án (T3).
- Bóng mới `glass-shadow-md`/`lg`: dùng `shadow-raised` có sẵn.
- Toast dùng kính: nguồn không nói tới.
- Số thời lượng, easing, `scale(0.98)`, "sáng lên 6%" cho tương tác.
- Giới hạn "≤ 4 phần tử kính", "≤ 2 phần tử lensing": không có trong nguồn (nguồn chỉ nói dùng ít, M5).
- `saturate(140%)`: Apple chỉ nói làm nhoè và chỉnh độ sáng (M8).
- Chế độ giảm trong suốt chuyển sang nền đặc: Apple nói "frostier", không nói đặc (§6).

## 9. Màn Đăng nhập và Đăng ký cửa hàng (đã chốt, P-72)

| Thành phần | Thiết kế | Nhãn |
|---|---|---|
| Thẻ | Bề mặt đặc `surface-raised`, viền `line`, bóng `shadow-raised`; rộng 420px; padding 24px; bo 46px; cùng chỗ và cùng cỡ ở hai trang (bắt đầu ngay dưới ghim đích của ảnh) | [Apple] M2; [Web] số đo |
| Ô nhập, nút | Cao 44px (vùng chạm tối thiểu 44pt), capsule bo 22px; ô nền fill nhạt; nhãn riêng phía trên ô (HIG Text fields: "include a separate label") | [Apple] S1, S3 + HIG Text fields; [Web] số đo |
| Bo góc | Đồng tâm: thẻ = 22 + 24 = 46px; khung báo lỗi 46 − 24 = 22px; nút con mắt 40px cách mép ô 2px → 22 − 2 = 20, hình tròn | [Apple] S1, S2 |
| Khoảng cách | Tiêu đề → ô 24px, nhãn → ô 8px, ô → ô 16px, ô cuối → nút chính 24px; gợi ý mật khẩu nằm bên phải hàng nhãn | [Apple] A8 (thứ bậc bằng nhóm), HIG Text fields ("evenly space", "stack vertically"); [Web] bậc DESIGN_SYSTEM |
| Chữ | Tiêu đề thẻ 32px (28px khi màn ≤ 600px), không câu phụ dưới tiêu đề; chữ phụ `ink-muted`, link `route` (đạt ≥ 6,3:1 sáng, ≥ 7,4:1 tối) | [Apple] T1; [Chủ dự án] |
| Focus, hover | Focus: viền ô sáng màu `route` 2px (bóng trong) + quầng sáng, không vòng ngoài (ngoại lệ DESIGN_SYSTEM, chỉ hai thẻ này; forced colors vẫn có vòng `Highlight`); hover: viền đậm, nền sáng nhẹ | [Apple] A1 ("illuminates from within") ; [Web] |
| Ảnh nền | `map-route.svg` phủ kín màn (hai cột) hoặc trong panel (điện thoại, tablet); lớp tối sau chữ giới thiệu; ảnh tĩnh `map-route-static.svg` khi giảm chuyển động | [Chủ dự án]; [Web] T4; [Apple] §6 |
| Chuyển trang | View Transitions: thẻ đứng yên, nội dung cũ mờ đi rồi nội dung mới hiện (hai cột), mờ chéo đồng thời (điện thoại); ảnh nền dùng chung qua layout route `AuthStage` nên hoạt ảnh không chạy lại; tắt khi giảm chuyển động | [Apple] (WWDC25 219: biến hình giữa các trạng thái); [Web] thời lượng |
| Form đăng ký | Tên cửa hàng, email, mật khẩu; **không có ô họ tên** (đặt sau ở Hồ sơ) | [Chủ dự án] |
| Chữ logo | "Path" + "**Wise**" có màu: bạc hà `#9ff0d3` trên nền tối/xanh thương hiệu (màn tài khoản, dải email, đăng nhập app tài xế), `route` trên nền sáng (header, trang lỗi) | [Chủ dự án]; HIG Branding (màu thương hiệu ở lớp nội dung) |

| Nút tạm dừng hoạt ảnh (WCAG 2.2.2) | Nút tròn 44px, Liquid Glass của web: nhoè 14px, chỉnh độ sáng (tối đi trên ảnh sáng ở hai cột, sáng lên trên lớp tối của panel điện thoại), viền sáng, mép trên sáng, bóng mềm; không khúc xạ (thử chỉ Chromium, ở 44px không thấy khác biệt). Biểu tượng ⏸/▶ ≥ 4,95:1; nền nút tách ảnh 3,3–3,7:1 (hai cột), viền 4,3:1 (panel). Nhấn: đậm hơn, thu 94%. Dừng = ảnh tĩnh; nhớ lựa chọn; ẩn khi giảm chuyển động; nền đặc khi giảm trong suốt hoặc tăng tương phản | [Apple] Materials ("functional layer for controls"), Buttons (44×44, press state, "discernible background shape and fill"), Accessibility (Reduce Motion, khoảng cách ~12pt); [Web] số đo, WCAG 2.2.2; [Chủ dự án] chọn nút |

Còn biết (CURRENT_STATE): ghim nhún hơi răng cưa trong `<img>` (F-64).
