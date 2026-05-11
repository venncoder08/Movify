# Codex Session Context

## Mục Đích

File này lưu lại ngữ cảnh quan trọng của dự án Movify để dùng cho các session Codex sau, tránh mất thời gian dò lại từ đầu.

## Cấu Trúc Dự Án

- `app/`: Android app native Java
- `backend/`: Node.js + Express + Prisma
- `docs/flow.md`: flow nghiệp vụ user-driver đã được mô tả

## Build Và Runtime

### Android

- App package: `vnuhcm.hcmus.movify`
- Build file chính:
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
- Backend URL hiện hardcode trong `app/build.gradle.kts`
- Google Maps API key đang được inject từ `local.properties` qua manifest placeholder

### Backend

- Entry point:
  - `backend/src/server.js`
  - `backend/src/app.js`
- ORM: Prisma
- Schema nguồn hiện tại:
  - `backend/db/schema.prisma`

## Các Việc Đã Làm Trong Session Này

### 1. Resolve Merge Conflict

Đã resolve conflict và push lên GitHub trước đó.

Các file từng conflict quan trọng:
- `backend/db/schema.prisma`
- `backend/src/routes/index.js`
- `backend/src/validations/index.js`

Lưu ý:
- Code backend hiện đang theo naming kiểu Prisma field PascalCase ở source schema như `ID`, `Email`, `PhoneNumber`, `DriverStatus`, `VehicleType`
- Nhưng đã từng có dấu hiệu Prisma Client runtime bị generate từ schema cũ camelCase

### 2. Sửa Lỗi 16 KB Compatibility

Đã nâng dependency Android để tránh warning native libs không tương thích 16 KB page size:

- ML Kit barcode scanning:
  - từ `com.google.mlkit:barcode-scanning:17.2.0`
  - sang `com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1`
- CameraX:
  - từ `1.2.3`
  - lên `1.6.1`

File đã sửa:
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

Build đã chạy thành công sau khi sửa.

### 3. Điều Tra Lỗi Google Maps Không Hiển Thị

Đã xác nhận:
- `SupportMapFragment` chạy được
- `onMapReady` chạy được
- API key có mặt trong manifest runtime

Đã thêm log chẩn đoán map:
- `app/src/main/java/vnuhcm/hcmus/movify/utils/MapDebugHelper.java`
- hook vào:
  - `MapFragment`
  - `MapBookingFragment`
  - `DriverMapFragment`

Kết luận hiện tại:
- App nhận key
- Maps SDK init được
- Vấn đề nhiều khả năng nằm ở:
  - Google Cloud API key restriction
  - Maps SDK for Android chưa bật
  - billing chưa bật
  - tile không tải được

Log quan trọng đã thấy:
- `DriverMapFragment: onMapReady`
- `Google Maps API key present`
- chưa thấy `onMapLoaded`

=> nghi vấn mạnh nhất là Google Cloud config, không phải lỗi fragment Java.

### 4. Xóa Toàn Bộ Android AVD

Đã xóa sạch máy ảo Android trên máy local:
- dọn `C:\Users\ACER\.android\avd`
- kill process `emulator.exe` và `qemu-system-x86_64.exe`

### 5. Sửa Lỗi Update Profile Không Sync

Đã phát hiện mismatch giữa payload Android và backend:

Android gửi:
- `fullName`
- `phoneNumber`
- `avatar`
- `gender`
- `dateOfBirth`

Backend cũ đọc:
- `FullName`
- `PhoneNumber`
- `Avatar`
- `Gender`
- `DateOfBirth`

Đã sửa backend để nhận cả `camelCase` lẫn `PascalCase`.

File đã sửa:
- `backend/src/controllers/authController.js`

### 6. Điều Tra Và Sửa “Tìm Khách” Không Sync

Đã phát hiện màn driver tìm khách không phải realtime:
- chỉ refresh khi mở màn
- hoặc bấm `Refresh`
- hoặc sau khi clear active trip

Đã thêm polling định kỳ cho `DriverMapFragment`:
- auto refresh nearby trips mỗi 10 giây khi driver đang rảnh
- reload khi quay lại màn hình
- dừng polling khi driver đã nhận chuyến

File đã sửa:
- `app/src/main/java/vnuhcm/hcmus/movify/fragments/DriverMapFragment.java`

Compile đã chạy thành công sau khi sửa.

### 7. Tài Liệu Flow Nghiệp Vụ

Đã tạo:
- `docs/flow.md`

Nội dung:
- flow user-driver từ `Pending -> Accepted -> InProgress -> Completed`
- nhánh `Cancelled`
- API chính theo từng bước

## Những Điểm Cần Ghi Nhớ Cho Session Sau

### Prisma Có Dấu Hiệu Không Đồng Bộ

Đây là điểm rủi ro lớn.

Đã từng thấy:
- source schema ở `backend/db/schema.prisma` theo field PascalCase
- nhưng `backend/node_modules/.prisma/client/schema.prisma` từng là camelCase với `@map(...)`

Triệu chứng:
- lỗi `Invalid prisma.account.findUnique() invocation`
- update/fetch có thể lệch field

Nếu backend có hành vi lạ, cần kiểm tra ngay:
1. tắt backend đang chạy
2. chạy trong `backend/`:
   - `npm run db:generate`
3. khởi động lại:
   - `npm run dev`

Nếu `db:generate` lỗi `EPERM`, thường là do Prisma engine đang bị process backend giữ file.

### Google Maps Cần Kiểm Tra Ở Google Cloud

Package:
- `vnuhcm.hcmus.movify`

Debug SHA-1 đã lấy được:
- `5C:B6:90:2B:74:44:A8:3A:DD:88:C2:47:72:D8:54:FE:14:E9:72:73`

Nếu map không load:
1. kiểm tra `Maps SDK for Android`
2. kiểm tra billing
3. tạm để API key `Application restrictions = None`
4. nếu map lên thì cấu hình lại restriction đúng package + SHA-1

### Driver Search Không Phải WebSocket

Hiện đồng bộ “tìm khách” dùng polling trong app, không phải realtime push/socket.

Nếu muốn mượt hơn sau này, có thể nâng cấp sang:
- WebSocket
- Firebase Realtime Database
- Firestore listeners
- SSE

## File Đã Chạm Tới Trong Session Này

### Android

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/vnuhcm/hcmus/movify/fragments/MapFragment.java`
- `app/src/main/java/vnuhcm/hcmus/movify/fragments/MapBookingFragment.java`
- `app/src/main/java/vnuhcm/hcmus/movify/fragments/DriverMapFragment.java`
- `app/src/main/java/vnuhcm/hcmus/movify/utils/MapDebugHelper.java`

### Backend

- `backend/src/controllers/authController.js`
- `backend/src/controllers/messageController.js`
- `backend/src/routes/index.js`
- `backend/src/validations/index.js`
- `backend/db/schema.prisma`

### Docs

- `docs/flow.md`
- `codex.md`

## Gợi Ý Điểm Bắt Đầu Cho Session Sau

Nếu quay lại làm tiếp, nên ưu tiên theo thứ tự:

1. Xác nhận Prisma client đã sync với schema thật
2. Test lại update profile và driver data sau khi restart backend
3. Chốt lỗi Google Maps bằng log `MapDebug` + kiểm tra Google Cloud
4. Nếu cần, tiếp tục tối ưu flow tìm khách realtime
