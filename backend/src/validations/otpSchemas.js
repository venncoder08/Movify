const { z } = require("zod");

/**
 * OTP validation schemas using Zod
 */

// Request OTP schema
const requestOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    purpose: z.enum(["verify-email", "reset-password", "two-factor"], {
      errorMap: () => ({
        message: 'Mục đích OTP phải là một trong: "verify-email", "reset-password", "two-factor"'
      })
    })
  })
});

// Verify OTP schema
const verifyOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    otp: z.string().regex(/^\d{6}$/, "Mã OTP phải gồm 6 chữ số"),
    purpose: z.enum(["verify-email", "reset-password", "two-factor"]).optional().default("verify-email")
  })
});

// Register with OTP schema
const registerWithOTPSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    otp: z.string().regex(/^\d{6}$/, "Mã OTP phải gồm 6 chữ số"),
    phoneNumber: z.string().min(10, "Số điện thoại phải có ít nhất 10 chữ số"),
    password: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự"),
    fullName: z.string().min(2, "Họ tên phải có ít nhất 2 ký tự")
  })
});

module.exports = {
  requestOTPSchema,
  verifyOTPSchema,
  registerWithOTPSchema
};
