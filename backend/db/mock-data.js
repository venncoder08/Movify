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
      fullName: "Nguyễn Minh Thu",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=32"
    },
    user2: {
      email: "user2@movify.mock",
      phoneNumber: "0900000011",
      password: "User2@123",
      role: "User",
      fullName: "Phạm Đức Minh",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=5"
    },
    user3: {
      email: "user3@movify.mock",
      phoneNumber: "0900000012",
      password: "User3@123",
      role: "User",
      fullName: "Hoàng Thị Lan",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=42"
    },
    user4: {
      email: "user4@movify.mock",
      phoneNumber: "0900000013",
      password: "User4@123",
      role: "User",
      fullName: "Vũ Anh Tú",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=18"
    },

    // Driver accounts
    driver: {
      email: "driver@movify.mock",
      phoneNumber: "0900000003",
      password: "Driver@123",
      role: "Driver",
      fullName: "Trần Quốc Bảo",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=48",
      idCard: "079201234567",
      driverLicense: "79A1-123456",
      vehicleRegistration: "59-S1 123.45",
      vehicleType: "Motor",
      driverStatus: "Approved",
      ratePoint: "4.90"
    },
    driver2: {
      email: "driver2@movify.mock",
      phoneNumber: "0900000009",
      password: "Driver2@123",
      role: "Driver",
      fullName: "Võ Thanh Linh",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=65",
      idCard: "079202345678",
      driverLicense: "79A1-234567",
      vehicleRegistration: "59-S2 234.56",
      vehicleType: "Taxi",
      driverStatus: "Approved",
      ratePoint: "4.75"
    },
    driver3: {
      email: "driver3@movify.mock",
      phoneNumber: "0900000010",
      password: "Driver3@123",
      role: "Driver",
      fullName: "Đặng Văn Hiếu",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=55",
      idCard: "079203456789",
      driverLicense: "79A1-345678",
      vehicleRegistration: "59-S3 345.67",
      vehicleType: "Motor",
      driverStatus: "Approved",
      ratePoint: "4.85"
    },

    // Applicant account (pending driver status)
    applicant: {
      email: "applicant@movify.mock",
      phoneNumber: "0900000004",
      password: "Applicant@123",
      role: "User",
      fullName: "Lê Hoàng Nam",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=14"
    },
    applicant2: {
      email: "applicant2@movify.mock",
      phoneNumber: "0900000014",
      password: "Applicant2@123",
      role: "User",
      fullName: "Trần Minh Khoa",
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
    // Chuyến của tài xế 1 (Trần Quốc Bảo)
    {
      key: "completedTrip1",
      id: 900301,
      distance: "6.80",
      totalAmount: "52000.00",
      pickUpLocation: "227 Nguyễn Văn Cừ, Quận 5, TP.HCM",
      destinationLocation: "Chợ Bến Thành, Quận 1, TP.HCM",
      pickUpLatitude: "10.7533",
      pickUpLongitude: "106.6550",
      destinationLatitude: "10.7913",
      destinationLongitude: "106.6957",
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
      pickUpLocation: "Công viên Tao Đàn, Quận 1, TP.HCM",
      destinationLocation: "Bến Xe Miền Đông, Quận 9, TP.HCM",
      pickUpLatitude: "10.7878",
      pickUpLongitude: "106.6990",
      destinationLatitude: "10.8190",
      destinationLongitude: "106.7550",
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
      pickUpLocation: "54/30 Lê Văn Lương, Quận 7, TP.HCM",
      destinationLocation: "RMIT Việt Nam, Quận 7, TP.HCM",
      pickUpLatitude: "10.7286",
      pickUpLongitude: "106.7055",
      destinationLatitude: "10.7310",
      destinationLongitude: "106.7180",
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

    // Chuyến của tài xế 2 (Võ Thanh Linh)
    {
      key: "completedTrip3",
      id: 900304,
      distance: "7.20",
      totalAmount: "58000.00",
      pickUpLocation: "Sân Bay Tân Sơn Nhất, Quận 1, TP.HCM",
      destinationLocation: "Landmark 81, Quận 7, TP.HCM",
      pickUpLatitude: "10.8145",
      pickUpLongitude: "106.6580",
      destinationLatitude: "10.7957",
      destinationLongitude: "106.7114",
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
      pickUpLocation: "Sài Gòn Square, Quận 1, TP.HCM",
      destinationLocation: "Đường Nguyễn Huệ, Quận 1, TP.HCM",
      pickUpLatitude: "10.7918",
      pickUpLongitude: "106.7032",
      destinationLatitude: "10.7770",
      destinationLongitude: "106.7076",
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

    // Chuyến của tài xế 3 (Đặng Văn Hiếu)
    {
      key: "completedTrip5",
      id: 900306,
      distance: "9.80",
      totalAmount: "78000.00",
      pickUpLocation: "Lotte Mart, Quận 7, TP.HCM",
      destinationLocation: "Bình Thạnh District Hall, Quận 2, TP.HCM",
      pickUpLatitude: "10.7270",
      pickUpLongitude: "106.7148",
      destinationLatitude: "10.8055",
      destinationLongitude: "106.7407",
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
      pickUpLatitude: "10.7644",
      pickUpLongitude: "106.6684",
      destinationLatitude: "10.7308",
      destinationLongitude: "106.7099",
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
    },

    // Chuyến tháng 5
    {
      key: "completedTrip7",
      id: 900312,
      distance: "7.50",
      totalAmount: "60000.00",
      pickUpLocation: "Crescent Mall, Quận 7, TP.HCM",
      destinationLocation: "Saigon Center, Quận 1, TP.HCM",
      pickUpLatitude: "10.7286",
      pickUpLongitude: "106.7055",
      destinationLatitude: "10.7770",
      destinationLongitude: "106.7076",
      duration: "25.00",
      tripType: "Taxi",
      status: "Completed",
      pickUpTime: "2026-05-08T09:00:00+07:00",
      completedTime: "2026-05-08T09:25:00+07:00",
      paymentMethod: "BankTransfer",
      paymentStatus: "Paid",
      userKey: "rider",
      driverKey: "driver",
      promotionKey: null
    },
    {
      key: "completedTrip8",
      id: 900313,
      distance: "5.20",
      totalAmount: "42000.00",
      pickUpLocation: "Quận 7 Branch Office, Quận 7, TP.HCM",
      destinationLocation: "Landmark 81, Quận 7, TP.HCM",
      pickUpLatitude: "10.7310",
      pickUpLongitude: "106.7180",
      destinationLatitude: "10.7957",
      destinationLongitude: "106.7114",
      duration: "18.00",
      tripType: "Motor",
      status: "Completed",
      pickUpTime: "2026-05-09T14:30:00+07:00",
      completedTime: "2026-05-09T14:48:00+07:00",
      paymentMethod: "Cash",
      paymentStatus: "Paid",
      userKey: "user2",
      driverKey: "driver2",
      promotionKey: null
    },

    // Pending trips for driver to accept
    {
      key: "pendingTrip1",
      id: 900308,
      distance: "3.20",
      totalAmount: "28000.00",
      pickUpLocation: "Bitexco Tower, Quận 1, TP.HCM",
      destinationLocation: "Diamond Plaza, Quận 1, TP.HCM",
      pickUpLatitude: "10.7715",
      pickUpLongitude: "106.7050",
      destinationLatitude: "10.8123",
      destinationLongitude: "106.6970",
      duration: "10.00",
      tripType: "Motor",
      status: "Pending",
      pickUpTime: null,
      completedTime: null,
      paymentMethod: "Cash",
      paymentStatus: "Unpaid",
      userKey: "user3",
      driverKey: null,
      promotionKey: null
    },
    {
      key: "pendingTrip2",
      id: 900309,
      distance: "5.80",
      totalAmount: "45000.00",
      pickUpLocation: "Công viên Tao Đàn, Quận 1, TP.HCM",
      destinationLocation: "Aeon Mall Bình Thạnh, Quận 2, TP.HCM",
      pickUpLatitude: "10.7878",
      pickUpLongitude: "106.6990",
      destinationLatitude: "10.8280",
      destinationLongitude: "106.7450",
      duration: "18.00",
      tripType: "Taxi",
      status: "Pending",
      pickUpTime: null,
      completedTime: null,
      paymentMethod: "BankTransfer",
      paymentStatus: "Unpaid",
      userKey: "user4",
      driverKey: null,
      promotionKey: null
    },
    {
      key: "pendingTrip3",
      id: 900310,
      distance: "4.50",
      totalAmount: "38000.00",
      pickUpLocation: "Phố Đi Bộ Nguyễn Huệ, Quận 1, TP.HCM",
      destinationLocation: "Học viện Công nghệ Bưu chính Viễn thông, Quận 7, TP.HCM",
      pickUpLatitude: "10.7770",
      pickUpLongitude: "106.7076",
      destinationLatitude: "10.7310",
      destinationLongitude: "106.7180",
      duration: "15.00",
      tripType: "Motor",
      status: "Pending",
      pickUpTime: null,
      completedTime: null,
      paymentMethod: "Cash",
      paymentStatus: "Unpaid",
      userKey: "rider",
      driverKey: null,
      promotionKey: null
    },
    {
      key: "pendingTrip4",
      id: 900311,
      distance: "7.20",
      totalAmount: "58000.00",
      pickUpLocation: "Bến Xe Miền Tây, Quận 6, TP.HCM",
      destinationLocation: "Sân Bay Tân Sơn Nhất, Quận 1, TP.HCM",
      pickUpLatitude: "10.7410",
      pickUpLongitude: "106.6055",
      destinationLatitude: "10.8145",
      destinationLongitude: "106.6580",
      duration: "22.00",
      tripType: "Taxi",
      status: "Pending",
      pickUpTime: null,
      completedTime: null,
      paymentMethod: "BankTransfer",
      paymentStatus: "Unpaid",
      userKey: "user2",
      driverKey: null,
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
      body: "Yêu cầu nâng cấp tài khoản lên tài xế đang được xem xét.",
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
      idCard: "https://placehold.co/900x560/png?text=CCCD+L%C3%AA+Ho%C3%A0ng+Nam",
      driverLicense: "https://placehold.co/900x560/png?text=GPLX+L%C3%AA+Ho%C3%A0ng+Nam",
      vehicleRegistration: "https://placehold.co/900x560/png?text=%C4%90%C4%83ng+k%C3%BD+xe+59-S2+678.90",
      vehicleType: "Motor",
      status: "Pending"
    },
    {
      id: 900602,
      driverKey: "applicant2",
      adminKey: "admin",
      idCard: "https://placehold.co/900x560/png?text=CCCD+Tr%E1%BA%A7n+Minh+Khoa",
      driverLicense: "https://placehold.co/900x560/png?text=GPLX+Tr%E1%BA%A7n+Minh+Khoa",
      vehicleRegistration: "https://placehold.co/900x560/png?text=%C4%90%C4%83ng+k%C3%BD+xe+59-S3+555.66",
      vehicleType: "Taxi",
      status: "Pending"
    }
  ],
  messages: [
    {
      id: 900701,
      senderKey: "rider",
      recipientKey: "driver",
      content: "Tài xế ơi, bạn còn bao lâu nữa đến nơi tôi?",
      contentType: "Text",
      isRead: true,
      createdAt: "2026-04-18T07:35:00+07:00"
    },
    {
      id: 900702,
      senderKey: "driver",
      recipientKey: "rider",
      content: "Dạ còn khoảng 5 phút nữa anh sẽ tới. Cảm ơn bạn đã chờ!",
      contentType: "Text",
      isRead: true,
      createdAt: "2026-04-18T07:36:00+07:00"
    },
    {
      id: 900703,
      senderKey: "user2",
      recipientKey: "driver",
      content: "Xin chào, có thể gọi cho tôi khi bạn tới gần nơi đón không?",
      contentType: "Text",
      isRead: false,
      createdAt: "2026-04-20T08:20:00+07:00"
    },
    {
      id: 900704,
      senderKey: "driver",
      recipientKey: "user2",
      content: "Chắc chắn rồi! Tôi sẽ gọi cho bạn khi tới nơi.",
      contentType: "Text",
      isRead: false,
      createdAt: "2026-04-20T08:21:00+07:00"
    },
    {
      id: 900705,
      senderKey: "user3",
      recipientKey: "driver2",
      content: "Chuyến đi tuyệt vời! Cảm ơn tài xế.",
      contentType: "Text",
      isRead: true,
      createdAt: "2026-04-19T14:30:00+07:00"
    },
    {
      id: 900706,
      senderKey: "driver2",
      recipientKey: "user3",
      content: "Cảm ơn bạn đã tin tưởng dịch vụ của tôi! Chúc bạn có một ngày tốt lành.",
      contentType: "Text",
      isRead: true,
      createdAt: "2026-04-19T14:31:00+07:00"
    }
  ]
};
