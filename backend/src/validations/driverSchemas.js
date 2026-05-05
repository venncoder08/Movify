const { z } = require("zod");

/**
 * Validation schema for GET /api/driver/nearby-trips
 */
const nearbyTripsSchema = z.object({
  query: z.object({
    lat: z
      .string()
      .refine((val) => !isNaN(parseFloat(val)), "lat must be a valid number")
      .transform((val) => parseFloat(val))
      .refine((val) => val >= -90 && val <= 90, "lat must be between -90 and 90"),
    lng: z
      .string()
      .refine((val) => !isNaN(parseFloat(val)), "lng must be a valid number")
      .transform((val) => parseFloat(val))
      .refine((val) => val >= -180 && val <= 180, "lng must be between -180 and 180"),
    radius: z
      .string()
      .optional()
      .refine((val) => !val || !isNaN(parseFloat(val)), "radius must be a valid number")
      .transform((val) => (val ? parseFloat(val) : 5))
      .refine((val) => val > 0, "radius must be greater than 0")
  })
});

/**
 * Validation schema for POST /api/driver/accept-trip
 */
const acceptTripSchema = z.object({
  body: z.object({
    tripId: z
      .number()
      .int("tripId must be an integer")
      .positive("tripId must be positive")
  })
});

module.exports = {
  nearbyTripsSchema,
  acceptTripSchema
};
