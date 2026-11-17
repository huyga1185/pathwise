# OWNER_UI_REDESIGN — Nghiên cứu thiết kế lại giao diện chính của chủ cửa hàng theo Apple

> **Trạng thái:** nghiên cứu xong; chủ dự án đã trả lời Q1–Q5 (§5, 2026-11-17), quyết định ở P-74. **G1 (khung app) đã làm** (§6, branch `docs/owner-ui-redesign`). **G2 (danh sách kế hoạch) đã làm** (§7, branch `feat/owner-ui-g2`). Tiếp: G3.
>
> Theo P-71 (hướng dẫn Apple đứng trước), quy tắc ở `docs/client/LIQUID_GLASS.md`, kiểm từng thành phần theo `docs/client/APPLE_CHECKLIST.md`. Nhãn: **[Apple]** trích nguyên văn HIG (bản tải 2026-11-17), **[Web]** điều web buộc phải có, chọn bằng đo, **[Chủ dự án]** chờ hoặc đã có quyết định. Phần "Đề xuất" là ý của agent, mỗi dòng ghi nguồn; chỗ nào Apple không nói thì ghi rõ.

## 0. Phạm vi

Khu đã đăng nhập của OWNER (và DISPATCHER, cùng màn): khung app (header, điều hướng, tài khoản) và các màn `/plans`, `/plans/new` + `/plans/:id/edit`, `/plans/:id` (tìm toạ độ, kết quả tối ưu, chỉnh tay, theo dõi), `/reports`, `/vehicles`, `/members`, `/store` + `/store/audit`, `/profile`.

Ngoài phạm vi: màn tài khoản (đã làm, P-72), khu quản trị ADMIN (làm sau, dùng lại kết quả), app tài xế.

**Nền tảng tham chiếu.** Web PathWise chạy trên máy tính (chuột, cửa sổ rộng) và điện thoại (chạm, màn hẹp). Apple chia theo kích thước, không theo thiết bị: [Apple] HIG Layout: "Determine layout based on size classes, not device type or orientation." Đề xuất: **rộng** (≥ 960px, như bước của màn tài khoản) theo iPadOS/macOS; **hẹp** theo iOS. Chức năng giữ nguyên ở mọi khổ: [Apple] "Don't change your app's functionality based on the space it occupies."

## 1. Hiện trạng (chụp 2026-11-17, tài khoản owner, 1440 và 390px)

| # | Quan sát | Bằng chứng |
|---|---|---|
| H1 | Header trắng có viền dưới: logo + chữ "PathWise", 5 link chữ (Kế hoạch, Báo cáo, Xe, Thành viên, Cửa hàng), tên cửa hàng, tên người dùng (link Hồ sơ), "Đăng xuất". | `AppHeader.tsx`, ảnh 1440 |
| H2 | Ở 390px header vỡ: 5 link xuống 2 hàng ("Cửa hàng" lẻ một hàng), tên cửa hàng/người dùng/Đăng xuất thêm một hàng; header chiếm ~200px. | ảnh `plans-390` |
| H3 | Tiêu đề trang to ("Kế hoạch giao hàng"), không có hành động chính trên đầu trang; "Tạo kế hoạch" chỉ có trong trạng thái trống. | ảnh `plans-1440` |
| H4 | Bộ lọc (ô ngày native, select trạng thái, "Xoá bộ lọc") nằm trong một thẻ có viền; ô và select cao 36–40px, bo 4px. | ảnh, `ui.css` |
| H5 | Mọi khối là thẻ trắng viền xám bo 8–16px chồng lên nền xám; nhóm bằng viền nhiều hơn bằng khoảng cách. | ảnh `plans_new-1440` |
| H6 | Nút mặc định cao 36px, ô nhập 36px: dưới 44 khi chạm trên điện thoại. | `ui.css` (`.pw-btn` 36px, `.pw-field-input` 36px) |
| H7 | Bảng (`DataTable`) dùng cho danh sách; trên điện thoại là bảng cuộn ngang hoặc thẻ (tuỳ màn). Tiêu đề cột viết HOA. | `DataTable.tsx`, ảnh `plans_new` |
| H8 | Cỡ chữ cố định bằng px (0 chỗ dùng `rem`): chữ không lớn theo cỡ chữ người dùng đặt trong trình duyệt; thân chữ 14px ở mọi khổ. | `grep` CSS |
| H9 | Hộp thoại (`Dialog`) một kiểu cho mọi việc: xác nhận, nhập file, sao chép, giao việc. | `Dialog.tsx`, SCREENS |
| H10 | Logo + "PathWise" ở mọi trang. | `AppHeader.tsx` |

Chưa chụp được màn chi tiết kế hoạch (tài khoản thử chưa có kho, kế hoạch); khi làm giai đoạn đó cần dữ liệu mẫu (kho, xe, kế hoạch đã tối ưu, đã giao việc).

## 2. Apple nói gì, theo thành phần

### 2.1 Điều hướng chính (5 khu)

- [Apple] Tab bars: "Use a tab bar to support navigation, not to provide actions." · "Make sure the tab bar is visible when people navigate to different sections of your app." · "Include tab labels… Use single words whenever possible." · "Don't disable or hide tab bar buttons, even when their content is unavailable." · "Avoid overflow tabs."
- [Apple] Tab bars, iOS: "A tab bar floats above content at the bottom of the screen. Its items rest on a Liquid Glass background."
- [Apple] Tab bars, iPadOS: "The system displays a tab bar near the top of the screen… Prefer a tab bar for navigation… If your app is more complex, you can provide the option to convert the tab bar to a sidebar."
- [Apple] Sidebars, iOS/iPadOS: "Consider using a tab bar first. A tab bar provides more space to feature content." Sidebar cần "a large amount of vertical and horizontal space"; macOS dùng sidebar.
- [Apple] Toolbars: "Don't title windows with your app name." · Branding: "Resist the temptation to display your logo throughout your app."

### 2.2 Thanh công cụ và tiêu đề trang

- [Apple] Toolbars: "Provide a useful title for each window… keep the title under 15 characters long" · "Provide actions that support the main tasks people perform" · "Use the standard Back and Close buttons… don't use a text label" (ý: nút quay lại dùng biểu tượng chuẩn) · "Minimize the number of groups… aim for a maximum of three" · "Reduce the use of toolbar backgrounds and tinted controls" · iOS: "Use a large title to help people stay oriented… transitions to a standard title as people begin scrolling."
- [Apple] Layout: "Differentiate controls from content. Take advantage of the Liquid Glass material… Instead of applying a solid or semi-opaque background color beneath controls, use a scroll edge effect."

### 2.3 Danh sách và bảng

- [Apple] Lists and tables: "Prefer displaying text in a list or table" · "Provide appropriate feedback when people select a list item" · "Keep item text succinct" · "Use descriptive column headings… Use nouns or short noun phrases with title-style capitalization" (không viết HOA toàn bộ) · macOS: "let people click a column heading to sort" · "Consider using alternating row colors in a multicolumn table."
- [Apple] Split views: "persistently highlight the current selection in each pane that leads to the detail view."

### 2.4 Lọc và tìm

