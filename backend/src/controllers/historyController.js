const { PrismaClient } = require("@prisma/client");
const { stripCurrentLocationLabel } = require("../utils/locationLabel");

const prisma = new PrismaClient();

function getReferenceDate(trip) {
  return trip.CompletedTime || trip.PickUpTime || trip.CreatedAt;
}

function serializeTrip(trip, currentUserId, currentUserRole) {
  const ratings = trip.Rating || [];
  const rating = ratings.find((r) => {
    if (currentUserRole === "Driver") {
      return r.RateeID === currentUserId;
    }

    return r.RaterID === currentUserId;
  });

  console.log("[history] serializeTrip", {
    tripId: trip.ID,
    currentUserId,
    currentUserRole,
    ratingCount: ratings.length,
    matchedRating: rating
      ? {
          id: rating.ID,
          tripId: rating.TripID,
          raterId: rating.RaterID,
          rateeId: rating.RateeID,
          score: Number(rating.Score),
          comment: rating.Comment
        }
      : null
  });

  const driver = trip.Account_Trip_DriverIDToAccount;

  return {
    id: trip.ID,
    status: trip.Status,
    tripType: trip.TripType,
    pickUpLocation: stripCurrentLocationLabel(trip.PickUpLocation),
    destinationLocation: stripCurrentLocationLabel(trip.DestinationLocation),
    duration: trip.Duration ? Number(trip.Duration) : null,
    distance: Number(trip.Distance),
    totalAmount: Number(trip.TotalAmount),
    pickUpTime: trip.PickUpTime,
    completedTime: trip.CompletedTime,
    createdAt: trip.CreatedAt,
    paymentMethod: trip.PaymentMethod,
    paymentStatus: trip.PaymentStatus,
    ratingScore: rating ? Number(rating.Score) : null,
    ratingComment: rating ? (rating.Comment || null) : null,
    driver: driver
      ? {
          id: driver.ID,
          fullName: driver.FullName,
          vehicleRegistration: driver.VehicleRegistration,
          ratePoint: Number(driver.RatePoint),
          avatar: driver.Avatar
        }
      : null
  };
}

async function getHistory(req, res) {
  try {
    const isDriver = req.user.role === "Driver";
    console.log("[history] request", {
      userId: req.user.id,
      role: req.user.role,
      isDriver
    });

    const trips = await prisma.trip.findMany({
      where: isDriver
        ? {
            DriverID: req.user.id,
            DeletedAt: null
          }
        : {
            UserID: req.user.id,
            DeletedAt: null
          },
      orderBy: {
        CreatedAt: "desc"
      },
      include: {
        Account_Trip_DriverIDToAccount: {
          select: {
            ID: true,
            FullName: true,
            VehicleRegistration: true,
            RatePoint: true,
            Avatar: true
          }
        },
        Rating: {
          select: {
            RaterID: true,
            RateeID: true,
            Score: true,
            Comment: true
          }
        }
      }
    });

    const now = new Date();
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const currentMonthTrips = trips.filter((trip) => {
      const referenceDate = getReferenceDate(trip);
      const keep = referenceDate >= monthStart && trip.Status !== "Cancelled";
      if (!keep) {
        console.log("[history] filtered trip", {
          tripId: trip.ID,
          status: trip.Status,
          referenceDate,
          monthStart
        });
      }
      return keep;
    });

    console.log("[history] trips loaded", {
      totalTrips: trips.length,
      currentMonthTrips: currentMonthTrips.length
    });

    const summary = currentMonthTrips.reduce((result, trip) => {
      if (trip.Status === "Completed") {
        result.totalTrips += 1;
        result.totalDistance += Number(trip.Distance);
        result.totalAmount += Number(trip.TotalAmount);
      }

      return result;
    }, {
      totalAmount: 0,
      totalTrips: 0,
      totalDistance: 0
    });

    res.status(200).json({
      data: {
        summary: {
          ...summary,
          periodLabel: "Tháng này"
        },
        items: currentMonthTrips.map((trip) => serializeTrip(trip, req.user.id, req.user.role))
      }
    });
  } catch (error) {
    console.error("Get history error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  getHistory
};
