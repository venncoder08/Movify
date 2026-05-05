const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

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
  return {
    id: trip.ID,
    pickupAddress: trip.PickUpLocation,
    pickupLat: trip.PickUpLatitude ? parseFloat(trip.PickUpLatitude) : null,
    pickupLng: trip.PickUpLongitude ? parseFloat(trip.PickUpLongitude) : null,
    destinationAddress: trip.DestinationLocation,
    destinationLat: trip.DestinationLatitude ? parseFloat(trip.DestinationLatitude) : null,
    destinationLng: trip.DestinationLongitude ? parseFloat(trip.DestinationLongitude) : null,
    fare: trip.TotalAmount ? parseFloat(trip.TotalAmount) : 0,
    distance: distance ? parseFloat(distance.toFixed(2)) : 0,
    tripType: trip.TripType,
    status: trip.Status,
    createdAt: trip.CreatedAt
  };
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
        message: "Invalid latitude or longitude"
      });
    }

    // Verify user is a driver
    const account = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: { Role: true, DriverStatus: true }
    });

    if (!account || (account.Role !== "Driver" && account.DriverStatus !== "Approved")) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Only approved drivers can view available trips"
      });
    }

    // Fetch all pending trips with no driver assigned
    const trips = await prisma.trip.findMany({
      where: {
        Status: "Pending",
        DriverID: null,
        DeletedAt: null
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
        searchRadius: radius
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
      select: { Role: true, DriverStatus: true }
    });

    if (!account || (account.Role !== "Driver" && account.DriverStatus !== "Approved")) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Only approved drivers can accept trips"
      });
    }

    // Check if trip exists and is still available
    const trip = await prisma.trip.findUnique({
      where: { ID: tripId },
      select: {
        ID: true,
        Status: true,
        DriverID: true,
        UserID: true
      }
    });

    if (!trip) {
      return res.status(404).json({
        error: "Not Found",
        message: "Trip not found"
      });
    }

    if (trip.Status !== "Pending") {
      return res.status(409).json({
        error: "Conflict",
        message: "Trip is no longer available"
      });
    }

    if (trip.DriverID !== null) {
      return res.status(409).json({
        error: "Conflict",
        message: "Trip has already been accepted by another driver"
      });
    }

    // Update trip with driver assignment
    const updatedTrip = await prisma.trip.update({
      where: { ID: tripId },
      data: {
        DriverID: req.user.id,
        Status: "Accepted",
        UpdatedAt: new Date()
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
        CreatedAt: true
      }
    });

    res.status(200).json({
      data: {
        message: "Trip accepted successfully",
        trip: serializeTrip(updatedTrip, 0)
      }
    });
  } catch (error) {
    console.error("Accept trip error:", error);
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
