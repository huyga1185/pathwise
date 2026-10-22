# PathWise

Backend Spring Boot 4.1 / Java 21 (`be/`) cho SaaS lập tuyến giao hàng đa tenant. Tài liệu thiết kế nằm trong `docs/`, quy tắc cho coding agent ở `AGENTS.md`.

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

## Xem email (OTP, lời mời)

Mọi email khi dev được gửi tới Mailpit: mở http://localhost:8025.

## Troubleshooting

**Flyway báo "Unable to check whether schema is empty" khi dùng MariaDB cài trên máy (không phải container).** Thường xảy ra sau khi nâng cấp MariaDB mà chưa chạy `mariadb-upgrade`, nên bảng hệ thống còn ở định dạng cũ. Chạy `sudo mariadb-upgrade` rồi khởi động lại MariaDB. Khuyến nghị dùng MariaDB trong compose (đã ghim `mariadb:11.8`, D-35) để tránh lệch version.

**Cổng đã bị chiếm.** Đổi `MARIADB_PORT`, `REDIS_PORT`, `MAILPIT_*_PORT` hoặc `APP_PORT` trong `.env`, rồi sửa `application-dev.yaml` cho khớp.