- [Apple] Search fields: "If possible, start search immediately when a person types" · "Place search as an inline field when its position alongside the content it searches strengthens that relationship… position an inline search field above the list it searches."
- [Apple] Segmented controls: "no more than about five to seven segments in a wide interface and no more than about five segments on iPhone" · "Use nouns or noun phrases for segment labels."
- [Apple] Pickers: "If you need to display a fairly short list of choices, consider using a Pull-down buttons instead" · "Use a compact date picker when space is constrained."

### 2.5 Nhóm, khoảng cách, hình

- [Apple] Layout: "Group related items… you might use negative space, container shapes, or separator lines" · "Order content by relative importance… place the most important items near the top and leading side" · "Use progressive disclosure."
- [Apple] WWDC25 356: "hierarchy should be expressed through layout and grouping"; ba loại hình, đồng tâm (`APPLE_CHECKLIST` H1–H4).

### 2.6 Control, cỡ, chữ

- [Apple] Accessibility (bảng cỡ control): iOS/iPadOS mặc định **44×44**, tối thiểu 28×28; macOS mặc định **28×28**, tối thiểu 20×20.
- [Apple] Typography: cỡ chữ mặc định iOS/iPadOS **17pt** (tối thiểu 11), macOS **13pt** (tối thiểu 10) · Layout: "Be prepared for text-size changes" (Dynamic Type; web tương ứng: cỡ chữ người dùng đặt trong trình duyệt).
- [Apple] Buttons: "Use style — not size — to visually distinguish the preferred choice" · "Always include a press state."

### 2.7 Hộp thoại, menu, phản hồi

- [Apple] Modality: "Present content modally only when there's a clear benefit" · "Aim to keep modal tasks simple, short, and streamlined" · "getting confirmation before closing a modal view" khi có thể mất dữ liệu.
- [Apple] Sheets: sheet cho việc ngắn có nhập liệu; "If you provide a Done button, always pair it with a Cancel button"; iOS: Cancel ở đầu, Done ở cuối thanh trên; iPhone: "consider supporting the medium detent", "Support swiping to dismiss."
- [Apple] Alerts: "Use alerts sparingly" · "Avoid using an alert merely to provide information" · "Avoid displaying alerts for common, undoable actions" · nút: "one- or two-word title that describes the result", không dùng "OK".
- [Apple] Popovers: "Use a popover to expose a small amount of information or functionality" · "Avoid using a popover to show a warning."
- [Apple] Menus / Pull-down buttons: hành động phụ gom vào menu; nhãn bắt đầu bằng động từ; "…" khi cần nhập thêm.
- [Apple] Loading: "Show something as soon as possible… consider showing placeholder text" · Feedback: "Consider integrating status feedback into your interface" (trạng thái đặt cạnh thứ nó mô tả).

### 2.8 Bản đồ (chi tiết kế hoạch)

- [Apple] Maps: "In general, make your map interactive" · "Clearly identify elements that people select" · "Cluster overlapping points of interest" · "Make sure there's enough contrast between custom controls and the map." Control nổi trên bản đồ là lớp chức năng → Liquid Glass (M1).

## 3. Đề xuất (agent, chờ duyệt)

| # | Thành phần | Hiện tại | Đề xuất | Nguồn |
|---|---|---|---|---|
| D1 | Điều hướng, **hẹp** | Link chữ trong header, vỡ 3 hàng | **Tab bar dưới đáy**, nổi, kính; 5 tab có biểu tượng + nhãn một từ (Kế hoạch, Báo cáo, Xe, Thành viên, Cửa hàng) | [Apple] Tab bars iOS; 5 tab vừa, không cần "More" |
| D2 | Điều hướng, **rộng** | Link chữ trong header trắng | **Sidebar bên trái** (chủ dự án chọn, Q1): kính nổi, biểu tượng + nhãn, ẩn/hiện được (§6) | [Apple] Sidebars; [Chủ dự án] |
| D3 | Tài khoản | Tên cửa hàng + tên người dùng + "Đăng xuất" luôn hiện | Một **nút tròn chữ viết tắt** ở cuối thanh trên → menu: tên, cửa hàng, "Hồ sơ và bảo mật", "Đăng xuất…" (giống bảng tài khoản app tài xế) | [Apple] Toolbars "Choose items deliberately to avoid overcrowding"; Menus; nhất quán với app tài xế [Chủ dự án] |
| D4 | Logo | Logo + chữ ở mọi trang | **Bỏ hẳn** khỏi trang trong app (Q2); tiêu đề là tên khu | [Apple] Toolbars "Don't title windows with your app name"; Branding |
| D5 | Tiêu đề trang | "Kế hoạch giao hàng" to, không hành động | Tiêu đề ngắn (< 15 ký tự: "Kế hoạch"), hành động chính ở cuối hàng tiêu đề ("Tạo kế hoạch"); hẹp: tiêu đề lớn thu nhỏ khi cuộn | [Apple] Toolbars Titles, Actions, iOS large title |
| D6 | Quay lại | "← Kế hoạch" chữ | Nút quay lại biểu tượng chuẩn (chevron) + tên khu cha | [Apple] Toolbars Navigation |
| D7 | Bộ lọc danh sách kế hoạch | Thẻ có ô ngày + select + "Xoá bộ lọc" | Bỏ thẻ; hàng ngay trên danh sách: chọn ngày dạng nút (compact date picker) + nút menu trạng thái (6 lựa chọn > 5 nên không dùng segmented); "Xoá bộ lọc" chỉ hiện khi đang lọc | [Apple] Pickers, Pull-down buttons, Segmented controls (giới hạn số mục), Layout progressive disclosure |
| D8 | Danh sách | Bảng trong thẻ viền | **Rộng:** bảng nhiều cột, tiêu đề cột chữ thường (không HOA), bấm tiêu đề để sắp xếp nếu API hỗ trợ, cả hàng bấm được, hover/nhấn rõ. **Hẹp:** danh sách hàng (tên, ngày, trạng thái, chevron) trong nhóm bo đồng tâm | [Apple] Lists and tables |
| D9 | Thẻ, nhóm | Nhiều thẻ viền | Nhóm bằng khoảng cách và nền nhóm (inset grouped) thay viền; ít viền hơn | [Apple] Layout grouping; WWDC25 356 |
| D10 | Cỡ control | 36px mọi nơi | Theo con trỏ: chạm (`pointer: coarse`) ≥ 44; chuột (`pointer: fine`) giữ 32–36 (≥ 28 của macOS); capsule cho control lớn, bo đồng tâm trong nhóm | [Apple] Accessibility bảng cỡ; WWDC25 356; [Web] media `pointer`  **Đã làm ở tầng chung trong G1 (F-71):** ≥ 44px khi chạm, link chữ có vùng bấm 28/44px, hover chỉ khi có chuột, không ô vuông chạm. |
| D11 | Chữ | px cố định, 14px mọi khổ | Đổi sang `rem` (theo cỡ chữ trình duyệt); thân chữ 16–17 trên điện thoại, 14 trên máy tính; ô nhập ≥ 16px trên điện thoại (iOS Safari không zoom) | [Apple] Typography mặc định 17/13pt, Dynamic Type; [Web] |
| D12 | Hộp thoại | Một kiểu | **Sheet** cho việc có nhập liệu (nhập file, sao chép, giao việc: Huỷ / Xong); **alert** chỉ cho hành động không hoàn tác (huỷ kế hoạch) với nút động từ; **popover** cho chọn nhanh (ngày, trạng thái); hẹp: sheet từ đáy có grabber | [Apple] Modality, Sheets, Alerts, Popovers |
| D13 | Đang tải | "Đang tải…" | Khung giữ chỗ (placeholder rows) theo đúng hình nội dung | [Apple] Loading |
| D14 | Trạng thái | `StatusPill` màu | Giữ, đặt cạnh mục nó mô tả; màu không phải cách duy nhất (có chữ) | [Apple] Feedback; APPLE_CHECKLIST C10 |
| D15 | Chi tiết kế hoạch | (chưa chụp) | Bản đồ phủ vùng nội dung; danh sách tuyến là panel bên (rộng) hoặc sheet từ đáy có các nấc (hẹp); control bản đồ kính nổi; chọn tuyến làm sáng tuyến trên bản đồ | [Apple] Maps, Split views, Sheets (detent) |
| D16 | Form tạo kế hoạch | 3 thẻ, bảng 6 cột ô nhập | Nhóm theo khoảng cách; danh sách điểm giao: rộng giữ bảng nhập nhanh, hẹp mỗi điểm một hàng mở sheet để sửa | [Apple] Layout, Sheets — §5 Q4 |
| D17 | Báo cáo | Thẻ số + biểu đồ + bảng | Áp HIG Charts khi tới giai đoạn đó (chưa nghiên cứu sâu) | [Apple] Charts |

