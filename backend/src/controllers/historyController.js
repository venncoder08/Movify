const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function getReferenceDate(trip) {
  return trip.CompletedTime || trip.PickUpTime || trip.CreatedAt;
}

function serializeTrip(trip) {
  return {
    id: trip.ID,
    status: trip.Status,
    tripType: trip.TripType,
    pickUpLocation: trip.PickUpLocation,
    destinationLocation: trip.DestinationLocation,
    duration: trip.Duration ? Number(trip.Duration) : null,
    distance: Number(trip.Distance),
    totalAmount: Number(trip.TotalAmount),
    pickUpTime: trip.PickUpTime,
    completedTime: trip.CompletedTime,
    createdAt: trip.CreatedAt,
    paymentMethod: trip.PaymentMethod,
    paymentStatus: trip.PaymentStatus,
    driver: trip.Account_Trip_DriverIDToAccount
      ? {
          id: trip.Account_Trip_DriverIDToAccount.ID,
          fullName: trip.Account_Trip_DriverIDToAccount.FullName,
          vehicleRegistration: trip.Account_Trip_DriverIDToAccount.VehicleRegistration,
          ratePoint: Number(trip.Account_Trip_DriverIDToAccount.RatePoint),
          avatar: trip.Account_Trip_DriverIDToAccount.Avatar
        }
      : null
  };
}

async function getHistory(req, res) {
  try {
    const trips = await prisma.trip.findMany({
      where: {
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
        }
      }
    });

    const now = new Date();
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const currentMonthTrips = trips.filter((trip) => {
      const referenceDate = getReferenceDate(trip);
      return referenceDate >= monthStart && trip.Status !== "Cancelled";
    });

    const summary = currentMonthTrips.reduce((result, trip) => {
      result.totalTrips += 1;
      result.totalDistance += Number(trip.Distance);

      if (trip.Status === "Completed") {
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
          periodLabel: "Thang nay"
        },
        items: trips.map(serializeTrip)
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
