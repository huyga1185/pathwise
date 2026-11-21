# PathWise — app tài xế (Flutter)

Quy tắc và kiến trúc: `docs/client/README.md`, `docs/client/ARCHITECTURE.md`. Màn hình: `docs/client/SCREENS.md` §2.

## Chuẩn bị

- Flutter 3.47.9 (ghi trong `.fvmrc`). Trên máy dev dùng bản ở `~/development/flutter/bin` (bản `/usr/bin/flutter` hỏng):
  `export PATH="$HOME/development/flutter/bin:$PATH"`.
- Android SDK (đã có ở `/opt/android-sdk`). Thiết bị: điện thoại Android cắm USB (bật "Gỡ lỗi USB") hoặc emulator.
- Backend chạy ở máy: `docker compose --profile app up -d --build` (API ở `http://localhost:8080`).

## Chạy

```bash
cd mobile
# Emulator: 10.0.2.2 là localhost của máy tính (mặc định của API_BASE).
flutter run
# Điện thoại thật qua USB: chuyển cổng 8080 của điện thoại về máy tính.
adb reverse tcp:8080 tcp:8080
flutter run --dart-define=API_BASE=http://localhost:8080
```

Khi đang chạy: `r` nạp lại code, `R` khởi động lại, `q` thoát.

Biến `--dart-define` (`lib/platform/config.dart`):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `API_BASE` | `http://10.0.2.2:8080` | Origin của API (đường dẫn đã có `/api/v1`). Bản release chỉ gọi được `https://`. |
| `WEB_BASE` | `API_BASE` + `/app` | Web dispatcher, nơi app mở "Quên mật khẩu" và "Nhập mã mời". Khi dev web chạy Vite riêng: `http://10.0.2.2:5173/app`. |
| `BASEMAP`, `MAPTILER_KEY` | `maptiler`, rỗng | Bản đồ của màn dẫn đường (M05). Không có key: bản debug dùng tile OSM, bản release không có nền (đường đi vẫn hiện). Key giới hạn theo package `uk.huy.pathwise.pathwise_driver` trong MapTiler. |

Tài khoản tài xế: OWNER mời tài xế trên web (`/members`), tài xế nhận lời mời bằng mã trong email (Mailpit `http://localhost:8025` khi dev), rồi dispatcher giao việc một plan có tuyến cho tài xế đó.

## Kiểm tra

```bash
flutter analyze
flutter test        # gồm test accessibility: 360×640, chữ 130 %, hai theme
```

### Kiểm tay chia sẻ vị trí (T32) trên máy Android

1. Cài APK, đăng nhập bằng tài khoản tài xế có tuyến trong plan đang giao hôm nay.
2. Mở chi tiết tuyến: hộp "Chia sẻ vị trí cho tuyến này?" hiện ra. Chọn "Chia sẻ vị trí", cho phép vị trí "Khi dùng ứng dụng".
3. Thấy dòng "Đang chia sẻ vị trí với điều phối viên" và thông báo cố định "Đang chia sẻ vị trí" trên Android (Android 13+ cần cho phép thông báo của PathWise trong cài đặt thì mới thấy thông báo).
4. Bấm "Chỉ đường" sang Google Maps, đi vài trăm mét: trên web, màn theo dõi plan hiện mũi tên của tài xế, cập nhật trong khoảng 30–60 giây.
5. Tắt GPS: app báo "Định vị (GPS) đang tắt.", web ghi "GPS trên máy của … đang tắt". Bật lại rồi quay về app: chia sẻ tiếp.
6. Báo kết quả điểm cuối: tuyến xong, thông báo cố định biến mất.
7. Menu tài khoản (chạm chữ viết tắt tên) → "Chia sẻ vị trí khi giao": đổi Hỏi / Luôn chia sẻ / Không chia sẻ.

## Build release Android

```bash
flutter build apk --release --dart-define=API_BASE=https://<domain>
```

Mặc định bản release ký bằng key debug: cài trực tiếp APK lên máy tài xế được (app không đưa lên Google Play). Ai cần ký bằng key riêng thì tạo upload key rồi ghi `android/key.properties` (đã nằm trong `.gitignore`, không commit):

```bash
keytool -genkeypair -v -keystore ~/pathwise-upload.jks -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

```properties
storeFile=/home/<user>/pathwise-upload.jks
storePassword=…
keyAlias=upload
keyPassword=…   # PKCS12: trùng storePassword
```

File ra ở `build/app/outputs/flutter-apk/app-release.apk`; cài bằng `adb install` hoặc gửi file cho tài xế (máy phải cho phép cài app ngoài Play Store). Kiểm chữ ký: `apksigner verify --print-certs <apk>` (trong `$ANDROID_HOME/build-tools/<bản>/`).

Giữ file `.jks` và mật khẩu ở chỗ an toàn ngoài máy: mất key thì bản sau không cài đè được nữa (tài xế phải gỡ app). Lần đầu đổi từ key debug sang key riêng, máy đang cài bản ký key debug cũng phải gỡ app một lần.

### APK trong GitHub Release (W15, P-79)

Publish một GitHub Release thì workflow `release` build APK, ký bằng key riêng và đính `pathwise-driver.apk` vào release. Link cố định của bản mới nhất (pre-release không tính): `https://github.com/huyga1185/pathwise/releases/latest/download/pathwise-driver.apk`, dùng cho `APK_URL` của landing (`docs/DEPLOYMENT.md` §8). Cần đặt một lần trên GitHub:

```bash
base64 -w0 ~/pathwise-upload.jks | gh secret set ANDROID_KEYSTORE_BASE64
gh secret set ANDROID_KEYSTORE_PASSWORD               # gõ mật khẩu khi được hỏi
gh secret set ANDROID_MAPTILER_KEY                    # tuỳ chọn: key MapTiler của màn dẫn đường
gh variable set APK_API_BASE --body https://<domain>  # origin của API mà app gọi
```

APK chỉ build cho release có tag nằm trên `main` (D-34); release gắn tag ở `develop` hay branch khác thì job báo lỗi, không có APK. Workflow cũng dừng sớm (báo lỗi) khi thiếu keystore hoặc `APK_API_BASE`, hoặc khi `version` của `pubspec.yaml` không khớp tag (`v1.2.0`, hay `v1.2.0-rc1` cho pre-release): nâng version trong commit `chore(release)` trước khi tạo tag. APK ký bằng key debug thì không được đính.