## 4. Kế hoạch triển khai (đề xuất)

Mỗi giai đoạn một branch, một PR, kiểm theo `APPLE_CHECKLIST.md`, cập nhật docs cùng PR:

1. **G1 Khung app:** sidebar/tab bar (D1, D2), nút tài khoản + menu (D3), logo (D4), hàng tiêu đề + hành động (D5, D6), nền tảng control/chữ (D10, D11) ở component dùng chung. Áp cho cả khu quản trị nếu dùng chung `AppHeader`.
2. **G2 Danh sách kế hoạch:** bộ lọc (D7), bảng/danh sách (D8), nhóm (D9), placeholder (D13).
3. **G3 Xe, Thành viên, Cửa hàng, Hồ sơ, Nhật ký:** cùng mẫu G2 (bộ lọc, bảng/danh sách hàng, hàng giữ chỗ, **menu ngữ cảnh của hàng** như G2.13, thanh trang); hộp thoại sang sheet/alert (D12); nút `secondary` viền rỗng (F-73); kéo đổi độ rộng cột.
4. **G4 Tạo/sửa kế hoạch** (D16) và hộp thoại nhập file, sao chép.
5. **G5 Chi tiết kế hoạch:** tìm toạ độ, kết quả tối ưu, chỉnh tay, theo dõi (D15) — cần dữ liệu mẫu.
6. **G6 Báo cáo** (D17).

Prototype (artboard) cập nhật theo từng giai đoạn hoặc sau cùng — §5 Q5.

## 5. Câu hỏi cho chủ dự án và trả lời (2026-11-17)

| # | Câu hỏi | Trả lời |
|---|---|---|
| Q1 | Điều hướng ở màn rộng: tab bar trên đầu hay sidebar trái? | **Sidebar bên trái** (kiểu macOS/iPadOS). Màn hẹp: tab bar dưới đáy. |
| Q2 | Logo trong app | **Bỏ hẳn**; tiêu đề là tên khu. |
| Q3 | Tài khoản | **Gom vào một nút tròn chữ viết tắt + menu** (tên, cửa hàng, Hồ sơ và bảo mật, Đăng xuất…). |
| Q4 | Form tạo kế hoạch trên điện thoại | **Để sau**, quyết khi tới G4. |
| Q5 | Thứ tự làm, cập nhật prototype | Làm theo G1 → G6, **code luôn, sửa trực tiếp** (không vẽ prototype trước). Khu quản trị đổi **cùng G1**. G5 tạo dữ liệu mẫu như đề xuất. |

## 6. Thiết kế G1 theo các trả lời (khung app)

**Sidebar (màn rộng ≥ 960px).** Nguồn: [Apple] Sidebars.
- 5 mục, mỗi mục biểu tượng + nhãn (Kế hoạch, Báo cáo, Xe, Thành viên, Cửa hàng); mục đang mở làm sáng liên tục ("persistently highlight the current selection", Split views); một cấp, không cần nhóm ("no more than two levels").
- Sidebar là lớp chức năng nổi trên nội dung ("sidebars can float above content in the Liquid Glass layer") → kính Regular, bo đồng tâm với mép cửa sổ (WWDC25 356: "For iPad and Mac, use a concentric shape that aligns with the window edge").
- Cho ẩn/hiện sidebar ("Consider letting people hide the sidebar") bằng nút ở đầu thanh trên; nhớ lựa chọn trên máy (cài đặt riêng của người dùng, không phải theme).
- **Không** đặt tài khoản ở đáy sidebar: [Apple] Sidebars macOS "Avoid putting critical information or actions at the bottom of a sidebar". Nút tài khoản ở cuối thanh trên của vùng nội dung.
- Màu biểu tượng: theo màu nhấn của app, không tô màu riêng từng mục ("Make sure any sidebar icon colors you choose serve a clear purpose").

**Tab bar (màn hẹp).** Nguồn: [Apple] Tab bars iOS.
- Nổi dưới đáy, kính; 5 tab biểu tượng + nhãn một từ (nhãn "Thành viên", "Cửa hàng" là hai từ: chấp nhận, không có từ đơn rõ nghĩa hơn); không ẩn khi chuyển khu, chỉ bị che bởi sheet/hộp thoại.
- Chừa vùng an toàn đáy (`env(safe-area-inset-bottom)`), nội dung cuộn dưới tab bar.

**Thanh trên vùng nội dung.** Nguồn: [Apple] Toolbars.
- Đầu: nút ẩn/hiện sidebar (rộng) hoặc nút quay lại (trang con). Giữa/đầu: tiêu đề khu < 15 ký tự (hẹp: tiêu đề lớn, thu nhỏ khi cuộn). Cuối: hành động chính của trang (tối đa ~3 nhóm) và nút tài khoản.
- Không nền đặc: kính / scroll edge effect khi nội dung cuộn dưới (Layout "Differentiate controls from content").

**Không còn:** logo + chữ PathWise, tên cửa hàng và nút "Đăng xuất" luôn hiện (vào menu tài khoản).

**Khu quản trị** dùng cùng `AppShell` (4 mục, tiêu đề sidebar "Quản trị"), làm cùng G1.

**Đã làm (G1):** `web/src/shared/ui/AppShell.tsx` thay `AppHeader`; chỉ render **một** vùng điều hướng theo khổ màn (sidebar hoặc tab bar, để trình đọc màn hình không gặp hai "Điều hướng chính"); biểu tượng mới `ChartIcon`, `TruckIcon`, `UsersIcon`, `ListIcon`, `OverviewIcon`, `SidebarIcon`. Thanh trên gần đặc, không làm nhoè: phần tử có `backdrop-filter` thành nền của phần tử con, nên nếu thanh trên nhoè thì menu tài khoản bên trong không nhoè được nội dung trang. Test `AppShell.test.tsx`.

### 6.2 Chuyển động của khung (chủ dự án yêu cầu trong G1, 2026-11-17)

Kế hoạch G1–G6 chưa có task riêng cho chuyển động; làm trong G1. Apple chỉ cho nguyên tắc, không cho số (HIG Motion); số là [Web].

