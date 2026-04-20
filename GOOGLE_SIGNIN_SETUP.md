# Hướng dẫn cấu hình Google Sign-In cho Movify

## 📋 Mục lục
1. [Tạo Firebase Project](#tạo-firebase-project)
2. [Cấu hình Google Sign-In](#cấu-hình-google-sign-in)
3. [Tải google-services.json](#tải-google-servicesjson)
4. [Cấu hình Android Project](#cấu-hình-android-project)
5. [Kiểm tra chứng chỉ SHA-1](#kiểm-tra-chứng-chỉ-sha-1)
6. [Kiểm tra hoạt động](#kiểm-tra-hoạt-động)

---

## Tạo Firebase Project

### Bước 1: Truy cập Firebase Console
1. Đi tới [Firebase Console](https://console.firebase.google.com/)
2. Đăng nhập bằng tài khoản Google

### Bước 2: Tạo Project mới
1. Click "Add project" hoặc "Create project"
2. Nhập tên project: **Movify**
3. Chọn quốc gia: **Vietnam**
4. Chấp nhận các điều khoản
5. Click "Create project"

### Bước 3: Đợi Project được tạo
- Quá trình này mất vài giây đến vài phút

---

## Cấu hình Google Sign-In

### Bước 1: Bật Authentication
1. Trong Firebase Console, vào tab **Build** → **Authentication**
2. Click **Get Started**
3. Click tab **Sign-in method**

### Bước 2: Bật Google Sign-In
1. Chọn provider **Google**
2. Click **Enable**
3. Nhập **Project support email**: Chọn email của bạn
4. Nhập **Project public-facing name**: **Movify**
5. Click **Save**

### Bước 3: Tạo OAuth Consent Screen (nếu cần)
1. Nếu được yêu cầu, click link dẫn tới Google Cloud Console
2. Tạo OAuth consent screen:
   - User type: **External**
   - Nhập App name: **Movify**
   - Nhập User support email: Email của bạn
   - Nhập Developer contact info: Email của bạn
   - Click **Save and Continue** cho tất cả bước

---

## Tải google-services.json

### Bước 1: Vào Project Settings
1. Trong Firebase Console, click biểu tượng ⚙️ (Settings)
2. Chọn **Project Settings**

### Bước 2: Tạo App Firebase cho Android
1. Tìm phần **Your apps** (hoặc **Apps**)
2. Nếu chưa có app Android, click **+Add app** → **Android**
3. Nhập thông tin:
   - **Android package name**: `vnuhcm.hcmus.movify`
   - **App nickname**: `Movify Android`
   - **Debug signing certificate SHA-1**: (xem phần kiểm tra SHA-1 dưới)
4. Click **Register app**

### Bước 3: Tải google-services.json
1. Click **Download google-services.json**
2. Lưu file vào: `app/google-services.json`
   ```
   Movify/
   ├── app/
   │   ├── google-services.json  ← Đặt file ở đây
   │   ├── src/
   │   └── build.gradle.kts
   ```

---

## Cấu hình Android Project

### Bước 1: Lấy SHA-1 Certificate
1. Mở terminal tại thư mục project
2. Chạy lệnh:
   ```bash
   ./gradlew signingReport
   ```
3. Tìm dòng `SHA1:` và copy giá trị
   - Ví dụ: `AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12`

### Bước 2: Thêm SHA-1 vào Firebase
1. Quay lại Firebase Console → Project Settings
2. Tìm app Android
3. Click **Add fingerprint**
4. Paste SHA-1 vừa copy
5. Click **Save**

### Bước 3: Build và Run
1. Mở Android Studio
2. Build project: `Build` → `Clean Project` → `Make Project`
3. Nếu gặp lỗi dependencies, chạy:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

---

## Kiểm tra Chứng chỉ SHA-1

### Trên Windows (PowerShell)
```powershell
cd app
./gradlew signingReport
```

### Trên Mac/Linux
```bash
cd app
./gradlew signingReport
```

### Output mong đợi
```
Variant: debug
Config: debug
Store: C:\Users\...\...\debug.keystore
Alias: androiddebugkey
MD5: ...
SHA1: AB:CD:EF:...
SHA-256: ...
```

---

## Kiểm tra Hoạt động

### 1. Chạy App
1. Kết nối thiết bị Android hoặc mở Emulator
2. Click **Run** trong Android Studio
3. Chọn thiết bị hoặc emulator

### 2. Kiểm tra Login Screen
- Nút "Đăng nhập bằng Google" xuất hiện
- Có dòng "hoặc" phân chia

### 3. Test Google Sign-In
1. Click nút "Đăng nhập bằng Google"
2. Chọn tài khoản Google
3. Cho phép quyền truy cập
4. Nếu thành công, chuyển tới Home Activity

### 4. Kiểm tra Logs
1. Mở Logcat trong Android Studio
2. Tìm tag `LoginActivity`
3. Xem messages:
   ```
   D/LoginActivity: Firebase authentication with Google successful
   D/LoginActivity: User: user@gmail.com
   ```

---

## Xử lý Lỗi Thường Gặp

### Lỗi: "Lỗi xác thực 12500"
**Nguyên nhân**: SHA-1 không khớp hoặc chưa thêm vào Firebase

**Cách sửa**:
1. Lấy lại SHA-1 mới
2. Thêm vào Firebase Console
3. Chờ vài phút để Firebase cập nhật

### Lỗi: "google-services.json not found"
**Nguyên nhân**: File không ở đúng vị trí

**Cách sửa**:
1. Kiểm tra file ở: `app/google-services.json`
2. File phải ở cấp level `app/`, không phải `app/src/main/`

### Lỗi: "Sign in with Google not available"
**Nguyên nhân**: Play Services chưa cập nhật

**Cách sửa**:
1. Cập nhật Google Play Services
2. Kiểm tra gradle version
3. Rebuild project: `./gradlew clean build`

### Lỗi: "DEVELOPER_ERROR"
**Nguyên nhân**: Web client ID không chính xác

**Cách sửa**:
1. Firebase Console → Project Settings
2. Xem **Web SDK configuration**
3. Sao chép `clientId`
4. Thêm vào `strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
   ```

---

## 🔒 Bảo Mật

### Tránh Commit Thông Tin Nhạy Cảm
1. File `google-services.json` chứa thông tin nhạy cảm
2. Thêm vào `.gitignore`:
   ```
   app/google-services.json
   ```
3. Chỉ share file với team thông qua kênh bảo mật

### Environment Variables
Nếu cần share project:
1. Tạo `google-services.json.example` (template)
2. Ghi hướng dẫn tạo file thực trong README
3. CI/CD có thể inject từ environment variables

---

## 📞 Hỗ Trợ Thêm

### Tài liệu Tham Khảo
- [Firebase Docs: Google Sign-In](https://firebase.google.com/docs/auth/android/google-signin)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Firebase Console Guide](https://firebase.google.com/docs/projects/learn-more)

### Các Bước Tiếp Theo
1. ✅ Google Sign-In hoạt động
2. ⏳ Kết nối với backend API
3. ⏳ Lưu JWT token từ backend
4. ⏳ Thêm Facebook/Apple Sign-In (tuỳ chọn)

---

**Ngày tạo**: 2026-04-20  
**Version**: 1.0  
**Status**: ✅ Đầy đủ
