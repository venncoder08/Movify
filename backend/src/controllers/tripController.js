const { PrismaClient } = require("@prisma/client");
const { stripCurrentLocationLabel } = require("../utils/locationLabel");

const prisma = new PrismaClient();

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Map Android vehicleType → Prisma TripType enum
 * "motorbike" → "Motor"
 * "car"       → "Taxi"
 */
function toTripType(vehicleType) {
  return vehicleType === "car" ? "Taxi" : "Motor";
}

/**
 * Map Android paymentMethod → Prisma PaymentMethod enum
 * "cash"          → "Cash"
 * "bank_transfer" → "BankTransfer"
 */
function toPaymentMethod(paymentMethod) {
  return paymentMethod === "bank_transfer" ? "BankTransfer" : "Cash";
}

const PICKUP_CONFIRM_RADIUS_METERS = 50;
const TRIP_COMPLETION_RADIUS_METERS = 50;
const ACTIVE_TRIP_STATUSES = ["Pending", "Accepted", "InProgress"];
const DRIVER_LOCATION_STATUSES = ["Accepted", "InProgress"];
const CANCELLABLE_TRIP_STATUSES = ["Pending", "Accepted"];

function createHttpError(statusCode, message) {
  const error = new Error(message);
  error.statusCode = statusCode;
  return error;
}

function calculateDistanceMeters(lat1, lng1, lat2, lng2) {
  const earthRadiusMeters = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusMeters * c;
}

function isValidCoordinate(lat, lng) {
  return Number.isFinite(lat)
    && Number.isFinite(lng)
    && lat >= -90
    && lat <= 90
    && lng >= -180
    && lng <= 180
    && !(Math.abs(lat) < 0.000001 && Math.abs(lng) < 0.000001);
}

function buildTripInclude() {
  return {
    Account_Trip_DriverIDToAccount: {
      select: {
        ID: true,
        FullName: true,
        PhoneNumber: true,
        Avatar: true,
        VehicleType: true
      }
    }
  };
}

function buildPickupStartedNotifications(trip) {
  const driverName = trip.Account_Trip_DriverIDToAccount?.FullName || "Tài xế";

  return [
    {
      AccountID: trip.UserID,
      Title: "Chuyến đi đã bắt đầu",
      Body: `${driverName} và bạn đã xác nhận điểm đón. Chuyến đi đang được thực hiện.`
    },
    {
      AccountID: trip.DriverID,
      Title: "Bắt đầu chuyến đi",
      Body: "Hai bên đã xác nhận điểm đón. Hãy di chuyển đến điểm đến."
    }
  ];
}

function buildTripCompletedNotifications(trip) {
  const driverName = trip.Account_Trip_DriverIDToAccount?.FullName || "Tài xế";

  return [
    {
      AccountID: trip.UserID,
      Title: "Chuyến đi đã hoàn thành",
      Body: `${driverName} đã hoàn thành chuyến đi. Cảm ơn bạn đã sử dụng Movify.`
    },
    {
      AccountID: trip.DriverID,
      Title: "Hoàn thành chuyến đi",
      Body: "Chuyến đi đã được ghi nhận hoàn thành."
    }
  ];
}

function buildTripCancelledNotifications(trip, actor) {
  if (actor === "passenger") {
    if (!trip.DriverID) {
      return [];
    }

    return [
      {
        AccountID: trip.DriverID,
        Title: "Hành khách đã hủy chuyến",
        Body: "Hành khách đã hủy chuyến bạn đã nhận. Bạn có thể tiếp tục tìm chuyến mới."
      }
    ];
  }

  if (actor === "driver") {
    return [
      {
        AccountID: trip.UserID,
        Title: "Tài xế đã hủy chuyến",
        Body: "Tài xế đã hủy chuyến. Vui lòng đặt lại chuyến khác."
      }
    ];
  }

  return [
    {
      AccountID: trip.UserID,
      Title: "Chuyến đi đã bị hủy",
      Body: "Quản trị viên đã hủy chuyến đi của bạn."
    },
    {
      AccountID: trip.DriverID,
      Title: "Chuyến đi đã bị hủy",
      Body: "Quản trị viên đã hủy chuyến đi bạn đã nhận."
    }
  ];
}

