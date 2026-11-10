# DEPLOYMENT — Runbook deploy thủ công lên VPS

> Không có workflow deploy tự động (D-31). Image do workflow `image` build và đẩy lên GHCR (D-34):
> - Staging: `ghcr.io/huyga1185/pathwise:develop` hoặc `develop-<sha>` (sau mỗi lần merge vào `develop`), profile `staging`.
> - Prod: `ghcr.io/huyga1185/pathwise:x.y.z` (khi push tag `vx.y.z`), profile `prod`.
>
> Mỗi môi trường là một thư mục riêng trên VPS, chứa đúng 2 file: `compose.prod.yaml` và `.env`.

## 1. Chuẩn bị VPS (một lần)

1. Cài Docker Engine kèm Compose plugin v2.24 trở lên (https://docs.docker.com/engine/install/). Kiểm tra: `docker compose version`.
2. Tạo thư mục, ví dụ `/opt/pathwise-prod` (hoặc `/opt/pathwise-staging`), và copy vào đó:
   - `compose.prod.yaml` từ repo (đúng phiên bản muốn deploy).
   - `.env.prod.example` từ repo, đổi tên thành `.env`.
3. Sửa `.env`:
   - `APP_IMAGE_TAG`: `0.1.0` cho prod, `develop` hoặc `develop-<sha>` cho staging.
   - `SPRING_PROFILES_ACTIVE`: `prod` hoặc `staging`.
   - Thay **mọi** giá trị `change-me`. JWT secret: `openssl rand -base64 32`. Mật khẩu DB: `openssl rand -base64 24`.
   - SMTP thật (ví dụ Brevo) và các `SERVICE_IDENTITY_*`.
   - `CORS_ALLOWED_ORIGINS`: origin của web app gọi API từ trình duyệt (đúng scheme và domain, ví dụ `https://app.example.com`, nhiều origin cách nhau dấu phẩy). Để trống thì không origin nào khác gọi được; app mobile không bị ảnh hưởng.
   - `chmod 600 .env`. Không commit file này ở bất kỳ đâu.
4. Đăng nhập GHCR (cần nếu package đang private): tạo Personal Access Token (classic) chỉ với quyền `read:packages` tại GitHub → Settings → Developer settings → Personal access tokens, rồi:
   ```bash
   echo "<PAT>" | docker login ghcr.io -u huyga1185 --password-stdin
   ```
5. Reverse proxy TLS (Caddy hoặc Nginx) nằm ngoài repo. App chỉ lắng nghe ở `127.0.0.1:${APP_PORT}` (mặc định 8080); proxy chuyển `https://<domain>` về đó. **Bắt buộc HTTPS**: cookie refresh token có cờ `Secure`, trình duyệt không gửi qua HTTP. Ví dụ Caddy tối thiểu:
   ```
   pathwise.example.com {
       reverse_proxy 127.0.0.1:8080
   }
   ```
   Profile `staging`/`prod` bật `server.forward-headers-strategy=native`: app lấy IP client và HTTPS từ `X-Forwarded-For`/`X-Forwarded-Proto` của proxy, chỉ khi request tới từ địa chỉ nội bộ (Caddy trên host đi vào container qua docker bridge `172.x`). Proxy phải tự đặt hai header này (Caddy làm mặc định); IP này dùng cho danh sách session và rate limit (F-21).

## 2. Lần deploy đầu

```bash
cd /opt/pathwise-prod
docker compose -f compose.prod.yaml pull
docker compose -f compose.prod.yaml up -d
docker compose -f compose.prod.yaml ps          # mariadb, redis healthy; app healthy sau khoảng 1 phút
curl -s http://127.0.0.1:8080/actuator/health   # {"status":"UP",...}
docker compose -f compose.prod.yaml logs app | grep -E "Flyway|Migrating|Started"
```

Flyway tự tạo schema ở lần chạy đầu. Swagger chỉ có ở staging: `https://<domain>/swagger-ui/index.html`.

## 3. Cập nhật phiên bản

1. **Backup DB trước** (mục 5).
2. Sửa `APP_IMAGE_TAG` trong `.env` sang tag mới (staging dùng `develop` thì chỉ cần pull lại).
3. Nếu bản mới có `compose.prod.yaml` hoặc `.env.prod.example` thay đổi, cập nhật file trên VPS cho khớp (so sánh bằng `diff`).
4. Chạy:
   ```bash
   docker compose -f compose.prod.yaml pull app
   docker compose -f compose.prod.yaml up -d app
   curl -s http://127.0.0.1:8080/actuator/health
   ```

## 4. Rollback

1. Đặt `APP_IMAGE_TAG` về tag cũ đang chạy tốt, rồi `pull app` và `up -d app` như mục 3.
2. **Nếu bản lỗi đã chạy migration Flyway mới**, bản cũ có thể không khớp schema (Hibernate `validate` báo lỗi khi khởi động). Khi đó phải khôi phục bản backup đã tạo trước lần cập nhật (mục 5) rồi mới chạy lại tag cũ. Flyway không có migration "down".

## 5. Backup và khôi phục MariaDB (thủ công)

Backup (chạy trong thư mục deploy, file nén để ngoài volume):

```bash
mkdir -p backups
docker compose -f compose.prod.yaml exec -T mariadb sh -c \
  'mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --routines "$MARIADB_DATABASE"' \
  | gzip > backups/pathwise-$(date +%Y%m%d-%H%M%S).sql.gz
```

Khôi phục (ghi đè dữ liệu hiện tại):

```bash
docker compose -f compose.prod.yaml stop app
gunzip -c backups/<file>.sql.gz | docker compose -f compose.prod.yaml exec -T mariadb sh -c \
  'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" "$MARIADB_DATABASE"'
docker compose -f compose.prod.yaml start app
```

Nên chép file backup ra khỏi VPS định kỳ. Redis không cần backup: mọi key đều có TTL.

## 6. Lệnh thường dùng

| Việc | Lệnh |
|---|---|
| Xem log app | `docker compose -f compose.prod.yaml logs -f app` |
| Khởi động lại app | `docker compose -f compose.prod.yaml restart app` |
| Dừng toàn bộ (giữ dữ liệu) | `docker compose -f compose.prod.yaml down` |
| Dọn image cũ | `docker image prune` |

## 7. OSRM và Nominatim tự host (`compose.routing.yaml`, P-55)

Không dùng server công khai (`router.project-osrm.org`, `nominatim.openstreetmap.org`) cho prod: server demo không cam kết hoạt động, Nominatim công khai giới hạn 1 request/giây. `compose.routing.yaml` chạy OSRM (ma trận quãng đường/thời gian) và Nominatim (tìm toạ độ) bằng dữ liệu Việt Nam của Geofabrik.

### 7.1 Vùng dữ liệu và tài nguyên

Dữ liệu cắt theo khung toạ độ `ROUTING_BBOX=minlon,minlat,maxlon,maxlat` (mặc định `107.80,15.80,108.40,16.25`: Đà Nẵng và Hội An). Điểm giao hay điểm xuất phát nằm ngoài vùng thì OSRM không có đường (tối ưu trả 502 `ROUTING_PROVIDER_ERROR`) và tìm địa chỉ không ra kết quả; mở rộng sang thành phố khác thì nới khung (§7.4). `ROUTING_BBOX=none` giữ cả nước nhưng **dựng OSRM cần hơn 11 GB RAM** (OSRM v26.10 và v6.0 đều bị OOM kill ở 11 GB), không hợp máy nhỏ.

Số đo trên máy dev (AMD Ryzen 5 PRO 4650U, mỗi container giới hạn 1 CPU bằng `cpus: 1`). Một vCPU của VPS (thường là một luồng hyperthread Xeon, chia với khách khác) có thể chậm hơn 2–3 lần, nên coi thời gian dưới đây là mức tốt nhất:

| Vùng | Dựng OSRM (RAM đỉnh / thời gian) | Import Nominatim (RAM đỉnh / thời gian) | RAM khi chạy | Đĩa |
|---|---|---|---|---|
| Đà Nẵng + Hội An (mặc định) | 151 MB / 20 s | ~700 MB / ~5 phút | OSRM 70 MB, Nominatim ~700 MB | ~1,1 GB (gồm file cả nước 330 MB) |
| TP.HCM (`106.35,10.35,107.05,11.20`, chỉ đo OSRM, 2 CPU) | 1,1 GB / 59 s | chưa đo | OSRM 250 MB | OSRM 405 MB |
| Cả nước (`none`) | > 11 GB, OOM | chưa đo | — | — |

Thời gian phục vụ (vùng mặc định, 1 CPU): `/table` 100 điểm khoảng 0,18 s, `/search` khoảng 0,7 s; app cache kết quả tìm địa chỉ 24 h. Lần khởi động sau chỉ mất vài giây vì dữ liệu nằm trong volume.

**Cả app và routing trên một VPS 1 vCPU, 2 GB RAM** (đo bằng Docker-in-Docker giới hạn `--cpus 1 --memory 2g`, không swap, chạy `compose.prod.yaml` + `compose.routing.yaml` với image `develop`, vùng mặc định, `JAVA_OPTS=-XX:MaxRAM=2g`): **chạy được nhưng sát**.
- Một lệnh `up -d` cho cả stack: cắt vùng 100 s, dựng OSRM 87 s, Nominatim sẵn sàng sau 7,5 phút; không container nào bị OOM. RAM tiến trình đỉnh 1,4 GB lúc import, 1,5 GB lúc tải.
- RAM khi chạy: app ~590 MB, Nominatim ~700 MB, MariaDB ~130 MB, OSRM ~75 MB, Redis ~10 MB.
- Plan 100 điểm, 3 tài xế: geocode 50 địa chỉ 6 s; tối ưu 11,6 s (giới hạn thời gian của solver là 10 s); trong lúc tối ưu, request khác của dispatcher trung bình 225 ms, chậm nhất 2,6 s. Tìm địa chỉ lần đầu tới 1,3 s, sau đó dưới 100 ms.
- Tải nặng hơn (3 store, mỗi store 5 tài xế và plan 100 điểm): 3 store tối ưu cùng lúc, mỗi plan 13,9 s, request khác trong lúc đó dưới 100 ms. Sau đó 5 phút giao hàng với 12 tài xế gửi vị trí mỗi 10 s, tải tuyến mỗi 30 s, báo kết quả khoảng 20 s một lần, và 3 dispatcher polling 5 s: không lỗi 5xx; p50 dưới 25 ms, p95 dưới 200 ms trừ tải tuyến của tài xế (690 ms) và tìm địa chỉ mới (4,5 s, khi nhiều request dồn vào 1 CPU); RAM tiến trình đỉnh 1,65 GB, không OOM.
- Phần dư khoảng 350–500 MB phải đủ cho hệ điều hành và dockerd của VPS, nên **bật swap 1–2 GB** làm đệm (lệnh ở §7.2 bước 1). Không chạy vùng lớn hơn một thành phố trên máy này.

Chọn chỗ chạy:

| Máy | Cách chạy |
|---|---|
| VPS của app từ 2 GB RAM, vùng một thành phố | Chạy cùng app (§7.3), có swap. |
| VPS của app không còn RAM, hoặc vùng lớn | Máy routing riêng (§7.2), nối qua **mạng riêng** của nhà cung cấp (VPC/private network) hoặc WireGuard. 1 vCPU đủ cho một thành phố. |

Máy 1 vCPU: đặt `OSRM_THREADS=1` và `NOMINATIM_THREADS=1`. Đo trên máy thật sau khi deploy (thay địa chỉ cho phù hợp):
```bash
P="108.2022,16.0544;108.2208,16.0678;108.3270,15.8801"
curl -s -o /dev/null -w '%{time_total}s\n' "http://127.0.0.1:5000/table/v1/driving/$P?annotations=distance,duration"
curl -s -o /dev/null -w '%{time_total}s\n' -G http://127.0.0.1:8088/search --data-urlencode 'q=Chợ Hàn' -d format=jsonv2
```
Hai số này phải nằm xa dưới timeout đọc của app (10 s).

### 7.2 Máy routing riêng

1. Cài Docker như §1. Tạo `/opt/pathwise-routing`, copy `compose.routing.yaml` và tạo `.env`:
   ```bash
   ROUTING_BIND=10.0.0.5          # IP mạng riêng của máy routing, KHÔNG để 0.0.0.0
   ROUTING_BBOX=107.80,15.80,108.40,16.25   # Đà Nẵng + Hội An, xem §7.1
   NOMINATIM_DB_PASSWORD=<openssl rand -base64 24>
   OSRM_THREADS=1                  # máy 1 vCPU
   NOMINATIM_THREADS=1
   ```
   Máy ít RAM thì bật swap trước khi chạy: `fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile`, thêm `/swapfile none swap sw 0 0` vào `/etc/fstab`.
   OSRM và Nominatim không có xác thực. Docker mở cổng không qua `ufw`, nên chỉ được publish trên IP mạng riêng/VPN; nếu bắt buộc dùng IP công khai thì phải chặn bằng chain `DOCKER-USER` của iptables, chỉ cho IP của VPS app.
2. `docker compose -f compose.routing.yaml up -d`. Lần đầu: `routing-data` tải file cả nước rồi cắt vùng, `osrm-prepare` dựng đồ thị rồi thoát, `osrm` chạy; `nominatim` import (theo dõi bằng `docker compose -f compose.routing.yaml logs -f nominatim`, xong khi log có `Nominatim is ready to accept requests` và `docker compose ps` báo `healthy`). Dòng `Control server error: ... '/root/.gunicorn'` trong log Nominatim là của image, không ảnh hưởng API.
3. Kiểm tra từ VPS app:
   ```bash
   curl "http://10.0.0.5:5000/table/v1/driving/108.2022,16.0544;108.2208,16.0678;108.3270,15.8801?annotations=distance,duration"
   curl "http://10.0.0.5:8088/search?q=L%C3%AA%20Du%E1%BA%A9n%20%C4%90%C3%A0%20N%E1%BA%B5ng&format=jsonv2&limit=1"
   ```
4. Trên VPS app, thêm vào `.env` (xem `.env.prod.example`) rồi `docker compose -f compose.prod.yaml up -d`:
   ```bash
   ROUTING_MATRIX_OSRM_BASEURL=http://10.0.0.5:5000
   ROUTING_MATRIX_OSRM_MAXTABLESIZE=200          # bằng OSRM_MAX_TABLE_SIZE của máy routing (F-41)
   ROUTING_GEOCODING_NOMINATIM_BASEURL=http://10.0.0.5:8088
   ROUTING_GEOCODING_NOMINATIM_RATELIMIT_ENABLED=false
   ```

### 7.3 Cùng máy với app

Copy thêm `compose.routing.yaml` vào thư mục của môi trường, `.env` như trên nhưng `ROUTING_MATRIX_OSRM_BASEURL=http://osrm:5000`, `ROUTING_GEOCODING_NOMINATIM_BASEURL=http://nominatim:8080` (mạng nội bộ của compose), giữ `ROUTING_BIND=127.0.0.1`. Mọi lệnh ở §2–§6 thêm `-f compose.routing.yaml`. Dev làm tương tự với `compose.yaml` (`.env.example`).

### 7.4 Đổi vùng và cập nhật dữ liệu bản đồ

Đổi vùng (ví dụ thêm thành phố khác): sửa `ROUTING_BBOX` trong `.env`, xoá volume của Nominatim (nó chỉ import một lần), rồi chạy lại. `routing-data` và `osrm-prepare` tự nhận ra vùng đã đổi và làm lại, không tải lại file cả nước:
```bash
docker compose -f compose.routing.yaml down
docker volume rm pathwise-routing_nominatim-data
docker compose -f compose.routing.yaml up -d
```

Cập nhật dữ liệu mới (đường mới, địa chỉ mới; không tự cập nhật vì `UPDATE_MODE=none`): xoá cả ba volume để tải lại từ Geofabrik:
```bash
docker compose -f compose.routing.yaml down
docker volume rm pathwise-routing_routing-pbf pathwise-routing_osrm-data pathwise-routing_nominatim-data
docker compose -f compose.routing.yaml up -d
```
Tên volume có tiền tố là tên thư mục/project (xem `docker volume ls`). Làm vào giờ vắng: trong lúc dựng lại, app trả 502/503 khi tối ưu hay tìm toạ độ; dispatcher thử lại sau.
