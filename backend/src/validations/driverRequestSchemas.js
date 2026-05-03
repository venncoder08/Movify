const { z } = require("zod");

const createDriverRequestSchema = z.object({
  body: z.object({
    idCard: z.string().trim().min(6, "ID card is required").max(50, "ID card is too long"),
    driverLicense: z.string().trim().min(6, "Driver license is required").max(50, "Driver license is too long"),
    vehicleRegistration: z.string().trim().min(6, "Vehicle registration is required").max(100, "Vehicle registration is too long")
  })
});

module.exports = {
  createDriverRequestSchema
};
