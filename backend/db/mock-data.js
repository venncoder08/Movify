module.exports = {
  accounts: {
    // Admin account
    admin: {
      email: "admin@movify.mock",
      phoneNumber: "0900000001",
      password: "Admin@123",
      role: "Admin",
      fullName: "Movify Admin",
      gender: "Other",
      avatar: "https://i.pravatar.cc/300?img=12"
    },

    // User/Rider accounts
    rider: {
      email: "rider@movify.mock",
      phoneNumber: "0900000002",
      password: "Rider@123",
      role: "User",
      fullName: "Nguyen Minh Thu",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=32"
    },
    user2: {
      email: "user2@movify.mock",
      phoneNumber: "0900000011",
      password: "User2@123",
      role: "User",
      fullName: "Pham Duc Minh",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=5"
    },
    user3: {
      email: "user3@movify.mock",
      phoneNumber: "0900000012",
      password: "User3@123",
      role: "User",
      fullName: "Hoang Thi Lan",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=42"
    },
    user4: {
      email: "user4@movify.mock",
      phoneNumber: "0900000013",
      password: "User4@123",
      role: "User",
      fullName: "Vu Anh Tu",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=18"
    },

    // Driver accounts
    driver: {
      email: "driver@movify.mock",
      phoneNumber: "0900000003",
      password: "Driver@123",
      role: "Driver",
      fullName: "Tran Quoc Bao",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=48",
      idCard: "079201234567",
      driverLicense: "79A1-123456",
      vehicleRegistration: "59-S1 123.45",
      driverStatus: "Approved",
      ratePoint: "4.90"
    },
    driver2: {
      email: "driver2@movify.mock",
      phoneNumber: "0900000009",
      password: "Driver2@123",
      role: "Driver",
      fullName: "Vo Thanh Linh",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=65",
      idCard: "079202345678",
      driverLicense: "79A1-234567",
      vehicleRegistration: "59-S2 234.56",
      driverStatus: "Approved",
      ratePoint: "4.75"
    },
    driver3: {
      email: "driver3@movify.mock",
      phoneNumber: "0900000010",
      password: "Driver3@123",
      role: "Driver",
      fullName: "Dang Van Hieu",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=55",
      idCard: "079203456789",
      driverLicense: "79A1-345678",
      vehicleRegistration: "59-S3 345.67",
      driverStatus: "Approved",
      ratePoint: "4.85"
    },

    // Applicant account (pending driver status)
    applicant: {
      email: "applicant@movify.mock",
      phoneNumber: "0900000004",
      password: "Applicant@123",
      role: "User",
      fullName: "Le Hoang Nam",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=14"
    },
    applicant2: {
      email: "applicant2@movify.mock",
      phoneNumber: "0900000014",
      password: "Applicant2@123",
      role: "User",
      fullName: "Tran Minh Khoa",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=25"
    }
  },
  promotions: [
    {
      key: "welcome15",
      id: 900201,
      name: "MOCK_WELCOME_15",
      type: "Percent",
      discountValue: "15.00",
      adminKey: "admin"
    }
  ],
  trips: [
    // Trips with driver 1 (Tran Quoc Bao)
    {
      key: "completedTrip1",
      id: 900301,
      distance: "6.80",
      totalAmount: "52000.00",
      pickUpLocation: "227 Nguyen Van Cu, Quan 5, TP.HCM",
      destinationLocation: "Cho Ben Thanh, Quan 1, TP.HCM",
      duration: "18.00",
      tripType: "Motor",
      status: "Completed",
      pickUpTime: "2026-04-18T07:30:00+07:00",
      completedTime: "2026-04-18T07:48:00+07:00",
      paymentMethod: "Cash",
      paymentStatus: "Paid",
      userKey: "rider",
      driverKey: "driver",
      promotionKey: "welcome15"
    },
    {
      key: "completedTrip2",
      id: 900303,
      distance: "8.50",
      totalAmount: "65000.00",
      pickUpLocation: "Tao Đàn Park, Quận 1, TP.HCM",
      destinationLocation: "Bến Xe Miền Đông, Quận 9, TP.HCM",
      duration: "22.00",
      tripType: "Taxi",
      status: "Completed",
      pickUpTime: "2026-04-19T10:15:00+07:00",
      completedTime: "2026-04-19T10:37:00+07:00",
      paymentMethod: "BankTransfer",
      paymentStatus: "Paid",
      userKey: "user2",
      driverKey: "driver",
      promotionKey: null
    },
    {
      key: "activeTrip1",
      id: 900302,
      distance: "4.20",
      totalAmount: "36000.00",
      pickUpLocation: "54/30 Le Van Luong, Quan 7, TP.HCM",
      destinationLocation: "RMIT Viet Nam, Quan 7, TP.HCM",
      duration: "12.00",
      tripType: "Taxi",
      status: "Accepted",
      pickUpTime: "2026-04-20T14:00:00+07:00",
      completedTime: null,
      paymentMethod: "BankTransfer",
      paymentStatus: "Unpaid",
      userKey: "rider",
      driverKey: "driver",
      promotionKey: null
    },

    // Trips with driver 2 (Vo Thanh Linh)
    {
      key: "completedTrip3",
      id: 900304,
      distance: "7.20",
      totalAmount: "58000.00",
      pickUpLocation: "Sân Bay Tân Sơn Nhất, Quận 1, TP.HCM",
      destinationLocation: "Landmark 81, Quận 7, TP.HCM",
      duration: "20.00",
      tripType: "Taxi",
      status: "Completed",
      pickUpTime: "2026-04-17T06:00:00+07:00",
      completedTime: "2026-04-17T06:20:00+07:00",
      paymentMethod: "BankTransfer",
      paymentStatus: "Paid",
      userKey: "user3",
      driverKey: "driver2",
      promotionKey: null
    },
    {
      key: "completedTrip4",
      id: 900305,
      distance: "5.50",
      totalAmount: "44000.00",
      pickUpLocation: "Saigon Square, Quận 1, TP.HCM",
      destinationLocation: "Đường Nguyễn Huệ, Quận 1, TP.HCM",
      duration: "16.00",
      tripType: "Motor",
      status: "Completed",
      pickUpTime: "2026-04-19T15:30:00+07:00",
      completedTime: "2026-04-19T15:46:00+07:00",
      paymentMethod: "Cash",
      paymentStatus: "Paid",
      userKey: "user4",
      driverKey: "driver2",
      promotionKey: "welcome15"
    },

    // Trips with driver 3 (Dang Van Hieu)
    {
      key: "completedTrip5",
      id: 900306,
      distance: "9.80",
      totalAmount: "78000.00",
      pickUpLocation: "Lotte Mart, Quận 7, TP.HCM",
      destinationLocation: "Bình Thạnh District Hall, Quận 2, TP.HCM",
      duration: "28.00",
      tripType: "Taxi",
      status: "Completed",
      pickUpTime: "2026-04-18T13:00:00+07:00",
      completedTime: "2026-04-18T13:28:00+07:00",
      paymentMethod: "BankTransfer",
      paymentStatus: "Paid",
      userKey: "rider",
      driverKey: "driver3",
      promotionKey: null
    },
    {
      key: "completedTrip6",
      id: 900307,
      distance: "6.50",
      totalAmount: "52000.00",
      pickUpLocation: "Trần Hưng Đạo, Quận 5, TP.HCM",
      destinationLocation: "Đại học Kinh tế, Quận 7, TP.HCM",
      duration: "19.00",
      tripType: "Motor",
      status: "Completed",
      pickUpTime: "2026-04-20T08:45:00+07:00",
      completedTime: "2026-04-20T09:04:00+07:00",
      paymentMethod: "Cash",
      paymentStatus: "Paid",
      userKey: "user2",
      driverKey: "driver3",
      promotionKey: null
    }
  ],
  ratings: [
    {
      id: 900401,
      tripKey: "completedTrip1",
      raterKey: "rider",
      rateeKey: "driver",
      score: "4.8",
      comment: "Tài xế đến đúng giờ và di chuyển rất ổn."
    },
    {
      id: 900402,
      tripKey: "completedTrip2",
      raterKey: "user2",
      rateeKey: "driver",
      score: "4.9",
      comment: "Dịch vụ xuất sắc, tài xế thân thiện và xe sạch sẽ."
    },
    {
      id: 900403,
      tripKey: "completedTrip3",
      raterKey: "user3",
      rateeKey: "driver2",
      score: "4.7",
      comment: "Tài xế kỹ năng tốt, di chuyển an toàn."
    },
    {
      id: 900404,
      tripKey: "completedTrip4",
      raterKey: "user4",
      rateeKey: "driver2",
      score: "5.0",
      comment: "Hoàn hảo! Tài xế chuyên nghiệp và tư cách tốt."
    },
    {
      id: 900405,
      tripKey: "completedTrip5",
      raterKey: "rider",
      rateeKey: "driver3",
      score: "4.85",
      comment: "Tuyệt vời, tài xế biết đường tắt, tiết kiệm thời gian."
    },
    {
      id: 900406,
      tripKey: "completedTrip6",
      raterKey: "user2",
      rateeKey: "driver3",
      score: "4.75",
      comment: "Rất tốt, tài xế lịch sự và an toàn."
    }
  ],
  notifications: [
    {
      id: 900501,
      accountKey: "rider",
      title: "Đặt xe thành công",
      body: "Tài xế Trần Quốc Bảo đã nhận chuyến đi của bạn.",
      isRead: false
    },
    {
      id: 900502,
      accountKey: "driver",
      title: "Chuyến đi mới",
      body: "Bạn vừa được gán chuyến đi từ Quận 7 đến Chợ Bến Thành.",
      isRead: false
    },
    {
      id: 900503,
      accountKey: "applicant",
      title: "Hồ sơ đang chờ duyệt",
      body: "Yêu cầu nâng cấp tài khoản lên driver đang được xem xét.",
      isRead: true
    },
    {
      id: 900504,
      accountKey: "user2",
      title: "Đánh giá chuyến đi",
      body: "Vui lòng đánh giá tài xế của bạn trong chuyến đi vừa rồi.",
      isRead: false
    },
    {
      id: 900505,
      accountKey: "user3",
      title: "Xác nhận thanh toán",
      body: "Thanh toán cho chuyến đi từ Sân Bay Tân Sơn Nhất đã được xác nhận.",
      isRead: true
    },
    {
      id: 900506,
      accountKey: "driver2",
      title: "Nhận xét từ hành khách",
      body: "Bạn nhận được 5 sao từ khách hàng Hoàng Thị Lan.",
      isRead: true
    },
    {
      id: 900507,
      accountKey: "user4",
      title: "Khuyến mãi mới",
      body: "Mã khuyến mãi mới SUMMER25 với giảm giá 25% cho bạn.",
      isRead: false
    },
    {
      id: 900508,
      accountKey: "driver3",
      title: "Chuyến đi hoàn thành",
      body: "Bạn vừa hoàn thành chuyến đi thứ 100 của mình!",
      isRead: true
    },
    {
      id: 900509,
      accountKey: "applicant2",
      title: "Yêu cầu tài liệu bổ sung",
      body: "Vui lòng cung cấp thêm bằng cấp và chứng thực lý lịch.",
      isRead: false
    },
    {
      id: 900510,
      accountKey: "admin",
      title: "Báo cáo hệ thống",
      body: "Có 3 yêu cầu chờ duyệt từ những tài xế ứng dụng.",
      isRead: true
    }
  ],
  driverRequests: [
    {
      id: 900601,
      driverKey: "applicant",
      adminKey: "admin",
      idCard: "079298765432",
      driverLicense: "79A1-654321",
      vehicleRegistration: "59-S2 678.90",
      status: "Pending"
    },
    {
      id: 900602,
      driverKey: "applicant2",
      adminKey: "admin",
      idCard: "079205555666",
      driverLicense: "79A1-555666",
      vehicleRegistration: "59-S3 555.66",
      status: "Pending"
    }
  ]
};
