const { z } = require("zod");

/**
 * OTP validation schemas using Zod
 */

// Request OTP schema
const requestOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    purpose: z.enum(["verify-email", "reset-password", "two-factor"], {
      errorMap: () => ({
        message: 'Purpose must be one of: "verify-email", "reset-password", "two-factor"'
      })
    })
  })
});

// Verify OTP schema
const verifyOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    otp: z.string().regex(/^\d{6}$/, "OTP must be a 6-digit number")
  })
});

// Register with OTP schema
const registerWithOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    otp: z.string().regex(/^\d{6}$/, "OTP must be a 6-digit number"),
    phoneNumber: z.string().min(10, "Phone number must be at least 10 digits"),
    password: z.string().min(6, "Password must be at least 6 characters"),
    fullName: z.string().min(2, "Full name must be at least 2 characters"),
    role: z.enum(["User", "Driver", "Admin"]).optional().default("User")
  })
});

module.exports = {
  requestOTPSchema,
  verifyOTPSchema,
  registerWithOTPSchema
};
