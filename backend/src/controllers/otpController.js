const { generateOTP, storeOTP, verifyOTP } = require("../utils/otp");
const { sendOTPEmail } = require("../services/emailService");
const { generateToken } = require("../utils/jwt");
const { hashPassword } = require("../utils/password");
const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

/**
 * Request OTP (send OTP to email)
 * POST /otp/request
 */
async function requestOTP(req, res) {
  try {
    const { email, purpose } = req.body;

    // Validation
    if (!email) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Vui lòng nhập email"
      });
    }

    if (!purpose || !["verify-email", "reset-password", "two-factor"].includes(purpose)) {
      return res.status(400).json({
        error: "Validation Error",
        message: 'Mục đích OTP phải là một trong: "verify-email", "reset-password", "two-factor"'
      });
    }

    // For verify-email, check if user doesn't exist yet
    if (purpose === "verify-email") {
      const existingUser = await prisma.account.findUnique({
        where: { Email: email }
      });

      if (existingUser && existingUser.IsActive) {
        return res.status(409).json({
          error: "Conflict",
          message: "Email đã được đăng ký"
        });
      }
    }

    // For reset-password, check if user exists
    if (purpose === "reset-password") {
      const user = await prisma.account.findUnique({
        where: { Email: email }
      });

      if (!user) {
        // Don't reveal if email doesn't exist (security)
        return res.status(200).json({
          message: "Nếu email này đã được đăng ký, mã OTP sẽ được gửi đến email"
        });
      }
    }

    // Generate OTP
    const otp = generateOTP();

    // Store OTP
    await storeOTP(email, otp, purpose);

    // Send OTP email
    const emailResult = await sendOTPEmail(email, otp, purpose);

    if (!emailResult.success) {
      return res.status(500).json({
        error: "Email Service Error",
        message: emailResult.message
      });
    }

    res.status(200).json({
      message: "Đã gửi mã OTP đến email",
      data: {
        email,
        purpose,
        expiresIn: 600 // 10 minutes
      }
    });
  } catch (error) {
    console.error("Request OTP error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Verify OTP
 * POST /otp/verify
 */
async function verifyOTPCode(req, res) {
  try {
    const { email, otp, purpose = "verify-email" } = req.body;

    // Validation
    if (!email || !otp) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Vui lòng nhập email và mã OTP"
      });
    }

    // Verify OTP
    const result = await verifyOTP(email, otp, purpose);

    if (!result.valid) {
      return res.status(400).json({
        error: "Invalid OTP",
        message: result.message
      });
    }

    res.status(200).json({
      message: result.message,
      data: {
        email,
        purpose,
        verified: true
      }
    });
  } catch (error) {
    console.error("Verify OTP error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Register with OTP verification
 * POST /otp/register
 * Combines OTP verification + user registration
 */
async function registerWithOTP(req, res) {
  try {
    const { email, otp, phoneNumber, password, fullName } = req.body;

    // Validation
    if (!email || !otp || !phoneNumber || !password || !fullName) {
      return res.status(400).json({
        error: "Validation Error",
      message: "Vui lòng nhập email, mã OTP, số điện thoại, mật khẩu và họ tên"
      });
    }

    // Verify OTP
    const otpResult = await verifyOTP(email, otp, "verify-email");
    if (!otpResult.valid) {
      return res.status(400).json({
        error: "Invalid OTP",
        message: otpResult.message
      });
    }

    // Check if user already exists
    const existingUser = await prisma.account.findFirst({
      where: {
        OR: [{ Email: email }, { PhoneNumber: phoneNumber }]
      }
    });

    if (existingUser) {
      return res.status(409).json({
        error: "Conflict",
        message: "Email hoặc số điện thoại đã được đăng ký"
      });
    }

    // Hash password
    const hashedPassword = await hashPassword(password);

    // Create user
    const user = await prisma.account.create({
      data: {
        Email: email,
        PhoneNumber: phoneNumber,
        HashedPassword: hashedPassword,
        FullName: fullName,
        Role: "User",
        IsActive: true
      },
      select: {
        ID: true,
        Email: true,
        PhoneNumber: true,
        FullName: true,
        Role: true,
        Avatar: true,
        CreatedAt: true
      }
    });

    // Generate JWT token
    const token = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
    });

    res.status(201).json({
      message: "Đăng ký thành công",
      data: {
        user,
        token
      }
    });
  } catch (error) {
    console.error("Register with OTP error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  requestOTP,
  verifyOTPCode,
  registerWithOTP
};
