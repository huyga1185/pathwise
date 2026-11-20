# UI_GUIDELINES — Quy tắc giao diện PathWise

> Quy tắc giao diện của web dispatcher (`web/`) và, khi áp dụng được, app tài xế (app tài xế theo Material 3, P-77: chỉ áp các quy tắc không phụ thuộc nền tảng, xem `DRIVER_UI_REDESIGN.md` §0). PathWise **học từ** hướng dẫn chính thức của Apple (HIG, *Adopting Liquid Glass*, WWDC25 219/356) chứ không chép: theo P-71, chỗ Apple có nói thì theo trước; chỗ web không làm được (vd kính trên web chỉ là nhoè + chỉnh độ sáng, không có khúc xạ, §A7.3) hoặc dự án chọn khác thì ghi **lệch** kèm lý do ngay tại quy tắc đó. Bổ sung cho `DESIGN_SYSTEM.md` (token, component), không thay nó; hành vi từng màn ở `SCREENS.md`.
>
> Gồm hai phần: **Phần A — Quy tắc** (mã quy tắc không gạch nối: M vật liệu, S hình dạng, T chữ/màu, A accessibility) và **Phần B — Danh sách kiểm** (§0–§3; mã mục kiểm theo nhóm: H, L, C, K…, và có gạch nối theo component: T-8, M-3…). Tài liệu này gộp `LIQUID_GLASS.md` (phần A, cùng số mục với tiền tố A) và `APPLE_CHECKLIST.md` (phần B, giữ số mục) ngày 2026-11-20; đặc tả màn tài khoản (`LIQUID_GLASS.md` §9 cũ) ở `SCREENS.md` §1.2.
>
> Nhãn nguồn: **[Apple]** trích nguyên văn tài liệu Apple; **[Web]** điều web buộc phải có (WCAG, trình duyệt), chọn bằng đo; **[Chủ dự án]** yêu cầu riêng. Mục nào không có nguồn thì không được ghi là "theo Apple".
>
> Đã áp: màn tài khoản (P-72), khu đã đăng nhập G1–G7 (P-74; sidebar, tab bar, menu tài khoản, popover, điều khiển và panel/sheet trên bản đồ, sheet, alert, action sheet theo §A2).

# Phần A — Quy tắc

## A0. Quy ước

Mỗi quy tắc mang **một** nhãn:

- **[Apple]**: có câu trong nguồn chính thức, trích nguyên văn (tiếng Anh) kèm mã nguồn.
- **[Web]**: Apple không quy định vì trên nền tảng của Apple hệ thống tự làm (API native), nhưng web buộc phải có một giá trị hoặc kỹ thuật. Ghi cách chọn; giá trị nào chọn được bằng đo thì đo, không chọn theo cảm giác.
- **[Chủ dự án]**: chủ dự án yêu cầu, không phải từ Apple.

Không có nhãn "agent tự suy": thứ gì không thuộc ba nhãn trên thì không đưa vào đặc tả (xem §A8).

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

## A1. Vật liệu: Liquid Glass và vật liệu tiêu chuẩn

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

## A2. Phân loại bề mặt của PathWise (áp M1–M4 nguyên văn)

| Bề mặt | Thuộc loại nào theo Apple | Vật liệu |
|---|---|---|
| Sidebar (màn rộng), tab bar (màn hẹp) — thay header có logo (P-74) | Thanh điều hướng (bar) — M1, M4 | Liquid Glass Regular (đã làm, G1). Thanh trên của vùng nội dung là dải đặc, lệch có chủ đích: phần tử nhoè thành nền của phần tử con nên menu tài khoản bên trong sẽ mất nhoè (`OWNER_UI_REDESIGN.md` §6.1 H5; G1 đo ở 94%, sau đổi thành đặc hẳn vì chữ cuộn dưới lộ mờ sau nút tài khoản, ghi chú ở `.pw-topbar`) |
| Điều khiển nổi trên bản đồ (phóng to, chọn tuyến, chú giải); panel và sheet ba nấc của trang chi tiết kế hoạch | Control nổi trên nội dung — M1 | Liquid Glass Regular (đã làm, G5) |
| Menu, dropdown, popover | M4 | Liquid Glass Regular (đã làm: menu tài khoản, `pw-popover` cho menu lọc, lịch, chọn trang) |
| Sheet, alert (thay hộp thoại cũ từ G3) | Sheet/alert modal — M4; có nhiều chữ — M7 | Theo Apple: Liquid Glass Regular + lớp làm tối khi chen ngang tác vụ ([Apple] "When a task interrupts the main flow, pair Liquid Glass with a dimming layer" [356]). Đã làm (2026-11-20): sheet, alert, action sheet dùng kính như popover (nền 96%, nhoè 24px) + lớp làm tối 45%. Đo so với nền đặc: theme sáng chỉ thấy bóng rất mờ của trang phía sau (chênh tối đa 18/255), theme tối không khác (0%); chủ dự án chọn giữ kính cho nhất quán với các bề mặt kính khác và theo M4 |
| Bảng, panel, thẻ dữ liệu, biểu đồ, form trong trang, danh sách quản trị | Lớp nội dung — M2 | Không Liquid Glass; giữ bề mặt đặc như hiện tại |
| Toast | Nguồn không nói tới | Giữ như hiện tại (không áp dụng) |
| **Thẻ đăng nhập / đăng ký trên ảnh giới thiệu** | **Không phải thanh, control, menu hay modal: là nội dung chính của trang — M2** | **Theo Apple: vật liệu tiêu chuẩn, không Liquid Glass** (xem §A7, cần chủ dự án quyết) |
| Ảnh giới thiệu, nền trang | Lớp nội dung (app background) — M2 | Ảnh / nền |

