# OWNER_UI_REDESIGN — Nghiên cứu thiết kế lại giao diện chính của chủ cửa hàng theo Apple

> **Trạng thái:** nghiên cứu xong; chủ dự án đã trả lời Q1–Q5 (§5, 2026-11-17), quyết định ở P-74. **G1 (khung app) đã làm** (§6, branch `docs/owner-ui-redesign`). **G2 (danh sách kế hoạch) đã làm** (§7, PR #129). **G3 (Xe, Thành viên, Cửa hàng, Nhật ký, Tài khoản) đã làm** (§8, PR #130). **G4 (tạo/sửa kế hoạch, nhập file, sao chép) đã làm** (§9, PR #131). **G5 (chi tiết kế hoạch) đã làm** (§10, PR #132). **G6 (Báo cáo) đang làm** (§11, branch `feat/owner-ui-g6`).
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
   - *Đã chốt cho G5 (chủ dự án, 2026-11-18):* các ô trắng của trang (khối "Còn N điểm…", "Chưa có tài xế…", thanh tiến độ, nhóm bảng, khung sửa điểm, thẻ ở màn kết quả / theo dõi) đổi sang nhóm **bo 16, không viền** như các trang đã redesign; bản đồ trong nhóm bo đồng tâm (S1/S2). Khối **lỗi** (vd "Còn N điểm giao chưa có toạ độ") bỏ viền đỏ, báo bằng **nền đỏ nhạt**. Hiện vẫn kiểu cũ (bo 8, bảng bo 4, có viền) — chủ dự án chọn để G5 cả, không đổi lẻ (§9.9).
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
| C3 trạng thái nhấn | ✅ (đã sửa) | Viên thuốc, tiêu đề cột, hàng: rê (chỉ khi có chuột) và nhấn có màu riêng. Chủ dự án thấy vùng rê/nhấn của "Xoá bộ lọc" và nút sao chép vẫn vuông, không nhất quán: cả app đổi nút không nền (`pw-btn-quiet`) và nút tiêu đề cột thành viên thuốc, nút chỉ có biểu tượng (`pw-icon-button`) thành hình tròn có trạng thái nhấn (*Đính chính 2026-11-18:* hai câu Buttons trích ở đây — "prefer circular or capsule-shape buttons", "an icon-only button uses a circle shape" — nằm trong mục visionOS; quy tắc đúng là S3 (WWDC25 356), xem §8.6) |
| C4 hover chỉ khi có chuột | ✅ | Mọi hover mới nằm trong `@media (hover: hover)` |
| C6 focus nhìn thấy | ✅ | Viên thuốc viền `route` 2px khi focus bên trong (`:focus-within`) |
| C8 nhãn trợ năng | ✅ | "Ngày giao: Tất cả" (nút mở `dialog` lịch, ô ngày đọc "Thứ Ba, 17 tháng 11, 2026", `aria-current=date` cho hôm nay), "Trạng thái: Tất cả" (nút mở `menu`, mục `menuitemradio` + `aria-checked`), "Tạo kế hoạch" (nút +), `aria-sort`, nhóm "Lọc kế hoạch" |
| C11 giảm chuyển động | ✅ | Hàng giữ chỗ đứng yên; menu và lịch hiện không bật nảy |
| C12 giảm trong suốt, tăng tương phản | ✅ | Menu và lịch nền đặc; tăng tương phản viền 2px `ink` |
| Nút viền rỗng | ⚠️ F-73 | Trang này không còn nút viền; kiểu `secondary` của cả app đổi ở G3 |
| C14 không chữ đè | ✅ (đã sửa) | Tên 150 ký tự: 2 dòng rồi "…" ở mọi khổ (G2.11); hàng lọc xuống dòng khi hẹp. Thấy thêm và đã sửa: tab bar ở 320px cắt nhãn "Thành vi…" (F-74, khung G1) — tab rộng theo nhãn, tối thiểu 44px |
| C15 sáng/tối | ✅ | Chụp cả hai |

## 8. Thiết kế G3: Xe, Thành viên, Cửa hàng, Nhật ký, Tài khoản (2026-11-17/18, đã làm)

### 8.1 Hiện trạng (chụp 2026-11-17, 1440 và 390px, tài khoản owner)

- **Xe** (`/vehicles`): tiêu đề + câu mô tả; ô đánh dấu "Hiện cả xe đã tắt"; bảng có viền; form "Thêm xe"/"Sửa xe" là thẻ bên phải (điện thoại: dưới bảng); bảng trống chỉ có câu chữ trong ô; nút "Sửa" chữ ở mỗi dòng.
- **Thành viên** (`/members`): hai tab gạch chân "Thành viên (4) / Lời mời (6)"; mỗi dòng có `select` vai trò, ô đánh dấu "Đi giao", "Vô hiệu hoá" chữ; tab lời mời: form "Mời thành viên" là thẻ bên trái; điện thoại: bảng bị cắt (chỉ còn 2 cột thấy được).
- **Cửa hàng** (`/store`): tab "Thông tin và kho / Nhật ký"; hai thẻ "Thông tin" và "Kho xuất phát" (bản đồ); "Lưu thay đổi" ở hàng tiêu đề.
- **Nhật ký** (`/store/audit`): bộ lọc `select` + hai ô ngày của trình duyệt, cao bằng ô nhập; bảng; phân trang "Mới hơn / Cũ hơn" viền rỗng (F-73).
- **Hồ sơ** (`/profile`): tiêu đề "Hồ sơ và bảo mật" (16 ký tự); thẻ thông tin, thẻ đổi mật khẩu; danh sách thiết bị dài hết cỡ (DB dev: hơn 70 phiên), mỗi dòng "Đăng xuất" chữ; nút "Đăng xuất" viền rỗng ở hàng tiêu đề.
- **Chung:** 26 chỗ `Button variant="secondary"` viền rỗng (F-73); `Dialog` một kiểu cho mọi hộp thoại (16 chỗ: xác nhận vô hiệu hoá, huỷ kế hoạch, giao việc, sao chép, nhập file, bản đồ chọn ghim…); ô bảng "Đang tải…".

### 8.2 Thành phần dùng chung (làm trước, các trang dùng lại)

| # | Làm gì | Nguồn |
|---|---|---|
| S1 | **Sheet** cho việc có nhập liệu (thêm/sửa xe, mời thành viên, sửa thành viên trên điện thoại, đổi mật khẩu; sau này sao chép, nhập file ở G4): tiêu đề; **điện thoại**: trồi từ đáy, có grabber, vuốt xuống để đóng, "Huỷ" ở đầu thanh trên và nút xong (động từ: "Thêm", "Lưu", "Gửi") ở cuối; vuốt đóng khi đang có thay đổi chưa lưu thì hỏi bằng action sheet. **Máy tính**: thẻ giữa màn hình, nền phía sau tối đi, cỡ vừa nội dung. HIG macOS không nói vị trí nút trong sheet: dùng cùng thanh trên như iOS cho mọi sheet giống nhau (lựa chọn của agent). Sheet đổi gì áp dụng ngay (thành viên) chỉ có "Đóng", không nút xong. Sheet, alert, menu ngữ cảnh, toast vẽ trên `body` (portal) để phủ cả thanh trên, sidebar, tab bar | [Apple] Sheets ("Cancel button belongs on the leading edge of the top toolbar… Done button… trailing edge", "Include a grabber in a resizable sheet", "Support swiping to dismiss… If people have unsaved changes… use an action sheet", macOS "The parent window is dimmed", "Present a sheet in a reasonable default size", "Provide an alternative to the Done button") |
| S2 | **Alert** chỉ cho xác nhận việc khó đảo ngược (vô hiệu hoá thành viên → bị đăng xuất ngay; huỷ kế hoạch): tiêu đề nói rõ chuyện gì, câu phụ chỉ khi cần, nút động từ 1–2 chữ, "Huỷ" ở đầu hàng (không là nút mặc định). **Không** tô đỏ nút khi người dùng đã chủ động chọn việc đó (vô hiệu hoá, huỷ kế hoạch đều do người dùng chọn) | [Apple] Alerts ("Use alerts sparingly", "Avoid displaying alerts for common, undoable actions", "Aim for a one- or two-word title", "Always use the title 'Cancel'", "Use the destructive style… people didn't deliberately choose… when people deliberately choose a destructive action… doesn't apply the destructive style") |
| S3 | **Nút `secondary` → nền xám, không viền** cho cả app (F-73) | [Apple] Buttons (kiểu hệ thống: nổi bật màu nhấn, xám, không nền, kính; không có viền rỗng) |
| S4 | **Menu ngữ cảnh của hàng** dùng chung (rút từ G2.13 thành component): chạm giữ / nhấp chuột phải | [Apple] Context menus ("Support context menus consistently throughout your app") |
| S5 | **Kéo đổi độ rộng cột** của bảng (máy tính) | [Apple] Lists and tables macOS ("Let people resize columns") |
| S6 | **Tiêu đề lớn thu nhỏ khi cuộn** trên điện thoại (D5 hẹp, để từ G2): cuộn qua tiêu đề lớn thì tên khu hiện nhỏ ở giữa thanh trên | [Apple] Toolbars iOS large title |
| S7 | Ô bảng "Đang tải…" → `PlaceholderRows`; trang/khung → `ActivityIndicator` | [Apple] Loading, Progress indicators (G2.6, G2.12) |

### 8.3 Theo trang

| Trang | Làm gì | Nguồn |
|---|---|---|
| **Xe** | Tiêu đề "Xe" + nút thêm ở hàng tiêu đề (rộng "+ Thêm xe", hẹp "+" tròn); form bên cạnh → **sheet** "Thêm xe" / "Sửa {tên}" (bấm hàng để sửa); "Hiện cả xe đã tắt" → bộ lọc pop-up "Trạng thái: Đang dùng / Tất cả" như trang Kế hoạch (`?active=` giữ trên URL); bảng trong nhóm (rộng), danh sách hàng (hẹp: tên, biển số · sức chứa, trạng thái, chevron); menu ngữ cảnh "Sửa…", "Tắt xe"/"Dùng lại"; trống: kiểu ContentUnavailableView có nút "Thêm xe"; câu mô tả dưới tiêu đề giữ, chữ phụ | [Apple] Toolbars (hành động chính ở hàng tiêu đề); Sheets; Toggles iOS ("Outside of a list, use a button that behaves like a toggle, not a switch") — bộ lọc thay ô đánh dấu; Pop-up buttons; Lists and tables; Context menus |
| **Thành viên** | Tiêu đề "Thành viên" + "+ Mời" (chỉ OWNER) → **sheet** "Mời thành viên" (Email, Vai trò); hai phần Thành viên / Lời mời: rộng tab view, hẹp segmented control (Q6); **rộng**: bảng, vai trò là pop-up button trong hàng, "Đi giao" ô đánh dấu (macOS dùng checkbox), "Vô hiệu hoá" trong menu ngữ cảnh + nút trong hàng → **alert** không tô đỏ; **hẹp**: danh sách hàng (tên, email, vai trò · trạng thái, chevron) → bấm hàng mở **sheet** thành viên: vai trò (pop-up), "Đi giao" (**switch** vì trong hàng danh sách), "Vô hiệu hoá"/"Kích hoạt lại"; lời mời: danh sách hàng, "Thu hồi" trong menu ngữ cảnh + nút, không alert (mời lại được) | [Apple] Sheets; Toggles ("Use the switch toggle style only in a list row", macOS checkbox); Pop-up buttons; Alerts; Context menus |
| **Cửa hàng** | Hai thẻ → hai nhóm nền (D9), tiêu đề nhóm nhỏ; "Lưu thay đổi" giữ ở cuối hàng tiêu đề, kiểu nổi bật; múi giờ: pop-up button thay `select` | [Apple] Toolbars ("Use the .prominent style for key actions such as Done or Submit… trailing side"); Layout grouping; Pop-up buttons |
| **Nhật ký** | Bộ lọc → `PopUpFilter` "Hành động" + hai `DatePickerFilter` "Từ ngày" / "Đến ngày" như trang Kế hoạch; bảng nhóm / danh sách hàng; phân trang → thanh trang của G2 (`?page=`) | G2 (D7, G2.9); [Apple] Pickers, Pop-up buttons |
| **Tài khoản** (`/profile`) | Tiêu đề "Tài khoản" (Q7; mục menu tài khoản cũng đổi thành "Tài khoản", chủ dự án 2026-11-18); hai thẻ → nhóm; đổi mật khẩu 2 bước → **sheet** (bước 2 có "Quay lại"); danh sách thiết bị: thiết bị này + 9 phiên mới nhất, cuộn tới cuối thì hiện thêm 10 (Q8; API trả cả danh sách, hiện dần phía client); "Đăng xuất" từng thiết bị: menu ngữ cảnh + nút trong hàng; "Đăng xuất" ở hàng tiêu đề bỏ (đã có trong menu tài khoản, G1) | [Apple] Toolbars ("< 15 characters"); Sheets ("The Back button lets people navigate to a previous step in a multi-step flow"); Lists and tables ("limit the number of rows… providing a way for people to view more") |

### 8.4 Câu hỏi cho chủ dự án

| # | Câu hỏi | Trả lời |
|---|---|---|
| Q6 | Thành viên / Lời mời: HIG macOS bảo dùng **tab view** trong vùng nội dung chính, HIG iOS bảo dùng **segmented control** | **Rộng: tab view** (như hiện tại), **hẹp: segmented control** (theo từng nền tảng) | *Cập nhật 2026-11-18 (§8.9): tab view trên máy tính vẽ như tabbed control của macOS, không gạch chân.*
| Q7 | "Hồ sơ và bảo mật" 16 ký tự > 15 (Toolbars) | Tiêu đề trang **"Tài khoản"** |
| Q8 | Danh sách thiết bị dài (hơn 70 phiên trên DB dev) | **Hiện một phần, cuộn tới cuối thì hiện thêm** ("xem thêm" tự động); không lệch HIG: Lists and tables chỉ nói "providing a way for people to view more", không quy định cách |
| Q9 | Giao G3 thành 1 PR hay 2 PR | **1 PR** |

### 8.5 Kết quả `APPLE_CHECKLIST` cho G3 (đo 2026-11-18, Chromium 320/390/1280/1440px, sáng và tối)

| Mục | Kết quả | Đo / ghi chú |
|---|---|---|
| C1 vùng bấm | ✅ | Nút viên thuốc 36 / 44 khi chạm; ô segmented 44 khi chạm; switch 51×31 trong hàng cao ≥ 44; mục menu ngữ cảnh 44 khi chạm; nút trong alert cao 44 |
| C3 trạng thái nhấn | ✅ | Nút xám, pop-up trong hàng, ô segmented, nút alert / action sheet: rê (chỉ khi có chuột) và nhấn |
| C6 focus nhìn thấy | ✅ | Sheet focus ô nhập đầu tiên; alert focus "Huỷ"; tay nắm cột hiện vạch màu nhấn khi focus |
| C8 nhãn trợ năng | ✅ | `dialog` / `alertdialog` có tên; `switch` + `aria-checked`; segmented là `tablist`; tay nắm cột là `separator` có tên |
| C11 giảm chuyển động | ✅ | Sheet, alert, action sheet chỉ mờ dần; switch không trượt núm; tiêu đề nhỏ không trồi |
| C12 giảm trong suốt, tăng tương phản | ✅ | Giữ quy tắc của G1/G2 cho menu, popover |
| C14 không chữ đè / tràn | ✅ (đã sửa) | Không tràn ngang ở cả 6 trang × 320/390/1280px; email trong hàng lời mời chỉ xuống dòng sau "@" (trước bẻ giữa chữ), email thành viên cắt "…" |
| C15 sáng/tối | ✅ | Chụp giao diện tối 390px cả 6 trang |
| F-73 nút viền rỗng | ✅ đã sửa | Không còn kiểu viền rỗng trong app |

Để lại: hộp thoại của kế hoạch (sao chép, nhập file, giao việc, huỷ kế hoạch, bản đồ chọn ghim) vẫn dùng `Dialog` cũ — đổi sang `Sheet` / `Alert` ở G4 (form, sao chép, nhập file) và G5 (giao việc, huỷ, ghim); khu quản trị (`/admin`) chưa đổi theo G3 (bảng có viền, nút trong bảng) — làm khi tới phần quản trị.

### 8.6 Sửa sau góp ý của chủ dự án (2026-11-18)

| # | Góp ý | Sửa | Nguồn |
|---|---|---|---|
| 1 | Thành viên → Lời mời không có chuyển động | Ô nổi của segmented control trượt sang ô mới; gạch chân của tab trượt và co giãn theo tab (`useTabUnderline`); phần mới trượt vào từ phía tab của nó (như lật trang G2.9); áp cả tab Cửa hàng / Nhật ký. Giảm chuyển động: không trượt | [Apple] Segmented controls; Motion |
| 2 | "Đi giao" nhảy | Chữ "Có/Không" cạnh checkbox đổi theo trạng thái làm cột rộng 122 → 155px: bỏ chữ, checkbox trong hàng bảng không có tiêu đề (tiêu đề cột nói nó là gì). Vẫn chờ server rồi mới đổi tick (chủ dự án giữ quy tắc SCREENS) | [Apple] Toggles: "In an editable checklist, a checkbox can appear without a title" |
| 3 | Menu vai trò ở hàng cuối đẩy bảng lên | Menu nằm trong khung bảng (cuộn được) nên khung cuộn 74px: mọi menu/khung bật ra (pop-up, lịch, chọn trang) vẽ trên `body`, đặt theo nút, lật lên khi bên dưới hết chỗ, bám theo khi cuộn | [Apple] Menus, Popovers (nổi trên nội dung) |
| 4 | Checkbox có thể lệch Apple | iOS không có checkbox, switch chỉ trong hàng danh sách: "Đang dùng" trong sheet xe → switch; checkbox chỉ còn trong bảng màn rộng (macOS) | [Apple] Toggles (iOS: "Use the switch toggle style only in a list row"; macOS: checkbox) |
| 5, 6, 8 | Ô nhập, ô chọn trong sheet, Cửa hàng, Tài khoản chưa chuẩn | Ô nhập/ô chọn của app dùng đúng kiểu đã đối chiếu ở trang đăng nhập (`APPLE_CHECKLIST` §3.2): nền fill nhạt, viền 1px đạt 3:1, focus sáng viền; ô chọn có ⌃⌄; nút trên bản đồ thành kính nổi có viền mảnh và bóng nhẹ, khung bản đồ bán kính dự phòng 8 | [Apple] HIG Text fields; Maps ("thin stroke or light drop shadow"); H5; S2 |
| — | **Đính chính hình dạng** | Câu "prefer circular or capsule-shape buttons" (dùng ở G2 và đầu G3) thuộc mục **visionOS**. Quy tắc đúng (S3, WWDC25 356): hình theo **cỡ** control, xem §8.7 | [Apple] WWDC25 356 (S3) |
| 7 | "Hồ sơ và bảo mật" chưa đổi | Mục trong menu tài khoản cũng thành "Tài khoản" | Q7 |
| 9 | Báo cáo đổi chút ít | Do component chung; trang Báo cáo làm ở G6 | — |

### 8.7 Vòng góp ý thứ hai (2026-11-18)

| # | Góp ý | Sửa | Nguồn |
|---|---|---|---|
| 1 | Nút "Lưu thay đổi" nên có biểu tượng | Dấu tích: biểu tượng chuẩn của Save/Done là `checkmark` (không phải đĩa mềm). Máy tính: dấu tích + chữ; điện thoại: nút tròn chỉ có dấu tích, như "+" ở các trang khác | [Apple] HIG Icons, Standard icons (Done, Save → `checkmark`) |
| 2 | Huỷ bo góc, Lưu/Thêm viên thuốc: thiếu nhất quán | Đúng là sai: vòng trước áp S3 theo **vai trò**. WWDC25 356: *"On macOS, Mini, Small, and Medium controls will continue using rounded rectangles… Large controls will now use capsule shapes"*, tức là hình theo **cỡ**. HIG Buttons: *"Use style — not size — to visually distinguish the preferred choice"*. Toolbars: Done là `.prominent`. Chủ dự án chọn: **chuột**: mọi nút (cả nút chính) bo góc 8, nút chính chỉ khác màu; **cảm ứng**: viên thuốc 44px; control cỡ Large 44px (thẻ tài khoản, nút ở trạng thái trống) vẫn viên thuốc | [Apple] WWDC25 356; HIG Buttons; Toolbars |
| 3 | Nhiều ô chọn còn danh sách mặc định của trình duyệt | Ô chọn mở menu của app như pop-up button (`useSelectMenu`): dấu tích ở lựa chọn hiện tại, nổi trên `body`, rộng ít nhất bằng ô, bàn phím Space/Enter/mũi tên/Escape. `<select>` vẫn là control của form (nhãn, giá trị, react-hook-form). Áp cho `SelectField`, ô tài xế/xe ở form kế hoạch, ô "chuyển tới" của bảng sửa tay | [Apple] HIG Pop-up buttons; Menus ("use a checkmark") |

### 8.8 Vòng góp ý thứ ba (2026-11-18)

| # | Góp ý | Nguyên nhân → sửa | Nguồn |
|---|---|---|---|
| 1 | Tab Cửa hàng nhảy khi đổi Thông tin ↔ Nhật ký | Hai trang có tiêu đề khác nhau (đoạn giới thiệu nằm trên tab ở một trang, dưới tab ở trang kia; hàng tab lệch 34px), chạy hiệu ứng đổi trang, gạch chân mọc lại từ 0. Giờ: cùng một đầu trang (tiêu đề, tab, rồi lời giới thiệu của tab); hai trang là tab của nhau nên không chạy hiệu ứng đổi trang (`TAB_PAGES`); gạch chân/ô nổi trượt từ tab cũ | [Apple] Tab views; Segmented controls |
| 2 | Checkbox "Đi giao" vẫn là ô của trình duyệt | `ToggleStyle.checkbox` (SwiftUI) chỉ có trên macOS 10.15+, khớp HIG Toggles (checkbox là kiểu của macOS) → dùng được trên máy tính. Vẽ lại theo mô tả hình trong HIG: tắt *"small rounded square with no fill"*, bật *"blue fill and a white checkmark"* (màu nhấn thay màu xanh); viền `ink-muted` đạt 3:1 | [Apple] HIG Toggles (Checkboxes); SwiftUI `ToggleStyle.checkbox` |
| 3 | Nút ẩn thanh bên: nền hover tròn | Thêm vào nhóm bo 8 khi dùng chuột (§8.7) | WWDC25 356 |
| 4 | Bấm ô chọn thì ô "mẹ" lúc sáng lúc không | Apple không có quy tắc riêng (đã tra Pop-up buttons, Menus, Focus and selection). Trước đây mọi nút bật menu (bộ lọc, "Trang x/y", vai trò, tài khoản) đều giữ dáng đang nhấn khi menu mở, chỉ ô chọn trong form là không → ô chọn cũng giữ dáng sáng khi menu mở | Nhất quán trong app |
| 5 | Không có con trỏ bàn tay | *Đính chính (cùng ngày):* lần đầu tao áp HIG Pointing devices (mũi tên cho control), nhưng trang đó nói về app macOS native. Web của Apple làm khác: CSS thật của apple.com và developer.apple.com có `button { cursor: pointer }`, `select { cursor: pointer }`, `button:disabled { cursor: default }`, mục menu đang mở `.current { cursor: default }`; WCAG 2.2 không có tiêu chí về hình con trỏ. Nên: bàn tay cho mọi thứ bấm được (checkbox giờ cũng có); mũi tên cho control bị tắt (thay `not-allowed`) và mục đang mở (`aria-current`, tab đang chọn) | apple.com, developer.apple.com (CSS); WCAG 2.2 |
| 6 | fb_12: sheet bị bàn phím che (Android) | Viewport `interactive-widget=resizes-content`: bàn phím thu nhỏ trang nên sheet bám đáy nổi lên trên bàn phím | — |
| 6 | fb_13: ⌃⌄ lặp kín ô Vai trò khi focus (giao diện tối) | Quy tắc focus dùng `background` rút gọn, làm mất `no-repeat` của ảnh ⌃⌄ giao diện tối → chỉ đổi `background-color` | — |
| 7 | fb_11: menu múi giờ to | Dòng 44px là cỡ control mặc định của iOS (HIG Accessibility: 44×44 pt), giữ. Menu rộng hơn ô vì tên dài → chủ dự án chọn rút tên: "Giờ Việt Nam (UTC+7)", "Bangkok (UTC+7)" | [Apple] HIG Accessibility |

### 8.9 Tab trên máy tính (2026-11-18)

Chủ dự án hỏi đổi tab "Thông tin và kho / Nhật ký", "Thành viên / Lời mời" trên PC sang viên thuốc. Đọc lại HIG bản macOS:

- Segmented controls, macOS: *"Use a tab view in the main window area — instead of a segmented control — for view switching. A tab view … is similar in appearance to a box combined with a segmented control."*
- Tab views, Anatomy: *"The tabbed control appears on the top edge of the content area"*; hình minh hoạ: *"a three-tab tabbed control is centered on the top edge of the content view"*; ảnh Calendar macOS (Day/Week/Month/Year).
- Kiểu gạch chân (Q6) không có nguồn trong HIG macOS.

Quyết định (chủ dự án chọn): máy tính dùng **tabbed control dạng segmented, đặt giữa** trên nội dung, ô nổi trượt; hình theo S3 (§8.7): **chuột bo 8** (ô nổi bo 6, đồng tâm 8 − 2), cảm ứng viên thuốc. Điện thoại giữ segmented control. Bỏ `useTabUnderline` và kiểu `.pw-tabs`/`.pw-store-tabs`.

### 8.10 Rà phân trang và cắt chữ các trang G3 (2026-11-18)

Đối chiếu G2.9 (phân trang) và G2.11 (chữ dài) với các trang G3, thử bằng dữ liệu dài tối đa (tên xe 100, họ tên 100, email ~100, tên cửa hàng 150, chuỗi không dấu cách) ở 1440/1024/960/390/320px.

**Phân trang.** Theo API_SPEC §1 chỉ Nhật ký phân trang phía server (xe, thành viên, lời mời, thiết bị là "danh sách nhỏ theo bản chất"; thiết bị hiện dần theo Q8). Nhật ký trước chỉ có `?page=` và thanh trang, cố định 20 dòng, trang cuộn. Muốn mỗi trang vừa một màn hình thì dòng phải có chiều cao giới hạn, tức phải cắt chữ, mà HIG Typography: *"Avoid truncating text in scrollable regions unless people can open a separate view to read the rest"*, và dòng nhật ký chưa có màn chi tiết. Chủ dự án chọn: **mỗi dòng mở sheet "Chi tiết"** (hành động, thời điểm có năm, người làm, IP, đủ chữ), nhờ đó áp đủ G2.9 như trang Kế hoạch: máy tính số dòng vừa cửa sổ (đo bằng dòng mẫu cao nhất: email 1 dòng + vai trò, câu 2 dòng; `useFitRows` tách ra `shared/hooks` dùng chung), điện thoại 20 dòng; giữ chiều cao một trang đầy nên thanh trang không nhảy; trang trượt, lọc mới trồi lên; trang quá cuối về trang cuối; đổi cỡ cửa sổ giữ dòng đầu. Đo: 1440×900 → 8 dòng, 1920×1080 → 11, 1280×640 → 3 (tối thiểu, đầu trang Nhật ký cao hơn Kế hoạch vì có tab và lời giới thiệu); thanh trang cùng y ở trang 1 và trang cuối; không cuộn.

**Chữ dài.** Điện thoại đã đúng. Máy tính trước đây: tên dài đẩy các cột khác ra khỏi bảng (bảng cuộn ngang), biển số bẻ 3 dòng, vai trò bẻ 2 dòng; tiêu đề sheet sửa xe 5 dòng; menu tài khoản 5 dòng. Sửa:

| Chỗ | Sửa | Nguồn |
|---|---|---|
| Bảng Xe, Thành viên, Lời mời, Nhật ký | Tên/email/câu hành động `pw-cell-title`: 2 dòng rồi "…", bẻ cả chuỗi không dấu cách, `title` có đủ chữ; email dưới tên `pw-cell-sub` 1 dòng; biển số, vai trò, IP một dòng (`pw-nowrap`); cột chính tối thiểu 12rem (bảng Kế hoạch 14rem; bảng thành viên 5 cột ở 960px thì 14 vẫn tràn 18px) | G2.11 |
| Bảng hẹp (960px, thanh bên mở) | Thành viên cần 710px trong khung 663: dưới 46rem cột Trạng thái gập xuống dưới tên; Lời mời cần 672: dưới 44rem cột Hết hạn thành dòng phụ "Hết hạn …" | G2.11; [Apple] Lists and tables |
| Sheet sửa xe, sheet thành viên | Tiêu đề ngắn: "Sửa xe", "Thành viên"; tên đầy đủ nằm trong sheet (ô Tên xe; dòng tên + email đầu sheet thành viên, không cắt vì đây là màn để đọc phần bị cắt). Tiêu đề sheet nếu vẫn dài thì một dòng "…" | [Apple] Toolbars: *"keep the title under 15 characters"*; Typography |
| Menu tài khoản | Họ tên, email, tên cửa hàng mỗi dòng tối đa 2 dòng rồi "…" (đủ chữ ở trang Tài khoản, Cửa hàng) | G2.11 |

### 8.11 Chọn ngày trên điện thoại: inline (2026-11-18)

Chủ dự án: lịch kiểu bật ra (compact) trên điện thoại có thể bị che. Đo: 320×568 trang Kế hoạch đáy lịch y = 519 dưới mép trên tab bar (496); 390×844 "Đến ngày" của Nhật ký đáy 766, sát tab bar (768); 320px Nhật ký lịch lật lên che tiêu đề và tab.

HIG Date pickers (iOS): *"Compact — A button that displays editable date and time content in a modal view"*; *"Inline — … for dates and times, an inline calendar view"* (hình: lịch tháng hiện ngay dưới hàng của nó); *"A picker works well when displayed in context, below or in proximity to the field people are editing."*

Quyết định: **điện thoại dùng inline**: bấm nút ngày thì lịch thành một khối của trang, dòng riêng dưới mọi bộ lọc, rộng bằng danh sách, đẩy danh sách xuống; mở ra thì tự cuộn vừa đủ để cả lịch nằm trên tab bar (Scroll views: *"only as much as necessary"*); chọn ngày áp dụng ngay và lịch vẫn mở; bấm lại nút ngày, Escape hoặc bấm ra ngoài thì đóng; mỗi lần một lịch. Nút không còn `aria-haspopup`, lịch là `role="group"` có tên. **Máy tính giữ compact** (popover; macOS graphical trong popover). Đo lại: 390×844 và 320×568, cả ba nút ngày (Kế hoạch, Nhật ký Từ/Đến), lịch không bị che.

### 8.12 Lịch: chọn nhanh tháng/năm, nhãn bỏ chọn, hình các nút (2026-11-18)

| # | Góp ý | Tra cứu | Sửa |
|---|---|---|---|
| 1 | Không chọn nhanh được tháng/năm; lịch Apple có "Tháng 11 2026 ›" | HIG Pickers chỉ có *"Date — Displays months, days of the month, and years"*; UIDatePicker, UIDatePickerStyle (*"a label that when tapped displays a calendar-style editor"*), UICalendarView, GraphicalDatePickerStyle, NSDatePicker không mô tả nút tháng/năm bằng chữ. Nút đó chỉ có trong hình minh hoạ của Apple và cách lịch iOS chạy: bấm thì lưới ngày đổi thành bánh xe tháng và năm | Chủ dự án chọn **như iOS**, cả PC và điện thoại: đầu lịch là nút "Tháng 11, 2026 ›" (dấu › màu nhấn); bấm thì dấu xoay xuống, tiêu đề màu nhấn, lưới ngày và hai mũi tên ‹ › nhường chỗ cho **hai bánh xe** (Tháng, Năm; năm từ năm nay − 20 tới + 10) cao bằng lưới ngày, dải chọn ở giữa, hai mép mờ (HIG Pickers: *"Wheels — A set of scrolling wheels"*). Cuộn, vuốt, con lăn hoặc bấm một giá trị để quay; bàn phím: mỗi bánh xe là `spinbutton` (mũi tên, Page Up/Down, Home/End). Đổi tháng/năm giữ ngày (ngày cuối nếu tháng ngắn hơn). Bấm lại tiêu đề, hoặc Escape (Escape lần nữa mới đóng lịch), thì về lưới ngày. Component `Wheel` |
| 2 | PC "Mọi ngày", điện thoại "Bỏ chọn ngày" | Khác nhau theo trang, không theo khổ: Kế hoạch "Mọi ngày", Nhật ký "Bỏ chọn ngày" | Cả hai trang "Bỏ chọn ngày" |
| 3 | Lịch trên PC: hình nút có nhất quán? | Đo: khung bo 16 (khung chứa), "Bỏ chọn ngày" bo 8 (đúng §8.7), **‹ › tròn (sai §8.7)**, ô ngày tròn. Ô ngày tròn như lịch của Apple nhưng tài liệu không có câu chữ, S3 cũng không nói ô ngày có phải nút | ‹ › và nút tiêu đề bo 8 khi dùng chuột; **ô ngày giữ tròn** (chủ dự án chọn) |

### 8.13 Ô mã xác nhận nhất quán (2026-11-18)

Chủ dự án đã chọn **một ô thật vẽ thành 6 khung** (`CodeField`, LIQUID_GLASS §9) nhưng chỉ Quên mật khẩu dùng; Xác nhận email còn `OtpInput` (6 ô nhập riêng), Đặt lại mật khẩu bằng link và sheet Đổi mật khẩu (Tài khoản) là ô nhập thường. Giờ cả bốn dùng `CodeField`; xoá `OtpInput` và kiểu `.pw-otp`, `.pw-field-code`, `.pw-auth-otp`. Thông báo dưới ô mã luôn nằm trong vùng `aria-live` (màn Xác nhận email trước có, để trình đọc màn hình báo mã sai). Trong app, khung mã theo đúng mô tả §9: nền fill nhạt và viền như ô nhập, khung đang nhận số sáng viền như ô đang focus; bo `radius-md` khi dùng chuột, **22 trên màn cảm ứng** (như thẻ tài khoản, cùng bán kính ô nhập viên thuốc 44px).

### 8.14 Ô mã trong sheet Đổi mật khẩu theo tỉ lệ ô nhập (2026-11-18)

Chủ dự án: các khung mã trong sheet Đổi mật khẩu trông không cân, lệch tỉ lệ với ô nhập khác (chỉ sửa chỗ này; màn đăng nhập giữ nguyên). Đo: PC khung 73×56 (bè ngang) cạnh ô nhập cao 36; điện thoại 52×56 cạnh ô cao 44. Sửa (chỉ trong `.pw-sheet`): khung **vuông, cao bằng ô nhập** — 36 khi dùng chuột, 44 cảm ứng (HIG: tối thiểu 44pt); không kéo giãn vì mỗi khung chỉ chứa một chữ số (HIG Text fields: *"match the size of a text field to the quantity of anticipated text"*); chữ số cùng cỡ chữ của ô nhập; hàng khung bắt đầu thẳng mép ô nhập, vùng bấm vừa sáu khung. Bo theo §8.13: 8 khi dùng chuột, 22 trên cảm ứng — khung 44×44 bo 22 là hình tròn (capsule: bán kính bằng nửa chiều cao, S1). Đo lại: 1440 → 36×36, 390/320 → 44×44, hàng 256/304px.

### 8.15 Sheet Đổi mật khẩu: mã là một thanh (2026-11-18)

Sau §8.14, khung mã vuông cao bằng ô nhập (36/44) quá nhỏ trong sheet; chủ dự án chọn **một thanh nhập** như các ô khác của sheet (bỏ §8.14). Ô vẫn là mã dùng một lần: `autocomplete="one-time-code"` (AutoFill), bàn phím số, chỉ giữ chữ số, tối đa 6. Các màn ngoài app (Quên mật khẩu, Xác nhận email, Đặt lại mật khẩu bằng link) giữ `CodeField` 6 khung trong thẻ tài khoản (§8.13).

### 8.16 Nút hiện mật khẩu trong sheet Đổi mật khẩu (2026-11-18)

Chủ dự án: hai ô mật khẩu của sheet không có nút hiện. Đối chiếu docs: APPLE_CHECKLIST T-8 (nút chức năng thêm ở cuối ô; HIG Text fields: *"the trailing end to offer additional features"*), §3.5 I-1 biểu tượng quen thuộc, **I-2 `aria-label` và `title` khi có chuột**, **I-3 bán kính đồng tâm với ô**; LIQUID_GLASS §9 (nút con mắt 40px cách mép ô 2px). Làm: component chung `PasswordField` (ô ẩn chữ — T-5 — với nút con mắt cuối ô, `aria-label`, `title`, `aria-pressed`), dùng cho "Mật khẩu hiện tại" và "Mật khẩu mới". Trong sheet nút nằm trong ô, cách mép 2px, đồng tâm: chuột — ô 36 bo 8 → nút 32 bo 6; cảm ứng — ô 44 viên thuốc → nút tròn 40 (bo 20). I-3 ghi "hình tròn" vì viết cho ô viên thuốc của thẻ tài khoản; với ô bo 8 khi dùng chuột thì đồng tâm là bo 6. Trang Đăng nhập giữ nút riêng của nó; các màn tài khoản khác (Đăng ký, Quên mật khẩu, Đặt lại mật khẩu bằng link, Nhận lời mời) chưa có nút hiện — chưa đổi, chờ chủ dự án.

### 8.17 Điện thoại: thanh trang ở trên, ghim khi cuộn (2026-11-18)

Chủ dự án: ở mọi danh sách phân trang có điểm kết thúc (Kế hoạch, Nhật ký), trên điện thoại đưa thanh trang lên trên danh sách, ghim lại khi cuộn qua. Đối chiếu tài liệu:

| Nguồn | Nội dung | Áp dụng |
|---|---|---|
| [Apple] HIG Toolbars (iOS) | *"You can place toolbar buttons in the top corners or along the bottom. If you place these buttons above scrolling content, the buttons always remain visible, as the content scrolls under them."* | Ghim phía trên, danh sách cuộn bên dưới: đúng |
| [Apple] HIG Scroll edge effects | Hiệu ứng mép cuộn tách lớp khi nội dung cuộn dưới phần tử nổi; *"Apply one scroll edge effect per view"* | Thanh trang cùng nền đặc với thanh trên cùng (một khối), khi dính có mép mờ 16px phía dưới |
| [Apple] HIG Page controls | *"Center a page control at the bottom of the view"* | Câu này G2.9 chỉ mượn vị trí (thanh chấm tròn, không phải loại thanh của mình); máy tính giữ ở đáy, điện thoại thôi |
| WCAG 2.2 SC 2.4.11 Focus Not Obscured | Header dính không được che phần tử đang focus; kỹ thuật C43 *"using scroll padding"* | `scroll-padding-top` 60px (thanh trên cùng, mọi trang) và 120px khi có thanh trang ghim |
| WCAG 2.2 SC 1.4.10 Reflow | Phần tử dính/cố định có thể che nội dung khi phóng to | Cửa sổ thấp (≤ 500px, điện thoại xoay ngang, phóng chữ) thì thanh trang không ghim |
| WCAG 2.2 SC 2.4.3 Focus Order | Thứ tự Tab theo thứ tự nhìn thấy | Thanh trang nằm trước danh sách trong DOM, không chỉ dời bằng CSS |

Làm: `Pager` có `pinned` (bọc `pw-pager-bar`, sticky dưới thanh trên cùng 60px, rộng hết màn, nền `surface`; `useStuck` dùng IntersectionObserver để bật mép mờ khi dính); khung nhập số trang mở xuống dưới khi ghim. Đổi trang trên điện thoại: cuộn về đầu danh sách nếu đầu danh sách bị hai thanh ghim che (so với `scroll-padding-top`). Đo 390×844: lúc nghỉ thanh trang ở trên danh sách; cuộn thì dính ở y = 60, mép mờ bật; khung số trang mở xuống cách nút 8px; 844×390 thanh không ghim.

## 9. Thiết kế G4: tạo/sửa kế hoạch, nhập file, sao chép (2026-11-18)

### 9.1 Hiện trạng (chụp 2026-11-18, 1440 và 390px, kế hoạch nháp 17 điểm)

- **Form** (`/plans/new`, `/plans/:id/edit`): link chữ "← Kế hoạch"; "Huỷ" + "Lưu và tìm toạ độ" **hai bộ** (đầu và cuối trang); ba **thẻ viền** (Thông tin chung, Phân công tài xế, Điểm giao); ngày giao là ô ngày của trình duyệt (hiện "11/18/2026", kiểu Mỹ); ca làm và khung giờ là ô giờ của trình duyệt kiểu 12 giờ "--:-- --"; bảng điểm giao 1440px **tràn 83px** (cột Ghi chú bị cắt); điện thoại: hai bảng ô nhập cuộn ngang (thấy 2,5 cột), nút lưu ngay dưới tiêu đề.
- **Nhập từ file**: `Dialog` cũ, trên máy tính **vỡ bố cục** (lệch trái dưới thanh bên, mất tiêu đề); nút "Quay lại" / "Thêm 0 điểm giao" ở đáy.
- **Sao chép**: `Dialog` cũ; tiêu đề "Sao chép "{tên}"" dài tới 3 dòng; ô ngày của trình duyệt; "Quay lại" / "Sao chép" ở đáy.
- **Kế hoạch bị sửa nơi khác** (`PLAN_MODIFIED`): `Dialog` cũ.

### 9.2 Câu hỏi và trả lời (2026-11-18)

| # | Câu hỏi | Trả lời |
|---|---|---|
| Q4 | Điểm giao trên điện thoại | **Hàng + sheet** (D16): mỗi điểm một hàng, bấm mở sheet có đủ ô. Áp cùng cách cho **phân công tài xế** (cũng là bảng ô nhập). |
| G4-Q1 | Nhập giờ (ca làm, khung giờ) | **Theo cách nhập**: chuột/bàn phím gõ "HH:mm" 24 giờ (macOS textual: *"useful when… you expect people to make specific date and time selections"*); cảm ứng: nút hiện giờ, bấm mở bánh xe giờ và phút (iOS compact) |
| G4-Q2 | Bước phút của bánh xe | **5 phút** (HIG Pickers: *"Consider providing less granularity when specifying minutes"*); ô gõ vẫn nhận mọi phút (API `HH:mm`) |
| G4-Q3 | Hai bộ nút lưu | **Một bộ ở hàng tiêu đề, ghim khi cuộn** (HIG Toolbars: nút đặt trên nội dung cuộn *"always remain visible"*) |

### 9.3 Thiết kế

| # | Phần | Thiết kế | Nguồn |
|---|---|---|---|
| G4.1 | Quay lại | Nút quay lại chuẩn: chevron + tên trang cha ("‹ Kế hoạch" / "‹ {tên kế hoạch}"), không chữ "Quay lại" | [Apple] Toolbars: *"Use the standard Back and Close buttons… don't use a text label that says Back"*; D6 |
| G4.2 | Hàng tiêu đề | Tiêu đề ngắn **"Tạo kế hoạch"** (giữ, 12 ký tự) / **"Sửa kế hoạch"** (thay "Sửa {tên}"; tên kế hoạch có trong ô Tên và nút quay lại); cuối hàng "Huỷ" (không nền) + "Lưu và tìm toạ độ" (nổi bật, `checkmark`); **một bộ**, hàng này ghim dưới thanh trên cùng khi cuộn (nền đặc như thanh trên, mép mờ khi dính — một hiệu ứng mép cuộn, §8.17); điện thoại: nút lưu thành nút tròn dấu tích như trang Cửa hàng | [Apple] Toolbars (Titles < 15 ký tự, primary action trailing, `.prominent`); Scroll edge effects; Icons (Save → `checkmark`) |
| G4.3 | Nhóm | Bỏ ba thẻ viền: ba nhóm có tiêu đề, cách nhau bằng khoảng trống, nội dung trong nhóm nền như trang Cửa hàng | [Apple] Layout (*"Group related items… negative space, container shapes"*); D9, D16 |
| G4.4 | Ngày giao | **Compact date picker** dạng ô của form: nút hiện ngày "T4, 18/11/2026", bấm mở lịch như bộ lọc (máy tính popover, điện thoại inline §8.11, "Tháng ›" mở bánh xe §8.12); không còn ô ngày của trình duyệt | [Apple] Date pickers (Compact: *"a button that shows the current value"*) |
| G4.5 | Giờ (ca làm, khung giờ) | `TimeField`: **chuột** — ô gõ 24 giờ, chữ số, gõ "830"/"8:30"/"0830" → "08:30" khi rời ô, sai thì báo dưới ô; **cảm ứng** — nút hiện giờ (hoặc "—" khi trống), bấm mở popover/inline hai bánh xe Giờ (00–23) và Phút (bước 5) + "Bỏ giờ"; khung giờ là hai ô "Từ" "Đến" | [Apple] Date pickers (macOS textual, iOS compact), Pickers (Wheels, phút bước 5 — G4-Q2) |
| G4.6 | Phân công, máy tính | Bảng ô nhập giữ (nhập nhanh, D16); ô chọn tài xế/xe là pop-up của app (§8.7); giờ ca theo G4.5; nút xoá hàng biểu tượng thùng rác, hàng có menu ngữ cảnh "Xoá" | [Apple] Lists and tables (*"Let people edit a table when it makes sense"*); Pop-up buttons; G2.13 |
| G4.7 | Điểm giao, máy tính | Bảng ô nhập giữ, **không tràn** khung (ô giờ gõ hẹp thay ô giờ trình duyệt); bảng hẹp hơn chỗ cần (đo) thì dùng kiểu hàng + sheet như điện thoại | G2.11 (không cuộn ngang); D16 |
| G4.8 | Phân công và điểm giao, điện thoại | **Hàng + sheet** (Q4): hàng = dòng chính (tài xế · xe / mã đơn · người nhận), dòng phụ (ca làm / địa chỉ · khung giờ), dấu lỗi khi hàng có ô lỗi, chevron; bấm hàng → sheet "Phân công" / "Điểm giao" (Huỷ / Xong) với đủ ô; "+ Thêm …" mở sheet trống; chạm giữ hàng → menu "Xoá" | [Apple] Lists and tables, Sheets; G3 (Xe, Thành viên) |
| G4.9 | Nhập từ file | **Sheet** "Nhập điểm giao" (< 15 ký tự): bước 1 chọn/kéo thả file + file mẫu (chỉ "Huỷ"); bước 2 xem trước (thanh: "Quay lại" — HIG: Back cho bước trước — và "Thêm N điểm" khi có dòng hợp lệ) | [Apple] Sheets (multi-step, *"Avoid showing all three buttons"*); Entering data (*"let people provide data by dragging and dropping"*) |
| G4.10 | Sao chép | **Sheet** "Sao chép" (Huỷ / "Sao chép"): tên kế hoạch gốc trong thân sheet; ngày giao theo G4.4; tên mới; lựa chọn điểm cần chép | [Apple] Sheets; Toolbars (Titles) |
| G4.11 | Bị sửa nơi khác | `PLAN_MODIFIED` → **Alert** "Kế hoạch đã thay đổi" với "Tải lại" / "Huỷ" | [Apple] Alerts |

Không làm trong G4: chi tiết kế hoạch, giao việc, huỷ kế hoạch, chọn ghim (G5).

### 9.4 Làm và đo (2026-11-18)

- **Khác thiết kế, có lý do:** tiêu đề tạo giữ "Tạo kế hoạch" (12 ký tự, đã dưới 15); bảng phân công cũng theo G4.6/G4.8 (Q4 áp cho cả hai danh sách); bảng điểm giao chọn chế độ bằng phép đo hàng tiêu đề ẩn (`useTableFits`): cột tối thiểu Mã đơn 6rem, Người nhận 7,5, SĐT 7, Địa chỉ ≥ 10, Khung giờ 11,5, Số lượng 4,5, Ghi chú 7 → cần 828px; 1280px dư 105px (lần đầu 41px — e2e dùng font Noto Sans rộng hơn nên chuyển sang hàng, đã nới), 1024/960px dùng hàng + sheet; bảng xem trước nhập file gộp "Dòng, kết quả", "Người nhận" + SĐT, "Khung giờ, số lượng" để vừa sheet lớn ở 1440/1024/960 (HIG Lists and tables: *"Choose a row style that fits the information you need to display"*); bỏ `min-width` cố định cũ (1180px bảng điểm giao, 1080px bảng xem trước). Sao chép: chỉ một lựa chọn (kế hoạch nháp) thì nói thành câu, không thành ô chọn.
- **Lỗi tìm thấy khi làm:** Escape trong lịch nằm trong sheet đóng luôn sheet (sheet nghe `keydown` trên `document`) → sheet/alert bỏ qua Escape đã được popover xử lý (`defaultPrevented`); test hồi quy trong `Sheet.test.tsx`.
- **Đo:** 1440/1280/1024/960/390px, sáng và tối: không trang nào cuộn ngang; hàng tiêu đề ghim ở y = 60 khi cuộn; ô ngày và ô tên thẳng hàng (lưới căn trên); sheet điểm giao, bánh xe giờ, sheet nhập file hai bước, sheet sao chép; test web 274, e2e 3/3.
- **Để G5:** chi tiết kế hoạch, giao việc, huỷ kế hoạch, chọn ghim vẫn dùng `Dialog` cũ (`PlanDialogs.tsx`).

### 9.5 Góp ý của chủ dự án sau G4 (2026-11-18)

| # | Góp ý | Nguyên nhân (đo) | Sửa |
|---|---|---|---|
| 1 | Nút "Phóng to bản đồ để đặt ghim chính xác hơn" lỗi | `Dialog` cũ render trong trang: nằm dưới thanh bên và thanh trên, lệch trái; điện thoại rộng 477px trên màn 390; bản đồ 16:9 cao hơn chỗ, chú thích đè lên hai nút | `Dialog` render trên `body` (như Sheet, Alert — sửa luôn các hộp G5 còn dùng nó); một cột rộng bằng hộp (`minmax(0, 1fr)`); bản đồ của hộp toàn màn hình lấp chỗ còn lại. Đo: 1440 hộp trên cùng, bản đồ 582px, không đè nút; 390 hộp 358px |
| 2 | "Tìm lại địa chỉ này" nhấp nháy khi đã có lỗi và cuộn hết trang | Bấm thì xoá thông báo lỗi trước khi gọi API: trang ngắn đi 54px, đang ở cuối nên trình duyệt kéo lên 54px; 22ms sau lỗi giống hệt hiện lại → nháy | Giữ lỗi cũ tới khi kết quả lần thử mới thay nó trong cùng một lần vẽ; lỗi lặp lại dựng mới banner (`key`) để trình đọc màn hình vẫn đọc (`role="alert"`). Đo từng khung hình: không còn thay đổi chiều cao hay vị trí cuộn (390 và 1440) |
| 3 | Ô Người nhận và Số điện thoại trong bảng quá ngắn | Vòng trước hạ cột còn 7,5rem / 7rem (nội dung cần: "Nguyễn Thị Lan" 130px, "0905 123 456" 121px, "ĐH-10234" 98px) | Cột theo nội dung đo được (HIG Text fields: *"match the size of a text field to the quantity of anticipated text"*): Mã đơn 7rem, Người nhận 10,5rem, Số điện thoại 8,5rem; bù chỗ bằng ô giờ trong bảng 4,5rem (đủ "08:30" 66px), Khung giờ 9,5rem, Ghi chú 6rem. 1280px: cần 868, khung 933 |
| 2b | Bấm lại vẫn nháy, lần này ở nút phóng to, "Đặt ghim ở giữa bản đồ"; trong lúc tìm nên có chỉ báo | Trong lúc gửi (~0,15 s) cả khung bị coi là tắt: bản đồ thành chỉ xem (nút "Đặt ghim ở giữa bản đồ" bị gỡ rồi gắn lại), các nút mờ đi rồi sáng lại | Gửi yêu cầu không khoá gì (HIG Loading: *"Let people do other things… while they wait"*); chỉ khi server đang xử lý (`processingStatus`) mới khoá; bấm lần nữa trong lúc chờ thì bỏ qua. Chỉ báo cạnh nút (*đã thay ở dòng 8: vòng xoay trong nút theo B-4*), chỉ hiện khi chờ quá 0,3 s (HIG Loading: *"takes more than a moment"*). Đo từng khung hình khi bấm nhanh: không đổi gì tới khi có kết quả |
| 4 | "Đang tìm" là nhãn đứng yên, sao không là vòng xoay? | Trong lúc server tìm toạ độ, trang có thanh tiến độ "15/16" (đúng HIG: *"When possible, use a determinate progress indicator"*) và 3 nhãn tĩnh "ĐANG TÌM TOẠ ĐỘ" (tiêu đề, hàng, khung sửa); HIG: *"Keep progress indicators moving"*, việc không biết trước thời gian dùng activity indicator | Chủ dự án chọn **vòng xoay thay nhãn**: điểm đang tìm hiện vòng xoay + "Đang tìm" (`ActivityText`) ở hàng và khung sửa; tiêu đề bỏ nhãn khi đang tìm toạ độ (thanh tiến độ ngay dưới đã nói), đang tối ưu thì vòng xoay + "Đang tối ưu" (trang này không có thanh cho việc đó). Màn kết quả tối ưu (`ResultView`) để G5 |
| 5 | Bảng lồng trong nhóm ở form có được không, bo góc có đúng không? | Bảng có khung viền 1px bo 4px nằm trong nhóm bo 16 đệm 20: trái DESIGN_SYSTEM (bảng trong nhóm *"không viền"*) và S1/S2 (đồng tâm 16 − 20 < 0 → bán kính dự phòng, không phải 4) | Bảng trong nhóm của form không có khung riêng: một vật chứa (nhóm), hàng giữ đường kẻ |
| 6 | Bảng chính ở trang chi tiết kế hoạch là bảng kiểu cũ | Đúng (khung viền, thẻ viền bên cạnh) | Chủ dự án chọn để G5 (trang chi tiết theo kế hoạch §4) |
| 7 | Hộp chọn vị trí (phóng to để đặt ghim) vẫn chưa có vòng xoay | Ô "Tìm địa chỉ" (`PinPicker`, dùng cả ở hộp chọn vị trí và trang Cửa hàng) đổi gợi ý thành chữ tĩnh "Đang tìm…" trong lúc tìm | Gợi ý trong lúc tìm là `ActivityText` "Đang tìm địa chỉ…" (HIG Progress indicators: vòng xoay cho việc không biết thời gian, mô tả cụ thể — *"Avoid vague terms"*), hiện sau 0,3 s (HIG Loading), dòng gợi ý giữ chiều cao. Đo: tìm trả lời sau 1,5 s → 0,15 s chưa hiện, 0,85 s hiện vòng xoay + chữ, có kết quả thì về gợi ý thường |
| 8 | "Tìm lại toạ độ" (còn N điểm chưa có toạ độ) đứng yên; phải có dấu hiệu app đang chạy | **Đọc lại docs (lần trước đọc thiếu):** APPLE_CHECKLIST **B-4** — *"Thao tác chưa xong ngay thì nút hiện vòng xoay"* (HIG Buttons: *"Configure a button to display an activity indicator when you need to provide feedback about an action that doesn't instantly complete. Displaying an activity indicator within a button…"*, nhãn có thể đổi như *"Checkout" → "Checking out…"*) — nhưng `Button busy` chỉ làm mờ nút, không có vòng xoay (lệch ở mọi nút `busy`); vòng trước tao còn đặt chỉ báo **cạnh** nút thay vì **trong** nút. HIG Progress indicators macOS: *"Avoid labeling a spinning progress indicator… a label is usually unnecessary"* (phần chung: *"If it's helpful, display a description"*). Thanh tiến độ đứng yên ~2 s giữa hai lần đọc (*"Keep progress indicators moving"*). WCAG 2.2.2 Note 4: hoạt ảnh lúc tải là cần thiết khi không có nó người dùng tưởng app treo | Chủ dự án chọn: **chữ chỉ trong nút**, nơi khác vòng xoay không chữ; **cả hai** cho thanh tiến độ. `Button busy` (+ `busyLabel`): sau 0,3 s vòng xoay + nhãn bận hiện trong nút (hai nhãn chung một ô nên nút không đổi bề rộng), nút không tắt (không nháy) mà không nhận bấm (`aria-disabled`, `aria-busy`); bỏ chỉ báo cạnh nút; "Tìm lại địa chỉ này", "Lưu toạ độ", "Tìm lại toạ độ" → "Đang tìm toạ độ…" / "Đang lưu toạ độ…". `ActivityText` không hiện chữ (chữ là tên cho trình đọc màn hình). Thanh tìm toạ độ có vòng xoay luôn quay cạnh; lần đọc lại đầu sau 0,5 s rồi 2 s (UX_BRIEF: 2–3 s). Đo: nút xoay từ 0,3 s, thanh + vòng xoay khi server nhận, đọc lại +0,52 s rồi +2 s |

### 9.6 Chuyển cảnh trong luồng kế hoạch (2026-11-18)

Chủ dự án: phần lớn luồng chưa có chuyển cảnh. Theo HIG Motion (*"Add motion purposefully"*, *"brevity and precision"*, *"Let people cancel motion"*, *"avoid adding motion to UI interactions that occur frequently"*) và các chuyển động đã chốt của dự án, không đặt kiểu mới:

| Chỗ | Chuyển động | Theo |
|---|---|---|
| Màn của kế hoạch đổi tại chỗ (tìm toạ độ → kết quả tối ưu → theo dõi) | Màn mới mờ vào, nhô lên 12px, 0,28 s; không chạy ở màn đầu (trang đã có chuyển cảnh) | §6.2 CĐ2b |
| Khối xuất hiện: thông báo lỗi/thông tin, thanh tiến độ, "Còn N điểm chưa có toạ độ", "Còn N ô cần sửa", "Đã thêm N điểm từ file", thông báo sao chép | Mờ vào, trồi 10px, 0,32 s | G2.10 |
| Cùng thông báo lỗi nói lại sau một lần thử | Không chạy lại (`ErrorBanner repeat`) — chạy lại là nháy (§9.5 #2) | §9.5 |
| Chọn điểm khác (khung sửa điểm) | Nội dung khung mờ vào 0,2 s, không trượt. *Ban đầu không chuyển động (thao tác lặp lại); chủ dự án thấy khung "chưa mượt" và chọn mờ chéo nhẹ (2026-11-18)* | HIG Motion: *"brevity and precision"* |
| Thêm / xoá phân công, điểm giao (form kế hoạch) | Dòng mới mờ vào 0,25 s, nền tô nhạt rồi tan (1,2 s) để thấy dòng vừa thêm; dòng xoá mờ ra 0,2 s rồi mới mất (`useRowMotion`). Trên điện thoại dòng thêm qua sheet chỉ "đến" khi sheet đóng (lúc mở nó bị sheet che), huỷ thì không đến. Dòng có sẵn khi mở form không chạy. *Ban đầu không chuyển động; chủ dự án chọn (2026-11-18)* | HIG Motion: *"The system already provides subtle animations for interactions with standard interface elements"* (bảng chuẩn chèn/xoá dòng có hoạt ảnh) |
| Hộp "Phóng to bản đồ" (`Dialog` cũ) | Mở: nền mờ vào, hộp bật ra (0,24 s); đóng: thu về (0,2 s) — như Sheet trên màn rộng. Trước đó bật/tắt đột ngột (chủ dự án) | Sheet G2.10 |
| Giảm chuyển động | Chỉ mờ | §6.2 CĐ6 |

Vòng xoay cạnh thanh tiến độ nằm thẳng hàng với dòng chữ của thanh (tâm trùng nhau, đo). Đo (`getAnimations`): bấm "Tìm lại toạ độ" → thanh tiến độ và khối "Còn N điểm…" chạy `pw-result-in` khi đổi chỗ cho nhau.

### 9.7 Lý do "Tối ưu lộ trình" bị khoá (2026-11-18, thử theo lựa chọn của chủ dự án)

Chủ dự án: dòng "Thêm phân công tài xế để tối ưu." (chữ 12px dưới cụm nút ở đầu trang) "lơ lửng" — đo: ở 1440px nó treo ngang phụ đề, ở 1024px và điện thoại treo lẻ dưới nút. Docs chỉ yêu cầu nút khoá có lý do, không nói đặt ở đâu; chủ dự án chọn thử phương án "khối có nút" (không ưng thì quay lại bằng revert commit này):

- Bỏ dòng chữ nhìn thấy dưới nút; lý do vẫn là `aria-describedby` của nút (chỉ trình đọc màn hình).
- Thiếu phân công: khối trong trang cùng kiểu khối "Còn N điểm chưa có toạ độ" nhưng viền trung tính (không phải lỗi): "Chưa có tài xế nào được phân công." + "Thêm phân công (tài xế, xe, ca làm) để tối ưu lộ trình." + nút "Thêm phân công" mở form sửa.
- Các lý do khác đã có chỗ riêng: thiếu toạ độ → khối "Còn N điểm…"; đang tìm toạ độ / đang tối ưu → thanh tiến độ, vòng xoay (§9.5).

### 9.8 Chữ dài ở trang kế hoạch và trang sửa (G2.11, đo 2026-11-18)

Chủ dự án hỏi đã xử lý tràn chữ cho tiêu đề và nút quay lại sau "Sửa điểm giao" chưa. Đo tên 150 ký tự (có dấu cách và không dấu cách, giả qua `page.route`) ở 1440/390/320px:

| Chỗ | Trước | Sau |
|---|---|---|
| Tiêu đề trang kế hoạch (`h1`), tên không dấu cách | **Tràn ra ngoài màn** (1440 và 390px) | Cắt ở mọi chỗ (`overflow-wrap: anywhere` cho mọi `.pw-page-title`) |
| Tiêu đề trang kế hoạch, tên có dấu cách | Đủ tên, nhiều dòng (7 dòng ở 390px) | Giữ — G2.11: tên đầy đủ ở trang kế hoạch; HIG Typography *"use as many lines as needed"* |
| Nút quay lại ở trang sửa (tên kế hoạch) | Một dòng rồi "…", rê chuột hiện tên (`title`) — đúng G4.1 | Giữ |
| "Sửa kế hoạch" ở 320px | Xuống 2 dòng để chừa Huỷ / ✓, không tràn | Giữ |
| Nút "Phóng to bản đồ để đặt ghim chính xác hơn" | **Không xuống dòng, tràn khỏi khung sửa điểm** ở 390 và 320px | Xuống dòng trong nút; một dòng vẫn cao 36px |

### 9.9 Bo góc các ô trắng ở trang chi tiết kế hoạch (2026-11-18)

Chủ dự án: bo của các ô trắng ở trang kế hoạch không khớp các trang khác. Đo 1440px: Kế hoạch, Xe, Thành viên, Cửa hàng, Sửa kế hoạch — ô trắng bo 16, không viền; chi tiết kế hoạch — khối "Còn N điểm…" / "Chưa có tài xế…" bo 8 viền 1px (đỏ / xám), bảng bo 4 viền, khung sửa điểm bo 8 viền; Báo cáo (ô trống) bo 8 viền. Nguyên nhân: trang chi tiết thuộc G5 (§9.5 #6, đã chọn để G5), chưa đổi. Docs lệch: `DESIGN_SYSTEM.md` mục Bo góc và `.pw-card` vẫn ghi thẻ `radius-md` có viền như quy tắc chung — đã ghi rõ quy tắc nhóm bo 16 của khu đã redesign và trang nào còn kiểu cũ. Chủ dự án chọn: **để G5 cả** (không đổi code bây giờ); khối lỗi ở G5 dùng **nền đỏ nhạt** thay viền đỏ (ghi ở mục G5, §4).

### 9.10 Nút "Sao chép" ở đầu trang chi tiết kế hoạch (2026-11-18)

Chủ dự án: "Sao chép" không nền đứng đầu cụm nút trông như chữ lệch, muốn đưa ra thứ 3. HIG Toolbars: *"Only specify one primary action, and put it on the trailing side of the toolbar"* → "Tối ưu lộ trình" phải ở cuối, không đặt "Sao chép" sau nó. Chủ dự án muốn màu khác nút xám bên cạnh; HIG Buttons chỉ dành màu cho vai trò chính (màu nhấn) và nguy hiểm (đỏ), nên dùng kiểu **tinted** của Apple (UIKit `tinted()`), không thêm màu mới. Kiểm WCAG trước khi làm: 1.4.3 chữ ≥ 4.5:1 ở mọi trạng thái (đo, `DESIGN_SYSTEM.md`); 1.4.11 không bắt nền nút đạt 3:1 khi nút nhận ra bằng chữ (như nút xám); 1.4.1 có chữ.

Làm: thứ tự **Sửa điểm giao (xám) · Sao chép (tinted) · Tối ưu lộ trình (nút chính, cuối)**. Ghi nhận: theme sáng, khi "Tối ưu lộ trình" bị khoá (mờ 50%) nó cũng xanh nhạt, gần màu "Sao chép" — phân biệt bằng chữ (xanh đậm / trắng mờ). Màn kết quả tối ưu và theo dõi (`ResultView`, `TrackView`) vẫn để "Sao chép" kiểu cũ — G5.

### 9.11 Nút quay lại lên thanh trên trên điện thoại (2026-11-18)

Chủ dự án hỏi có được đưa nút quay lại lên thanh trên, ngang hàng avatar không. Kiểm tra: **thiết kế G1 (§6) đã quyết** — thanh trên *"Đầu: nút ẩn/hiện sidebar (rộng) hoặc nút quay lại (trang con)… Cuối: … nút tài khoản"*; G4 đặt `BackLink` trong nội dung trang là lệch với §6. HIG Toolbars: *"Elements that let people return to the previous document… appear at the far leading edge"*. WCAG: không tiêu chí nào cấm; giữ 2.4.3 (thứ tự focus quay lại → tài khoản → nội dung), 2.5.8 (vùng bấm 44px), 1.4.10 (320px không tràn: tên cha dài cắt "…", tên đầy đủ vẫn là tên trợ năng và `title`), 2.4.11 (thanh dính không che focus: `scroll-padding` có sẵn); 3.2.3 tốt hơn (nút ở cùng chỗ mọi trang con).

Làm: điện thoại — `BackLink` vào đầu thanh trên (`AppShell` có chỗ `pw-topbar-leading`, chỉ ở màn hẹp; nút chờ chỗ đó sẵn sàng rồi mới vẽ, không hiện trong trang một khung hình rồi nhảy lên). Cuộn qua tiêu đề lớn, tiêu đề nhỏ hiện giữa thanh → nút thu về chỉ còn chevron (như iOS khi tên trang trước không đủ chỗ), tên vẫn đọc được. Màn rộng giữ nút trong trang (đầu thanh trên là nút ẩn/hiện sidebar). Trang tạo/sửa: hàng tiêu đề sát dưới thanh trên như các trang khác. Đo 390 và 320px: nút và avatar cùng tâm (y = 30), không tràn; khoảng thanh trên → tiêu đề 16–21px ở mọi trang (trang sửa trước đó 53px).

### 9.12 Ba nút đầu trang chi tiết trên màn hẹp (2026-11-18)

Chủ dự án: "Sửa điểm giao", "Sao chép", "Tối ưu lộ trình" trên màn hẹp đặt chưa hợp lý. Đo: ba nút vừa một hàng từ 440px; hẹp hơn thì vỡ 2 hàng, nút chính đứng lẻ hàng dưới (HIG Toolbars: *"avoid layouts that cause toolbar items to overflow by default"*). HIG gợi ý cho iPhone: giữ nút quan trọng, đưa phần còn lại vào menu More (*"Create a More menu to include additional items"*); chủ dự án chọn **xếp dọc, rộng hết** để luôn thấy đủ ba nút.

Làm: dưới 440px (`(max-width: 439px)`) ba nút xếp dọc rộng hết, **nút chính trên cùng**, rồi "Sửa điểm giao", "Sao chép"; thứ tự trong DOM đổi theo (không đảo bằng CSS) để trình đọc màn hình và Tab đi đúng thứ tự nhìn thấy (WCAG 1.3.2, 2.4.3). Từ 440px: một hàng, nút chính cuối (§9.10). Đo 390 / 320px: ba nút 343 / 273px, cao 44px, không tràn. Màn kết quả và theo dõi: G5.

## 10. Thiết kế G5: chi tiết kế hoạch (2026-11-18, đang làm)

Phạm vi (SCREENS.md, `/plans/:id` là một route, màn chọn theo trạng thái): **tìm toạ độ** (DRAFT chưa tối ưu), **kết quả tối ưu** (DRAFT đã tối ưu), **chỉnh tay tuyến**, **giao việc / huỷ**, **theo dõi** (PUBLISHED → COMPLETED / CANCELLED). Hành vi (API, polling, lỗi, câu chữ) giữ như SCREENS §1.1 và UX_BRIEF; G5 đổi bố cục, control và hộp thoại.

### 10.1 Hiện trạng (chụp 2026-11-18, 1440 và 390px, dữ liệu mẫu `docs/ux/samples/plan-detail-*.json` giả qua `page.route`)

- **Kết quả tối ưu:** 6 nút ở đầu trang (Sao chép, Huỷ kế hoạch, Sửa điểm giao, Chỉnh tay, Tối ưu lại, Giao việc cho N tài xế); 4 thẻ số viền (điểm, quãng đường, thời gian, tài xế); khối "Không xếp được" viền cam; mỗi tuyến một thẻ viền có bảng 5 cột; bản đồ dính bên phải (rộng). **Điện thoại:** 6 nút vỡ 3 hàng; 4 thẻ số xếp dọc chiếm gần cả màn; **bảng tuyến bị cắt cột** (Địa chỉ, Khung giờ, Dự kiến tràn ra ngoài thẻ); bản đồ ở cuối trang (sau 23 điểm).
- **Chỉnh tay:** băng thông báo + Huỷ / Lưu thay đổi; mỗi điểm một hàng có tay nắm kéo và **`select` của trình duyệt** "Chuyển sang…"; cột "Chưa xếp tuyến".
- **Giao việc / Huỷ:** `Dialog` cũ (nút "Quay lại" / hành động ở đáy).
- **Theo dõi:** 4 thẻ số có pill; mỗi tuyến thẻ viền + thanh tiến độ nhiều màu + bảng có cột Kết quả (pill); bản đồ có vị trí tài xế; chú giải màu dưới bản đồ.
- **Tìm toạ độ:** như §9 (bảng điểm + khung sửa điểm có bản đồ nhỏ, "Phóng to bản đồ").

### 10.2 Câu hỏi và trả lời (2026-11-18)

| # | Câu hỏi | Trả lời |
|---|---|---|
| G5-Q1 | Bố cục | **Bản đồ phủ vùng nội dung** (D15, kiểu Apple Maps): rộng — panel nổi bên trái; điện thoại — sheet từ đáy có nấc. |
| G5-Q2 | Màn nào dùng bố cục bản đồ | **Cả bốn:** kết quả tối ưu, theo dõi, chỉnh tay, tìm toạ độ. |
| G5-Q3 | Panel (rộng) | **~460px, điểm giao là hàng hai dòng** (bảng 5 cột không vừa panel; thay cho "bảng khi rộng, hàng khi hẹp" đã chọn trước đó với bố cục cũ). |
| G5-Q4 | Nấc sheet (điện thoại) | **Ba nấc:** thấp, giữa (nửa màn), cao (gần đầy). HIG chỉ định nghĩa medium/large; nấc thấp là nấc tự định nghĩa (UIKit custom detent). *Sửa 2026-11-19 (§10.11, phác thảo fb_14):* nấc thấp **chỉ còn tay cầm** có chevron ↑ (trước đó: tiêu đề, trạng thái, nút chính). |
| G5-Q5 | 6 nút ở đầu màn kết quả | **Menu … + 2 nút:** (…) [Chỉnh tay] [Giao việc cho N tài xế]; menu: Sửa điểm giao, Tối ưu lại, Sao chép, Huỷ kế hoạch (đỏ, cuối). |
| G5-Q6 | Chọn một điểm | **Thẻ điểm trong panel/sheet** (kiểu place card): panel/sheet chuyển sang thẻ của điểm, có nút quay lại danh sách; marker được làm nổi. |
| G5-Q7 | Hộp xác nhận giao việc | **Sheet** "Giao việc" (Huỷ / Giao việc; thân liệt kê tài xế, xe, số điểm, giờ; điểm không xếp được; hậu quả; trùng ca hiện trong sheet). Huỷ kế hoạch: **Alert**. |
| G5-Q8 | Ô trắng | Nhóm **bo 16, không viền**; khối lỗi **nền đỏ nhạt** (đã chốt ở §4 G5, §9.9). |
| G5-Q9 | Tổng quan trong panel | **Một dòng tóm tắt** thay 4 thẻ số (G5.5). |
| G5-Q10 | Điểm không xếp được | **Nhóm "Chưa xếp tuyến"** trong panel, màu `signal` (cần chú ý, P-76), không nền đỏ vì không phải lỗi (G5.10). |
| G5-Q11 | Đặt ghim ở màn tìm toạ độ | **Trên bản đồ lớn**; bỏ bản đồ nhỏ và hộp "Phóng to bản đồ" (G5.15). |
| G5-Q12 | Nút màn theo dõi | **Chỉ nút …**: Sao chép, Huỷ kế hoạch (đỏ, cuối, chỉ khi đã giao việc); bỏ "Về danh sách kế hoạch" (nút quay lại đã làm việc đó). |
| G5-Q13 | Tổng quan tiến độ | **Dòng tóm tắt + thanh chung** nhiều màu cho cả kế hoạch (chữ đi kèm màu, WCAG 1.4.1). |
| G5-Q14 | Bấm vị trí tài xế | **Thẻ tài xế** (như thẻ điểm): tài xế, xe, vị trí cập nhật lúc nào, tiến độ, điểm kế tiếp, nút gọi. |
| G5-Q15 | Chú giải màu bản đồ theo dõi | **Trong panel, dưới tóm tắt** (một dòng nhỏ). |
| G5-Q16 | Hình ghim của điểm giao (màn tìm toạ độ) | **Ghim điểm giao riêng** (ghim màu nhấn, tâm trắng, chân nhọn chỉ đúng chỗ); kho giữ ngôi nhà (HIG Maps: *"Use annotations that match the visual style of your app"*). |
| G5-Q17 | Điện thoại: gõ địa chỉ trong thẻ sửa điểm | **Sheet lên nấc cao khi focus ô địa chỉ** (bàn phím không che ô và nút, WCAG 2.4.11); rời ô giữ nguyên nấc. |

### 10.3 Thiết kế

| # | Phần | Thiết kế | Nguồn |
|---|---|---|---|
| G5.1 | Khung bản đồ (`MapStage`, dùng chung 4 màn) | Bản đồ phủ hết vùng nội dung dưới thanh trên (không đệm trang, không cuộn trang); bản đồ tương tác (kéo, thu phóng); control +/− và "vừa khung" là kính nổi góc trên phải; chú thích bản đồ (MapTiler/OSM) không bị panel/sheet che lâu dài | [Apple] Maps: *"make your map interactive"*, *"enough contrast between custom controls and the map"*, logo/legal *"don't cover these elements all the time"*; LIQUID_GLASS (control nổi = kính) |
| G5.2 | Panel (rộng ≥ 960px) | Panel nổi bên trái trên bản đồ, rộng ~460px, cao theo vùng nội dung (cách mép 12px, bo đồng tâm), nền kính/đặc theo LIQUID_GLASS; panel tự cuộn, bản đồ đứng yên; khung nhìn bản đồ chừa phần panel che (fit bounds có đệm trái = bề rộng panel) | [Apple] Sheets/Split views (nội dung phụ bên cạnh việc chính, *"nonmodal"*); Maps (place card nổi) |
| G5.3 | Sheet có nấc (điện thoại) | Sheet không modal từ đáy, nằm trên tab bar; **ba nấc** thấp / giữa / cao (G5-Q4); grabber kéo được và **bấm grabber chuyển nấc** (HIG: *"tap it to cycle through the detents"*; cũng là cách không cần kéo — WCAG 2.5.7); nội dung cuộn trong sheet; bản đồ phía trên vẫn tương tác; khung nhìn bản đồ chừa phần sheet che | [Apple] Sheets: detents, *"Include a grabber in a resizable sheet"*, nonmodal; WCAG 2.5.7 Dragging Movements, 2.4.11 |
| G5.4 | Đầu panel/sheet | Nút quay lại (rộng: trong panel; điện thoại: thanh trên, §9.11); tên kế hoạch + pill; dòng phụ (ngày · tổng quan ngắn); hàng nút theo màn (G5.6). Nấc thấp (điện thoại) chỉ còn phần này | [Apple] Toolbars; §9.11 |
| G5.5 | Tổng quan | Thay 4 thẻ số bằng **một dòng tóm tắt** (G5-Q9) trong đầu panel ("23/24 điểm đã xếp · 15,4 km · 5 giờ 06 phút · 2 tài xế"); theo dõi: thanh tiến độ chung + số đã giao / không giao được / bỏ qua | [Apple] Layout (*"Order content by relative importance"*); panel hẹp |
| G5.6 | Nút | **Kết quả:** (…) [Chỉnh tay] [Giao việc cho N tài xế] (G5-Q5; menu: Sửa điểm giao, Tối ưu lại, Sao chép, Huỷ kế hoạch — đỏ, cuối). **Tìm toạ độ:** [Sửa điểm giao] [Sao chép] [Tối ưu lộ trình] như §9.10 (hẹp xếp dọc §9.12). **Theo dõi:** Sao chép (+ Huỷ kế hoạch khi PUBLISHED) — chi tiết khi làm G5.11. Nút chính luôn cuối | [Apple] Toolbars: *"Create a More menu"*, primary *"trailing side"*; Menus; Action sheets/Buttons (*"destructive… visually prominent"*) |
| G5.7 | Tuyến | Mỗi tuyến một nhóm bo 16 không viền trong panel: đầu nhóm là chấm màu tuyến + tài xế · xe + tóm tắt ("11 điểm · 8,3 km · 10:50–13:23 · tải 22/30"), thu gọn được; chọn tuyến làm nổi tuyến đó trên bản đồ (các tuyến khác mờ đi) | [Apple] Maps: *"Clearly identify elements that people select"*; Lists and tables |
| G5.8 | Điểm giao | **Hàng hai dòng** (G5-Q3): số thứ tự (vòng màu tuyến) · mã đơn · người nhận / địa chỉ · khung giờ; cuối hàng giờ dự kiến (kết quả) hoặc pill kết quả (theo dõi); bấm hàng → thẻ điểm (G5.9); hàng và marker là một cặp chọn | [Apple] Lists and tables (*"Choose a row style that fits"*); G2.5 |
| G5.9 | Thẻ điểm | Panel/sheet chuyển sang thẻ của điểm (G5-Q6): nút quay lại danh sách ("‹ Tài xế A"), mã đơn · người nhận, điện thoại, địa chỉ, khung giờ, dự kiến, kết quả + lý do, ghi chú; marker nổi (to hơn, viền); Escape / quay lại về danh sách và trả focus về hàng | [Apple] Maps (place cards); WCAG 2.4.3 |
| G5.10 | Không xếp được | Nhóm "Chưa xếp tuyến" trong panel (G5-Q10), đầu nhóm màu `signal` và câu chung "vẫn giao việc được" (SCREENS: không tự suy lý do); không nền đỏ — không phải lỗi | P-76; SCREENS §1.1 |
| G5.11 | Theo dõi | Như kết quả nhưng hàng có pill kết quả; vị trí tài xế trên bản đồ (mũi tên); tuyến tắt chia sẻ vị trí ghi trong đầu nhóm tuyến; tự tải lại 30 s (giữ) | SCREENS §1.1; P-76 |
| G5.12 | Chỉnh tay | Trong panel/sheet: các tuyến xếp dọc (không còn cột cạnh nhau), kéo thả hàng giữa tuyến (tay nắm; bàn phím như hiện nay); "Chuyển sang…" là **pop-up menu của app** (thay `select` trình duyệt, §8.7); thanh Huỷ / Lưu thay đổi ghim ở đầu panel/sheet | SCREENS (W10); [Apple] Lists and tables (*"Let people edit"*), Menus; WCAG 2.5.7 (có cách không kéo: menu Chuyển sang) |
| G5.13 | Giao việc | **Sheet** "Giao việc" (G5-Q7): Huỷ / "Giao việc"; thân: từng tài xế (chấm màu, xe, số điểm, giờ đi–về), điểm không xếp được, câu hậu quả; `SHIFT_OVERLAP` hiện trong sheet (danh sách trùng + link + "Sửa phân công") | [Apple] Sheets; Alerts (*"Keep alert messages short"* → nội dung dài dùng sheet) |
| G5.14 | Huỷ kế hoạch | **Alert** "Huỷ kế hoạch?" + câu hậu quả; "Huỷ kế hoạch" đỏ, "Không" là nút huỷ | [Apple] Alerts (destructive) |
| G5.15 | Tìm toạ độ | Danh sách điểm (hàng; điểm lỗi nổi, vòng xoay khi đang tìm) trong panel/sheet; thanh tiến độ, khối "Còn N điểm…" (nền đỏ nhạt), "Chưa có tài xế…" ở đầu panel; **sửa điểm = thẻ điểm** (G5.9) có ô địa chỉ, "Tìm lại địa chỉ này", đặt ghim **trực tiếp trên bản đồ lớn** (G5-Q11; bỏ bản đồ nhỏ và hộp "Phóng to bản đồ" — bản đồ đã phủ trang) | §9; G5-Q2 |
| G5.16 | Hộp thoại cũ | Bỏ `Dialog` cũ khỏi trang kế hoạch (giao việc → Sheet, huỷ → Alert, phóng to bản đồ → không cần) | §9.5 (để G5) |



### 10.4 Thứ tự làm

1. `MapStage` (bản đồ phủ, panel rộng, sheet ba nấc điện thoại, chừa chỗ khi fit bounds) + test bàn phím/trình đọc màn hình của sheet.
2. Màn kết quả trong `MapStage`: đầu panel, menu …, tóm tắt, nhóm tuyến, hàng điểm, chọn hàng ↔ marker, thẻ điểm, chưa xếp tuyến.
3. Giao việc (Sheet), huỷ (Alert).
4. Theo dõi.
5. Chỉnh tay.
6. Tìm toạ độ (thẻ sửa điểm, ghim trên bản đồ lớn); bỏ `Dialog` cũ ở trang kế hoạch.
7. Đo theo APPLE_CHECKLIST (1440/1024/390/320, sáng/tối, giảm chuyển động, tăng tương phản), cập nhật SCREENS, DESIGN_SYSTEM, CURRENT_STATE.

### 10.5 Làm (2026-11-18, đang làm)

- **Bước 1–2 xong:** `MapStage` (`shared/ui/MapStage.tsx`): bản đồ phủ vùng nội dung (trang không đệm, không cuộn), panel kính 460px (rộng), sheet kính ba nấc trên tab bar (điện thoại; grabber bấm chuyển nấc, mũi tên lên/xuống, kéo; nấc thấp đo theo phần đầu đánh dấu `data-peek`); bản đồ canh khung chừa phần panel/sheet che (`RouteMap inset`, `staged` bỏ nút mở rộng và chú giải); chú thích bản quyền bản đồ trên nấc thấp của sheet 10px (HIG Maps). Màn kết quả trong `MapStage`: đầu panel (quay lại, tên, dòng phụ, dòng tóm tắt, nút … / Chỉnh tay / Giao việc), nhóm tuyến (màu tuyến, thu gọn), hàng điểm hai dòng, thẻ điểm (focus vào tiêu đề, quay lại trả focus về hàng), nhóm "Chưa xếp tuyến". Nhóm trong panel bo 8: panel bo 16 đệm 16 nên bán kính đồng tâm là 0 → bán kính dự phòng (S1/S2), khác nhóm bo 16 ở cấp trang. Chỉnh tay và hộp thoại tạm giữ như cũ trong panel (bước 3, 5).
- **Bước 3 xong:** giao việc là **Sheet** "Giao việc" (Huỷ / Giao việc hoặc "Vẫn giao việc" khi còn vi phạm; câu hỏi "Giao việc cho N tài xế?" là dòng đầu thân sheet — tiêu đề < 15 ký tự như G4; trùng ca biến sheet thành "Trùng ca làm" với link kế hoạch kia và "Sửa phân công"); huỷ kế hoạch là **Alert** với nút "Huỷ" và "Huỷ kế hoạch" đỏ (HIG Alerts: *"Always use the title "Cancel" for a button that cancels an alert's action"*; lỗi nói trong lời nhắn); "Huỷ" khi đang chỉnh tay có thay đổi là **action sheet** "Bỏ thay đổi" / "Tiếp tục chỉnh" (HIG Alerts: *"Use an action sheet — not an alert — to offer choices related to an intentional action"*), có dòng tiêu đề + lời nhắn như iOS (cũng là tên trợ năng); "Tối ưu lại?" và "Rời trang khi chưa lưu?" là Alert. Màn kết quả và theo dõi không còn dùng `Dialog` cũ.
- **Bước 4 xong:** màn theo dõi trong `MapStage`: đầu panel có thanh tiến độ chung nhiều màu + dòng "2/23 đã giao · 1 không giao được · …" (G5-Q13), chú giải màu một dòng (G5-Q15), nút … ở cuối hàng tiêu đề (Sao chép, Huỷ kế hoạch khi đã giao việc; G5-Q12); mỗi tuyến một nhóm có pill trạng thái tuyến của server, thanh tiến độ, dòng vị trí (bấm được khi có vị trí → thẻ tài xế); hàng điểm có pill kết quả và lý do không giao được; **thẻ tài xế** (G5-Q14: xe, vị trí cập nhật lúc nào, sai số, tốc độ, tiến độ, điểm kế tiếp, điện thoại) mở từ mũi tên trên bản đồ hoặc dòng vị trí; "Về danh sách kế hoạch" bỏ. Đo 1440 / 390px với dữ liệu mẫu đang giao: không lỗi trang.
- **Bước 5 xong:** chỉnh tay trong panel/sheet: các tuyến xếp dọc thành nhóm bo 8 không viền (tuyến quá tải: khung đỏ + chữ tải đỏ), cao theo nội dung — panel tự cuộn (khối trong panel không co: `flex-shrink: 0`); thanh "Bạn đang chỉnh tay" + Huỷ / Lưu thay đổi ở đầu panel; điện thoại: "Chuyển sang…" (pop-up menu của app, `useSelectMenu`) xuống dòng dưới tên điểm. Đo: cột cao 855/916/165px (rộng), không bị cắt.
- **Bước 6 xong:** màn tìm toạ độ trong `MapStage`: đầu panel như §9.10/§9.12, tiến độ và hai khối ("Còn N điểm…", "Chưa có tài xế…") dưới đầu; danh sách điểm là hàng (không số thứ tự; cuối hàng pill / vòng xoay / "Nhập sẵn"); bấm hàng hoặc marker → **thẻ sửa điểm** (địa chỉ, "Tìm lại địa chỉ này", "hoặc đặt ghim" **trên bản đồ lớn** — bấm, kéo ghim, "Đặt ghim ở giữa bản đồ" dời ra ngoài chỗ panel che; Bỏ qua / Lưu toạ độ); bỏ bản đồ nhỏ và hộp "Phóng to bản đồ" (G5-Q11). Mở trang thì thẻ của điểm lỗi đầu tiên mở sẵn (SCREENS) nhưng **focus không bị kéo vào** (WCAG 2.4.3); quay lại danh sách không tự mở lại; mở thẻ thì cuộn panel/sheet tới thẻ (cả thẻ điểm, thẻ tài xế); điện thoại: sheet về nấc giữa để còn thấy bản đồ đặt ghim. Trang kế hoạch không còn `Dialog` cũ (G5.16). Ghim của điểm giao: ghim riêng (G5-Q16; trước đó dùng ngôi nhà của kho, có từ trước G5); điện thoại gõ địa chỉ thì sheet lên nấc cao (G5-Q17).
- **Bước 7 (đo, 2026-11-19):** ba màn (kết quả, theo dõi, tìm toạ độ) ở 1440 / 1024 (chuột) và 390 / 320 (cảm ứng), sáng và tối: trang không cuộn, không phần tử tràn khỏi panel/sheet, không lỗi trang; vùng bấm < 44px trên cảm ứng chỉ có grabber 28px → nâng lên 44px; ở 390px tên kế hoạch bị bẻ dòng vì hàng tiêu đề không xuống dòng → cho xuống dòng (pill và … sang hàng dưới khi thiếu chỗ). Giảm chuyển động: sheet đổi nấc không chuyển tiếp; giảm trong suốt / tăng tương phản: panel và sheet nền đặc, tăng tương phản có viền 2px. axe sạch ở ba màn (test). E2e 3/3 (luồng MVP thật: tìm toạ độ → tối ưu → chỉnh tay → giao việc → theo dõi).
- Đo 1440 / 390px (dữ liệu mẫu): trang không cuộn; nấc cao dưới thanh trên (top 84 > 60), nấc thấp chỉ còn phần đầu; số thứ tự màu tuyến; không lỗi trang; axe sạch.

### 10.6 Màn rộng: bản đồ phủ cả cửa sổ (2026-11-19)

Chủ dự án hỏi mở rộng bản đồ ra toàn màn hình trên PC có khả thi, có vi phạm gì không. Đo trước đó: bản đồ x 244–1425, y 60–900 (dưới thanh trên đặc 60px, cạnh sidebar, mép phải trống 15px do `scrollbar-gutter: stable`). Kiểm tra: HIG Sidebars *"Extend visually rich content beneath the sidebar… sidebars can float above content in the Liquid Glass layer"*; Toolbars: nút trên nội dung *"always remain visible"*; Going full screen (cách khác: toàn màn hình của hệ thống) — không vi phạm; WCAG: giữ 1.4.11 (nút trên bản đồ đủ nổi), chú thích bản quyền luôn thấy (P-45). Chủ dự án chọn **tràn dưới sidebar và thanh trên** (không làm nút toàn màn hình).

Làm (chỉ màn rộng; điện thoại giữ như §10.5): `MapStage` cố định phủ cả cửa sổ (0–1440 × 0–900), dưới sidebar (kính) và thanh trên; thanh trên không còn dải nền, nút ẩn/hiện sidebar và avatar thành nút kính có viền mảnh và bóng (HIG Maps; giảm trong suốt / tăng tương phản: nền đặc); panel đặt cạnh sidebar dưới hàng nút (top 60), ẩn sidebar thì panel trượt sang trái (0,24 s; giảm chuyển động: không trượt); nút +/− và "Đặt ghim ở giữa bản đồ" dời xuống dưới hàng nút; canh khung tuyến đo vị trí thật của panel; trang có bản đồ phủ không giữ chỗ thanh cuộn (không cuộn). Đo: bản đồ 0–1440 × 0–900, trang không cuộn, không lỗi, sáng và tối.

### 10.7 Nút bản đồ, thanh bên tự thu (2026-11-19)

Chủ dự án: (1) "Đặt ghim ở giữa bản đồ" thành nút hình ghim; (2) +/− thẳng hàng avatar, nút ghim dưới +/−; (3) trên PC, vào trang chi tiết kế hoạch thì thanh bên tự thu (vẫn còn nút mở), ra khỏi trang thì về như trước khi vào. Kiểm tra trước khi làm:
- (1) HIG Buttons *"associate familiar actions with familiar icons"* — ghim là biểu tượng thả ghim quen (Maps `mappin`); nhưng *"Consider using text when a short label communicates more clearly than an icon"*: hình ghim không nói "ở giữa bản đồ" → tên đầy đủ giữ làm tên trợ năng (WCAG 4.1.2) và chú thích khi rê chuột. `PinMap` dùng chung nên bản đồ đặt kho (Cửa hàng) cũng đổi.
- (2) Chỉ là vị trí; vẫn viền mảnh + bóng (HIG Maps).
- (3) HIG Sidebars: *"Consider automatically hiding and revealing a sidebar when its container window resizes… Mail… can automatically collapse its sidebar, making more room"*; WCAG 3.2.3: nút mở thanh bên luôn ở cùng chỗ. Không vi phạm; giữ: tự thu không ghi đè lựa chọn ẩn/hiện đã lưu của người dùng.

Làm: nút ghim 38px (bằng cụm +/−; cảm ứng 46px tròn) ngay dưới +/−; cột nút bản đồ thẳng mép phải avatar (`--map-edge` theo đệm của thanh trên; màn rộng `--map-top` 64px dưới avatar). `AppShell` có `useFoldedSidebar`: `MapStage` (màn rộng) thu thanh bên khi mở, trong trang nút ẩn/hiện chỉ có tác dụng tạm (không lưu), rời trang thì về trạng thái đã lưu. Đo: 1440 — avatar và cột nút cùng mép phải 1416, ghim y 147 dưới +/− (64–138); 390 — cùng mép phải 374; thanh bên mở ở danh sách → thu ở trang kế hoạch → mở lại khi về danh sách.

### 10.8 Bản đồ bị xé hình khi mở (2026-11-19)

Chủ dự án: mở bản đồ bị xé hình. Đo từng khung hình khi bấm từ danh sách vào kế hoạch (1440px): trong ~0,35 s trang mới trượt vào bằng `transform` (CĐ3), bản đồ `position: fixed` bị bố cục trong trang đang trượt thay vì cửa sổ (cao 360px, y 60, lệch 24px) rồi nhảy thành 1440×900 khi hết hiệu ứng; Leaflet chỉ tự vẽ lại khi cửa sổ đổi cỡ nên phần mới lộ ra trống / vẽ dở. Sửa: khi `MapStage` xuất hiện, hiệu ứng vào trang đang chạy đổi thành chỉ mờ (cùng thời lượng; mờ cũng là hiệu ứng của giảm chuyển động, CĐ6); đổi màn tại chỗ trên bản đồ (`ViewSwitch`) chỉ mờ; `MapFrame` theo dõi kích thước khung bản đồ (`ResizeObserver` → `invalidateSize`) cho mọi lần khung đổi cỡ (thanh bên, sheet). Đo lại: từ khung hình đầu bản đồ đã 1440×900 ở (0, 0).

### 10.9 Nhóm mở/đóng có hoạt ảnh, hàng nút lấp panel (2026-11-19)

Chủ dự án: nhóm "Điểm giao" mở/đóng chưa có hoạt ảnh; ba nút Sửa điểm giao / Sao chép / Tối ưu lộ trình không lấp hết khung, dư một khoảng ở đầu. Làm: nhóm tuyến / "Điểm giao" / "Chưa xếp tuyến" co giãn chiều cao khi đóng mở (hàng lưới 0fr ↔ 1fr, 0,3 s, vào chậm ra chậm — đường "nhanh đầu" làm danh sách dài tụt gần hết trong một khung); đóng thì danh sách `inert` và ẩn (Tab, trình đọc màn hình không vào); giảm chuyển động: không co giãn. Hàng nút ở đầu panel / sheet: các nút giãn lấp bề ngang (nút … giữ hình tròn). Đo 1440px: ba nút 29–455 = đúng bề ngang nội dung panel; đóng 896 → 0 và mở 0 → 896 qua nhiều khung liền nhau.

### 10.10 Sheet trên điện thoại: cách mép, bo nhiều hơn (2026-11-19)

Chủ dự án: sheet nên bo hơn và không bám sát mép màn hình — có nên không. HIG Sheets không nói; Apple, *Adopting Liquid Glass* (https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass): *"Sheets feature an increased corner radius, and half sheets are inset from the edge of the display to allow content to peek through from beneath them. When a half sheet expands to full height, it transitions to a more opaque appearance to help maintain focus on the task."* và *"check for content and controls that might appear too close to rounder sheet corners"*. WCAG: không cấm; giữ 1.4.10 (320px không tràn). → Nên làm, như iOS 26.

Làm: nấc thấp và giữa — cách mép trái/phải 12px và bo 32px, bằng tab bar ngay dưới (cách mép 12px, viên thuốc cao 64px) để hai khối cùng một họ hình (Apple không cho số cho web); nội dung đệm 20px cho xa góc bo. Nấc cao — áp sát hai mép, nền đặc (nền trang, để nhóm nền nổi lên như danh sách nhóm), bo trên `radius-lg`. Chuyển giữa các nấc có chuyển tiếp (giảm chuyển động: không). Đo: 390px sheet 12–378 = tab bar 12–378; 320px 8–312; nấc cao 0–390; không tràn. Ghi nhận: nấc giữa hẹp đi 24px nên ở 390px "Giao việc cho N tài xế" xuống hàng riêng (rộng hết).

### 10.11 Sheet thu nhỏ chỉ còn tay cầm (2026-11-19)

Chủ dự án (phác thảo `docs/ux/samples/fb_14.png`): khi thu nhỏ, sheet trên điện thoại chỉ còn một khối bo tròn có chevron ↑. Kiểm tra: docs — sửa G5-Q4 (nấc thấp trước đó hiện tiêu đề, trạng thái, nút chính), chủ dự án quyết; HIG Sheets — nấc tự định nghĩa được, cần grabber (*"Include a grabber in a resizable sheet"*, bấm grabber chuyển nấc): tay cầm có chevron là grabber; WCAG — tên trợ năng (4.1.2), vùng bấm 44px (C1; 2.5.8 cần 24), mở bằng bấm không cần kéo (2.5.7); HIG Maps — chú thích bản quyền trên nấc thấp nhất 10px. Không vi phạm. Chủ dự án chọn **khối nhỏ ở giữa** (không phải dải rộng bằng sheet).

Làm: nấc thấp = nút kính 96 × 44px, bo 22px, ở giữa ngay trên tab bar, chỉ có chevron ↑; bấm hoặc kéo lên → nấc giữa; nội dung sheet `inert` và ẩn khi thu nhỏ; tên trợ năng "Đổi chiều cao danh sách (đang thu nhỏ)". Đo 390px: tay cầm x 147–243, y 712–756 (tab bar từ 768); chú thích bản quyền đáy 694; bấm → nấc giữa.

### 10.12 Chú thích bản đồ trên điện thoại ở góc trên trái (2026-11-19)

Chủ dự án: chú thích "© MapTiler © OpenStreetMap contributors" trên điện thoại nằm lơ lửng giữa màn, nên ra góc. Nguyên nhân: §10.5 đặt nó trên nấc thấp nhất của sheet 10px (HIG Maps); khi nấc thấp chỉ còn tay cầm (§10.11), tab bar rộng hết màn vẫn che dải dưới nên chú thích bị đẩy lên giữa (đáy y 694 ở 390px) và chạy theo sheet. Kiểm tra: HIG Maps chỉ yêu cầu không che mãi, có đệm với control, *"Avoid causing the logo and link to move with your interface"* — không bắt góc dưới; P-45 không quy định góc. Làm (chỉ điện thoại; màn rộng giữ góc dưới phải): chú thích ở **góc trên trái** bản đồ, ngay dưới thanh trên, cách mép như cột nút (`--map-edge`), đứng yên ở mọi nấc (góc trên phải là +/− và ghim, góc dưới bị tab bar che); màn hẹp thì xuống dòng thay vì chạy dưới cột nút. Đo: 390px x 16–284, y 68–88 ở cả ba nấc; 320px x 16–250, hai dòng, không đè +/− (x 258).

### 10.13 Android: trang có bản đồ bị cuộn lệch (2026-11-19)

Chủ dự án (Chrome mới nhất, Galaxy S20 FE; `docs/ux/samples/fb_15.png`, `fb_16.png`): trang kế hoạch cuộn được — sheet bị đẩy lên dưới thanh trên, dải bản đồ lộ giữa sheet và tab bar (fb_15); nút "+" bị thanh trên che (fb_16). Nguyên nhân: `.pw-app` và `.pw-shell` cao tối thiểu `100vh`, khung bản đồ `100dvh − 60px`; trên Chrome Android `100vh` là chiều cao khi thanh địa chỉ đã ẩn, lớn hơn phần nhìn thấy (~56px) → trang dài hơn màn hình và cuộn; trên máy tính và giả lập hai đơn vị bằng nhau nên không thấy. Thêm: vị trí cuộn của trang trước (danh sách dài) còn giữ khi mở kế hoạch. Sửa (chỉ trang có bản đồ phủ): khung app cao tối thiểu `100dvh`; `html`, `body` không cuộn, không kéo nảy; mở trang thì về đầu (`scrollTo(0, 0)`). Đo bằng giả lập (`.pw-shell` cao `100vh + 56px`, danh sách cuộn 400px rồi mở kế hoạch): trang về đầu, không cuộn được kể cả lăn / chạm, khung bản đồ ngay dưới thanh trên (y 60), "+" ở y 70. Cần chủ dự án thử lại trên máy thật.

### 10.14 Sheet nấc cao chạm đáy màn, tab bar nổi trên (2026-11-19)

Chủ dự án hỏi nấc cao áp sát mép là chủ đích (đúng: §10.10, Apple chỉ cho sheet nửa màn cách mép) và chọn giữa "tràn xuống hết" và "bo cả góc dưới", với tab bar nổi trên sheet. Kiểm tra: HIG Tab bars *"Make sure the tab bar is visible when people navigate to different sections of your app… The exception is when a modal view covers the tab bar"* — sheet này không modal nên **không được che** tab bar; để tab bar nổi trên thì tab bar vẫn hiện → đạt. LIQUID_GLASS M6 (*"avoid glass on glass"*): ở nấc cao sheet đặc → tab bar kính nằm trên nền đặc, đạt. WCAG 2.4.11: cuối danh sách và phần tử đang focus không được nằm dưới tab bar → đệm đáy và `scroll-padding-bottom` bằng tab bar. Cả hai cách đều không vi phạm; chọn **tràn xuống hết, tab bar nổi trên** (giống iOS nhất).

Làm: nấc cao chạm đáy màn (thấp và giữa vẫn trên tab bar); khung bản đồ điện thoại là lớp riêng (`isolation`) để tab bar (z 30) luôn trên sheet và các lớp Leaflet; thân sheet ở nấc cao đệm đáy = tab bar + khoảng cách. Đo 390px: sheet y 84–844, tại tab bar phần tử trên cùng là tab bar; cuộn hết thì hàng cuối dừng ở y 739 < 768 (đỉnh tab bar).

### 10.15 Điểm giao trên bản đồ là ghim (2026-11-19)

Chủ dự án: đổi các chấm tròn thành ghim; điểm đã xếp thứ tự thì có số trong ghim. Kiểm tra: HIG Maps *"Use annotations that match the visual style of your app"*; marker mặc định của Apple là ghim, *"You can change the icon to a string"* → ghim có số đúng kiểu; WCAG: viền trắng + bóng (1.4.11), số đi cùng màu (1.4.1), tên trợ năng giữ (4.1.2). Làm: marker điểm (`RouteMap`) là ghim giọt nước chân nhọn (hình vuông một góc nhọn xoay −45°, số dựng thẳng bên trong), màu tuyến / màu kết quả như cũ, điểm chưa xếp thì không số; neo ở chân ghim (0,707 × cạnh dưới tâm) nên chân chạm đúng đỉnh đường tuyến; ghim được chọn to hơn, viền theo hình ghim. Chú giải và số trong danh sách giữ hình tròn (`.pw-marker` dùng chung).
- *Sửa cùng ngày (phác thảo `docs/ux/samples/fb_17.png`):* ghim kiểu ghim bản đồ cổ điển — đầu tròn, thân thon xuống mũi nhọn, **vòng trắng ở giữa đầu**; có số thì số nằm trong vòng trắng, cùng màu ghim (đo tương phản trên nền trắng: mọi màu tuyến và màu kết quả 5,86–10,10:1 ≥ 4,5:1, WCAG 1.4.3); không số thì vòng trắng rỗng. Vẽ bằng SVG (`pinSvg`, viền trắng 1,5, bóng; chọn: 36px, viền đậm); ghim đặt toạ độ cho điểm giao (`PinMap kind="stop"`) dùng cùng hình, màu nhấn.

## 11. Thiết kế G6: Báo cáo (2026-11-19, đang làm)

Phạm vi: `/reports?from=&to=` (SCREENS §1; API `GET /reports/summary`, `/reports/stops.csv`, T63). Hành vi và câu chữ giữ như SCREENS, UX_BRIEF (khoảng ≤ 31 ngày, `INVALID_DATE_RANGE`, `DATE_RANGE_TOO_LONG`); G6 đổi bố cục, control và biểu đồ. Biểu đồ dùng chung với Tổng quan của khu quản trị (P-59).

### 11.1 Hiện trạng (chụp 2026-11-19, 1440 và 390px, báo cáo mẫu 7 ngày giả qua `page.route`)

- Tiêu đề + lời dẫn; "Xuất CSV" (điện thoại: nằm lẻ dưới tiêu đề); "Hôm nay / 7 ngày" kiểu segmented cũ; hai **ô ngày của trình duyệt** (hiện kiểu Mỹ "12/01/2026").
- 4 **thẻ số viền** (kế hoạch giao việc, điểm giao, giao được, quãng đường dự kiến).
- Biểu đồ cột chồng theo ngày: **không chú giải** (đã giao / không giao được / bỏ qua chỉ phân biệt bằng màu), các phần cột dính liền, số từng ngày **chỉ xem được khi rê chuột** (tooltip); trình đọc màn hình chỉ nghe một câu chung.
- Bảng tài xế 9 cột: rộng bị cắt cột cuối, điện thoại tràn khung; bảng lý do.

### 11.2 Apple / WCAG (HIG Charts, Charting data)

- *"Avoid relying solely on color to differentiate between different pieces of data"* (WCAG 1.4.1) → cần chú giải có chữ.
- *"in a bar chart that stacks marks… adding separators between the marks"*.
- *"Summarize the main message of your chart"* — tiêu đề + phụ đề tóm ý chính.
- *"don't require interaction to reveal critical information"*; *"consider expanding the hit target to include the entire plot area"*; *"Make an interactive chart easy to navigate when using keyboard commands"*.
- Nhãn trợ năng mỗi mark có ngữ cảnh, *"June 6" is clearer than "6/6"*; *"Hide visible text labels for axes and ticks from assistive technologies"*; không dùng từ chủ quan.
- *"In a compact environment, maximize the width of the plot area"*; *"Align a chart with surrounding interface elements"*; *"Prefer consistency across multiple charts"*.
- *"Not every collection of data needs to be displayed in a chart… list or table"*.

### 11.3 Câu hỏi và trả lời (2026-11-19)

| # | Câu hỏi | Trả lời |
|---|---|---|
| G6-Q1 | Chọn khoảng ngày | **Segmented [Hôm nay \| 7 ngày \| 30 ngày \| Tùy chọn]**; Tùy chọn thì hiện hai `DateField` (G4, lịch của app). |
| G6-Q2 | Thẻ số | **Một nhóm 4 ô, không viền** (rộng một hàng, điện thoại 2×2). |
| G6-Q3 | Số từng ngày | **Chọn cột + bàn phím** (kiểu Health): bấm / rê / mũi tên chọn một ngày, dòng tóm tắt trên biểu đồ đổi sang số của ngày đó; vùng bấm là cả vùng vẽ; mỗi cột có nhãn trợ năng đầy đủ. |
| G6-Q4 | Biểu đồ dùng chung với quản trị | **Có, dùng chung** (bố cục trang quản trị vẫn để sau). |

### 11.4 Thiết kế

| # | Phần | Thiết kế | Nguồn |
|---|---|---|---|
| G6.1 | Hàng tiêu đề | "Báo cáo" + "Xuất CSV" ở cuối hàng (điện thoại: nút tròn biểu tượng xuất `square.and.arrow.up`, tên trợ năng "Xuất CSV"); lời dẫn dưới | G2.1; [Apple] Toolbars, Icons |
| G6.2 | Khoảng ngày | G6-Q1; "30 ngày" = 30 ngày tới hôm nay (≤ 31, API); khoảng trên URL không khớp ba mục thì là Tùy chọn; lỗi khoảng dưới ô như cũ | [Apple] Segmented controls; G4.4 |
| G6.3 | Số tổng | G6-Q2 | G2/G3 nhóm bo 16 |
| G6.4 | Biểu đồ | Nhóm bo 16; tiêu đề + **dòng tóm tắt** (cả khoảng: "190 đã giao · 12 không giao được · 3 bỏ qua"; đang chọn ngày: số của ngày đó); **chú giải có chữ**; **đường tách** giữa các phần cột chồng; G6-Q3 chọn ngày (cột chọn nổi, cột khác nhạt); nhãn trục ẩn khỏi trình đọc màn hình | §11.2 |
| G6.5 | Tài xế, lý do | Rộng: bảng trong nhóm, cột không vừa thì thành dòng phụ dưới tên (G2.11); điện thoại: danh sách hàng (tên; dòng phụ "N tuyến · đã giao · không giao được…"), bấm mở sheet chi tiết (G3) | G2.11, G3 |

### 11.5 Thứ tự làm

1. Biểu đồ dùng chung: chú giải, đường tách, chọn ngày (chuột, chạm, bàn phím), nhãn trợ năng, tóm tắt; test.
2. Trang Báo cáo: hàng tiêu đề, khoảng ngày, nhóm số, nhóm biểu đồ.
3. Tài xế, lý do: bảng / hàng + sheet.
4. Đo theo APPLE_CHECKLIST; SCREENS, DESIGN_SYSTEM, CURRENT_STATE; e2e.

