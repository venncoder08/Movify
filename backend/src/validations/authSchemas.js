const { z } = require("zod");

/**
 * Auth validation schemas using Zod
 */

// Register schema
const registerSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    phoneNumber: z.string().min(10, "Số điện thoại phải có ít nhất 10 chữ số"),
    password: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự"),
    fullName: z.string().min(2, "Họ tên phải có ít nhất 2 ký tự"),
    otp: z.string().regex(/^\d{6}$/, "Mã OTP phải gồm 6 chữ số")
  })
});

// Login schema
const loginSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    password: z.string().min(1, "Vui lòng nhập mật khẩu")
  })
});

// Profile update schema
const profileUpdateSchema = z.object({
  body: z.object({
    fullName: z.string().min(2, "Họ tên phải có ít nhất 2 ký tự").max(100, "Họ tên chỉ được tối đa 100 ký tự").optional(),
    phoneNumber: z.string().min(10, "Số điện thoại phải có ít nhất 10 chữ số").max(15, "Số điện thoại chỉ được tối đa 15 chữ số").optional(),
    avatar: z.string().url("Đường dẫn ảnh đại diện không hợp lệ").optional(),
    gender: z.enum(["Male", "Female", "Other"]).optional(),
    dateOfBirth: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Ngày sinh không hợp lệ").optional()
  }).refine((data) => Object.values(data).some((value) => value !== undefined), {
    message: "Vui lòng cung cấp ít nhất một trường cần cập nhật"
  })
});

// Forgot password schema
const forgotPasswordSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ")
  })
});

// Reset password schema
const resetPasswordSchema = z.object({
  body: z.object({
    email: z.string().email("Email không hợp lệ"),
    otp: z.string().regex(/^\d{6}$/, "Mã OTP phải gồm 6 chữ số"),
    newPassword: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự"),
    confirmPassword: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự")
  }).refine((data) => data.newPassword === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"]
  })
});

// Google Sign-In schema
const googleSignInSchema = z.object({
  body: z.object({
    idToken: z.string().min(1, "Vui lòng cung cấp mã phiên Google")
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