## A3. Hình dạng

| # | Quy tắc | Nguồn |
|---|---|---|
| S1 | Ba loại hình: cố định (bán kính không đổi), capsule (bán kính = nửa chiều cao), đồng tâm (bán kính = bán kính cha − padding). | [Apple] [356] |
| S2 | Hình đồng tâm có bán kính dự phòng khi đứng riêng hoặc khi phép trừ không còn dương. | [Apple] "use a concentric shape with a fallback radius" [356] |
| S3 | Giao diện desktop dày đặc: capsule cho hành động nổi bật; control nhỏ, vừa dùng hình chữ nhật bo góc. | [Apple] [356] |
| S4 | Giá trị bán kính: token DESIGN_SYSTEM (`radius-sm` 4, `radius-md` 8, `radius-lg` 16, `radius-full`) cho phần tử đứng riêng (bán kính dự phòng, S2). Thẻ màn tài khoản (P-72) tính ngược từ control: capsule 44px bo 22 + padding 24 = thẻ 46; mọi phần tử lồng trong thẻ bo 22 (`SCREENS.md` §1.2). | [Web] token có sẵn; [Apple] S1, S2 |

## A4. Chữ, màu, khoảng cách

| # | Quy tắc | Nhãn |
|---|---|---|
| T1 | Ở khoảnh khắc quan trọng (alert, giới thiệu), chữ đậm hơn và canh trái. | [Apple] [356] |
| T2 | Thang chữ, màu, khoảng cách: giữ DESIGN_SYSTEM. | [Web] không thêm token khi Apple không yêu cầu |
| T3 | Tiêu đề giới thiệu to hơn `display`, logo to hơn trên màn đăng nhập desktop. | [Chủ dự án] |
| T4 | Chữ trên kính: Apple dùng màu vibrant của hệ thống (M11), web không có. Thay bằng màu chữ đạt 4,5:1 (chữ thường) trên nền **xấu nhất** đo được ở chỗ đặt; chỉ đổi màu chữ khi token hiện có không đạt. | [Web] |
| T5 | Tint của kính: giá trị **nhỏ nhất** đạt T4 ở chỗ đặt (theo M8 "letting as much of the content through as possible"). | [Web] đo, không chọn tay |

## A5. Chuyển động và tương tác

| # | Quy tắc | Nhãn |
|---|---|---|
| A1 | Trạng thái nghỉ yên tĩnh; phần tử sáng lên và co giãn khi tương tác. | [Apple] [219] |
| A2 | Menu "bật mở" từ nút gọi; action sheet xuất phát từ phần tử gọi. | [Apple] [219], [ADOPT §Windows and modals] |
| A3 | Không đặt thời lượng, easing hay hiệu ứng tự chế cho đến khi có thành phần cụ thể cần; khi cần thì ghi vào đây cùng lý do. | [Web] Apple không cho số |

## A6. Accessibility

