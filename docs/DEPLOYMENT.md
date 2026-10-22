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
