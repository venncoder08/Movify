const { z } = require("zod");

/**
 * Auth validation schemas using Zod
 */

// Register schema
const registerSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    phoneNumber: z.string().min(10, "Phone number must be at least 10 digits"),
    password: z.string().min(6, "Password must be at least 6 characters"),
    fullName: z.string().min(2, "Full name must be at least 2 characters"),
    role: z.enum(["User", "Driver", "Admin"]).optional().default("User")
  })
});

// Login schema
const loginSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    password: z.string().min(1, "Password is required")
  })
});

// Profile update schema
const profileUpdateSchema = z.object({
  body: z.object({
    fullName: z.string().min(2, "Full name must be at least 2 characters").max(100, "Full name must be at most 100 characters").optional(),
    phoneNumber: z.string().min(10, "Phone number must be at least 10 digits").max(15, "Phone number must be at most 15 digits").optional(),
    avatar: z.string().url("Invalid avatar URL").optional(),
    gender: z.enum(["Male", "Female", "Other"]).optional(),
    dateOfBirth: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Invalid date of birth").optional()
  }).refine((data) => Object.values(data).some((value) => value !== undefined), {
    message: "At least one field must be provided"
  })
});

// Forgot password schema
const forgotPasswordSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address")
  })
});

// Reset password schema
const resetPasswordSchema = z.object({
  body: z.object({
    email: z.string().email("Invalid email address"),
    otp: z.string().regex(/^\d{6}$/, "OTP must be a 6-digit number"),
    newPassword: z.string().min(6, "Password must be at least 6 characters"),
    confirmPassword: z.string().min(6, "Password must be at least 6 characters")
  }).refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"]
  })
});

// Google Sign-In schema
const googleSignInSchema = z.object({
  body: z.object({
    idToken: z.string().min(1, "ID token is required")
  })
});

module.exports = {
  registerSchema,
  loginSchema,
  profileUpdateSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  googleSignInSchema
};
