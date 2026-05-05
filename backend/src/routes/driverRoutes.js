const express = require("express");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const { getNearbyTrips, acceptTrip } = require("../controllers/driverController");
const { nearbyTripsSchema, acceptTripSchema } = require("../validations");

const router = express.Router();

/**
 * Get nearby available trips for driver
 * @route GET /api/driver/nearby-trips
 * @param {number} lat - Driver's current latitude
 * @param {number} lng - Driver's current longitude
 * @param {number} radius - Search radius in km (default: 5)
 */
router.get("/nearby-trips", authMiddleware, validateRequest(nearbyTripsSchema), getNearbyTrips);

/**
 * Accept a trip
 * @route POST /api/driver/accept-trip
 * @param {number} tripId - Trip ID to accept
 */
router.post("/accept-trip", authMiddleware, validateRequest(acceptTripSchema), acceptTrip);

module.exports = router;
