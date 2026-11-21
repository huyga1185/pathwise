# Landing

Trang giới thiệu PathWise ở `/` (P-47, task W14 trong `docs/client/PLAN.md`). HTML, CSS và JS viết tay trong một file, không build, không framework.

- `index.html`: toàn bộ trang. Màu theo token của `brand/design-system/tokens.json`, có chế độ tối, tôn trọng "giảm chuyển động".
- `web/1..10.webp`: màn web điều phối ở khổ máy tính (1440×900) cho phần "Thử điều phối một chuyến giao hàng"; `mweb/1..10.webp`: cùng các màn ở khổ điện thoại (chụp 390×844, lưu 540×1169), dùng khi màn hẹp hơn 760 px.
- `phone/1..6.webp`: màn app tài xế cho phần "Thử một ngày giao hàng".
- `img/bao-cao.webp`: màn báo cáo.

Ảnh chụp từ stack e2e với dữ liệu thử. Toạ độ vùng bấm của mỗi màn nằm trong `index.html` (đối tượng `S` của hai phần bấm thử, tính bằng % ảnh), chụp lại màn thì đo lại toạ độ.

- `404.html`: trang "Không tìm thấy trang", Caddy trả kèm mã 404 cho mọi địa chỉ ngoài landing, `/app/` và API.
- `robots.txt`, `sitemap.xml`, `og.png` (ảnh chia sẻ 1200×630).

Caddy (`deploy/caddy/Caddyfile`) chỉ phục vụ đúng các file trên (README này thì không). `index.html` là template của Caddy: nút "Tải app tài xế cho Android" chỉ có khi đặt `PATHWISE_APK_URL` (P-79, `docs/DEPLOYMENT.md` §8). Landing đi chung image `pathwise-web` và tarball của release.

Xem thử đúng như prod (có template, 404, favicon): build image web rồi chạy

```bash
docker build -f web/Dockerfile -t pathwise-web:local .
docker run --rm -p 8080:80 -e PATHWISE_DOMAIN=http://localhost:80 -e PATHWISE_APK_URL=https://example.com/app.apk pathwise-web:local
```

rồi mở http://localhost:8080/ (phải là `localhost`, Caddy chỉ trả lời đúng domain). Xem nhanh không cần Caddy: `cd landing && python3 -m http.server 8000`; khi đó chỗ nút tải app hiện nguyên chữ template `{{...}}`.