| # | Chuyển động | Thiết kế | Nguồn |
|---|---|---|---|
| CĐ1 | Đổi khu (sidebar, tab bar) | Một **ô đánh dấu** dưới mục đang mở **trượt** sang mục mới, có nảy nhẹ (0,34 s, `cubic-bezier(0.34, 1.3, 0.64, 1)`); không trượt ở lần vẽ đầu | [Apple] WWDC25 219: "controls continually shape shift", "morphs between the controls"; [Web] đo vị trí mục bằng JS, chạy mọi trình duyệt |
| CĐ2 | Nội dung trang khi đổi khu | **Màn rộng: View Transition** (link của sidebar, menu tài khoản): trang cũ mờ ra 0,12 s rồi trang mới mờ vào và nhô lên 12px (0,28 s, sau 0,1 s), không chồng chữ hai trang; khung (sidebar, tab bar, ô đánh dấu) vẫn chạy trực tiếp (root hiện ngay, `app.css`). Tab bar, thanh trên, sidebar có tên chuyển cảnh riêng (`pw-tabbar`, `pw-topbar`, `pw-sidebar`): trước đó trên điện thoại trang (lớp chuyển cảnh `pw-page`) đè lên tab bar cho tới khi chuyển xong (chủ dự án báo; đo lại 17 khung, tab bar luôn ở trên). Khung `pw-page` không biến hình giữa kích thước trang cũ và mới (`animation: none` cho group), và trang đã đăng nhập giữ chỗ thanh cuộn (`scrollbar-gutter: stable`): trước đó đi sang Cửa hàng (trang dài, hiện thanh cuộn) trang bị hẹp và lệch rồi mới nhô lên, chủ dự án thấy "chạy ngang rồi mới lên". Điện thoại (chủ dự án báo 2026-11-17): (1) lúc chuyển tab, vùng nhoè của tab bar bị vẽ thành khối chữ nhật quanh mép (khi trình duyệt chụp thanh để chuyển cảnh) → trong lúc chuyển cảnh tab bar và sidebar tắt nhoè, giữ màu kính; (2) nút tài khoản nháy/nhảy khi cuộn — không tái hiện được trên emulator; sửa hai nguyên nhân khả dĩ: thanh trên đặc hẳn (94% để lộ chữ cuộn phía sau), nút không thu nhỏ khi nhấn trên màn cảm ứng (chỉ đổi nền). Sau đó chủ dự án báo thêm: đã cuộn rồi chuyển tab thì thanh trên nháy, và tab bar gần như trong suốt một lúc khi chuyển (do tắt nhoè). Đổi hướng: **điện thoại không dùng View Transitions** — trang mới mờ vào và nhô lên 12px (0,28 s) tại chỗ, thanh trên và tab bar không bị chụp nên giữ nguyên như lúc đứng yên; màn rộng giữ chuyển cảnh mờ ra/mờ vào. Đo ở 390px, đã cuộn 380px: không có chuyển cảnh, thanh trên đứng yên, tab bar giữ nhoè, trang có chuyển động. Trình duyệt không có View Transitions: trang mới chỉ mờ vào 0,16 s. Lần đầu tiên chủ dự án thấy bản chỉ mờ vào là "chưa có animation" (trang cũ đứng yên lúc tải code trang mới, mờ vào quá nhẹ) nên đổi sang chuyển cảnh thật | [Apple] HIG Motion (chuyển động ngắn, có mục đích); [Chủ dự án] muốn thấy rõ |
| CĐ2b | Mọi lần chuyển trang khác (Hồ sơ và bảo mật, kế hoạch này sang kế hoạch khác) | Như đổi khu: mờ vào và nhô lên 12px. Trước đó chỉ mờ 0,16 s, chủ dự án thấy "Hồ sơ và bảo mật không có animation" | [Chủ dự án]; HIG Motion |
| CĐ3 | Đi sâu vào trang con / quay lên | **Trượt vào từ bên phải** 32px (0,26 s); quay lên thì **từ bên trái** | [Apple] HIG Motion: "follows people's gestures and expectations" |
| CĐ4 | Menu tài khoản | **Bật ra từ nút** (phóng từ góc trên phải, 0,22 s, nảy nhẹ), đóng thì **thu về** nút (0,14 s); lúc đang đóng không còn là menu với trình đọc màn hình | [Apple] WWDC25 219: "the bubble simply pops open… right where you just tapped" |
| CĐ5 | Ẩn/hiện sidebar | Sidebar **trượt** từ mép trái vào/ra (0,24 / 0,2 s), vùng nội dung giãn/co theo | [Apple] HIG Motion |
| CĐ7 | Đăng nhập → app, đăng xuất → màn đăng nhập (chủ dự án yêu cầu) | `features/auth/sceneTransition.ts` (`changeScene`): màn cũ **mờ ra** và lùi nhẹ về 99% (0,26 s; nút Đăng nhập vẫn xoay), đổi phiên, màn mới **mờ vào** từ 6px dưới, 99% (0,5 s) khi đã vẽ xong; cả hai cùng đường cong vào chậm ra chậm `cubic-bezier(0.4, 0, 0.2, 1)` để hai nửa liền nhau; chỉ đổi độ mờ trên `#root`, chuyển động (lùi/nhô) đặt trên vùng nội dung (`.pw-shell-main`, `.pw-auth`) — `transform` trên `#root` biến nó thành khung chứa của mọi thứ `fixed` bên trong, tab bar bị đẩy ra ngoài màn ~0,5 s sau khi đăng nhập trên điện thoại (chủ dự án báo; đo lại tab bar đứng yên ở mọi khung) (chủ dự án muốn mượt hơn; đo độ mờ từng khung: tăng/giảm đều, không nhảy); nếu màn mới chưa xong sau 120 ms thì giữa màn hiện **vòng xoay nhỏ kiểu Apple** (8 nan, xoay theo bước, 24px, không chữ, có `role="status"` cho trình đọc màn hình). Bản trước dùng View Transition: trình duyệt **đóng băng** màn hình 110–210 ms trong lúc dựng trang (chủ dự án thấy giật, và một vòng xoay cũng sẽ đứng yên trong lúc đó); bản này không đóng băng — khoảng hở dài nhất giữa hai khung hình đo được 50 ms. Đăng xuất phía máy trước, `POST /auth/log-out` chạy nền (đăng nhập lại chờ nó xong); tải sẵn trang Kế hoạch lúc rảnh. Giảm chuyển động: không mờ, chỉ vòng xoay khi cần | [Apple] HIG Progress indicators (activity indicator "small and unobtrusive", "Avoid labeling a spinning progress indicator", "Keep progress indicators moving"), HIG Loading ("Show something as soon as possible"); [Web] số |
| CĐ6 | Giảm chuyển động | Không trượt, không phóng, không nảy; trang chỉ mờ vào | [Apple] WWDC25 219: Reduce Motion "disables any elastic properties"; HIG Accessibility |

Không làm: biến hình giữa hai trang (hàng danh sách phình thành trang chi tiết) — View Transitions, để G2/G5; khúc xạ kính khi biến hình — chỉ Chromium (§7.3 `LIQUID_GLASS.md`).

### 6.3 Trang không tìm thấy / không có quyền (chủ dự án yêu cầu trong G1, 2026-11-17)

