const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function serializeDriverRequest(request) {
  if (!request) {
    return null;
  }

  return {
    id: request.ID,
    idCard: request.IDCard,
    driverLicense: request.DriverLicense,
    vehicleRegistration: request.VehicleRegistration,
    status: request.Status,
    createdAt: request.CreatedAt,
    updatedAt: request.UpdatedAt
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
        VehicleRegistration: true
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
          vehicleRegistration: account?.VehicleRegistration ?? null
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
    const { idCard, driverLicense, vehicleRegistration } = req.body;

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
        message: "User not found"
      });
    }

    if (account.Role === "Driver" || account.DriverStatus === "Approved") {
      return res.status(409).json({
        error: "Conflict",
        message: "Tai khoan nay da la tai xe"
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
        message: "Ban da co yeu cau cho duyet"
      });
    }

    const createdRequest = await prisma.$transaction(async (transaction) => {
      const request = await transaction.toDriverRequest.create({
        data: {
          DriverID: req.user.id,
          IDCard: idCard,
          DriverLicense: driverLicense,
          VehicleRegistration: vehicleRegistration,
          Status: "Pending"
        }
      });

      await transaction.account.update({
        where: { ID: req.user.id },
        data: {
          IDCard: idCard,
          DriverLicense: driverLicense,
          VehicleRegistration: vehicleRegistration,
          DriverStatus: "Pending"
        }
      });

      await transaction.notification.create({
        data: {
          AccountID: req.user.id,
          Title: "Gui yeu cau tro thanh tai xe thanh cong",
          Body: `Movify da nhan ho so cua ${account.FullName || "ban"} va se xem xet trong thoi gian som nhat.`,
          IsRead: false
        }
      });

      return request;
    });

    res.status(201).json({
      message: "Gui yeu cau thanh cong",
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

module.exports = {
  getMyDriverRequest,
  createDriverRequest
};