| Cài đặt (Apple) | Apple mô tả | Web |
|---|---|---|
| Reduced Transparency | "makes Liquid Glass frostier and obscures more of the content behind it" [219] | `prefers-reduced-transparency: reduce` → tăng tint và độ nhoè (vẫn là kính, mờ hơn) |
| Increased Contrast | "makes elements predominantly black or white and highlights them with a contrasting border" [219] | `prefers-contrast: more` → nền đặc trắng/đen theo theme, viền 2px `ink` |
| Reduced Motion | "decreases the intensity of some effects and disables any elastic properties" [219] | `prefers-reduced-motion: reduce` → tắt hoạt ảnh (ảnh nền dùng bản tĩnh) |
| Kiểm thử với các cài đặt | "Test your interface with a variety of display and accessibility settings." [ADOPT §Visual refresh] | Chụp và kiểm từng chế độ |
| — | — | [Web] `forced-colors: active` (Windows) → bỏ kính, viền `CanvasText`; trình duyệt không có `backdrop-filter` → nền đặc |

Tương phản đo theo T4: chụp ảnh thật, ẩn chữ, lấy nền sau chữ, theme sáng và tối, 1280×800, 1366×768, 1920×1080.

## A7. Câu hỏi đã được chủ dự án quyết (2026-11-16)

1. **Thẻ đăng nhập/đăng ký:** theo M1, M2, M4 thẻ là nội dung chính của trang (không phải thanh, control, menu hay modal) → **bề mặt đặc của lớp nội dung**, không Liquid Glass, không lensing hay lớp highlights. Chủ dự án đã thử Clear, Regular với nhiều độ phủ rồi chọn bề mặt đặc.
2. **Ảnh `map-route.svg`:** dùng làm nền màn đăng nhập/đăng ký (lớp nội dung), giữ tông xanh lá của ảnh; đường tuyến trong ảnh uốn theo nét vẽ của chủ dự án (`docs/ux/samples/fb_4.png`).
3. **Lensing:** không dùng (H9 của `APPLE_CHECKLIST.md`: chỉ Chromium, thử ở nút 44px không thấy khác). Kính trên web chỉ là nhoè + chỉnh độ sáng (M8).
4. **Thứ tự triển khai các bề mặt ở §A2:** theo các giai đoạn G1–G7 của `OWNER_UI_REDESIGN.md` (P-74).

## A8. Đã bỏ khỏi bản trước vì là tự suy (không có nguồn, không bắt buộc cho web)

- Token "glass-thick" và mọi khái niệm "Liquid Glass dày": Apple không có (M10).
- Bán kính mới `radius-xl` 24, padding 20 của thẻ, thang chữ `hero`/`lead` với số cụ thể, khoảng cách `space-8`/`space-12`: không có nguồn; thay bằng token có sẵn hoặc chuyển thành yêu cầu của chủ dự án (T3).
- Bóng mới `glass-shadow-md`/`lg`: dùng `shadow-raised` có sẵn.
- Toast dùng kính: nguồn không nói tới.
- Số thời lượng, easing, `scale(0.98)`, "sáng lên 6%" cho tương tác.
- Giới hạn "≤ 4 phần tử kính", "≤ 2 phần tử lensing": không có trong nguồn (nguồn chỉ nói dùng ít, M5).
- `saturate(140%)`: Apple chỉ nói làm nhoè và chỉnh độ sáng (M8).
- Chế độ giảm trong suốt chuyển sang nền đặc: Apple nói "frostier", không nói đặc (§A6).

# Phần B — Danh sách kiểm

## 0. Cách dùng

