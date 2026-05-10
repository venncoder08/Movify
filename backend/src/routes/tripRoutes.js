const express = require("express");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const { createTrip, getActiveTrip, getTrip, updateDriverLocation, confirmPickup, completeTrip, cancelTrip, rateTrip } = require("../controllers/tripController");
const { createTripSchema, getActiveTripSchema, getTripSchema, updateDriverLocationSchema, confirmPickupSchema, completeTripSchema, cancelTripSchema, rateTripSchema } = require("../validations");

const router = express.Router();

/**
 * Create a new trip request (passenger)
 * @route POST /api/trips
 * @access Authenticated users (Role: User)
 */
router.post("/", authMiddleware, validateRequest(createTripSchema), createTrip);

/**
 * Get current active trip for passenger or driver.
 * @route GET /api/trips/active
 * @access Authenticated user
 */
router.get("/active", authMiddleware, validateRequest(getActiveTripSchema), getActiveTrip);

/**
 * Get a trip status/details
 * @route GET /api/trips/:id
 * @access Trip passenger, assigned driver, or admin
 */
router.get("/:id", authMiddleware, validateRequest(getTripSchema), getTrip);

/**
 * Update assigned driver's realtime location for an active trip.
 * @route PATCH /api/trips/:id/driver-location
 */
router.patch("/:id/driver-location", authMiddleware, validateRequest(updateDriverLocationSchema), updateDriverLocation);

/**
 * Confirm pickup from passenger or assigned driver.
 * Driver must be within 50m of pickup.
 * @route POST /api/trips/:id/pickup-confirm
 */
router.post("/:id/pickup-confirm", authMiddleware, validateRequest(confirmPickupSchema), confirmPickup);

/**
 * Complete an in-progress trip from the assigned driver.
 * Driver must be within 50m of destination.
 * @route POST /api/trips/:id/complete
 */
router.post("/:id/complete", authMiddleware, validateRequest(completeTripSchema), completeTrip);

/**
 * Cancel a pending trip (passenger)
 * @route POST /api/trips/:id/cancel
 * @access Authenticated users (trip owner only)
 */
router.post("/:id/cancel", authMiddleware, validateRequest(cancelTripSchema), cancelTrip);

/**
 * Rate the assigned driver after a completed trip.
 * @route POST /api/trips/:id/rating
 * @access Trip passenger only
 */
router.post("/:id/rating", authMiddleware, validateRequest(rateTripSchema), rateTrip);

module.exports = router;
