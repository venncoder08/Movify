const { z } = require("zod");

/**
 * Validation schema for POST /api/trips  (create trip)
 */
const createTripSchema = z.object({
  body: z.object({
    pickupAddress: z
      .string({ required_error: "pickupAddress is required" })
      .min(1, "pickupAddress cannot be empty")
      .max(500),

    pickupLat: z
      .number()
      .min(-90)
      .max(90)
      .optional()
      .default(0),

    pickupLng: z
      .number()
      .min(-180)
      .max(180)
      .optional()
      .default(0),

    destinationAddress: z
      .string({ required_error: "destinationAddress is required" })
      .min(1, "destinationAddress cannot be empty")
      .max(500),

    destinationLat: z
      .number()
      .min(-90)
      .max(90)
      .optional()
      .default(0),

    destinationLng: z
      .number()
      .min(-180)
      .max(180)
      .optional()
      .default(0),

    vehicleType: z
      .enum(["motorbike", "car"], {
        errorMap: () => ({ message: "vehicleType must be 'motorbike' or 'car'" })
      }),

    paymentMethod: z
      .enum(["cash", "bank_transfer"], {
        errorMap: () => ({ message: "paymentMethod must be 'cash' or 'bank_transfer'" })
      }),

    promotionId: z
      .number()
      .int()
      .positive()
      .nullable()
      .optional(),

    estimatedFare: z
      .number()
      .min(0)
      .optional()
      .default(0),

    distanceMeters: z
      .number()
      .int()
      .min(0)
      .optional()
      .default(0)
  })
});

/**
 * Validation schema for POST /api/trips/:id/cancel
 */
const cancelTripSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "id must be a valid integer")
      .transform((v) => parseInt(v, 10))
  })
});

module.exports = {
  createTripSchema,
  cancelTripSchema
};