1. Chọn các mục ở §1 (mọi thành phần) và mục của từng thành phần bị đụng ở §3. **Mỗi trang / màn mới hoặc làm lại phải qua đủ §1b (trang và danh sách) trước khi báo xong** — không đợi chủ dự án nhắc (chủ dự án yêu cầu 2026-11-19, sau nhiều lần phải nhắc cắt chữ, phân trang, chuyển động).
2. Kiểm bằng cách ghi ở cột "Cách kiểm": đo thật, chụp thật, có test; không kiểm bằng trí nhớ.
3. Báo cáo cuối task (và mô tả PR) liệt kê từng mục: ✅ đạt, ❌ chưa đạt (kèm lý do, hoặc ghi lệch có chủ đích ở phần A), — không áp dụng.
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
| C5 | Không có ô vuông tô sáng của trình duyệt khi chạm (`-webkit-tap-highlight-color: transparent`), trạng thái nhấn tự vẽ theo hình của control; chạm đúp lên control không phóng to trang (`touch-action: manipulation`, chụm hai ngón vẫn phóng to); hiệu ứng trượt ngang không làm trang rộng hơn màn hình (F-75) | [Web] (C3 thay thế) | Thử trên điện thoại thật: chạm nhanh hai lần một nút; chuyển trang/vào trang con rồi xem có thanh cuộn ngang hay trang bị thu nhỏ không (đo `document.documentElement.scrollWidth` = bề rộng màn hình) |
| C6 | Focus bàn phím nhìn thấy được, ôm theo hình control | [Web] WCAG 2.4.7 | Tab qua trang, chụp |
| C7 | Chữ thường ≥ 4,5:1; chữ ≥ 18pt hoặc đậm ≥ 3:1; viền/biểu tượng cần để nhận ra control ≥ 3:1 | [Apple] HIG Accessibility (bảng tương phản theo cỡ chữ); [Web] WCAG 1.4.11 cho phần không phải chữ | Đo theo bộ kiểm §0 |
| C8 | Mọi control có nhãn đọc được (chữ thấy hoặc `aria-label`); phần chỉ để trang trí thì `aria-hidden` | [Apple] HIG VoiceOver: "Provide alternative labels for all key interface elements" | Test `getByRole(…, { name })` |
| C9 | Nội dung đổi tại chỗ (đổi bước, thay thẻ) thì báo cho trình đọc màn hình (focus về tiêu đề mới hoặc `role="status"`) | [Apple] HIG VoiceOver: "Inform VoiceOver when visible content or layout changes occur" | Test `toHaveFocus()` sau khi đổi |
| C10 | Không dùng chuyển động làm cách **duy nhất** để báo thông tin | [Apple] HIG Motion: "avoid using it as the only way to communicate important information" | Tắt chuyển động, thông tin vẫn còn |
| C11 | Giảm chuyển động: tắt hoặc giảm hoạt ảnh tự chạy và lặp | [Apple] HIG Accessibility: "reducing automatic and repetitive animations"; WWDC25 219 | Chụp với `reducedMotion: 'reduce'` |
| C12 | Tăng tương phản / giảm trong suốt / forced colors có cách hiển thị riêng (nền đặc, viền rõ, màu hệ thống) | [Apple] WWDC25 219; [Web] `forced-colors` | Chụp từng chế độ |
| C13 | Cùng loại thành phần thì cùng hình, cỡ, trạng thái ở mọi màn | [Chủ dự án] "ưu tiên tính nhất quán" | So ảnh các màn |
| C14 | Không có chữ/hint đè lên nhau ở mọi khổ (đặc biệt 320–390px) | [Web] | Đo giao cắt `getBoundingClientRect` |
| C15 | Sáng/tối theo hệ điều hành, đổi ngay khi hệ điều hành đổi; không thêm nút hay lựa chọn giao diện riêng; đẹp và đủ tương phản ở cả hai, kể cả khi bật tăng tương phản/giảm trong suốt | [Apple] HIG Dark Mode: "Avoid offering an app-specific appearance setting", "Ensure that your app looks good in both appearance modes"; P-73 | Chụp hai theme; đổi `colorScheme` khi trang đang mở |

## 1b. Trang và danh sách (mọi trang mới hoặc làm lại)

| # | Yêu cầu | Nguồn | Cách kiểm |
|---|---|---|---|
| L1 | **Chữ dài:** tên/câu dài tối đa 2 dòng rồi "…" (bẻ cả chuỗi không dấu cách), `title` có đủ chữ; chữ bị cắt thì **phải có chỗ đọc đủ** (sheet chi tiết, trang con); chữ không mở được ở đâu khác thì **không cắt**, cho xuống dòng. Hàng điện thoại: dòng chính 1 dòng "…", dòng phụ xuống dòng; tiêu đề trang không tràn | `OWNER_UI_REDESIGN.md` G2.11, §8.10, §9.8; [Apple] HIG Typography: *"Avoid truncating text in scrollable regions unless people can open a separate view to read the rest"* | Thử dữ liệu dài tối đa (tên 100–150 ký tự, chuỗi không dấu cách) ở 1440 / 1024 / 960 / 390 / 320px; đo không phần tử nào tràn |
| L2 | **Phân trang:** danh sách có thể dài (API phân trang, hoặc số dòng không giới hạn theo dữ liệu) có điểm kết thúc "‹ Trang x/y ›", trang trên URL, đổi lọc về trang 1, trang quá cuối về trang cuối; điện thoại thanh trang ở trên danh sách. Danh sách nhỏ theo bản chất (xe, thành viên, tối đa N theo API) thì không | G2.9, §8.17, §11 | Thử đủ dòng cho ≥ 2 trang; tải lại / Back giữ trang |
| L3 | **Bảng không cuộn ngang:** bảng hẹp thì cột ít quan trọng thành dòng phụ dưới tên; điện thoại là danh sách hàng, bấm mở sheet đủ mọi trường | G2.11, G2.5, G3 | Đo `scrollWidth` của bảng ở 960–1440px; điện thoại không còn bảng |
| L4 | **Trống / đang tải / lỗi:** trống kiểu `ContentUnavailableView` không thẻ; đang tải là vòng xoay hoặc hàng giữ chỗ sau 0,3 s; lỗi là `ErrorBanner` | G2.6, G2.7, G2.12 | Giả từng trạng thái bằng `page.route` |
| L5 | **Chuyển động:** khối xuất hiện, đổi màn tại chỗ, mở/đóng (sheet, alert, nhóm thu gọn) có hoạt ảnh theo §6.2 / §9.6; thao tác lặp nhiều không thừa chuyển động; giảm chuyển động chỉ mờ | §6.2, §9.6, §10.9; [Apple] HIG Motion | Ghi `getAnimations()` khi thao tác; thử `prefers-reduced-motion` |
| L6 | **Nhóm, bo, viền** theo khu đã redesign (nhóm nền bo 16 không viền; lồng trong panel bo đồng tâm hoặc dự phòng) | DESIGN_SYSTEM "Bo góc"; §9.9 | So ảnh với các trang đã redesign |

