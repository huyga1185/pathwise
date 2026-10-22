# CONTRIBUTING — Quy trình branch, CI và cấu hình GitHub

## 1. Quy trình branch (D-34)

- Mỗi task một branch `task/Txx-...` tạo từ `develop`, mở PR vào `develop`.
- `develop` → PR vào `main` kèm tag `v*` khi phát hành.
- `develop` và `main` đã bật Ruleset: chỉ merge qua PR, check `ci` phải xanh, cấm force push. Không push thẳng lên hai branch này.
- Commit theo dạng `feat(module): ...`, `fix(module): ...`, `docs: ...`. Không thêm attribution của AI.

## 2. Workflow GitHub Actions (`ARCHITECTURE.md` §12.3)

| Workflow | Chạy khi | Nội dung | Bắt buộc để merge |
|---|---|---|---|
| `ci` | PR vào `develop`/`main`; push lên `develop`/`main` | `./mvnw -B verify` (test bằng Testcontainers + JaCoCo). Artifact `test-reports` và `coverage-report`; bảng coverage trong job summary. | **Có** (check `ci`) |
| `quality` | Mọi PR | `dependency-review` (chặn dependency mới có lỗ hổng high trở lên), `gitleaks` (quét secret trong các commit của PR). | Khuyến nghị |
| Dependabot | Hằng tuần | Cập nhật `maven` (`be/`), `github-actions`, `docker` (`be/Dockerfile`); PR vào `develop`. | — |

- Run cũ của cùng một PR bị huỷ khi có commit mới (concurrency group).
- Coverage hiện chỉ báo cáo, chưa có ngưỡng chặn.
- GitHub chỉ đọc `.github/dependabot.yml` trên branch mặc định (`main`), nên Dependabot bắt đầu chạy sau lần merge `develop` vào `main` đầu tiên.

## 3. Khi `gitleaks` báo lỗi

- Secret thật: **thu hồi/đổi secret ngay** (xoá commit không đủ, vì lịch sử đã lên GitHub), rồi xoá khỏi code và đưa vào biến môi trường hoặc `application-dev.yaml` (gitignored).
- Báo nhầm (giá trị giả có chủ đích): thêm fingerprint của finding vào file `.gitleaksignore` ở root, ghi lý do trong PR.
