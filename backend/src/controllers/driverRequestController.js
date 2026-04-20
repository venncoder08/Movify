const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function serializeDriverRequest(request) {
  if (!request) {
    return null;
  }

  return {
    id: request.id,
    idCard: request.idCard,
    driverLicense: request.driverLicense,
    vehicleRegistration: request.vehicleRegistration,
    status: request.status,
    createdAt: request.createdAt,
    updatedAt: request.updatedAt
  };
}

async function getMyDriverRequest(req, res) {
  try {
    const account = await prisma.account.findUnique({
      where: { id: req.user.id },
      select: {
        role: true,
        driverStatus: true,
        idCard: true,
        driverLicense: true,
        vehicleRegistration: true
      }
    });

    const request = await prisma.toDriverRequest.findFirst({
      where: {
        driverId: req.user.id,
        deletedAt: null
      },
      orderBy: {
        createdAt: "desc"
      }
    });

    res.status(200).json({
      data: {
        accountRole: account?.role ?? "User",
        driverStatus: account?.driverStatus ?? null,
        profileDocuments: {
          idCard: account?.idCard ?? null,
          driverLicense: account?.driverLicense ?? null,
          vehicleRegistration: account?.vehicleRegistration ?? null
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
      where: { id: req.user.id },
      select: {
        id: true,
        role: true,
        fullName: true,
        driverStatus: true
      }
    });

    if (!account) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    if (account.role === "Driver" || account.driverStatus === "Approved") {
      return res.status(409).json({
        error: "Conflict",
        message: "Tai khoan nay da la tai xe"
      });
    }

    const pendingRequest = await prisma.toDriverRequest.findFirst({
      where: {
        driverId: req.user.id,
        status: "Pending",
        deletedAt: null
      },
      orderBy: {
        createdAt: "desc"
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
          driverId: req.user.id,
          idCard,
          driverLicense,
          vehicleRegistration,
          status: "Pending"
        }
      });

      await transaction.account.update({
        where: { id: req.user.id },
        data: {
          idCard,
          driverLicense,
          vehicleRegistration,
          driverStatus: "Pending"
        }
      });

      await transaction.notification.create({
        data: {
          accountId: req.user.id,
          title: "Gui yeu cau tro thanh tai xe thanh cong",
          body: `Movify da nhan ho so cua ${account.fullName || "ban"} va se xem xet trong thoi gian som nhat.`,
          isRead: false
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