## 2. Hình dạng, vật liệu, bóng

| # | Kiểm | Nguồn | Cách kiểm |
|---|---|---|---|
| H1 | Mỗi hình thuộc đúng một trong ba loại: cố định, capsule (bán kính = nửa chiều cao), đồng tâm (bán kính cha − padding) | [Apple] WWDC25 356 (S1) | Ghi loại và phép tính cạnh mỗi `border-radius` |
| H2 | Hình lồng trong khối khác (ô trong thẻ, khối trong thẻ, nút trong ô) dùng **đồng tâm**; đứng riêng thì có bán kính dự phòng | [Apple] WWDC25 356: "nested containers—like artwork in a card"; "a concentric shape with a fallback radius" (S2) | Tính lại: thẻ màn tài khoản 46 = 22 + padding 24 → phần tử lồng 22; nút 40px trong ô 44px → 22 − 2 = 20 |
| H3 | Không có góc "too pinched— or flared" | [Apple] WWDC25 356 | Nhìn ảnh phóng to |
| H4 | Control lớn, cảm ứng: capsule; giao diện desktop dày đặc: capsule chỉ cho hành động nổi bật | [Apple] WWDC25 356 (S3) | Kiểm theo màn |
| H5 | Liquid Glass chỉ ở lớp chức năng (thanh, control nổi, menu, popover, hộp thoại); lớp nội dung (thẻ, danh sách, ảnh) dùng bề mặt thường | [Apple] HIG Materials (M1, M2) | Phân loại bề mặt theo §A2 |
| H6 | Không kính chồng kính; thứ đặt trên kính dùng fill | [Apple] WWDC25 219 (M6) | Kiểm cây phần tử |
| H7 | Dùng kính ít | [Apple] HIG Materials: "Use Liquid Glass effects sparingly" (M5) | Đếm bề mặt kính trên màn |
| H8 | Bóng: chỉ cho lớp nổi (thẻ trên ảnh, control kính); ô nhập, khối lồng không có bóng đổ | [Apple] WWDC25 219 (M14 nói bóng của kính); [Web] token `shadow-raised` | Kiểm CSS |
| H9 | Khúc xạ (lensing) không dùng trên web | [Chủ dự án] §A7.3 (chỉ Chromium, thử ở nút 44px không thấy khác) | — |

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
| B-4 | Thao tác chưa xong ngay thì nút hiện vòng xoay **trong nút** (`busy`, nhãn bận `busyLabel`), sau 0,3 s để việc nhanh không nháy | [Apple] HIG Buttons: "display an activity indicator", "within a button", "a different label alongside the activity indicator"; *(trước 2026-11-18 `busy` chỉ làm mờ nút — đã sửa, OWNER_UI_REDESIGN §9.5 #8)* |
| B-5 | Nút phụ cạnh nhau: cùng cỡ nên cùng hình (cỡ 44 → capsule), tự xuống dòng khi hẹp | [Apple] WWDC25 356 (Large → capsule); HIG Buttons: "Use style — not size"; C1. *Đính chính 2026-11-18: trước trích câu của mục visionOS* |
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
| P-6 | Ghi rõ lệch: Apple không có component "bước X trên Y" cho form | `SCREENS.md` §1.2 |

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
