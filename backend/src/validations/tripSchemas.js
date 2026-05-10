const { z } = require("zod");

/**
 * Validation schema for POST /api/trips  (create trip)
 */
const createTripSchema = z.object({
  body: z.object({
    pickupAddress: z
      .string({ required_error: "Vui lòng nhập điểm đón" })
      .min(1, "Điểm đón không được để trống")
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
      .string({ required_error: "Vui lòng nhập điểm đến" })
      .min(1, "Điểm đến không được để trống")
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
        errorMap: () => ({ message: "Loại xe phải là 'motorbike' hoặc 'car'" })
      }),

    paymentMethod: z
      .enum(["cash", "bank_transfer"], {
        errorMap: () => ({ message: "Phương thức thanh toán phải là 'cash' hoặc 'bank_transfer'" })
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
 * Validation schema for GET /api/trips/:id
 */
const getTripSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  })
});

/**
 * Validation schema for GET /api/trips/active
 */
const getActiveTripSchema = z.object({});

/**
 * Validation schema for PATCH /api/trips/:id/driver-location
 */
const updateDriverLocationSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  }),
  body: z.object({
    driverLat: z.number().min(-90).max(90),
    driverLng: z.number().min(-180).max(180)
  })
});

/**
 * Validation schema for POST /api/trips/:id/pickup-confirm
 */
const confirmPickupSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  }),
  body: z.object({
    driverLat: z.number().min(-90).max(90).optional(),
    driverLng: z.number().min(-180).max(180).optional()
  }).optional().default({})
});

/**
 * Validation schema for POST /api/trips/:id/complete
 */
const completeTripSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  }),
  body: z.object({
    driverLat: z.number().min(-90).max(90),
    driverLng: z.number().min(-180).max(180)
  })
});

/**
 * Validation schema for POST /api/trips/:id/cancel
 */
const cancelTripSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  })
});

/**
 * Validation schema for POST /api/trips/:id/rating
 */
const rateTripSchema = z.object({
  params: z.object({
    id: z
      .string()
      .refine((v) => !isNaN(parseInt(v, 10)), "Mã chuyến phải là số nguyên hợp lệ")
      .transform((v) => parseInt(v, 10))
  }),
  body: z.object({
    score: z.number().min(1, "Điểm đánh giá tối thiểu là 1").max(5, "Điểm đánh giá tối đa là 5"),
    comment: z.string().trim().max(500, "Nhận xét chỉ được tối đa 500 ký tự").optional().default("")
  })
});

module.exports = {
  createTripSchema,
  getActiveTripSchema,
  getTripSchema,
  updateDriverLocationSchema,
  confirmPickupSchema,
  completeTripSchema,
  cancelTripSchema,
  rateTripSchema
};