Trước: trang đứng riêng có logo + chữ PathWise, `EmptyState` chung, nút "Về trang chính" luôn tới `/plans` (ADMIN bị đưa sang khu cửa hàng rồi bị đẩy về, người chưa đăng nhập bị đưa tới đăng nhập), người đã đăng nhập mất sidebar/tab bar.

| Ai mở | Hiển thị | Lối ra |
|---|---|---|
| OWNER, DISPATCHER | Trong khung app (`DispatcherShell`: sidebar/tab bar, menu tài khoản) | "Về trang chính" → `/plans`; "Quay lại" nếu có trang trước trong app |
| ADMIN | Trong khung quản trị (`AdminShell`, tải lười) | "Về trang chính" → `/admin`; "Quay lại" |
| Chưa đăng nhập | Thẻ trên ảnh màn tài khoản (`AuthStage` + `AuthLayout`, chữ giới thiệu như Đăng nhập) | "Đăng nhập" → `/login`; "Quay lại" |
| DRIVER (không dùng web) | Như chưa đăng nhập | "Về trang chính" → `/driver` |

**403** dùng cùng thiết kế, biểu tượng ổ khoá. Hiện **tại địa chỉ đã mở** khi sai vai trò (P-75: khu quản trị chỉ ADMIN, trang cửa hàng chỉ OWNER/DISPATCHER, `/store/audit` chỉ OWNER, `/driver` chỉ DRIVER), và ở `/forbidden`. Câu mô tả **chung**, không nói trang dành cho ai (chủ dự án): đã đăng nhập "Về trang chính hoặc quay lại trang trước để tiếp tục.", chưa đăng nhập "Đăng nhập bằng tài khoản có quyền để xem trang này.". Khi guard nằm trong khung app (trang chỉ OWNER), trang 403 chỉ vẽ nội dung, không thêm khung thứ hai (`useInAppShell`).

Bố cục theo **`ContentUnavailableView` của Apple** ("display when the content of your app is unavailable to users"; ví dụ của Apple: `Label("No Mail", systemImage: "tray.fill")` + `description`): **một biểu tượng đơn sắc lớn** (56px, màu chữ phụ; ghim gạch chéo cho không tìm thấy, ổ khoá cho không có quyền — vẽ theo kiểu SF Symbols), **tiêu đề** đậm (22px, tự chia dòng cân đối), **mô tả** màu chữ phụ, rồi **nút** hình viên thuốc 44px ("Về trang chính"/"Đăng nhập", và "Quay lại" khi lịch sử có trang trước của app) — HIG Writing: "Provide clear next steps on any blank screens… give them a button or link"; HIG Buttons: nút chính bắt đầu bằng động từ. Canh giữa ở cả hai dạng; trong khung app khối nằm **giữa vùng an toàn** — [Apple] HIG Layout: "A safe area defines the area within a window that isn't covered… by a toolbar, tab bar", "Respecting the safe area is essential"; `ContentUnavailableView` canh giữa trong vùng này — tức bên phải sidebar, dưới thanh trên, trên tab bar ở điện thoại (đo lệch 0 px dọc ở 1440, 2 px ở 390; ngang 0 px khi trừ chỗ giữ cho thanh cuộn). Có một lần tao đổi sang canh theo cả cửa sổ (tính cả thanh trên) vì chủ dự án thấy hơi thấp; như vậy lệch Apple nên đã trả về. Cảm giác "hơi lệch" của chủ dự án ghi ở F-70: theo Apple giữ nguyên, chủ dự án sẽ hỏi thêm người khác như `ContentUnavailableView` — bản trước đặt cách mép trên 12% chiều cao màn (tự chọn), chủ dự án thấy chưa ở giữa. Không logo (P-74). Bản đầu dùng hình bản đồ nhiều màu, dòng "Địa chỉ: /…" và nút vuông — chủ dự án chỉ ra là không theo Apple, đã bỏ ("Check each word to be sure it needs to be there", HIG Writing). Lớp vẽ hình `pw-art-*` chuyển từ `plans.css` sang `ui.css` (dùng chung). Test `app/pages.test.tsx`.

### 6.1 Kết quả `APPLE_CHECKLIST` cho G1 (đo 2026-11-17)

| Mục | Kết quả | Đo / ghi chú |
|---|---|---|
| C1 vùng bấm | ✅ | Mục tab bar 61×50 (360px); nút tài khoản, nút ẩn/hiện sidebar 44×44 khi chạm, 36×36 với chuột (≥ 28 của macOS); mục sidebar và menu cao 36 / 44 khi chạm |
| C3 trạng thái nhấn | ✅ (đã chỉnh) | Một thang trạng thái cho mọi control của khung (màu nhấn): nghỉ → rê chuột +8 (chỉ khi có chuột) → đang mở menu +10 → nhấn +16, đổi màu mượt 0,15 s. Nút tài khoản trước đây nhấn đậm hơn các control khác, đổi màu tức thì (nháy trên điện thoại) và không có trạng thái "đang mở menu" (chủ dự án thấy không nhất quán); thu 94% khi nhấn chỉ với chuột |
| C4 hover chỉ khi có chuột | ✅ (đã sửa) | Mục menu tài khoản trước đây hover không nằm trong `@media (hover: hover)` |
| C5 không ô vuông khi chạm | ✅ | `-webkit-tap-highlight-color: transparent` trên mọi control của khung |
| C6 focus nhìn thấy | ✅ | Viền `route` 2px của link/nút; mục menu viền trong |
| C7 tương phản | ✅ | Nhãn tab chưa chọn ≥ 5,0:1 (sáng), ≥ 5,9:1 (tối); nhãn đang chọn 6,4:1 / 7,6:1 |
| C8 nhãn cho trình đọc màn hình | ✅ | "Điều hướng chính", "Tài khoản: {tên}", "Ẩn/Hiện thanh bên", `aria-current` cho khu đang mở; chỉ một vùng điều hướng |
| C11 giảm chuyển động | ✅ (đã sửa) | Bỏ thu nhỏ khi nhấn nút tài khoản; mọi chuyển động của khung chỉ còn mờ vào (CĐ6) |
| C12 tăng tương phản, giảm trong suốt, forced colors | ✅ | Tăng tương phản: viền 2px `ink` (đúng fb_6: "highlights them with a contrasting border", WWDC25 219); giảm trong suốt: nền đặc; forced colors: viền và mục đang mở theo màu hệ thống |
| C13 nhất quán | ✅ | Menu tài khoản giống bảng tài khoản app tài xế; cùng khung cho khu quản trị |
| C14 không chữ đè | ✅ (đã sửa) | Lỗi trùng class `.pw-tab` (fb_5); nhãn vừa ở 320/360/390px (320px chỉ vừa khít trên điện thoại, cắt khi cửa sổ có thanh cuộn: F-74, sửa ở G2 bằng tab rộng theo nhãn) |
| C15 sáng/tối theo máy | ✅ | P-73 |
| Cỡ chữ theo trình duyệt (Layout "Be prepared for text-size changes") | ✅ một phần (đã sửa) | Chữ của khung đổi sang `rem` (lớn theo cỡ chữ trình duyệt); riêng nhãn tab bar cố định 11px (cỡ tối thiểu của Apple) vì 5 nhãn chỉ có ~60px mỗi nhãn; kiểm 125% không cắt. Phần còn lại của app vẫn px (H8, làm theo từng giai đoạn) |
| H1–H2 hình đồng tâm | ✅ | Sidebar 16 − padding 8 = mục 8; tab bar viên thuốc 64, padding 6 → mục (64 − 2 − 12)/2 = 25 (viên thuốc); menu 16 − 8 = mục 8 |
| H5 kính ở lớp chức năng | ✅ / lệch có chủ đích | Sidebar, tab bar, menu là kính (nhoè 20px); thanh trên gần đặc (94%) vì phần tử nhoè thành nền của phần tử con, làm menu bên trong mất nhoè |
| H8 bóng chỉ cho lớp nổi | ✅ | Bóng `shadow-raised` cho sidebar, tab bar, menu |
| Tab bars: nhãn một từ | ⚠️ | "Kế hoạch", "Báo cáo", "Thành viên", "Cửa hàng" là hai từ; không có từ đơn rõ nghĩa hơn trong tiếng Việt |
| Toolbars: tiêu đề < 15 ký tự | ✅ trang Kế hoạch (G2, §7) / ⏳ G3 | "Kế hoạch" 8 ký tự; các khu khác làm ở G3 |

