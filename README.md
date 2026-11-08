# PathWise

SaaS lập tuyến giao hàng đa tenant: backend Spring Boot 4.1 / Java 21 (`be/`), web dispatcher (`web/`) và app tài xế (`mobile/`) (client đang dựng, xem mục Client). Tài liệu thiết kế nằm trong `docs/`, quy tắc cho coding agent ở `AGENTS.md`.

## Yêu cầu môi trường

- Docker (kèm Docker Compose v2.24 trở lên).
- JDK 21 nếu chạy app từ IDE hoặc `./mvnw`. Không cần cài MariaDB, Redis hay SMTP.

## Cách 1: hạ tầng trong container, app chạy từ IDE (mặc định khi dev)

```bash
cp .env.example .env
cp be/src/main/resources/application-dev.example.yaml be/src/main/resources/application-dev.yaml
docker compose up -d                 # MariaDB 11.8 (cổng 3307), Redis (6380), Mailpit (1025 / 8025)
cd be && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.yaml` đã được gitignore, có thể sửa thoải mái (secret thật chỉ để ở đây hoặc trong `.env`). Nếu đổi cổng hay mật khẩu trong `.env` thì sửa `application-dev.yaml` cho khớp.

## Cách 2: chạy toàn bộ stack trong container

```bash
cp .env.example .env
docker compose --profile app up -d --build
curl http://localhost:8080/actuator/health   # {"status":"UP"} sau khoảng 1 phút
```

App được build từ `be/Dockerfile` (multi-stage), chạy bằng user không phải root, mọi cấu hình đi qua biến môi trường trong `.env`. API nằm dưới `http://localhost:8080/api/v1`.

Dừng: `docker compose --profile app down`. Thêm `-v` để xoá luôn dữ liệu MariaDB và Redis.

## Chạy test

```bash
cd be && ./mvnw test
```

Chỉ cần Docker: test tích hợp tự bật MariaDB và Redis bằng Testcontainers, không dùng stack ở trên.

## Client

Kế hoạch, kiến trúc và quy tắc của web và app tài xế: `docs/client/` (bắt đầu từ `docs/client/README.md`). Hai client dùng chung:

- `api/openapi.json`: hợp đồng API máy đọc, sinh từ backend. Đổi API thì chạy `scripts/sync_openapi.sh` (cần Docker) và commit file này cùng PR; CI fail khi file lệch backend.
- `brand/design-system/tokens.json`: token giao diện. `python3 scripts/build_tokens.py` sinh theme cho web và mobile; `--check` kiểm file sinh còn khớp.
- Test của các script: `python3 -m unittest discover -s scripts/tests`.

Web dispatcher (`web/`, Node 24+, pnpm theo `packageManager`):

```bash
cd web && pnpm install
pnpm dev          # http://localhost:5173/app/, proxy /api tới backend ở :8080
pnpm test         # Vitest + MSW, không cần backend
pnpm lint && pnpm typecheck && pnpm build
```

App tài xế (`mobile/`, Flutter theo `mobile/.fvmrc`):

```bash
cd mobile && flutter pub get
flutter run --dart-define=API_BASE=http://10.0.2.2:8080   # emulator Android tới backend trên máy
flutter analyze && flutter test
../scripts/gen_dart_api.sh        # sinh lại client sau khi đổi api/openapi.json (cần Docker)
../scripts/build_mobile_brand.sh  # sinh lại icon/logo từ brand/logo (cần rsvg-convert)
```

E2E (Playwright, cần Docker): stack riêng `pathwise-e2e` (cổng trong `web/e2e/e2e.env`, không đụng DB/Mailpit dev; server giả thay Nominatim và OSRM, P-49):

```bash
export E2E_JWT_SECRET="$(openssl rand -base64 32)"   # sinh mỗi lần, không commit
docker compose -p pathwise-e2e --env-file web/e2e/e2e.env -f compose.yaml -f compose.e2e.yaml --profile app up -d --build --wait
cd web && pnpm exec playwright install chromium   # lần đầu
pnpm e2e          # build web, vite preview :4173 proxy /api tới :8091, chạy web/e2e/
docker compose -p pathwise-e2e --env-file web/e2e/e2e.env -f compose.yaml -f compose.e2e.yaml --profile app down -v
```

## Xem email (OTP, lời mời)

Mọi email khi dev được gửi tới Mailpit: mở http://localhost:8025.

## Đóng góp

Quy trình branch và CI: `docs/CONTRIBUTING.md`.

Deploy lên VPS (staging/prod): `docs/DEPLOYMENT.md`.

## Troubleshooting

**Flyway báo "Unable to check whether schema is empty" khi dùng MariaDB cài trên máy (không phải container).** Thường xảy ra sau khi nâng cấp MariaDB mà chưa chạy `mariadb-upgrade`, nên bảng hệ thống còn ở định dạng cũ. Chạy `sudo mariadb-upgrade` rồi khởi động lại MariaDB. Khuyến nghị dùng MariaDB trong compose (đã ghim `mariadb:11.8`, D-35) để tránh lệch version.

**Cổng đã bị chiếm.** Đổi `MARIADB_PORT`, `REDIS_PORT`, `MAILPIT_*_PORT` hoặc `APP_PORT` trong `.env`, rồi sửa `application-dev.yaml` cho khớp.
