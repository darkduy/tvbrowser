# TV Browser

Ứng dụng trình duyệt web đơn giản dành cho **Android TV / Google TV**, viết bằng Kotlin,
build tự động bằng **GitHub Actions**, xuất ra file APK release đã ký sẵn.

## Tính năng

- Duyệt web cơ bản (WebView): nhập URL, tìm kiếm Google, Lùi/Tới/Tải lại/Trang chủ.
- Giao diện tối ưu điều khiển bằng D-pad (remote TV): các nút focus nối tiếp nhau,
  có viền sáng khi được chọn.
- Phím BACK trên remote sẽ lùi trang web trước, chỉ thoát app khi hết lịch sử.

## Cấu trúc project

```
tvbrowser/
├── app/
│   ├── build.gradle.kts          # Cấu hình build + ký release
│   └── src/main/
│       ├── AndroidManifest.xml   # Khai báo app cho Android TV (leanback)
│       ├── java/.../MainActivity.kt
│       └── res/                  # Layout, string, màu, icon
├── .github/workflows/build.yml   # Pipeline CI build APK release
└── keystore.properties.example   # Mẫu cấu hình keystore cho build local
```

## 1. Tạo keystore để ký ứng dụng (chỉ làm 1 lần)

Nếu chưa có keystore, tạo bằng lệnh (cần cài JDK):

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias tvbrowser \
  -keyalg RSA -keysize 2048 -validity 10000
```

Lệnh sẽ hỏi mật khẩu keystore, mật khẩu key, và vài thông tin nhận dạng — nhớ lưu lại các mật khẩu này.

## 2. Thiết lập GitHub Secrets

Vào repo trên GitHub → **Settings → Secrets and variables → Actions → New repository secret**,
thêm 4 secret sau:

| Tên secret          | Giá trị                                                             |
|---------------------|----------------------------------------------------------------------|
| `KEYSTORE_BASE64`   | Nội dung file keystore mã hóa base64: `base64 -w0 release.keystore`  |
| `KEYSTORE_PASSWORD` | Mật khẩu keystore đã đặt ở bước 1                                    |
| `KEY_ALIAS`         | Alias đã đặt (vd: `tvbrowser`)                                       |
| `KEY_PASSWORD`      | Mật khẩu key đã đặt ở bước 1                                         |

**Không commit file keystore hoặc mật khẩu vào git** — `.gitignore` đã loại trừ sẵn.

## 3. Build tự động qua GitHub Actions

Workflow tại `.github/workflows/build.yml` sẽ tự chạy khi:
- Push code lên nhánh `main`
- Tạo pull request vào `main`
- Kích hoạt thủ công (tab **Actions** → chọn workflow → **Run workflow**)

Sau khi build xong, APK được ký sẵn nằm trong mục **Artifacts** của lần chạy đó
(tên `tv-browser-release-apk`), tải về và cài trực tiếp lên Android TV
(qua ADB hoặc trình quản lý file hỗ trợ cài APK).

## 4. Build ở máy local (tùy chọn)

```bash
cp keystore.properties.example keystore.properties
# Sửa các giá trị trong keystore.properties cho đúng keystore của bạn
./gradlew assembleRelease
```

APK xuất ra tại `app/build/outputs/apk/release/app-release.apk`.

## Cài lên Android TV qua ADB

```bash
adb connect <IP-cua-Android-TV>:5555
adb install -r app-release.apk
```

## Đề xuất cải tiến tiếp theo

- **Bookmark/lịch sử duyệt web**: lưu vào Room database để không mất khi tắt app.
- **Chặn quảng cáo**: tích hợp danh sách domain chặn quảng cáo đơn giản trong `WebViewClient.shouldInterceptRequest`.
- **Đa tab**: hiện tại chỉ có 1 WebView; có thể mở rộng bằng `ViewPager2` + danh sách WebView.
- **Bàn phím ảo tối ưu TV**: thay `EditText` mặc định bằng bàn phím on-screen dạng lưới để nhập URL bằng D-pad nhanh hơn (không cần bàn phím vật lý/remote có mic).
- **Cấu hình trang chủ**: cho phép người dùng đổi URL trang chủ mặc định và lưu bằng `SharedPreferences`.