## 7. Thiết kế G2: danh sách kế hoạch (`/plans`, 2026-11-17)

Làm theo D5, D7, D8, D9, D13 ở §3. Dữ liệu mẫu để đo: 24 kế hoạch trên DB dev (21 nháp, 3 đã huỷ).

| # | Làm gì | Nguồn |
|---|---|---|
| G2.1 | Tiêu đề ngắn **"Kế hoạch"** (h1; caption của bảng vẫn là "Kế hoạch giao hàng" cho trình đọc màn hình). Hành động chính ở cuối hàng tiêu đề: rộng là nút viên thuốc "+ Tạo kế hoạch"; hẹp là nút tròn "+" 44px có nhãn trợ năng "Tạo kế hoạch" | [Apple] Toolbars: Titles ("< 15 characters"), Actions |
| G2.2 | Bộ lọc **không còn thẻ**: một hàng ngay dưới tiêu đề gồm hai nút viên thuốc **nền xám, không viền** (HIG Buttons chỉ có kiểu nền màu nhấn, nền xám, không nền, kính; không có kiểu viền rỗng; hàng ngang dùng viên thuốc). Đang lọc thì nền màu nhấn nhạt. **"Xoá bộ lọc" chỉ hiện khi đang lọc** | [Apple] Buttons (Style, shapes), Layout progressive disclosure |
| G2.3 | **Rộng (≥ 960px): bảng** trong nhóm nền `surface-raised` bo 16, không viền ngoài; tiêu đề cột viết như thường (bỏ chữ HOA cho mọi `DataTable`); tên kế hoạch chữ đậm màu chữ thường, gạch chân khi rê; cả hàng bấm được (hover/nhấn có từ trước); nút sao chép thành biểu tượng (nhãn trợ năng "Sao chép {tên}"); ngày dạng ngắn "T3, 01/12/2026" một dòng | [Apple] Lists and tables |
| G2.4 | **Sắp xếp bằng tiêu đề cột** "Tên kế hoạch" và "Ngày giao" (hai cột API hỗ trợ: `sort=name`, `planDate`); bấm lại cột đang sắp thì đảo chiều; mũi tên chỉ hiện ở cột đang sắp (rê chuột hiện mờ ở cột khác); `aria-sort`; thứ tự nằm trên URL `?order=name-asc` (URL không còn `page`) (mặc định ngày mới nhất trước thì không ghi) | [Apple] Lists and tables ("let people click a column heading to sort") |
| G2.2a | **Ngày giao = compact date picker:** nút hiện ngày bằng màu nhấn ("Ngày giao: 17/11/2026"); bấm mở khung lịch tháng nổi (kính dày) ngay dưới nút: tiêu đề "Tháng 11, 2026" với hai mũi tên tháng, hàng thứ T2…CN, ngày dạng nút tròn; hôm nay chữ màu nhấn, ngày đang chọn hình tròn đặc màu nhấn. Chọn ngày áp dụng ngay, **lịch vẫn mở tới khi bấm ra ngoài hoặc Escape** (đúng câu HIG). "Mọi ngày" (chỉ khi đang có ngày) bỏ ngày và đóng. Bàn phím: mũi tên đi theo ngày/tuần, PageUp/PageDown đổi tháng, Home/End đầu/cuối tuần. Bỏ bộ chọn ngày của trình duyệt (Chrome vẽ kiểu riêng, không phải kiểu Apple) | [Apple] Pickers: "The compact style displays a button that shows the current value in your app's accent color… opens a modal view… calendar-style editor… before tapping outside the view to confirm" |
| G2.2b | **Trạng thái = pop-up button:** nút "Trạng thái: Tất cả" có dấu ⌃⌄ (như `chevron.up.chevron.down`); bấm mở menu kính, mục đang chọn có dấu tích bên trái; chọn xong menu đóng và nút hiện lựa chọn mới. Bàn phím: mũi tên, Home/End, Enter, Escape. Bỏ `select` gốc (menu của trình duyệt không phải kiểu Apple) | [Apple] Pop-up buttons ("a flat list of mutually exclusive options… the button can update its content to indicate the current selection", "introductory label"); Menus ("use a checkmark") |
| G2.9 | **Phân trang có điểm kết thúc** (chủ dự án chọn, 2026-11-17: sản phẩm cần điểm kết thúc rõ, không tải vô cực). Trang trên URL `?page=` (tải lại, gửi link, Back về đúng trang); đổi lọc/thứ tự thì về trang 1; trang quá cuối (link cũ) thì về trang cuối. **Rộng (≥ 960px): mỗi trang vừa một màn hình, không cuộn** — số hàng = chỗ còn lại dưới đầu bảng ÷ chiều cao một hàng (đo bằng hàng mẫu ẩn có tên 2 dòng, nên tự giảm khi chữ to), tối thiểu 3 (cửa sổ quá thấp thì cuộn; bản trước 5 làm cửa sổ 1280×640 phải cuộn 72px), tối đa 100 (API_SPEC §1); đo lại khi đổi cỡ cửa sổ/chữ, giữ kế hoạch đầu trang đang xem. Đo (sau khi sửa hàng mẫu cao hơn hàng thật 4,5px): 1280×640 → 5 hàng, 1366×768 → 7, 1440×900 → 10, 1920×1080 → 13; chỗ trống ở đáy luôn < 1 hàng. Không cuộn trên máy tính là chủ ý (chủ dự án, 2026-11-17: trang chỉ có một bảng): phím cuộn/con lăn không đưa sang trang sau, phải bấm thanh trang — lệch với Scroll views "Support default scrolling gestures and keyboard shortcuts" ở mức chấp nhận, vì trang không có gì để cuộn. **Hẹp: 20 kế hoạch một trang, cuộn trong trang** (chủ dự án: không cuộn thì điện thoại quá ít hàng); bấm sang trang thì cuộn về đầu danh sách, chỉ khi đầu danh sách đã khuất. Thanh trang (chủ dự án chốt, 2026-11-17, sau khi thử thanh kính fb_9 và ô nhập ngay trên thanh): **"‹ Trang 4/12 ›"** — hai nút tròn nền xám lùi/tiến một trang; bấm "Trang 4/12" bật **khung nhỏ phía trên** (căn giữa nút) gồm **« [ô số trang] »**: về trang đầu, ô điền sẵn trang hiện tại (focus và bôi đen sẵn, chỉ nhận chữ số, Enter/"Đi" để chuyển và đóng khung, số ngoài khoảng thì trả về trang cũ), tới trang cuối; Escape hoặc bấm ra ngoài thì đóng. Khung là **viên thuốc**, bo đồng tâm với các viên thuốc bên trong: 18 (nửa 36px) + đệm 8 + viền 1 = 27 = nửa chiều cao 54px ([Apple] Toolbars: "ensure that its corner radius is also concentric"; bản trước bo 16px, lệch), ô số cao bằng nút. Đóng khung thì focus **luôn** trở về nút "Trang x/y" (mẫu Dialog của WAI-ARIA; WCAG 2.4.3 Focus Order), nên sau khi gõ số + Enter có vòng focus quanh nút — chủ dự án chấp nhận để theo WCAG (2026-11-17), sau khi thử bỏ trả focus khi mở bằng chuột/chạm (rủi ro người dùng trình đọc màn hình mất chỗ). Bỏ menu liệt kê trang (nhiều trang thì không hợp). Không ghi tổng số kế hoạch. Thanh trang nằm ngoài khối kết quả (khối này dựng lại mỗi trang) nên focus không rơi khi chuyển trang. Máy tính: khối danh sách giữ chiều cao một trang đầy, nên **thanh trang không nhảy lên ở trang cuối ít hàng** (chủ dự án yêu cầu; [Apple] Page controls: "Center a page control at the bottom of the view or window. To ensure people always know where to find a page control, center it horizontally and position it near the bottom of the view"); đo 1440×900: thanh trang ở y = 827 cả trang 1 (10 hàng) và trang cuối (6 hàng). Điện thoại: thanh trang theo cuối danh sách (danh sách cuộn). Một trang thì không có thanh trang. Trang mới trượt vào từ phía nó đến (sau: từ phải, trước: từ trái), giảm chuyển động thì chỉ mờ dần. **Lưu ý:** số hàng mỗi trang trên máy tính theo cửa sổ, nên link `?page=2` mở ở cửa sổ khác cỡ có thể lệch vài kế hoạch. *Đính chính:* bản trước của mục này (tải thêm khi cuộn) ghi "HIG không có nút Trang trước/Trang sau" và "như Mail, Files" — hai ý đó là suy luận của agent, không có trong HIG | [Apple] Lists and tables: "When possible, limit the number of rows. Short lists are easier for people to scan… providing a way for people to view more" (không nói cách nào); Scroll views: page-by-page, cỡ trang "typically the current height… of the view", "scroll automatically… only as much as necessary"; Toolbars ("grouped into logical sections", "Prefer using standard components… standard buttons, text fields… have corner radii that are concentric with bar corners", "Reduce the use of toolbar backgrounds and tinted controls"); Steppers ("pairing a stepper with a text field when large value changes are likely"); Text fields ("number formatter", "Validate fields", "match the size of a text field to the quantity of anticipated text"); Segmented controls ("Keep control types consistent"); Page controls: "Center a page control at the bottom of the view" (chỉ mượn vị trí; chấm tròn "Not supported in macOS", > ~10 chấm khó đếm); Layout: hàng cao thêm khi chữ to; Buttons: nút chỉ biểu tượng hình tròn. HIG **không** có câu nào cho phép/cấm phân trang có nút hay yêu cầu tải vô cực; câu "một trang vừa một màn hình" chỉ có cho watchOS nên không lấy làm căn cứ |
| G2.10 | **Chuyển động khi lọc** (chủ dự án yêu cầu): đổi bộ lọc/thứ tự thì kết quả cũ vẫn ở đó, mờ xuống 50% sau 0,15 s nếu chưa có dữ liệu mới (trả lời nhanh thì không nháy), `aria-busy`; dữ liệu mới tới thì kết quả mới (các hàng hoặc trạng thái "Không có kế hoạch nào…") hiện mờ dần và trồi lên 10px trong 0,32 s, cùng kiểu trang ở §6.2. Kết quả đổi (đang tải → hàng → trống → hàng) mới chạy, tải thêm khi cuộn thì không. "Xoá bộ lọc" bật ra / thu vào cạnh hai nút lọc như menu (0,22 / 0,14 s); lúc thu vào nó được ghim đúng chỗ cũ (hàng lọc xếp lại không kéo nó theo) và chỉ `inert`, không `disabled` (bản trước mờ 50% một nhịp rồi mới thu → chớp, và nhảy sang chỗ mới rồi mới mất — chủ dự án thấy, đo: độ mờ 1 → 0,5, x 187 → 16); đổi lọc mà nó đổi chỗ (ví dụ từ hàng 3 lên hàng 2) thì trượt tới chỗ mới 0,28 s thay vì nhảy. Giảm chuyển động: chỉ mờ dần, không trồi, nút không bật | [Apple] Motion ("brief and precise", có mục đích: cho thấy kết quả đã đổi); WWDC25 219; Accessibility (giảm chuyển động) |
| G2.11 | **Chữ dài** (chủ dự án yêu cầu; tên kế hoạch tối đa 150 ký tự): tên **tối đa 2 dòng rồi "…"**, cắt cả chuỗi không có dấu cách (`overflow-wrap: anywhere`); tên đầy đủ vẫn nằm trong link cho trình đọc màn hình, hiện khi rê chuột (`title`) và ở trang kế hoạch. Bảng: cột tên rộng ≥ 14rem (40%), các cột ngày/số một dòng; bảng hẹp thì cột ít quan trọng (cuối dòng) **chuyển thành dòng phụ dưới tên** thay vì ép cột tên, cuộn ngang hay mất số liệu: hẹp hơn 66rem cột "Thời gian", "Tối ưu lúc" thành "5 giờ 06 phút · tối ưu 03/11 16:20"; hẹp hơn 52rem thêm "3 tuyến · 15,4 km" (đo: đủ 8 cột cần ~1060px, bậc 1 ~830px). Chỉ hiện phần có giá trị (kế hoạch nháp không có dòng phụ). Hàng mẫu đo số hàng/trang cũng có dòng phụ và rộng đúng bằng bảng. *Đính chính:* bản trước trích Typography "Reduce the number of columns" để ẩn cột — câu đó nói về chữ chia nhiều cột khi cỡ chữ to, không về bảng hẹp; HIG không có câu nào bảo tự ẩn cột của bảng. Còn lệch: chưa cho kéo đổi độ rộng cột (Lists and tables macOS "Let people resize columns"), để G3 làm chung các bảng. Điện thoại: tên theo cỡ chữ trình duyệt (1rem), dòng phụ xuống dòng thay vì cắt; chữ to hoặc màn hẹp (danh sách < 21rem, ví dụ màn 360px trở xuống) thì **nhãn trạng thái xuống dưới chữ**. Đo 1200/390/320px và cỡ chữ 150%: không tràn ngang | [Apple] Lists and tables ("When a table is narrow… you want content to remain recognizable and easy to read", "Choose a row style that fits the information you need to display"); Layout ("Order content by relative importance"); Typography ("Avoid truncating text in scrollable regions unless people can open a separate view to read the rest", "use as many lines as needed", "consider using a stacked layout where text appears above secondary items"); Layout ("table rows… may need to grow in height"); Lists and tables ("Keep item text succinct") |
| G2.12 | **Vòng xoay thay chữ "Đang tải…"** (chủ dự án thấy chữ khi mạng chậm): component dùng chung `ActivityIndicator` (8 nan xoay, giống vòng xoay lúc đăng nhập), chỉ hiện sau 0,3 s để tải nhanh không nháy, không có chữ hiện ra, trình đọc màn hình đọc "Đang tải…". Thay ở: chờ khôi phục phiên (`RequireAuth`), trang không tìm thấy/không có quyền lúc chờ phiên, tạo/sửa và chi tiết kế hoạch, cửa hàng, quản trị cửa hàng, hộp thoại sao chép. Còn ô bảng Xe, Thành viên ghi "Đang tải…": G3 đổi sang hàng giữ chỗ | [Apple] Progress indicators (indeterminate → activity indicator; "Avoid vague terms like loading"); Loading |
| G2.13 | **Menu ngữ cảnh của hàng** (chủ dự án chọn, 2026-11-17): điện thoại **chạm giữ** hàng 0,5 s (di ngón > 10px thì huỷ, để còn cuộn) → hàng nổi lên, menu "Sao chép…" dưới hàng (sát đáy thì phía trên); chạm giữ không mở trang; chặn menu link của trình duyệt (`-webkit-touch-callout: none`, `contextmenu`). Máy tính: **nhấp chuột phải** hàng của bảng → cùng menu, mở **tại con trỏ** (sát đáy thì lật lên). Menu đóng khi chọn, chạm ra ngoài, Escape, cuộn; focus vào mục đầu rồi trả về chỗ cũ. Cửa hàng khoá thì không có menu (không còn mục nào). **Trang chi tiết có nút "Sao chép" ở mọi trạng thái** (trước chỉ có khi đã giao việc/đang giao/xong/huỷ; nay thêm cho nháp — màn tìm toạ độ và màn kết quả tối ưu). **G3 phải thêm menu ngữ cảnh cho các danh sách khác** (Xe, Thành viên) cho nhất quán | [Apple] Context menus ("Always make context menu items available in the main interface, too", "Support context menus consistently throughout your app", "Aim for a small number of menu items", "Hide unavailable menu items", gần chỗ ngón tay/con trỏ); Menus (dấu "…" khi cần nhập thêm) |
| G2.5 | **Hẹp: danh sách hàng** trong nhóm bo 16: tên (đậm, một dòng, cắt "…"), dòng phụ "T3, 01/12/2026 · 16 điểm giao", trạng thái, chevron; đường kẻ giữa các hàng thụt 16px **ở cả hai đầu** (chủ dự án thấy lệch ở fb_8: trước đó thụt trái nhưng chạy sát mép phải; [Apple] UIKit `separatorInset`: "cell separators don't extend all the way to the edge of the table view"), hàng giữ chỗ khi tải cùng kiểu đường kẻ; cả hàng là một link. Không có nút sao chép ở hàng (chật): **chạm giữ hàng** mở menu ngữ cảnh "Sao chép…" (G2.13) | [Apple] Lists and tables; Layout grouping |
| G2.6 | **Đang tải:** 6 hàng giữ chỗ (thanh xám, nhấp nháy nhẹ; giảm chuyển động thì đứng yên), `role="status"` "Đang tải danh sách kế hoạch" | [Apple] Loading ("Show something as soon as possible") |
| G2.7 | **Trống:** kiểu `ContentUnavailableView` không thẻ: biểu tượng tuyến, tiêu đề 22px, mô tả, nút "Tạo kế hoạch" 44px; bỏ hình minh hoạ bản đồ cũ | [Apple] `ContentUnavailableView` |
| G2.8 | Bộ chia rộng/hẹp dùng chung hook `useMediaQuery(WIDE_QUERY)` (960px, cùng mốc với khung app) | — |

