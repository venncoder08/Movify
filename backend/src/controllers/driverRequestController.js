const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function toVehicleType(value) {
  return value === "car" || value === "Taxi" ? "Taxi" : "Motor";
}

const DRIVER_REQUEST_INCLUDE = {
  Account_ToDriverRequest_DriverIDToAccount: {
    select: {
      ID: true,
      FullName: true,
      Email: true,
      PhoneNumber: true,
      Avatar: true,
      Role: true,
      DriverStatus: true,
      VehicleType: true
    }
  },
  Account_ToDriverRequest_AdminIDToAccount: {
    select: {
      ID: true,
      FullName: true,
      Email: true
    }
  }
};

function serializeDriverRequest(request) {
  if (!request) {
    return null;
  }

  const driver = request.Account_ToDriverRequest_DriverIDToAccount;
  const admin = request.Account_ToDriverRequest_AdminIDToAccount;

  return {
    id: request.ID,
    idCard: request.IDCard,
    driverLicense: request.DriverLicense,
    vehicleRegistration: request.VehicleRegistration,
    vehicleType: request.VehicleType,
    status: request.Status,
    createdAt: request.CreatedAt,
    updatedAt: request.UpdatedAt,
    driver: driver
      ? {
          id: driver.ID,
          fullName: driver.FullName,
          email: driver.Email,
          phoneNumber: driver.PhoneNumber,
          avatar: driver.Avatar,
          role: driver.Role,
          driverStatus: driver.DriverStatus,
          vehicleType: driver.VehicleType
        }
      : null,
    admin: admin
      ? {
          id: admin.ID,
          fullName: admin.FullName,
          email: admin.Email
        }
      : null
  };
}