function buildTripRatedNotification(trip, score) {
  return {
    AccountID: trip.DriverID,
    Title: "Bạn vừa nhận được đánh giá",
    Body: `Hành khách đã đánh giá chuyến đi ${Number(score).toFixed(1)} sao.`
  };
}

/**
 * Serialize a Prisma Trip row into the JSON shape that Android expects
 */
function serializeTrip(trip) {
  const serialized = {
    id:                 trip.ID,
    pickupAddress:      stripCurrentLocationLabel(trip.PickUpLocation),
    pickupLat:          trip.PickUpLatitude  ? parseFloat(trip.PickUpLatitude)  : 0,
    pickupLng:          trip.PickUpLongitude ? parseFloat(trip.PickUpLongitude) : 0,
    destinationAddress: stripCurrentLocationLabel(trip.DestinationLocation),
    destinationLat:     trip.DestinationLatitude  ? parseFloat(trip.DestinationLatitude)  : 0,
    destinationLng:     trip.DestinationLongitude ? parseFloat(trip.DestinationLongitude) : 0,
    fare:               trip.TotalAmount ? parseFloat(trip.TotalAmount) : 0,
    distance:           trip.Distance   ? parseFloat(trip.Distance)    : 0,
    status:             trip.Status,
    tripType:           trip.TripType,
    paymentMethod:      trip.PaymentMethod,
    createdAt:          trip.CreatedAt,
    pickupConfirmation: {
      driverConfirmed: Boolean(trip.DriverPickupConfirmedAt),
      passengerConfirmed: Boolean(trip.PassengerPickupConfirmedAt),
      driverConfirmedAt: trip.DriverPickupConfirmedAt,
      passengerConfirmedAt: trip.PassengerPickupConfirmedAt,
      requiredDriverDistanceMeters: PICKUP_CONFIRM_RADIUS_METERS,
      requiredCompletionDistanceMeters: TRIP_COMPLETION_RADIUS_METERS
    },
    driverLocation: {
      lat: trip.DriverCurrentLatitude ? parseFloat(trip.DriverCurrentLatitude) : null,
      lng: trip.DriverCurrentLongitude ? parseFloat(trip.DriverCurrentLongitude) : null,
      updatedAt: trip.DriverLocationUpdatedAt || null
    }
  };

  if (trip.Account_Trip_DriverIDToAccount) {
    serialized.driver = {
      id: trip.Account_Trip_DriverIDToAccount.ID,
      fullName: trip.Account_Trip_DriverIDToAccount.FullName,
      phoneNumber: trip.Account_Trip_DriverIDToAccount.PhoneNumber,
      avatar: trip.Account_Trip_DriverIDToAccount.Avatar,
      vehicleType: trip.Account_Trip_DriverIDToAccount.VehicleType
    };
  }

  return serialized;
}

// ---------------------------------------------------------------------------
// Controllers
// ---------------------------------------------------------------------------

/**
 * POST /api/trips
 * Create a new trip request (passenger side).
 */
