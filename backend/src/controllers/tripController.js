const { PrismaClient } = require("@prisma/client");

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

/**
 * Serialize a Prisma Trip row into the JSON shape that Android expects
 */
function serializeTrip(trip) {
  return {
    id:                 trip.ID,
    pickupAddress:      trip.PickUpLocation,
    pickupLat:          trip.PickUpLatitude  ? parseFloat(trip.PickUpLatitude)  : 0,
    pickupLng:          trip.PickUpLongitude ? parseFloat(trip.PickUpLongitude) : 0,
    destinationAddress: trip.DestinationLocation,
    destinationLat:     trip.DestinationLatitude  ? parseFloat(trip.DestinationLatitude)  : 0,
    destinationLng:     trip.DestinationLongitude ? parseFloat(trip.DestinationLongitude) : 0,
    fare:               trip.TotalAmount ? parseFloat(trip.TotalAmount) : 0,
    distance:           trip.Distance   ? parseFloat(trip.Distance)    : 0,
    status:             trip.Status,
    tripType:           trip.TripType,
    paymentMethod:      trip.PaymentMethod,
    createdAt:          trip.CreatedAt
  };
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

    // If a promotionId was supplied, verify it exists and is not soft-deleted
    if (promotionId !== null) {
      const promo = await prisma.promotion.findFirst({
        where: { ID: promotionId, DeletedAt: null }
      });
      if (!promo) {
        return res.status(404).json({
          error: "Not Found",
          message: "Promotion not found or has been removed"
        });
      }
    }

    const distanceKm = distanceMeters / 1000;

    const newTrip = await prisma.trip.create({
      data: {
        PickUpLocation:       pickupAddress,
        PickUpLatitude:       (pickupLat  != null && pickupLat  !== 0) ? pickupLat  : null,
        PickUpLongitude:      (pickupLng  != null && pickupLng  !== 0) ? pickupLng  : null,
        DestinationLocation:  destinationAddress,
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
        PickUpTime:           new Date(),
        UpdatedAt:            new Date()
      }
    });

    return res.status(201).json({
      data: {
        message: "Trip created successfully",
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
 * POST /api/trips/:id/cancel
 * Cancel a pending trip (passenger side).
 * Only the trip owner can cancel, and only while status is Pending.
 */
async function cancelTrip(req, res) {
  try {
    const tripId = Number(req.params.id);

    const trip = await prisma.trip.findFirst({
      where: { ID: tripId, DeletedAt: null },
      select: { ID: true, Status: true, UserID: true }
    });

    if (!trip) {
      return res.status(404).json({
        error:   "Not Found",
        message: "Trip not found"
      });
    }

    if (trip.UserID !== req.user.id) {
      return res.status(403).json({
        error:   "Forbidden",
        message: "You can only cancel your own trips"
      });
    }

    if (trip.Status !== "Pending") {
      return res.status(409).json({
        error:   "Conflict",
        message: `Cannot cancel a trip with status '${trip.Status}'`
      });
    }

    await prisma.trip.update({
      where: { ID: tripId },
      data:  {
        Status:       "Cancelled",
        CancelReason: "Passenger cancelled while searching for driver",
        UpdatedAt:    new Date()
      }
    });

    return res.status(200).json({
      data: { message: "Trip cancelled successfully" }
    });
  } catch (error) {
    console.error("cancelTrip error:", error);
    return res.status(500).json({
      error:   "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = { createTrip, cancelTrip };
