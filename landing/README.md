# Landing

Trang giới thiệu PathWise ở `/` (P-47, task W14 trong `docs/client/PLAN.md`). HTML, CSS và JS viết tay trong một file, không build, không framework.

- `index.html`: toàn bộ trang. Màu theo token của `brand/design-system/tokens.json`, có chế độ tối, tôn trọng "giảm chuyển động".
- `web/1..10.webp`: màn web điều phối ở khổ máy tính (1440×900) cho phần "Thử điều phối một chuyến giao hàng"; `mweb/1..10.webp`: cùng các màn ở khổ điện thoại (chụp 390×844, lưu 540×1169), dùng khi màn hẹp hơn 760 px.
- `phone/1..6.webp`: màn app tài xế cho phần "Thử một ngày giao hàng".
- `img/bao-cao.webp`: màn báo cáo.

Ảnh chụp từ stack e2e với dữ liệu thử. Toạ độ vùng bấm của mỗi màn nằm trong `index.html` (đối tượng `S` của hai phần bấm thử, tính bằng % ảnh), chụp lại màn thì đo lại toạ độ.

Xem thử: `cd landing && python3 -m http.server 8000` rồi mở http://localhost:8000/. Favicon trỏ `/app/...` nên chỉ hiện khi chạy sau Caddy.

Còn lại của W14: Caddy phục vụ `/`, `robots.txt`, `sitemap.xml`, `noindex` cho `/app/`, `og.png`, link APK cho nút "Tải app tài xế cho Android" (`id="apk-link"`).
