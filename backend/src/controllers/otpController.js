const { generateOTP, storeOTP, verifyOTP } = require("../utils/otp");
const { sendOTPEmail } = require("../services/emailService");
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
        message: "Email is required"
      });
    }

    if (!purpose || !["verify-email", "reset-password", "two-factor"].includes(purpose)) {
      return res.status(400).json({
        error: "Validation Error",
        message: 'Purpose must be one of: "verify-email", "reset-password", "two-factor"'
      });
    }

    // For verify-email, check if user doesn't exist yet
    if (purpose === "verify-email") {
      const existingUser = await prisma.account.findUnique({
        where: { email }
      });

      if (existingUser && existingUser.isActive) {
        return res.status(409).json({
          error: "Conflict",
          message: "Email already registered"
        });
      }
    }

    // For reset-password, check if user exists
    if (purpose === "reset-password") {
      const user = await prisma.account.findUnique({
        where: { email }
      });

      if (!user) {
        // Don't reveal if email doesn't exist (security)
        return res.status(200).json({
          message: "If this email is registered, an OTP will be sent"
        });
      }
    }

    // Generate OTP
    const otp = generateOTP();

    // Store OTP
    storeOTP(email, otp);

    // Send OTP email
    const emailResult = await sendOTPEmail(email, otp, purpose);

    if (!emailResult.success) {
      return res.status(500).json({
        error: "Email Service Error",
        message: emailResult.message
      });
    }

    res.status(200).json({
      message: "OTP sent to your email",
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
    const { email, otp } = req.body;

    // Validation
    if (!email || !otp) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email and OTP are required"
      });
    }

    // Verify OTP
    const result = verifyOTP(email, otp);

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
    const { email, otp, phoneNumber, password, fullName, role } = req.body;

    // Validation
    if (!email || !otp || !phoneNumber || !password || !fullName) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email, OTP, phone number, password, and full name are required"
      });
    }

    // Verify OTP
    const otpResult = verifyOTP(email, otp);
    if (!otpResult.valid) {
      return res.status(400).json({
        error: "Invalid OTP",
        message: otpResult.message
      });
    }

    // Check if user already exists
    const existingUser = await prisma.account.findFirst({
      where: {
        OR: [{ email }, { phoneNumber }]
      }
    });

    if (existingUser) {
      return res.status(409).json({
        error: "Conflict",
        message: "Email or phone number already registered"
      });
    }

    // Hash password (use bcryptjs)
    const bcrypt = require("bcryptjs");
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user = await prisma.account.create({
      data: {
        email,
        phoneNumber,
        hashedPassword,
        fullName,
        role: role || "User",
        isActive: true
      },
      select: {
        id: true,
        email: true,
        phoneNumber: true,
        fullName: true,
        role: true,
        avatar: true,
        createdAt: true
      }
    });

    // Generate JWT token
    const { generateToken } = require("../utils/jwt");
    const token = generateToken({
      id: user.id,
      email: user.email,
      role: user.role
    });

    res.status(201).json({
      message: "User registered successfully",
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
