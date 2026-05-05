const express = require("express");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const { createTrip, cancelTrip } = require("../controllers/tripController");
const { createTripSchema, cancelTripSchema } = require("../validations");

const router = express.Router();

/**
 * Create a new trip request (passenger)
 * @route POST /api/trips
 * @access Authenticated users (Role: User)
 */
router.post("/", authMiddleware, validateRequest(createTripSchema), createTrip);

/**
 * Cancel a pending trip (passenger)
 * @route POST /api/trips/:id/cancel
 * @access Authenticated users (trip owner only)
 */
router.post("/:id/cancel", authMiddleware, validateRequest(cancelTripSchema), cancelTrip);

module.exports = router;
