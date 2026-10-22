# CONTRIBUTING — Quy trình branch, CI và cấu hình GitHub

## 1. Quy trình branch (D-34)

- Mỗi task một branch `task/Txx-...` tạo từ `develop`, mở PR vào `develop`.
- `develop` → PR vào `main` kèm tag `v*` khi phát hành.
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

## 3. Cấu hình branch protection (chủ dự án tự làm một lần)

Làm cho **cả `develop` và `main`**. Vào **Settings → Rules → Rulesets → New ruleset → New branch ruleset**:

1. **Ruleset name**: `protect-develop` (tương tự `protect-main`). **Enforcement status**: Active.
2. **Target branches → Add target → Include by pattern**: `develop` (hoặc `main`).
3. Bật các rule:
   - **Restrict deletions**.
   - **Require a pull request before merging** (số approval có thể để 0 vì dự án có một người).
   - **Require status checks to pass** → **Add checks** → chọn `ci`. Check chỉ hiện trong danh sách sau khi workflow `ci` đã chạy ít nhất một lần. Có thể thêm `gitleaks` và `dependency-review` nếu muốn chặn cứng.
   - **Block force pushes**.
4. **Create**.

Cách cũ (Settings → Branches → Add classic branch protection rule) cũng được: bật *Require a pull request before merging*, *Require status checks to pass before merging* với check `ci`, và để tắt *Allow force pushes*.

Kiểm tra: push thẳng lên `develop` phải bị từ chối; PR có `ci` đỏ không merge được.

## 4. Khi `gitleaks` báo lỗi

- Secret thật: **thu hồi/đổi secret ngay** (xoá commit không đủ, vì lịch sử đã lên GitHub), rồi xoá khỏi code và đưa vào biến môi trường hoặc `application-dev.yaml` (gitignored).
- Báo nhầm (giá trị giả có chủ đích): thêm fingerprint của finding vào file `.gitleaksignore` ở root, ghi lý do trong PR.
