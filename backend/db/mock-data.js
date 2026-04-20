module.exports = {
  accounts: {
    admin: {
      email: "admin@movify.mock",
      phoneNumber: "0900000001",
      password: "Admin@123",
      role: "Admin",
      fullName: "Movify Admin",
      gender: "Other",
      avatar: "https://i.pravatar.cc/300?img=12"
    },
    rider: {
      email: "rider@movify.mock",
      phoneNumber: "0900000002",
      password: "Rider@123",
      role: "User",
      fullName: "Nguyen Minh Thu",
      gender: "Female",
      avatar: "https://i.pravatar.cc/300?img=32"
    },
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
    applicant: {
      email: "applicant@movify.mock",
      phoneNumber: "0900000004",
      password: "Applicant@123",
      role: "User",
      fullName: "Le Hoang Nam",
      gender: "Male",
      avatar: "https://i.pravatar.cc/300?img=14"
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
    {
      key: "completedTrip",
      id: 900301,
      distance: "6.80",
      totalAmount: "52000.00",
      pickUpLocation: "227 Nguyen Van Cu, Quan 5, TP.HCM",
      destinationLocation: "Cho Ben Thanh, Quan 1, TP.HCM",
      duration: "18.00",
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
      key: "activeTrip",
      id: 900302,
      distance: "4.20",
      totalAmount: "36000.00",
      pickUpLocation: "54/30 Le Van Luong, Quan 7, TP.HCM",
      destinationLocation: "RMIT Viet Nam, Quan 7, TP.HCM",
      duration: "12.00",
      status: "Accepted",
      pickUpTime: "2026-04-20T14:00:00+07:00",
      completedTime: null,
      paymentMethod: "BankTransfer",
      paymentStatus: "Unpaid",
      userKey: "rider",
      driverKey: "driver",
      promotionKey: null
    }
  ],
  ratings: [
    {
      id: 900401,
      tripKey: "completedTrip",
      raterKey: "rider",
      rateeKey: "driver",
      score: "4.8",
      comment: "Tai xe den dung gio va di chuyen rat on."
    }
  ],
  notifications: [
    {
      id: 900501,
      accountKey: "rider",
      title: "Dat xe thanh cong",
      body: "Tai xe Tran Quoc Bao da nhan chuyen di cua ban.",
      isRead: false
    },
    {
      id: 900502,
      accountKey: "driver",
      title: "Chuyen di moi",
      body: "Ban vua duoc gan chuyen di tu Quan 7 den Cho Ben Thanh.",
      isRead: false
    },
    {
      id: 900503,
      accountKey: "applicant",
      title: "Ho so dang cho duyet",
      body: "Yeu cau nang cap tai khoan len driver dang duoc xem xet.",
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
    }
  ]
};
