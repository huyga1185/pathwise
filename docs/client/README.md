# Client — Quy tắc cho agent làm `web/` và `mobile/`

> Điểm vào cho mọi phiên làm client. Quy tắc chung của repo (giao tiếp tiếng Việt, commit, không attribution AI, không push khi chưa được yêu cầu, file nóng) vẫn theo `AGENTS.md` ở gốc; file này thêm phần riêng của client.

## 1. Đọc trước khi làm

1. Task của bạn trong `docs/client/PLAN.md`: phạm vi, ngoài phạm vi, acceptance, phụ thuộc.
2. `docs/client/ARCHITECTURE.md`: lớp, quy tắc phụ thuộc, port, auth, polling, lỗi, bản đồ.
3. Màn hình của task trong `docs/client/SCREENS.md`, rồi mở artboard tương ứng trong prototype (`https://claude.ai/artifact/JPW7uzXLFrpqfgFmQVjs5N`, xem bằng Artifact tool `read` nếu cần mã nguồn của artboard).
4. `docs/client/DESIGN_SYSTEM.md` và `brand/design-system/` (token, props component).
5. Endpoint dùng tới trong `docs/API_SPEC.md`, thông điệp và trạng thái trong `docs/ux/UX_BRIEF.md`, dữ liệu mẫu trong `docs/ux/samples/`.
6. `docs/DECISIONS.md`: P-40 → P-47 (client) cùng các quyết định backend mà màn hình dựa vào (P-34 không hoàn tác, P-35/P-39 trạng thái route, P-36 idempotent…); khu quản trị W11: P-59 → P-66 (P-66: Recharts).
7. `docs/CURRENT_STATE.md` §5: task client nào đã xong.

## 2. Quy tắc cứng

- **Chỉ làm trong phạm vi task.** Gặp quyết định chưa có trong docs (thư viện mới, đổi luồng, đổi bố cục khác prototype) thì dừng lại hỏi.
- **Không sửa backend, không đổi hợp đồng API trong task client.** Thiếu field/endpoint, hoặc API_SPEC lệch OpenAPI → ghi vào báo cáo, đề xuất task backend (T46+). Không tự suy dữ liệu server lẽ ra phải trả (ARCHITECTURE §2 quy tắc 5).
- **Không sửa tay file sinh:** `web/src/data/schema.d.ts`, `mobile/packages/pathwise_api/**`, `web/src/shared/theme/tokens.css`, `mobile/lib/shared/theme/tokens.g.dart`. Sửa nguồn (`api/openapi.json` qua `sync_openapi.sh`, `brand/design-system/tokens.json`) rồi sinh lại.
- **Giữ ranh giới lớp** (ARCHITECTURE §2) và port (§2.1): thư viện bản đồ chỉ trong `platform/map`; HTTP chỉ trong `data/`; token chỉ qua `TokenStore`.
- **Giao diện theo design system:** chỉ dùng token; nhãn trạng thái qua `statusInfo`; thông điệp lỗi theo UX_BRIEF §6; tuân thủ accessibility (nhãn cho ô nhập, `aria-*`/semantics, focus thấy được, tương phản 4,5:1 ở cả hai theme).
- **Bảo mật:** access token chỉ trong bộ nhớ; không log token/OTP/mật khẩu; không commit `.env`, key MapTiler hay keystore.
- **Dependency:** chỉ thêm thư viện có trong P-43/P-44 hoặc task cho phép; thêm thư viện khác phải hỏi. Chọn bản ổn định phát hành ít nhất 2 tuần, khoá phiên bản bằng lockfile.
- **Test không gọi dịch vụ ngoài thật** (MapTiler, backend thật chỉ trong E2E của W07).
- **Tên test:** web `describe('<Component|hook>') / it('<hành vi> khi <điều kiện>')` bằng tiếng Việt hoặc tiếng Anh nhất quán trong file; mobile `testWidgets('<hành vi> khi <điều kiện>')`.

## 3. Definition of Done

1. Web: `pnpm lint && pnpm typecheck && pnpm test && pnpm build` xanh. Mobile: `flutter analyze && flutter test` xanh. CI xanh trên PR.
2. Acceptance của task được kiểm bằng test hoặc ghi cách kiểm tay (kèm ảnh chụp màn hình nếu là giao diện, đặt trong mô tả PR, không commit ảnh).
3. Giao diện khớp prototype và DESIGN_SYSTEM; chỗ cố ý khác prototype ghi lý do trong báo cáo.
4. Làm đủ checklist docs §4 trong cùng PR.

## 4. Checklist docs (bắt buộc, trước khi commit)

Đọc code thật để đối chiếu, không dựa trí nhớ. Báo cáo cuối task liệt kê từng mục: đã cập nhật gì, hoặc "không cần vì …".

1. **`docs/CURRENT_STATE.md`**: §5 thêm dòng (ngày, branch, task, thay đổi, kết quả test web/mobile). §1 cập nhật dòng "Cấu trúc repo"/"Client" khi thêm thư mục hoặc lệnh build mới. Lỗi phát hiện mà chưa sửa → F-xx mới kèm mức bằng chứng.
2. **`docs/client/PLAN.md`**: thêm mục "Phát sinh khi làm (ngày)" vào task nếu phạm vi, quyết định nhỏ hay ước lượng thay đổi; không xoá phạm vi cũ.
3. **`docs/client/ARCHITECTURE.md`**: khi thêm port, đổi cấu trúc thư mục, đổi cách xử lý lỗi/auth/polling.
4. **`docs/client/SCREENS.md`**: khi thêm/đổi route, màn hình hay hành vi bắt buộc.
5. **`docs/client/DESIGN_SYSTEM.md` + `brand/design-system/`**: khi đổi token hay quy tắc giao diện. Đồng thời cập nhật design system trong artifact (Claude Design) để chủ dự án xem được; nếu phiên không truy cập được artifact thì ghi "chưa đồng bộ artifact" vào báo cáo.
6. **`docs/DECISIONS.md`**: quyết định mới ghi **Proposed** (P-xx tiếp theo) và hỏi chủ dự án; không tự đánh Approved.
7. **`docs/ux/UX_BRIEF.md`**: khi đổi câu thông báo hay hành vi mà backend cần biết.
8. **API**: không đổi trong task client; nếu phát hiện lệch thì ghi F-xx và đề xuất task backend.

## 5. Làm việc với thiết kế (Claude Design)

- Design system: `https://claude.ai/artifact/CBQwFPSiBSH6JavBwHgutU`. Prototype: `https://claude.ai/artifact/JPW7uzXLFrpqfgFmQVjs5N`.
- Repo là nguồn đúng cho code; artifact để chủ dự án xem và góp ý. Đổi token/quy tắc → sửa repo trước, rồi đồng bộ artifact.
- Chủ dự án thường góp ý bằng ảnh chụp đặt trong `docs/ux/samples/` (tên tự do, ví dụ `feedback1.png`): đọc ảnh, sửa, rồi xoá ảnh khi được yêu cầu. Không commit ảnh góp ý hay ảnh bản đồ chụp từ dịch vụ ngoài.
- Không đưa thêm màn hình hay luồng không có trong SCREENS/prototype mà không hỏi.

## 6. Báo cáo cuối task

Tiếng Việt, gồm: việc đã làm theo acceptance; kết quả lệnh kiểm (số test); chỗ khác prototype và lý do; thiếu sót của API (đề xuất task backend); checklist docs §4 từng mục; việc chủ dự án cần làm (key MapTiler, ký app…).
