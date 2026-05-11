const { PrismaClient } = require("@prisma/client");
const { stripCurrentLocationLabel } = require("../utils/locationLabel");
const { emitConversationEligibilityForTrip } = require("../services/messageTripService");

const prisma = new PrismaClient();
const ACTIVE_DRIVER_TRIP_STATUSES = ["Accepted", "InProgress"];

/**
 * Calculate distance between two coordinates using Haversine formula (in km)
 */
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371; // Earth's radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Serialize trip data
 */
function serializeTrip(trip, distance) {
  const serialized = {
    id: trip.ID,
    pickupAddress: stripCurrentLocationLabel(trip.PickUpLocation),
    pickupLat: trip.PickUpLatitude ? parseFloat(trip.PickUpLatitude) : null,
    pickupLng: trip.PickUpLongitude ? parseFloat(trip.PickUpLongitude) : null,
    destinationAddress: stripCurrentLocationLabel(trip.DestinationLocation),
    destinationLat: trip.DestinationLatitude ? parseFloat(trip.DestinationLatitude) : null,
    destinationLng: trip.DestinationLongitude ? parseFloat(trip.DestinationLongitude) : null,
    fare: trip.TotalAmount ? parseFloat(trip.TotalAmount) : 0,
    distance: distance ? parseFloat(distance.toFixed(2)) : 0,
    tripType: trip.TripType,
    status: trip.Status,
    createdAt: trip.CreatedAt,
    pickupConfirmation: {
      driverConfirmed: Boolean(trip.DriverPickupConfirmedAt),
      passengerConfirmed: Boolean(trip.PassengerPickupConfirmedAt),
      driverConfirmedAt: trip.DriverPickupConfirmedAt,
      passengerConfirmedAt: trip.PassengerPickupConfirmedAt,
      requiredDriverDistanceMeters: 50
    },
    driverLocation: {
      lat: trip.DriverCurrentLatitude ? parseFloat(trip.DriverCurrentLatitude) : null,
      lng: trip.DriverCurrentLongitude ? parseFloat(trip.DriverCurrentLongitude) : null,
      updatedAt: trip.DriverLocationUpdatedAt || null
    }
  };

  if (trip.Account_Trip_UserIDToAccount) {
    serialized.passenger = {
      id: trip.Account_Trip_UserIDToAccount.ID,
      fullName: trip.Account_Trip_UserIDToAccount.FullName,
      phoneNumber: trip.Account_Trip_UserIDToAccount.PhoneNumber,
      avatar: trip.Account_Trip_UserIDToAccount.Avatar
    };
  }

  return serialized;
}

function buildDriverAcceptedNotification(driver) {
  const driverName = driver.FullName || "Tài xế";
  const phoneSuffix = driver.PhoneNumber ? ` SĐT: ${driver.PhoneNumber}.` : "";

  return {
    title: "Tài xế đã nhận chuyến",
    body: `${driverName} đã nhận chuyến và đang di chuyển đến điểm đón của bạn.${phoneSuffix}`
  };
}

function ensureDriverVehicleType(account, res) {
  if (account.VehicleType !== "Motor" && account.VehicleType !== "Taxi") {
    res.status(409).json({
      error: "Conflict",
      message: "Tài xế chưa được cấu hình loại xe"
    });
    return null;
  }

  return account.VehicleType;
}

/**
 * Get nearby available trips for driver
 * @param {Request} req
 * @param {Response} res
 */
