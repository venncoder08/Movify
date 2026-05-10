const { z } = require("zod");

/**
 * Validation schema for GET /api/driver/nearby-trips
 */
const nearbyTripsSchema = z.object({
  query: z.object({
    lat: z
      .string()
      .refine((val) => !isNaN(parseFloat(val)), "Vĩ độ phải là số hợp lệ")
      .transform((val) => parseFloat(val))
      .refine((val) => val >= -90 && val <= 90, "Vĩ độ phải nằm trong khoảng -90 đến 90"),
    lng: z
      .string()
      .refine((val) => !isNaN(parseFloat(val)), "Kinh độ phải là số hợp lệ")
      .transform((val) => parseFloat(val))
      .refine((val) => val >= -180 && val <= 180, "Kinh độ phải nằm trong khoảng -180 đến 180"),
    radius: z
      .string()
      .optional()
      .refine((val) => !val || !isNaN(parseFloat(val)), "Bán kính phải là số hợp lệ")
      .transform((val) => (val ? parseFloat(val) : 5))
      .refine((val) => val > 0, "Bán kính phải lớn hơn 0")
  })
});

/**
 * Validation schema for POST /api/driver/accept-trip
 */
const acceptTripSchema = z.object({
  body: z.object({
    tripId: z
      .number()
      .int("Mã chuyến phải là số nguyên")
      .positive("Mã chuyến phải lớn hơn 0")
  })
});

module.exports = {
  nearbyTripsSchema,
  acceptTripSchema
};