async function createTrip(req, res) {
  try {
    const {
      pickupAddress,
      pickupLat       = 0,
      pickupLng       = 0,
      destinationAddress,
      destinationLat  = 0,
      destinationLng  = 0,
      vehicleType,
      paymentMethod,
      promotionId     = null,
      estimatedFare   = 0,
      distanceMeters  = 0
    } = req.body;

    const activeTrip = await prisma.trip.findFirst({
      where: {
        UserID: req.user.id,
        DeletedAt: null,
        Status: { in: ACTIVE_TRIP_STATUSES }
      },
      include: buildTripInclude(),
      orderBy: { UpdatedAt: "desc" }
    });

    if (activeTrip) {
      return res.status(409).json({
        error: "Conflict",
        message: "Bạn đang có một chuyến chưa kết thúc",
        data: {
          trip: serializeTrip(activeTrip)
        }
      });
    }

    // If a promotionId was supplied, verify it exists and is not soft-deleted
    if (promotionId !== null) {
      const promo = await prisma.promotion.findFirst({
        where: { ID: promotionId, DeletedAt: null }
      });
      if (!promo) {
        return res.status(404).json({
          error: "Not Found",
          message: "Không tìm thấy khuyến mãi hoặc khuyến mãi đã bị xóa"
        });
      }
    }

    const distanceKm = distanceMeters / 1000;

    const newTrip = await prisma.trip.create({
      data: {
        PickUpLocation:       stripCurrentLocationLabel(pickupAddress),
        PickUpLatitude:       (pickupLat  != null && pickupLat  !== 0) ? pickupLat  : null,
        PickUpLongitude:      (pickupLng  != null && pickupLng  !== 0) ? pickupLng  : null,
        DestinationLocation:  stripCurrentLocationLabel(destinationAddress),
        DestinationLatitude:  (destinationLat != null && destinationLat !== 0) ? destinationLat : null,
        DestinationLongitude: (destinationLng != null && destinationLng !== 0) ? destinationLng : null,
        TripType:             toTripType(vehicleType),
        PaymentMethod:        toPaymentMethod(paymentMethod),
        PromotionID:          (promotionId != null && promotionId > 0) ? promotionId : null,
        TotalAmount:          estimatedFare,
        Distance:             distanceKm,
        Status:               "Pending",
        PaymentStatus:        "Unpaid",
        UserID:               req.user.id,
        UpdatedAt:            new Date()
      }
    });

    return res.status(201).json({
      data: {
        message: "Tạo chuyến thành công",
        trip: serializeTrip(newTrip)
      }
    });
  } catch (error) {
    console.error("createTrip error:", error);
    return res.status(500).json({
      error:   "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * GET /api/trips/active
 * Read the newest active trip for the authenticated passenger or driver.
 */
async function getActiveTrip(req, res) {
  try {
    const trip = await prisma.trip.findFirst({
      where: {
        DeletedAt: null,
        Status: { in: ACTIVE_TRIP_STATUSES },
        OR: [
          { UserID: req.user.id },
          { DriverID: req.user.id }
        ]
      },
      include: buildTripInclude(),
      orderBy: { UpdatedAt: "desc" }
    });

    return res.status(200).json({
      data: {
        trip: trip ? serializeTrip(trip) : null
      }
    });
  } catch (error) {
    console.error("getActiveTrip error:", error);
    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * GET /api/trips/:id
 * Read a trip owned by the passenger or assigned to the driver.
 */
async function getTrip(req, res) {
  try {
    const tripId = Number(req.params.id);

    const trip = await prisma.trip.findFirst({
      where: {
        ID: tripId,
        DeletedAt: null
      },
      include: buildTripInclude()
    });

    if (!trip) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy chuyến"
      });
    }

    const isPassenger = trip.UserID === req.user.id;
    const isAssignedDriver = trip.DriverID === req.user.id;
    const isAdmin = req.user.role === "Admin";
    if (!isPassenger && !isAssignedDriver && !isAdmin) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Bạn không có quyền truy cập chuyến này"
      });
    }

    return res.status(200).json({
      data: {
        trip: serializeTrip(trip)
      }
    });
  } catch (error) {
    console.error("getTrip error:", error);
    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * PATCH /api/trips/:id/driver-location
 * Driver publishes the current location for passenger realtime tracking.
 */
async function updateDriverLocation(req, res) {
  try {
    const tripId = Number(req.params.id);
    const driverLat = Number(req.body?.driverLat);
    const driverLng = Number(req.body?.driverLng);

    if (!isValidCoordinate(driverLat, driverLng)) {
      return res.status(400).json({
        error: "Bad Request",
        message: "Vui lòng cung cấp vị trí tài xế hợp lệ"
      });
    }

    const trip = await prisma.trip.findFirst({
      where: {
        ID: tripId,
        DeletedAt: null
      },
      select: {
        ID: true,
        DriverID: true,
        Status: true
      }
    });

    if (!trip) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy chuyến"
      });
    }

    if (trip.DriverID !== req.user.id) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Chỉ tài xế đã nhận chuyến mới có thể cập nhật vị trí chuyến này"
      });
    }

    if (!DRIVER_LOCATION_STATUSES.includes(trip.Status)) {
      return res.status(409).json({
        error: "Conflict",
        message: "Chỉ có thể cập nhật vị trí tài xế khi chuyến đã được nhận hoặc đang diễn ra"
      });
    }

    const updatedTrip = await prisma.trip.update({
      where: { ID: tripId },
      data: {
        DriverCurrentLatitude: driverLat,
        DriverCurrentLongitude: driverLng,
        DriverLocationUpdatedAt: new Date(),
        UpdatedAt: new Date()
      },
      include: buildTripInclude()
    });

    return res.status(200).json({
      data: {
        message: "Đã cập nhật vị trí tài xế",
        trip: serializeTrip(updatedTrip)
      }
    });
  } catch (error) {
    console.error("updateDriverLocation error:", error);
    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * POST /api/trips/:id/pickup-confirm
 * Driver confirms pickup only within 50m; passenger confirms after driver arrival.
 */
async function confirmPickup(req, res) {
  try {
    const tripId = Number(req.params.id);
    const { driverLat, driverLng } = req.body || {};

    const result = await prisma.$transaction(async (transaction) => {
      const trip = await transaction.trip.findFirst({
        where: {
          ID: tripId,
          DeletedAt: null
        },
        include: buildTripInclude()
      });

      if (!trip) {
        throw createHttpError(404, "Không tìm thấy chuyến");
      }

      const isPassenger = trip.UserID === req.user.id;
      const isDriver = trip.DriverID === req.user.id;
      if (!isPassenger && !isDriver) {
        throw createHttpError(403, "Bạn không có quyền truy cập chuyến này");
      }

      if (trip.Status !== "Accepted" && trip.Status !== "InProgress") {
        throw createHttpError(409, "Chỉ có thể xác nhận đón khách sau khi chuyến đã được nhận");
      }

      if (trip.Status === "InProgress") {
        return {
          trip,
          pickupDistanceMeters: null
        };
      }

      const now = new Date();
      const data = { UpdatedAt: now };
      let pickupDistanceMeters = null;
      let newlyDriverConfirmed = false;

      if (isDriver) {
        const lat = Number(driverLat);
        const lng = Number(driverLng);
        if (!isValidCoordinate(lat, lng)) {
          throw createHttpError(400, "Vui lòng gửi vĩ độ và kinh độ của tài xế");
        }

        const pickupLat = trip.PickUpLatitude ? parseFloat(trip.PickUpLatitude) : null;
        const pickupLng = trip.PickUpLongitude ? parseFloat(trip.PickUpLongitude) : null;
        if (!isValidCoordinate(pickupLat, pickupLng)) {
          throw createHttpError(409, "Chuyến đang thiếu tọa độ điểm đón");
        }

        pickupDistanceMeters = calculateDistanceMeters(lat, lng, pickupLat, pickupLng);
        if (pickupDistanceMeters > PICKUP_CONFIRM_RADIUS_METERS) {
          const error = createHttpError(
            409,
            `Tài xế phải cách điểm đón tối đa ${PICKUP_CONFIRM_RADIUS_METERS}m để xác nhận`
          );
          error.pickupDistanceMeters = pickupDistanceMeters;
          throw error;
        }

        data.DriverCurrentLatitude = lat;
        data.DriverCurrentLongitude = lng;
        data.DriverLocationUpdatedAt = now;

        if (!trip.DriverPickupConfirmedAt) {
          data.DriverPickupConfirmedAt = now;
          newlyDriverConfirmed = true;
        }
      } else {
        if (!trip.DriverPickupConfirmedAt) {
          throw createHttpError(409, "Tài xế chưa xác nhận đã đến điểm đón");
        }

        if (!trip.PassengerPickupConfirmedAt) {
          data.PassengerPickupConfirmedAt = now;
        }
      }

      const driverConfirmedAt = data.DriverPickupConfirmedAt || trip.DriverPickupConfirmedAt;
      const passengerConfirmedAt = data.PassengerPickupConfirmedAt || trip.PassengerPickupConfirmedAt;
      const startsTrip = Boolean(driverConfirmedAt && passengerConfirmedAt);
      if (startsTrip) {
        data.Status = "InProgress";
        data.PickUpTime = now;
      }

      const updatedTrip = await transaction.trip.update({
        where: { ID: tripId },
        data,
        include: buildTripInclude()
      });

      if (startsTrip) {
        await transaction.notification.createMany({
          data: buildPickupStartedNotifications(updatedTrip)
            .filter((notification) => notification.AccountID)
            .map((notification) => ({
              ...notification,
              UpdatedAt: now
            }))
        });
      } else if (newlyDriverConfirmed) {
        await transaction.notification.create({
          data: {
            AccountID: trip.UserID,
            Title: "Tài xế đã đến điểm đón",
            Body: "Tài xế đã ở gần điểm đón. Vui lòng xác nhận khi bạn đã lên xe.",
            UpdatedAt: now
          }
        });
      }

      return {
        trip: updatedTrip,
        pickupDistanceMeters
      };
    });

    return res.status(200).json({
      data: {
        message: result.trip.Status === "InProgress"
          ? "Hai bên đã xác nhận đón khách; chuyến đang diễn ra"
          : "Đã lưu xác nhận đón khách",
        trip: serializeTrip(result.trip),
        pickupDistanceMeters: result.pickupDistanceMeters
      }
    });
  } catch (error) {
    console.error("confirmPickup error:", error);
    if (error.statusCode) {
      return res.status(Number(error.statusCode)).json({
        error: Number(error.statusCode) === 404
          ? "Not Found"
          : (Number(error.statusCode) === 403 ? "Forbidden" : (Number(error.statusCode) === 400 ? "Bad Request" : "Conflict")),
        message: error.message,
        pickupDistanceMeters: error.pickupDistanceMeters
      });
    }

    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * POST /api/trips/:id/complete
 * Driver completes an in-progress trip only within 50m of the destination.
 */
async function completeTrip(req, res) {
  try {
    const tripId = Number(req.params.id);
    const driverLat = Number(req.body?.driverLat);
    const driverLng = Number(req.body?.driverLng);

    if (!isValidCoordinate(driverLat, driverLng)) {
      return res.status(400).json({
        error: "Bad Request",
        message: "Vui lòng cung cấp vị trí tài xế hợp lệ"
      });
    }

    const result = await prisma.$transaction(async (transaction) => {
      const trip = await transaction.trip.findFirst({
        where: {
          ID: tripId,
          DeletedAt: null
        },
        include: buildTripInclude()
      });

      if (!trip) {
        throw createHttpError(404, "Không tìm thấy chuyến");
      }

      if (trip.DriverID !== req.user.id) {
        throw createHttpError(403, "Chỉ tài xế được gán mới có thể hoàn thành chuyến này");
      }

      if (trip.Status === "Completed") {
        return {
          trip,
          completionDistanceMeters: null
        };
      }

      if (trip.Status !== "InProgress") {
        throw createHttpError(409, "Chỉ chuyến đang diễn ra mới có thể hoàn thành");
      }

      const destinationLat = trip.DestinationLatitude ? parseFloat(trip.DestinationLatitude) : null;
      const destinationLng = trip.DestinationLongitude ? parseFloat(trip.DestinationLongitude) : null;
      if (!isValidCoordinate(destinationLat, destinationLng)) {
        throw createHttpError(409, "Chuyến đang thiếu tọa độ điểm đến");
      }

      const completionDistanceMeters = calculateDistanceMeters(
        driverLat,
        driverLng,
        destinationLat,
        destinationLng
      );
      if (completionDistanceMeters > TRIP_COMPLETION_RADIUS_METERS) {
        const error = createHttpError(
          409,
          `Tài xế phải cách điểm đến tối đa ${TRIP_COMPLETION_RADIUS_METERS}m để hoàn thành chuyến`
        );
        error.completionDistanceMeters = completionDistanceMeters;
        throw error;
      }

      const now = new Date();
      const updatedTrip = await transaction.trip.update({
        where: { ID: tripId },
        data: {
          Status: "Completed",
          CompletedTime: now,
          DriverCurrentLatitude: driverLat,
          DriverCurrentLongitude: driverLng,
          DriverLocationUpdatedAt: now,
          UpdatedAt: now
        },
        include: buildTripInclude()
      });

      await transaction.notification.createMany({
        data: buildTripCompletedNotifications(updatedTrip)
          .filter((notification) => notification.AccountID)
          .map((notification) => ({
            ...notification,
            UpdatedAt: now
          }))
      });

      return {
        trip: updatedTrip,
        completionDistanceMeters
      };
    });

    return res.status(200).json({
      data: {
        message: "Hoàn thành chuyến thành công",
        trip: serializeTrip(result.trip),
        completionDistanceMeters: result.completionDistanceMeters
      }
    });
  } catch (error) {
    console.error("completeTrip error:", error);
    if (error.statusCode) {
      return res.status(Number(error.statusCode)).json({
        error: Number(error.statusCode) === 404
          ? "Not Found"
          : (Number(error.statusCode) === 403 ? "Forbidden" : (Number(error.statusCode) === 400 ? "Bad Request" : "Conflict")),
        message: error.message,
        completionDistanceMeters: error.completionDistanceMeters
      });
    }

    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * POST /api/trips/:id/cancel
 * Cancel a trip before it starts.
 * Passenger can cancel Pending/Accepted trips; assigned driver can cancel Accepted trips.
 */
async function cancelTrip(req, res) {
  try {
    const tripId = Number(req.params.id);

    const result = await prisma.$transaction(async (transaction) => {
      const trip = await transaction.trip.findFirst({
        where: { ID: tripId, DeletedAt: null },
        include: buildTripInclude()
      });

      if (!trip) {
        throw createHttpError(404, "Không tìm thấy chuyến");
      }

      const isPassenger = trip.UserID === req.user.id;
      const isAssignedDriver = trip.DriverID === req.user.id;
      const isAdmin = req.user.role === "Admin";
      if (!isPassenger && !isAssignedDriver && !isAdmin) {
        throw createHttpError(403, "Bạn không có quyền hủy chuyến này");
      }

      if (!CANCELLABLE_TRIP_STATUSES.includes(trip.Status)) {
        throw createHttpError(409, `Không thể hủy chuyến ở trạng thái '${trip.Status}'`);
      }

      if (isAssignedDriver && trip.Status !== "Accepted" && !isAdmin) {
        throw createHttpError(409, "Tài xế chỉ có thể hủy chuyến đã nhận");
      }

      const actor = isAdmin ? "admin" : (isAssignedDriver ? "driver" : "passenger");
      const cancelReason = actor === "driver"
        ? "Tài xế đã hủy chuyến"
        : (actor === "admin"
          ? "Quản trị viên đã hủy chuyến"
          : (trip.Status === "Pending"
            ? "Hành khách đã hủy khi đang tìm tài xế"
            : "Hành khách đã hủy chuyến"));
      const now = new Date();

      const updatedTrip = await transaction.trip.update({
        where: { ID: tripId },
        data: {
          Status: "Cancelled",
          CancelReason: cancelReason,
          UpdatedAt: now
        },
        include: buildTripInclude()
      });

      const notifications = buildTripCancelledNotifications(updatedTrip, actor)
        .filter((notification) => notification.AccountID)
        .map((notification) => ({
          ...notification,
          UpdatedAt: now
        }));

      if (notifications.length > 0) {
        await transaction.notification.createMany({
          data: notifications
        });
      }

      return {
        trip: updatedTrip
      };
    });

    return res.status(200).json({
      data: {
        message: "Hủy chuyến thành công",
        trip: serializeTrip(result.trip)
      }
    });
  } catch (error) {
    console.error("cancelTrip error:", error);
    if (error.statusCode) {
      const statusCode = Number(error.statusCode);
      return res.status(statusCode).json({
        error: statusCode === 404
          ? "Not Found"
          : (statusCode === 403 ? "Forbidden" : "Conflict"),
        message: error.message
      });
    }

    return res.status(500).json({
      error:   "Internal Server Error",
      message: error.message
    });
  }
}

async function rateTrip(req, res) {
  try {
    const tripId = Number(req.params.id);
    const score = Number(req.body?.score);
    const comment = String(req.body?.comment || "").trim();

    const result = await prisma.$transaction(async (transaction) => {
      const trip = await transaction.trip.findFirst({
        where: {
          ID: tripId,
          DeletedAt: null
        },
        include: buildTripInclude()
      });

      if (!trip) {
        throw createHttpError(404, "Không tìm thấy chuyến");
      }

      if (trip.UserID !== req.user.id) {
        throw createHttpError(403, "Chỉ hành khách của chuyến này mới có thể đánh giá tài xế");
      }

      if (trip.Status !== "Completed") {
        throw createHttpError(409, "Chỉ có thể đánh giá sau khi chuyến đã hoàn thành");
      }

      if (!trip.DriverID) {
        throw createHttpError(409, "Chuyến này chưa có tài xế để đánh giá");
      }

      const rating = await transaction.rating.upsert({
        where: {
          TripID_RaterID: {
            TripID: trip.ID,
            RaterID: req.user.id
          }
        },
        update: {
          RateeID: trip.DriverID,
          Score: score,
          Comment: comment || null,
          DeletedAt: null
        },
        create: {
          TripID: trip.ID,
          RaterID: req.user.id,
          RateeID: trip.DriverID,
          Score: score,
          Comment: comment || null
        }
      });

      const aggregate = await transaction.rating.aggregate({
        where: {
          RateeID: trip.DriverID,
          DeletedAt: null
        },
        _avg: {
          Score: true
        }
      });
      const averageScore = aggregate._avg.Score ? Number(aggregate._avg.Score) : 0;
      const roundedAverage = Math.round(averageScore * 100) / 100;

      await transaction.account.update({
        where: { ID: trip.DriverID },
        data: {
          RatePoint: roundedAverage.toFixed(2)
        }
      });

      await transaction.notification.create({
        data: {
          ...buildTripRatedNotification(trip, score),
          IsRead: false
        }
      });

      return {
        rating,
        driverRatePoint: roundedAverage
      };
    });

    return res.status(200).json({
      data: {
        message: "Cảm ơn bạn đã đánh giá tài xế",
        rating: {
          id: result.rating.ID,
          tripId: result.rating.TripID,
          score: Number(result.rating.Score),
          comment: result.rating.Comment,
          createdAt: result.rating.CreatedAt
        },
        driverRatePoint: result.driverRatePoint
      }
    });
  } catch (error) {
    console.error("rateTrip error:", error);
    return res.status(error.statusCode || 500).json({
      error: error.statusCode ? "Request Error" : "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = { createTrip, getActiveTrip, getTrip, updateDriverLocation, confirmPickup, completeTrip, cancelTrip, rateTrip };
