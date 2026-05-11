# Movify User-Driver Flow

## Tổng Quan

Flow chính giữa user và driver đi theo chuỗi trạng thái:

`Pending -> Accepted -> InProgress -> Completed`

Ngoài ra có nhánh phụ:

`Pending/Accepted -> Cancelled`

## 1. User Tạo Chuyến

### App User

- User nhập:
  - điểm đón
  - điểm đến
  - loại xe
  - phương thức thanh toán
  - khuyến mãi nếu có
- `BookingFragment` gọi API tạo chuyến.

### Backend

- API chính: `POST /api/trips`
- Controller chính: `backend/src/controllers/tripController.js`
- Backend tạo bản ghi `Trip` với:
  - `Status = Pending`
  - `DriverID = null`
  - `TripType`
  - thông tin pickup/destination
  - giá tiền và khoảng cách

### Kết quả

- User chuyển sang trạng thái tìm tài xế.
- App user bắt đầu poll trạng thái chuyến.

## 2. Driver Mở Màn Tìm Khách

### App Driver

- `DriverMapFragment` lấy vị trí hiện tại của tài xế.
- App gọi API lấy danh sách chuyến gần đó.

### Backend

- API chính: `GET /api/driver/nearby-trips`
- Controller chính: `backend/src/controllers/driverController.js`

### Điều kiện lọc chuyến

- `Status = Pending`
- `DriverID = null`
- `DeletedAt = null`
- `TripType` phải khớp với `VehicleType` của tài xế

### Kết quả

- Driver thấy danh sách chuyến khả dụng.
- Mỗi chuyến gồm:
  - điểm đón
  - điểm đến
  - loại xe
  - khoảng cách
  - tiền chuyến

## 3. Driver Nhận Chuyến

### App Driver

- Driver chọn chuyến và bấm nhận.
- App gọi API nhận chuyến.

### Backend

- API chính: `POST /api/driver/accept-trip`
- Controller chính: `backend/src/controllers/driverController.js`

### Backend kiểm tra

- tài xế có role `Driver`
- `DriverStatus = Approved`
- tài xế chưa có chuyến active khác
- chuyến vẫn còn `Pending`
- chưa có tài xế khác nhận
- loại xe khớp

### Backend cập nhật

- `DriverID = req.user.id`
- `Status = Accepted`

### Notification

- Backend tạo notification báo cho user rằng tài xế đã nhận chuyến.

### Kết quả

- Driver bắt đầu đi tới điểm đón.
- User poll lại và thấy chuyến đã có tài xế nhận.

## 4. Đồng Bộ Khi Tài Xế Đang Tới Điểm Đón

### App Driver

- Driver app cập nhật vị trí hiện tại liên tục.
- Driver thấy route tới pickup.

### App User

- User app poll active trip định kỳ.
- User thấy:
  - trạng thái `Accepted`
  - thông tin tài xế
  - vị trí tài xế
  - route trên map

### Backend

- API cập nhật vị trí driver nằm trong flow trip hiện tại.
- Dữ liệu vị trí lưu vào:
  - `DriverCurrentLatitude`
  - `DriverCurrentLongitude`
  - `DriverLocationUpdatedAt`

## 5. Xác Nhận Đón Khách

Flow này có 2 bước:

### Bước 1: Driver xác nhận đã tới đón

- Driver chỉ xác nhận được khi ở gần pickup trong ngưỡng cho phép.
- Backend lưu:
  - `DriverPickupConfirmedAt`

### Bước 2: User xác nhận đã lên xe

- User chỉ xác nhận được sau khi driver đã xác nhận tới điểm đón.
- Backend lưu:
  - `PassengerPickupConfirmedAt`

### Khi đủ cả hai phía

- Backend chuyển:
  - `Status = InProgress`

### Notification

- Backend gửi notification cho cả user và driver rằng chuyến đã bắt đầu.

## 6. Chuyến Đang Diễn Ra

### App Driver

- Driver app đổi route từ pickup sang destination.
- Driver tiếp tục cập nhật vị trí hiện tại.

### App User

- User tiếp tục poll active trip.
- User thấy chuyến đang diễn ra và vị trí tài xế được cập nhật.

### Backend

- Trạng thái chuyến lúc này là `InProgress`.

## 7. Kết Thúc Chuyến

### App Driver

- Khi tài xế tới gần điểm đến, driver có thể bấm hoàn thành chuyến.

### Backend

- API hoàn thành chuyến nằm trong trip flow.
- Controller chính: `backend/src/controllers/tripController.js`

### Backend kiểm tra

- tài xế đang ở gần destination trong ngưỡng cho phép

### Backend cập nhật

- `Status = Completed`
- `CompletedTime`
- vị trí driver cuối cùng nếu có

### Notification

- Backend gửi thông báo hoàn thành chuyến.

### Kết quả

- Driver quay về màn tìm khách.
- User được chuyển sang flow đánh giá.

## 8. User Đánh Giá Tài Xế

### App User

- Khi user thấy trạng thái `Completed`, app mở `TripRatingActivity`.

### Backend

- Backend nhận rating và lưu vào bảng `Rating`.
- Có thể cập nhật `RatePoint` cho tài xế.

## 9. Nhánh Hủy Chuyến

### User hủy

- User có thể hủy khi chuyến đang ở:
  - `Pending`
  - `Accepted`

### Driver hủy

- Driver có thể hủy khi đã nhận nhưng chưa vào `InProgress`

### Backend

- Chuyển trạng thái:
  - `Status = Cancelled`
- Gửi notification cho phía còn lại

### Kết quả

- User quay về flow đặt chuyến mới
- Driver quay về flow tìm khách

## API Chính Theo Từng Bước

- Tạo chuyến: `POST /api/trips`
- Lấy chuyến active của user/driver: các API trip trong `tripController.js`
- Lấy chuyến gần đó cho driver: `GET /api/driver/nearby-trips`
- Driver nhận chuyến: `POST /api/driver/accept-trip`
- Driver cập nhật vị trí: API trip location update
- Driver xác nhận đón khách: API confirm pickup
- User xác nhận lên xe: API confirm pickup phía passenger
- Hoàn thành chuyến: API complete trip
- Hủy chuyến: API cancel trip
- Đánh giá chuyến: API rating trong trip flow

## Thành Phần App Chính

- User booking flow:
  - `app/src/main/java/vnuhcm/hcmus/movify/fragments/BookingFragment.java`
- Driver finding flow:
  - `app/src/main/java/vnuhcm/hcmus/movify/fragments/DriverMapFragment.java`
- Driver nearby trip API:
  - `backend/src/controllers/driverController.js`
- Trip lifecycle API:
  - `backend/src/controllers/tripController.js`