async function getNearbyTrips(req, res) {
  try {
    const { lat, lng, radius = 5 } = req.query;
    const driverLat = parseFloat(lat);
    const driverLng = parseFloat(lng);

    // Validate coordinates
    if (isNaN(driverLat) || isNaN(driverLng)) {
      return res.status(400).json({
        error: "Bad Request",
        message: "Vĩ độ hoặc kinh độ không hợp lệ"
      });
    }

    // Verify user is a driver
    const account = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: { Role: true, DriverStatus: true, VehicleType: true }
    });

    if (!account || account.Role !== "Driver" || account.DriverStatus !== "Approved") {
      return res.status(403).json({
        error: "Forbidden",
        message: "Chỉ tài xế đã được duyệt mới có thể xem chuyến đang có"
      });
    }

    const driverVehicleType = ensureDriverVehicleType(account, res);
    if (!driverVehicleType) {
      return;
    }

    // Fetch all pending trips with no driver assigned
    const trips = await prisma.trip.findMany({
      where: {
        Status: "Pending",
        DriverID: null,
        DeletedAt: null,
        TripType: driverVehicleType
      },
      select: {
        ID: true,
        PickUpLocation: true,
        PickUpLatitude: true,
        PickUpLongitude: true,
        DestinationLocation: true,
        DestinationLatitude: true,
        DestinationLongitude: true,
        TotalAmount: true,
        TripType: true,
        Status: true,
        DriverPickupConfirmedAt: true,
        PassengerPickupConfirmedAt: true,
        DriverCurrentLatitude: true,
        DriverCurrentLongitude: true,
        DriverLocationUpdatedAt: true,
        CreatedAt: true
      },
      orderBy: {
        CreatedAt: "desc"
      }
    });

    // Split trips into: those with coordinates (can filter by radius) and those without
    const withCoords    = [];
    const withoutCoords = [];

    trips.forEach((trip) => {
      if (trip.PickUpLatitude && trip.PickUpLongitude) {
        withCoords.push(trip);
      } else {
        withoutCoords.push(trip);
      }
    });

    // Filter coordinate-based trips by radius, sort by distance ascending
    const nearbyWithCoords = withCoords
      .map((trip) => {
        const distance = calculateDistance(
          driverLat,
          driverLng,
          parseFloat(trip.PickUpLatitude),
          parseFloat(trip.PickUpLongitude)
        );
        return distance <= radius ? { trip, distance } : null;
      })
      .filter(Boolean)
      .sort((a, b) => a.distance - b.distance)
      .map(({ trip, distance }) => serializeTrip(trip, distance));

    // Trips without coordinates are always shown (address-only bookings)
    const nearbyWithoutCoords = withoutCoords.map((trip) => serializeTrip(trip, null));

    const nearbyTrips = [...nearbyWithCoords, ...nearbyWithoutCoords];

    res.status(200).json({
      data: {
        trips: nearbyTrips,
        count: nearbyTrips.length,
        driverLocation: {
          lat: driverLat,
          lng: driverLng
        },
        searchRadius: radius,
        vehicleType: driverVehicleType
      }
    });
  } catch (error) {
    console.error("Get nearby trips error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Accept a trip
 * @param {Request} req
 * @param {Response} res
 */
async function acceptTrip(req, res) {
  try {
    const { tripId } = req.body;

    // Verify user is a driver
    const account = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: {
        Role: true,
        DriverStatus: true,
        VehicleType: true,
        FullName: true,
        PhoneNumber: true
      }
    });

    if (!account || account.Role !== "Driver" || account.DriverStatus !== "Approved") {
      return res.status(403).json({
        error: "Forbidden",
        message: "Chỉ tài xế đã được duyệt mới có thể nhận chuyến"
      });
    }

    const driverVehicleType = ensureDriverVehicleType(account, res);
    if (!driverVehicleType) {
      return;
    }

    const updatedTrip = await prisma.$transaction(async (transaction) => {
      const activeTrip = await transaction.trip.findFirst({
        where: {
          DriverID: req.user.id,
          DeletedAt: null,
          Status: { in: ACTIVE_DRIVER_TRIP_STATUSES }
        },
        select: { ID: true, Status: true }
      });

      if (activeTrip) {
        const error = new Error("Tài xế đang có chuyến hoạt động");
        error.statusCode = 409;
        throw error;
      }

      const trip = await transaction.trip.findUnique({
        where: { ID: tripId },
        select: {
          ID: true,
          Status: true,
          DriverID: true,
          UserID: true,
          TripType: true
        }
      });

      if (!trip) {
        const error = new Error("Không tìm thấy chuyến");
        error.statusCode = 404;
        throw error;
      }

      if (trip.Status !== "Pending") {
        const error = new Error("Chuyến không còn khả dụng");
        error.statusCode = 409;
        throw error;
      }

      if (trip.DriverID !== null) {
        const error = new Error("Chuyến đã được tài xế khác nhận");
        error.statusCode = 409;
        throw error;
      }

      if (trip.TripType !== driverVehicleType) {
        const error = new Error("Loại xe của tài xế không phù hợp với chuyến này");
        error.statusCode = 409;
        throw error;
      }

      const claimResult = await transaction.trip.updateMany({
        where: {
          ID: tripId,
          Status: "Pending",
          DriverID: null,
          TripType: driverVehicleType
        },
        data: {
          DriverID: req.user.id,
          Status: "Accepted",
          UpdatedAt: new Date()
        }
      });

      if (claimResult.count !== 1) {
        const error = new Error("Chuyến không còn khả dụng");
        error.statusCode = 409;
        throw error;
      }

      const acceptedTrip = await transaction.trip.findUnique({
        where: { ID: tripId },
        select: {
          ID: true,
          UserID: true,
          DriverID: true,
          PickUpLocation: true,
          PickUpLatitude: true,
          PickUpLongitude: true,
          DestinationLocation: true,
          DestinationLatitude: true,
          DestinationLongitude: true,
          TotalAmount: true,
          TripType: true,
          Status: true,
          DriverPickupConfirmedAt: true,
          PassengerPickupConfirmedAt: true,
          DriverCurrentLatitude: true,
          DriverCurrentLongitude: true,
          DriverLocationUpdatedAt: true,
          CreatedAt: true,
          Account_Trip_UserIDToAccount: {
            select: {
              ID: true,
              FullName: true,
              PhoneNumber: true,
              Avatar: true
            }
          }
        }
      });

      const notification = buildDriverAcceptedNotification(account);
      await transaction.notification.create({
        data: {
          AccountID: trip.UserID,
          Title: notification.title,
          Body: notification.body,
          UpdatedAt: new Date()
        }
      });

      return acceptedTrip;
    });

    res.status(200).json({
      data: {
        message: "Nhận chuyến thành công",
        trip: serializeTrip(updatedTrip, 0)
      }
    });
    emitConversationEligibilityForTrip(updatedTrip);
  } catch (error) {
    console.error("Accept trip error:", error);
    if (error.statusCode) {
      return res.status(error.statusCode).json({
        error: error.statusCode === 404 ? "Not Found" : "Conflict",
        message: error.message
      });
    }

    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  getNearbyTrips,
  acceptTrip
};