Component dùng chung mới (cho G3): `DatePickerFilter`, `PopUpFilter`, `PlaceholderRows` (`shared/ui/Filters.tsx`; khung nổi `pw-popover`, menu `pw-menu`, lịch `pw-calendar`); hook `useExit` chuyển sang `shared/hooks`; `DataTable` có `sortable`/`sort`/`onSort`; class `pw-list-head`, `pw-list-filters`, `pw-list-group`, `pw-list-rows`/`pw-list-row`, `pw-list-empty`.

Không làm trong G2: tiêu đề lớn thu nhỏ khi cuộn (D5 hẹp; cần thanh trên trong suốt có tiêu đề nhỏ — làm chung cho mọi khu ở G3); vuốt hàng để sao chép; biến hình hàng → trang chi tiết (G5).

### 7.1 Kết quả `APPLE_CHECKLIST` cho G2 (đo 2026-11-17, Chromium 1440 và 390px, sáng và tối)

| Mục | Kết quả | Đo / ghi chú |
|---|---|---|
| C1 vùng bấm | ✅ | Viên thuốc lọc 36px với chuột, 44px khi chạm; mục menu 36 / 44; ô ngày trong lịch 40 / 44 (lịch rộng 320 / 344, hẹp hơn thì ô co theo màn); nút "+" 44×44; hàng danh sách cao 60px; nút sao chép 36 / 44 |
| C3 trạng thái nhấn | ✅ (đã sửa) | Viên thuốc, tiêu đề cột, hàng: rê (chỉ khi có chuột) và nhấn có màu riêng. Chủ dự án thấy vùng rê/nhấn của "Xoá bộ lọc" và nút sao chép vẫn vuông, không nhất quán: cả app đổi nút không nền (`pw-btn-quiet`) và nút tiêu đề cột thành viên thuốc, nút chỉ có biểu tượng (`pw-icon-button`) thành hình tròn có trạng thái nhấn ([Apple] Buttons: "prefer circular or capsule-shape buttons", "an icon-only button uses a circle shape") |
| C4 hover chỉ khi có chuột | ✅ | Mọi hover mới nằm trong `@media (hover: hover)` |
| C6 focus nhìn thấy | ✅ | Viên thuốc viền `route` 2px khi focus bên trong (`:focus-within`) |
| C8 nhãn trợ năng | ✅ | "Ngày giao: Tất cả" (nút mở `dialog` lịch, ô ngày đọc "Thứ Ba, 17 tháng 11, 2026", `aria-current=date` cho hôm nay), "Trạng thái: Tất cả" (nút mở `menu`, mục `menuitemradio` + `aria-checked`), "Tạo kế hoạch" (nút +), `aria-sort`, nhóm "Lọc kế hoạch" |
| C11 giảm chuyển động | ✅ | Hàng giữ chỗ đứng yên; menu và lịch hiện không bật nảy |
| C12 giảm trong suốt, tăng tương phản | ✅ | Menu và lịch nền đặc; tăng tương phản viền 2px `ink` |
| Nút viền rỗng | ⚠️ F-73 | Trang này không còn nút viền; kiểu `secondary` của cả app đổi ở G3 |
| C14 không chữ đè | ✅ (đã sửa) | Tên 150 ký tự: 2 dòng rồi "…" ở mọi khổ (G2.11); hàng lọc xuống dòng khi hẹp. Thấy thêm và đã sửa: tab bar ở 320px cắt nhãn "Thành vi…" (F-74, khung G1) — tab rộng theo nhãn, tối thiểu 44px |
| C15 sáng/tối | ✅ | Chụp cả hai |
