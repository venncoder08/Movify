const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function getReferenceDate(trip) {
  return trip.completedTime || trip.pickUpTime || trip.createdAt;
}

function serializeTrip(trip) {
  return {
    id: trip.id,
    status: trip.status,
    tripType: trip.tripType,
    pickUpLocation: trip.pickUpLocation,
    destinationLocation: trip.destinationLocation,
    duration: trip.duration ? Number(trip.duration) : null,
    distance: Number(trip.distance),
    totalAmount: Number(trip.totalAmount),
    pickUpTime: trip.pickUpTime,
    completedTime: trip.completedTime,
    createdAt: trip.createdAt,
    paymentMethod: trip.paymentMethod,
    paymentStatus: trip.paymentStatus,
    driver: trip.driver
      ? {
          id: trip.driver.id,
          fullName: trip.driver.fullName,
          vehicleRegistration: trip.driver.vehicleRegistration,
          ratePoint: Number(trip.driver.ratePoint),
          avatar: trip.driver.avatar
        }
      : null
  };
}

async function getHistory(req, res) {
  try {
    const trips = await prisma.trip.findMany({
      where: {
        userId: req.user.id,
        deletedAt: null
      },
      orderBy: {
        createdAt: "desc"
      },
      include: {
        driver: {
          select: {
            id: true,
            fullName: true,
            vehicleRegistration: true,
            ratePoint: true,
            avatar: true
          }
        }
      }
    });

    const now = new Date();
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const currentMonthTrips = trips.filter((trip) => {
      const referenceDate = getReferenceDate(trip);
      return referenceDate >= monthStart && trip.status !== "Cancelled";
    });

    const summary = currentMonthTrips.reduce((result, trip) => {
      result.totalTrips += 1;
      result.totalDistance += Number(trip.distance);

      if (trip.status === "Completed") {
        result.totalAmount += Number(trip.totalAmount);
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
