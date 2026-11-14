# DEPLOYMENT — Runbook deploy thủ công lên VPS

> Không có workflow deploy tự động (D-31). Image do workflow `image` build và đẩy lên GHCR (D-34):
> - Staging: `ghcr.io/huyga1185/pathwise:develop` hoặc `develop-<sha>` (sau mỗi lần merge vào `develop`), profile `staging`.
> - Prod: `ghcr.io/huyga1185/pathwise:x.y.z` (khi push tag `vx.y.z`), profile `prod`.
> - Web dispatcher kèm Caddy: `ghcr.io/huyga1185/pathwise-web` với cùng tag (W08, P-56).
>
> Mỗi môi trường là một thư mục riêng trên VPS, chứa `compose.prod.yaml`, `.env` và tuỳ chọn `compose.backup.yaml` (backup tự động, §5, nên có), `compose.web.yaml` (Caddy trong container, §8.1), `compose.routing.yaml` (§7). Giám sát và cảnh báo: §9.

## 1. Chuẩn bị VPS (một lần)

1. Cài Docker Engine kèm Compose plugin v2.24 trở lên (https://docs.docker.com/engine/install/). Kiểm tra: `docker compose version`.
2. Tạo thư mục, ví dụ `/opt/pathwise-prod` (hoặc `/opt/pathwise-staging`), và copy vào đó:
   - `compose.prod.yaml` từ repo (đúng phiên bản muốn deploy), cùng `compose.web.yaml` nếu chạy Caddy trong container (§8.1).
   - `.env.prod.example` từ repo, đổi tên thành `.env`.
3. Sửa `.env`:
   - `APP_IMAGE_TAG`: `0.1.0` cho prod, `develop` hoặc `develop-<sha>` cho staging.
   - `SPRING_PROFILES_ACTIVE`: `prod` hoặc `staging`.
   - Thay **mọi** giá trị `change-me`. JWT secret: `openssl rand -base64 32`. Mật khẩu DB: `openssl rand -base64 24`.
   - SMTP thật (ví dụ Brevo) và các `SERVICE_IDENTITY_*`. `SERVICE_IDENTITY_APP_URL` là gốc của domain, ví dụ `https://pathwise.example.com` (không thêm `/app`): link đặt lại mật khẩu ADMIN gửi (T57) là `{SERVICE_IDENTITY_APP_URL}/app/reset-password?token=…`.
   - `PATHWISE_DOMAIN` (domain trỏ về VPS, ví dụ `pathwise.example.com`), `MAPTILER_KEY` (key bản đồ nền của web, giới hạn domain trong trang quản lý MapTiler): xem §8.
   - `CORS_ALLOWED_ORIGINS`: web chạy cùng origin với API (P-47) nên để trống; chỉ điền khi có web ở origin khác (đúng scheme và domain, nhiều origin cách nhau dấu phẩy). App mobile không bị ảnh hưởng.
   - `chmod 600 .env`. Không commit file này ở bất kỳ đâu.
4. Đăng nhập GHCR (cần nếu package đang private): tạo Personal Access Token (classic) chỉ với quyền `read:packages` tại GitHub → Settings → Developer settings → Personal access tokens, rồi:
   ```bash
   echo "<PAT>" | docker login ghcr.io -u huyga1185 --password-stdin
   ```
5. HTTPS và web dispatcher: Caddy, chạy trong container (§8.1) hoặc cài trên host (§8.2). App chỉ lắng nghe ở `127.0.0.1:${APP_PORT}` (mặc định 8080). **Bắt buộc HTTPS**: cookie refresh token có cờ `Secure`, trình duyệt không gửi qua HTTP.
   Profile `staging`/`prod` bật `server.forward-headers-strategy=native`: app lấy IP client và HTTPS từ `X-Forwarded-For`/`X-Forwarded-Proto` của proxy, chỉ khi request tới từ địa chỉ nội bộ (Caddy trong container đi qua mạng của compose, Caddy trên host đi vào container qua docker bridge, đều là `172.x`). Proxy phải tự đặt hai header này (Caddy làm mặc định); IP này dùng cho danh sách session và rate limit (F-21).

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

**ADMIN đầu tiên (T59).** Trước lần `up -d` đầu, thêm vào `.env`:

```bash
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
# Tuỳ chọn, 8–72 ký tự. Bỏ trống: app gửi email mã đặt lại mật khẩu (hạn 5 phút; quá hạn thì dùng "Quên mật khẩu" trên web).
BOOTSTRAP_ADMIN_PASSWORD=
BOOTSTRAP_ADMIN_FULL_NAME=Quản trị
```

Khi khởi động, nếu chưa có ADMIN nào, app tạo một ADMIN đã xác thực email và log `First ADMIN created`:
```bash
docker compose -f compose.prod.yaml logs app | grep -i admin
```
Đã có ADMIN thì app bỏ qua và log cảnh báo nhắc xoá biến. Đăng nhập được rồi thì **xoá cả ba dòng khỏi `.env`** (nhất là mật khẩu) và `up -d app`. Email đã thuộc tài khoản khác hoặc mật khẩu sai độ dài: app vẫn chạy, log lỗi, không tạo gì; sửa `.env` rồi `up -d app`. Cách dự phòng bằng SQL: `be/db/seed-admin.example.sql`.

Caddy trong container (§8.1): mọi lệnh `docker compose` ở mục này và các mục sau thêm `-f compose.web.yaml` sau `-f compose.prod.yaml`; lần cập nhật phiên bản thì `pull`/`up -d` cả `app web`.

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

## 5. Backup và khôi phục MariaDB (`compose.backup.yaml`, T52)

### 5.1 Backup tự động

Copy `compose.backup.yaml` vào thư mục của môi trường và chạy kèm (mọi lệnh `docker compose` ở các mục khác cũng thêm `-f compose.backup.yaml`):
```bash
docker compose -f compose.prod.yaml -f compose.backup.yaml up -d
```
- Service `backup`: mỗi đêm lúc `BACKUP_TIME` (mặc định 02:30, giờ Việt Nam) dump DB (`mariadb-dump --single-transaction`, kèm `CREATE DATABASE`), nén gzip, kiểm file nén, ghi vào `./backups/daily/pathwise-<ngày-giờ>.sql.gz` kèm `.sha256`; giữ `BACKUP_KEEP_DAILY` bản mới nhất (7). Ngày `BACKUP_WEEKLY_DAY` (7 = Chủ nhật) chép thêm vào `./backups/weekly/`, giữ `BACKUP_KEEP_WEEKLY` bản (4).
- Mã hoá (tuỳ chọn): đặt `BACKUP_PASSPHRASE` (ví dụ `openssl rand -base64 32`) thì file thành `.sql.gz.enc` (AES-256, `openssl enc -pbkdf2`). **Mất passphrase là mất backup**: lưu nó ở chỗ khác VPS (trình quản lý mật khẩu).
- Trạng thái cho giám sát (T54): `./backups/status/last-success` (giờ, tên file, kích thước), `last-error` khi lần gần nhất thất bại, `last-offsite-success`/`last-offsite-error` của bản đẩy ra ngoài.
- Backup ngay một lần: `docker compose -f compose.prod.yaml -f compose.backup.yaml run --rm -e BACKUP_ONCE=true backup`.
- Xem log: `docker compose -f compose.prod.yaml -f compose.backup.yaml logs backup` (dòng `next backup at …` và `ok`/`FAILED`).

### 5.2 Đẩy backup ra ngoài VPS (S3 tương thích)

Service `backup-offsite` mỗi giờ (`BACKUP_S3_EVERY` giây) **chép** `./backups/daily` và `./backups/weekly` lên bucket bằng rclone; không đặt `BACKUP_S3_BUCKET` thì không làm gì (backup chỉ nằm trên VPS). Nó chỉ chép, không xoá trên bucket: đặt luật tự xoá trên bucket (ví dụ sau 60 ngày) để không tốn dung lượng.

Cloudflare R2: tạo bucket, tạo API token quyền "Object Read & Write" chỉ cho bucket đó, rồi trong `.env`:
```bash
BACKUP_S3_BUCKET=pathwise-backups
BACKUP_S3_PROVIDER=Cloudflare
BACKUP_S3_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
BACKUP_S3_ACCESS_KEY_ID=<access key>
BACKUP_S3_SECRET_ACCESS_KEY=<secret>
```
Backblaze B2: `BACKUP_S3_PROVIDER=Other`, `BACKUP_S3_ENDPOINT=https://s3.<region>.backblazeb2.com`, `BACKUP_S3_REGION=<region>`, application key chỉ cho bucket. Đổi `.env` xong: `up -d backup-offsite`; kiểm `./backups/status/last-offsite-success` có giờ mới.

### 5.3 Khôi phục

Ghi đè dữ liệu hiện tại; dừng app trước. File thường:
```bash
docker compose -f compose.prod.yaml stop app
gunzip -c backups/daily/<file>.sql.gz | docker compose -f compose.prod.yaml exec -T mariadb sh -c \
  'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'
docker compose -f compose.prod.yaml start app
```
File mã hoá (`.enc`): thay dòng `gunzip` bằng
```bash
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSPHRASE -in backups/daily/<file>.sql.gz.enc | gunzip -c | …
```
(`export BACKUP_PASSPHRASE=…` trước; không có `openssl` trên host thì chạy trong `docker run --rm -i mariadb:11.8`). Kiểm toàn vẹn trước khi khôi phục: `cd backups/daily && sha256sum -c <file>.sha256`.

### 5.4 Tập khôi phục (mỗi tháng)

Một backup chưa từng khôi phục thử thì chưa chắc dùng được. Khôi phục vào một MariaDB tạm, không đụng DB thật:
```bash
docker run -d --name restore-drill -e MARIADB_ROOT_PASSWORD=drill mariadb:11.8
sleep 20
gunzip -c backups/daily/<file mới nhất>.sql.gz | docker exec -i restore-drill mariadb -uroot -pdrill
docker exec restore-drill mariadb -uroot -pdrill -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='pathwise'; SELECT COUNT(*) FROM pathwise.route_plans;"
docker rm -f restore-drill
```
So số bảng và số plan với DB thật (`docker compose -f compose.prod.yaml exec mariadb sh -c 'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" -N -e "…"'`). Làm thêm một lần với file tải về từ bucket.

Redis không cần backup: mọi key đều có TTL.

### 5.5 Dọn dữ liệu cũ (T53)

App tự xoá dòng cũ mỗi đêm lúc 03:30 giờ Việt Nam (sau backup 02:30): refresh token hết hạn hoặc bị thu hồi quá 30 ngày, job nền đã kết thúc quá 30 ngày, lời mời hết hạn hoặc bị huỷ quá 90 ngày (lời mời đã nhận giữ lại), nhật ký thao tác quá 1 năm (T58). Quy tắc đủ ở DATA_MODEL §6. Mỗi lần chạy log số dòng đã xoá của từng loại:

```bash
docker compose -f compose.prod.yaml logs app | grep -i housekeeping
```

Đổi lịch hoặc số ngày trong `.env` (app đọc `.env`), rồi `up -d` lại app:

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `HOUSEKEEPING_CRON` | `0 30 3 * * *` | cron Spring 6 trường; `-` tắt hẳn |
| `HOUSEKEEPING_ZONE` | `Asia/Ho_Chi_Minh` | |
| `SPRING_APPLICATION_JSON` | — | số ngày giữ, ví dụ `{"housekeeping":{"retention":{"jobs":"60d","invitations":"180d","refresh-tokens":"30d","audit-events":"730d"}}}`. Dùng JSON vì tên `refresh-tokens` có gạch nối không viết được thành biến môi trường thường. |

Dòng đã xoá chỉ lấy lại được từ backup.

## 6. Lệnh thường dùng

| Việc | Lệnh |
|---|---|
| Xem log app | `docker compose -f compose.prod.yaml logs -f app` |
| Khởi động lại app | `docker compose -f compose.prod.yaml restart app` |
| Dừng toàn bộ (giữ dữ liệu) | `docker compose -f compose.prod.yaml down` |
| Dọn image cũ | `docker image prune` |
| Log trong 1 giờ qua | `docker compose -f compose.prod.yaml logs --since 1h app` |
| Dung lượng log của các container | `sudo du -ch /var/lib/docker/containers/*/*-json.log \| tail -1` |

**Giới hạn log (T51):** mọi service trong các file compose giữ tối đa `LOG_MAX_FILE` file × `LOG_MAX_SIZE` (mặc định 3 × 10 MB, tức 30 MB mỗi container); dòng cũ hơn bị bỏ. Muốn giữ nhiều hơn để điều tra thì tăng hai biến trong `.env` rồi `up -d` lại (container được tạo lại mới nhận giới hạn mới). Log đã bị bỏ không lấy lại được: cần lưu lâu thì gom log ra ngoài (chưa làm). Kiểm tra một container: `docker inspect -f '{{json .HostConfig.LogConfig}}' <container>`.

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
- **VPS 2 GB thật** còn khoảng 1,75 GB cho container: Ubuntu kèm Docker, ufw, ufw-docker và CrowdSec dùng sẵn ~270 MB khi chưa chạy container. Đo lại với giới hạn 1.776 MB, không swap, bật các giới hạn RAM ở khối cuối `.env.prod.example` (`JAVA_OPTS=-Xmx384m …`, `MARIADB_INNODB_BUFFER_POOL_SIZE=64M`, `shared_buffers` Nominatim 128 MB), cài routing trước rồi mới bật app: routing sẵn sàng sau 8 phút (đỉnh 670 MB), app healthy sau 92 s, RAM tiến trình lúc nghỉ 1,37 GB. Cùng bài tải nặng: không lỗi 5xx, không OOM, đỉnh **1,68 GB**; 3 store tối ưu cùng lúc 12,4 s; p95 dưới 300 ms trừ tìm địa chỉ mới (2,4 s) và bật chia sẻ vị trí (0,4 s).
- Phần dư chỉ còn ~100 MB và page cache gần như hết, nên trên VPS 2 GB **bắt buộc bật swap 2 GB** (lệnh ở §7.2 bước 1) và dùng các giới hạn RAM trên. Các giới hạn này chặn RAM tăng dần theo thời gian chạy (heap JVM, cache InnoDB, PostgreSQL), chứ không làm số đo lúc tải nhỏ đi đáng kể. Không chạy vùng lớn hơn một thành phố trên máy này; nhiều store hơn thì lên 4 GB hoặc tách routing.

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
   # Dẫn đường trong app (T33) dùng cùng OSRM; chỉ đặt ROUTING_DIRECTIONS_OSRM_BASEURL khi muốn server khác.
   ROUTING_GEOCODING_NOMINATIM_BASEURL=http://10.0.0.5:8088
   ROUTING_GEOCODING_NOMINATIM_RATELIMIT_ENABLED=false
   ```

### 7.3 Cùng máy với app

Lần cài đầu trên máy nhỏ, chạy routing trước để import Nominatim không tranh RAM với app: `docker compose -f compose.routing.yaml up -d`, chờ `docker compose -f compose.routing.yaml ps` báo `nominatim` `healthy`, rồi mới chạy lệnh ghép bên dưới. Copy thêm `compose.routing.yaml` vào thư mục của môi trường, `.env` như trên nhưng `ROUTING_MATRIX_OSRM_BASEURL=http://osrm:5000`, `ROUTING_GEOCODING_NOMINATIM_BASEURL=http://nominatim:8080` (mạng nội bộ của compose), giữ `ROUTING_BIND=127.0.0.1`. Mọi lệnh ở §2–§6 thêm `-f compose.routing.yaml`. Dev làm tương tự với `compose.yaml` (`.env.example`).

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

## 8. Web dispatcher và HTTPS (Caddy, W08, P-56)

Caddy phục vụ web dispatcher ở `https://<domain>/app/`, chuyển `/api/` (và Swagger ở staging) về app, tự lấy chứng chỉ Let's Encrypt cho `PATHWISE_DOMAIN`. `/` chuyển sang `/app/` cho tới khi có trang landing (P-47); `/actuator` không mở ra ngoài. Cấu hình chung ở `deploy/caddy/Caddyfile`, dùng cho cả hai cách dưới.

Trước khi bắt đầu: bản ghi DNS `A` của domain trỏ về IP của VPS, cổng 80 và 443 (TCP, thêm 443/UDP cho HTTP/3) mở từ internet. Let's Encrypt kiểm tra domain qua cổng 80/443, nên chứng chỉ chỉ cấp được khi DNS đã trỏ đúng.

Bản đồ nền của web cần key MapTiler (`MAPTILER_KEY`), đọc lúc chạy từ `/app/config.js` nên đổi key không cần build lại. Key nằm trong trình duyệt của người dùng, nên ở trang quản lý MapTiler phải giới hạn key theo domain. Không có key thì bản đồ không có nền, điểm giao vẫn hiện.

### 8.1 Caddy trong container (`compose.web.yaml`)

1. Copy `compose.web.yaml` vào thư mục của môi trường. Trong `.env`: `PATHWISE_DOMAIN`, `MAPTILER_KEY` (và `WEB_BASEMAP` nếu khác `maptiler`). Image web lấy cùng `APP_IMAGE_TAG` với app.
2. Mở cổng cho container. Với `ufw-docker`, `ufw allow` thường không áp cho container; sau lần `up -d` đầu:
   ```bash
   ufw-docker allow pathwise-prod-web-1 80/tcp
   ufw-docker allow pathwise-prod-web-1 443/tcp
   ufw-docker allow pathwise-prod-web-1 443/udp
   ```
   (Tên container theo `docker compose ps`; thư mục `/opt/pathwise-prod` cho tên project `pathwise-prod`.)
3. Chạy:
   ```bash
   docker compose -f compose.prod.yaml -f compose.web.yaml pull
   docker compose -f compose.prod.yaml -f compose.web.yaml up -d
   docker compose -f compose.prod.yaml -f compose.web.yaml logs -f web   # "certificate obtained successfully"
   curl -sI https://<domain>/app/ | head -1                               # HTTP/2 200
   ```
4. Chứng chỉ nằm trong volume `caddy-data`: không xoá volume này, nếu không mỗi lần tạo lại container sẽ xin chứng chỉ mới và dễ chạm giới hạn của Let's Encrypt.

### 8.2 Caddy cài trên host

Dùng khi VPS đã có Caddy (nhiều site). Web lấy từ tarball đính kèm GitHub Release (`pathwise-web-<tag>.tar.gz`, workflow `release` tạo khi publish release), gồm `app/` và `Caddyfile`.

1. Cài Caddy theo https://caddyserver.com/docs/install. Giải nén tarball: `mkdir -p /var/www/pathwise && tar -xzf pathwise-web-<tag>.tar.gz --strip-components=1 -C /var/www/pathwise` (có `/var/www/pathwise/app/index.html`).
2. Sửa `/var/www/pathwise/app/config.js` để có key bản đồ:
   ```js
   window.__PATHWISE_CONFIG__ = {"basemap":"maptiler","maptilerKey":"<key>"};
   ```
3. Copy `Caddyfile` của tarball vào `/etc/caddy/Caddyfile` (hoặc `import` nó từ Caddyfile sẵn có), rồi khai báo biến cho service:
   ```bash
   systemctl edit caddy
   # [Service]
   # Environment=PATHWISE_DOMAIN=pathwise.example.com PATHWISE_UPSTREAM=127.0.0.1:8080 PATHWISE_WEB_ROOT=/var/www/pathwise
   systemctl restart caddy
   ```
4. Mở cổng: `ufw allow 80/tcp`, `ufw allow 443/tcp`, `ufw allow 443/udp` (Caddy trên host không qua Docker nên dùng `ufw` thường).
5. Cập nhật web (không cần restart Caddy); tarball có `config.js` rỗng nên giữ lại bản đã sửa:
   ```bash
   cp /var/www/pathwise/app/config.js /tmp/pathwise-config.js
   rm -r /var/www/pathwise/app
   tar -xzf pathwise-web-<tag>.tar.gz --strip-components=1 -C /var/www/pathwise
   cp /tmp/pathwise-config.js /var/www/pathwise/app/config.js
   ```

## 9. Giám sát và cảnh báo (T54)

Hai lớp, không cần Prometheus/Grafana:

| Lớp | Bắt được | Không bắt được |
|---|---|---|
| Uptime bên ngoài (§9.1) | VPS tắt, mất mạng, Caddy/app chết, DB/Redis chết (health 503), chứng chỉ hết hạn | đĩa sắp đầy, backup lỗi |
| Script trên VPS (§9.2) | đĩa, RAM, container dừng/unhealthy/tự khởi động lại, health, backup và bản chép lên S3 | VPS tắt hẳn, SMTP chết (không gửi được email) |

### 9.1 Uptime bên ngoài

`https://<domain>/actuator/health` công khai qua Caddy, trả `{"status":"UP",…}` hoặc 503 khi DB/Redis lỗi; không có chi tiết nào khác.

1. Đăng ký gói miễn phí của UptimeRobot (https://uptimerobot.com) hoặc Better Stack (https://betterstack.com/uptime).
2. Tạo monitor **HTTP(s) – Keyword**: URL `https://<domain>/actuator/health`, keyword `"status":"UP"`, chu kỳ 5 phút, báo khi keyword **không** có.
3. Thêm monitor HTTP thường cho `https://<domain>/app/` (web và Caddy), và bật cảnh báo chứng chỉ nếu dịch vụ có.
4. Kênh cảnh báo: email (khác hộp thư của SMTP gửi đi nếu được), thêm app điện thoại của dịch vụ nếu muốn.
5. Thử: `docker compose -f compose.prod.yaml stop app`, chờ báo, rồi `start app`.

### 9.2 Script `deploy/monitor/pathwise-monitor.sh`

Cron trên host chạy mỗi 5 phút, gửi email qua SMTP của app (`SPRING_MAIL_*` trong `.env`, bằng `curl`). Mỗi sự cố báo **một lần** khi bắt đầu, nhắc lại sau `MONITOR_REMIND_HOURS` (mặc định 6 giờ) nếu còn, và báo khi đã hết. Kiểm tra:

- đĩa của thư mục môi trường và của Docker > `MONITOR_DISK_PERCENT` (85 %);
- RAM khả dụng < `MONITOR_MEM_AVAILABLE_PERCENT` (10 %);
- container của project: dừng với mã thoát khác 0 (kèm hết RAM nếu bị OOM kill), đang restart, unhealthy, hoặc đã tự khởi động lại kể từ lần trước; service trong `MONITOR_SERVICES` (`mariadb redis app`) không chạy. Container chạy một lần rồi thoát 0 (`routing-data`, `osrm-prepare`) là bình thường;
- `http://127.0.0.1:${APP_PORT}/actuator/health` không `UP`;
- có `compose.backup.yaml` hoặc `./backups/status`: `last-error` có mặt, bản thành công gần nhất cũ hơn `MONITOR_BACKUP_MAX_AGE_HOURS` (26 giờ); có `BACKUP_S3_BUCKET`: `last-offsite-error`, lần chép thành công gần nhất cũ hơn `MONITOR_OFFSITE_MAX_AGE_HOURS` (3 giờ).

Cài (cần `bash`, `curl`, `docker`, `flock`; có sẵn trên Ubuntu/Debian):

```bash
cd /opt/pathwise-prod
mkdir -p monitor
curl -fsSLo monitor/pathwise-monitor.sh https://raw.githubusercontent.com/huyga1185/pathwise/<tag>/deploy/monitor/pathwise-monitor.sh
chmod 700 monitor/pathwise-monitor.sh
# Thêm vào .env: MONITOR_ALERT_EMAIL=ban@example.com  (các biến khác xem đầu script và .env.prod.example)
./monitor/pathwise-monitor.sh --test-email /opt/pathwise-prod   # phải nhận được email
./monitor/pathwise-monitor.sh --dry-run /opt/pathwise-prod      # in sự cố hiện có, không gửi; "OK" là sạch
echo '*/5 * * * * root /opt/pathwise-prod/monitor/pathwise-monitor.sh /opt/pathwise-prod >> /var/log/pathwise-monitor.log 2>&1' \
  | sudo tee /etc/cron.d/pathwise-monitor
```

Script chạy bằng root vì `.env` là `chmod 600` và cần quyền Docker; nó chỉ đọc `.env` (không chạy file này) và chỉ ghi vào `.monitor/` (trạng thái các sự cố đang mở) và log ở trên. Log chỉ có dòng khi gửi cảnh báo hoặc gửi lỗi. Mỗi môi trường (prod, staging) một dòng cron với thư mục riêng; `MONITOR_NAME` đặt tên trong tiêu đề email (mặc định là tên thư mục).

Khi nhận email: làm theo lệnh gợi ý trong email (`docker logs`, `docker stats`, log của `backup`). Đĩa đầy thường do image cũ (`docker image prune`), backup (`BACKUP_KEEP_*`) hoặc dữ liệu Nominatim. Gửi lỗi thì script ghi `alert email failed` vào log và thử lại ở lần sau.

### 9.3 Lỗi trong log của app

Mỗi lần gửi email lỗi, OSRM/Nominatim lỗi, hoặc job nền kết thúc `FAILED`, app ghi một dòng cố định (T54, ARCHITECTURE §10):

```
WARN … failure source=ROUTING operation=osrm.table reason=unreachable message="OSRM table request failed"
WARN … failure source=JOB operation=OPTIMIZE_PLAN reason=ROUTING_PROVIDER_ERROR jobId=42
```

```bash
docker compose -f compose.prod.yaml logs --since 24h app | grep 'failure source='                  # mọi lỗi trong 24 giờ
docker compose -f compose.prod.yaml logs --since 24h app | grep -o 'failure source=[A-Z]*' | sort | uniq -c
```

`source`: `EMAIL` (SMTP; `authentication_failed` là sai tài khoản SMTP), `ROUTING` (OSRM; `unreachable` là không kết nối được, xem §7), `GEOCODING` (Nominatim), `JOB` (`reason` là mã lỗi của job, ví dụ `INTERRUPTED` sau khi app khởi động lại).