async function getMyDriverRequest(req, res) {
  try {
    const account = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: {
        Role: true,
        DriverStatus: true,
        IDCard: true,
        DriverLicense: true,
        VehicleRegistration: true,
        VehicleType: true
      }
    });

    const request = await prisma.toDriverRequest.findFirst({
      where: {
        DriverID: req.user.id,
        DeletedAt: null
      },
      orderBy: {
        CreatedAt: "desc"
      }
    });

    res.status(200).json({
      data: {
        accountRole: account?.Role ?? "User",
        driverStatus: account?.DriverStatus ?? null,
        profileDocuments: {
          idCard: account?.IDCard ?? null,
          driverLicense: account?.DriverLicense ?? null,
          vehicleRegistration: account?.VehicleRegistration ?? null,
          vehicleType: account?.VehicleType ?? null
        },
        latestRequest: serializeDriverRequest(request)
      }
    });
  } catch (error) {
    console.error("Get my driver request error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function createDriverRequest(req, res) {
  try {
    const { idCard, driverLicense, vehicleRegistration, vehicleType } = req.body;
    const normalizedVehicleType = toVehicleType(vehicleType);

    const account = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: {
        ID: true,
        Role: true,
        FullName: true,
        DriverStatus: true
      }
    });

    if (!account) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy người dùng"
      });
    }

    if (account.Role === "Driver" || account.DriverStatus === "Approved") {
      return res.status(409).json({
        error: "Conflict",
        message: "Tài khoản này đã là tài xế"
      });
    }

    const pendingRequest = await prisma.toDriverRequest.findFirst({
      where: {
        DriverID: req.user.id,
        Status: "Pending",
        DeletedAt: null
      },
      orderBy: {
        CreatedAt: "desc"
      }
    });

    if (pendingRequest) {
      return res.status(409).json({
        error: "Conflict",
        message: "Bạn đã có yêu cầu chờ duyệt"
      });
    }

    const createdRequest = await prisma.$transaction(async (transaction) => {
      const request = await transaction.toDriverRequest.create({
        data: {
          DriverID: req.user.id,
          IDCard: idCard,
          DriverLicense: driverLicense,
          VehicleRegistration: vehicleRegistration,
          VehicleType: normalizedVehicleType,
          Status: "Pending"
        }
      });

      await transaction.account.update({
        where: { ID: req.user.id },
        data: {
          IDCard: idCard,
          DriverLicense: driverLicense,
          VehicleRegistration: vehicleRegistration,
          VehicleType: normalizedVehicleType,
          DriverStatus: "Pending"
        }
      });

      await transaction.notification.create({
        data: {
          AccountID: req.user.id,
          Title: "Gửi yêu cầu trở thành tài xế thành công",
          Body: `Movify đã nhận hồ sơ của ${account.FullName || "bạn"} và sẽ xem xét trong thời gian sớm nhất.`,
          IsRead: false
        }
      });

      const admins = await transaction.account.findMany({
        where: {
          Role: "Admin",
          DeletedAt: null,
          IsActive: true
        },
        select: { ID: true }
      });

      if (admins.length > 0) {
        await transaction.notification.createMany({
          data: admins.map((admin) => ({
            AccountID: admin.ID,
            Title: "Có hồ sơ tài xế mới",
            Body: `${account.FullName || "Người dùng"} vừa gửi yêu cầu trở thành tài xế.`,
            IsRead: false
          }))
        });
      }

      return request;
    });

    res.status(201).json({
      message: "Gửi yêu cầu thành công",
      data: serializeDriverRequest(createdRequest)
    });
  } catch (error) {
    console.error("Create driver request error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function listDriverRequests(req, res) {
  try {
    const status = String(req.query.status || "").trim();
    const where = {
      DeletedAt: null
    };

    if (["Pending", "Approved", "Rejected"].includes(status)) {
      where.Status = status;
    }

    const requests = await prisma.toDriverRequest.findMany({
      where,
      include: DRIVER_REQUEST_INCLUDE,
      orderBy: {
        CreatedAt: "desc"
      }
    });

    res.status(200).json({
      data: requests.map(serializeDriverRequest)
    });
  } catch (error) {
    console.error("List driver requests error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function updateDriverRequestStatus(req, res) {
  try {
    const requestId = Number(req.params.id);
    const { status } = req.body;

    const request = await prisma.toDriverRequest.findUnique({
      where: { ID: requestId },
      include: {
        Account_ToDriverRequest_DriverIDToAccount: {
          select: {
            ID: true,
            FullName: true,
            Role: true
          }
        }
      }
    });

    if (!request || request.DeletedAt) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy hồ sơ tài xế"
      });
    }

    if (request.Status !== "Pending") {
      return res.status(409).json({
        error: "Conflict",
        message: "Hồ sơ tài xế đã được duyệt trước đó"
      });
    }

    const updatedRequest = await prisma.$transaction(async (transaction) => {
      await transaction.toDriverRequest.update({
        where: { ID: request.ID },
        data: {
          Status: status,
          AdminID: req.user.id
        }
      });

      if (status === "Approved") {
        await transaction.account.update({
          where: { ID: request.DriverID },
          data: {
            Role: "Driver",
            DriverStatus: "Approved",
            IDCard: request.IDCard,
            DriverLicense: request.DriverLicense,
            VehicleRegistration: request.VehicleRegistration,
            VehicleType: request.VehicleType
          }
        });
      } else {
        await transaction.account.update({
          where: { ID: request.DriverID },
          data: {
            DriverStatus: "Rejected"
          }
        });
      }

      await transaction.notification.create({
        data: {
          AccountID: request.DriverID,
          Title: status === "Approved" ? "Hồ sơ tài xế đã được duyệt" : "Hồ sơ tài xế bị từ chối",
          Body: status === "Approved"
            ? "Tài khoản của bạn đã được kích hoạt chế độ tài xế."
            : "Hồ sơ tài xế của bạn chưa được duyệt. Vui lòng kiểm tra và gửi lại ảnh tài liệu rõ ràng hơn.",
          IsRead: false
        }
      });

      return transaction.toDriverRequest.findUnique({
        where: { ID: request.ID },
        include: DRIVER_REQUEST_INCLUDE
      });
    });

    res.status(200).json({
      message: status === "Approved" ? "Đã duyệt hồ sơ tài xế" : "Đã từ chối hồ sơ tài xế",
      data: serializeDriverRequest(updatedRequest)
    });
  } catch (error) {
    console.error("Update driver request status error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  getMyDriverRequest,
  createDriverRequest,
  listDriverRequests,
  updateDriverRequestStatus
};
