const { PrismaClient } = require("@prisma/client");
const { generateToken } = require("../utils/jwt");
const { hashPassword, comparePassword, needsPasswordRehash } = require("../utils/password");
const { generateOTP, storeOTP } = require("../utils/otp");
const { sendOTPEmail } = require("../services/emailService");

const prisma = new PrismaClient();

function decodeJwtPayload(token) {
  const parts = token.split(".");
  if (parts.length < 2) {
    throw new Error("Invalid JWT format");
  }

  const payload = parts[1]
    .replace(/-/g, "+")
    .replace(/_/g, "/");
  const normalizedPayload = payload.padEnd(Math.ceil(payload.length / 4) * 4, "=");

  return JSON.parse(Buffer.from(normalizedPayload, "base64").toString("utf8"));
}

/**
 * Register new user
 * POST /auth/register
 */
async function register(req, res) {
  try {
    const { email, phoneNumber, password, fullName, role } = req.body;

    // Validation
    if (!email || !phoneNumber || !password || !fullName) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email, phone number, password, and full name are required"
      });
    }

    // Check if user exists
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

    // Hash password
    const hashedPassword = await hashPassword(password);

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

    // Generate token
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
    console.error("Register error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Login user
 * POST /auth/login
 */
async function login(req, res) {
  try {
    const { email, password } = req.body;

    // Validation
    if (!email || !password) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email and password are required"
      });
    }

    // Find user
    const user = await prisma.account.findUnique({
      where: { email },
      select: {
        id: true,
        email: true,
        phoneNumber: true,
        fullName: true,
        role: true,
        avatar: true,
        hashedPassword: true,
        isActive: true
      }
    });

    if (!user) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid email or password"
      });
    }

    if (!user.isActive) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Account is deactivated"
      });
    }

    if (!user.hashedPassword) {
      return res.status(400).json({
        error: "Bad Request",
        message: "This account uses Google Sign-In. Please continue with Google."
      });
    }

    // Compare password
    const isPasswordValid = await comparePassword(password, user.hashedPassword);
    if (!isPasswordValid) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid email or password"
      });
    }

    if (needsPasswordRehash(user.hashedPassword)) {
      await prisma.account.update({
        where: { id: user.id },
        data: {
          hashedPassword: await hashPassword(password)
        }
      });
    }

    // Generate token
    const token = generateToken({
      id: user.id,
      email: user.email,
      role: user.role
    });

    // Remove hashedPassword from response
    const { hashedPassword, ...userWithoutPassword } = user;

    res.status(200).json({
      message: "Login successful",
      token,
      user: userWithoutPassword,
      data: {
        user: userWithoutPassword,
        token
      }
    });
  } catch (error) {
    console.error("Login error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Get current user profile
 * GET /auth/me (protected)
 */
async function getProfile(req, res) {
  try {
    const user = await prisma.account.findUnique({
      where: { id: req.user.id },
      select: {
        id: true,
        email: true,
        phoneNumber: true,
        fullName: true,
        role: true,
        gender: true,
        avatar: true,
        dateOfBirth: true,
        idCard: true,
        driverLicense: true,
        vehicleRegistration: true,
        driverStatus: true,
        isActive: true,
        createdAt: true,
        updatedAt: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    res.status(200).json({
      data: user
    });
  } catch (error) {
    console.error("Get profile error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Refresh token
 * POST /auth/refresh (protected)
 */
async function refreshToken(req, res) {
  try {
    const user = await prisma.account.findUnique({
      where: { id: req.user.id },
      select: {
        id: true,
        email: true,
        role: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    const newToken = generateToken({
      id: user.id,
      email: user.email,
      role: user.role
    });

    res.status(200).json({
      message: "Token refreshed successfully",
      data: { token: newToken }
    });
  } catch (error) {
    console.error("Refresh token error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Logout user
 * POST /auth/logout (protected)
 * Note: In a stateless JWT architecture, logout is primarily client-side.
 * This endpoint serves as a confirmation and can be used for logging/auditing.
 */
async function logout(req, res) {
  try {
    const userId = req.user?.id;

    // Optional: Log logout event for security auditing
    console.log(`User ${userId} logged out at ${new Date().toISOString()}`);

    res.status(200).json({
      message: "Logout successful. Please delete the token from your device.",
      data: {
        userId,
        loggedOutAt: new Date().toISOString()
      }
    });
  } catch (error) {
    console.error("Logout error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Forgot Password - Request OTP
 * POST /auth/forgot-password
 */
async function forgotPassword(req, res) {
  try {
    const { email } = req.body;

    // Validation
    if (!email) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email is required"
      });
    }

    // Check if user exists
    const user = await prisma.account.findUnique({
      where: { email },
      select: { id: true, email: true, fullName: true }
    });

    if (!user) {
      // Don't reveal if email doesn't exist (security best practice)
      return res.status(200).json({
        message: "If this email is registered, an OTP will be sent to reset your password"
      });
    }

    // Generate OTP
    const otp = generateOTP();

    // Store OTP
    storeOTP(email, otp);

    // Send OTP email
    const emailResult = await sendOTPEmail(email, otp, "reset-password");

    if (!emailResult.success) {
      return res.status(500).json({
        error: "Email Service Error",
        message: emailResult.message
      });
    }

    res.status(200).json({
      message: "OTP sent to your email. Check your inbox to reset your password.",
      data: {
        email,
        expiresIn: 600 // 10 minutes
      }
    });
  } catch (error) {
    console.error("Forgot password error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Reset Password - Verify OTP and set new password
 * POST /auth/reset-password
 */
async function resetPassword(req, res) {
  try {
    const { email, otp, newPassword, confirmPassword } = req.body;

    // Validation
    if (!email || !otp || !newPassword || !confirmPassword) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Email, OTP, and new password are required"
      });
    }

    if (newPassword !== confirmPassword) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Passwords do not match"
      });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Password must be at least 6 characters"
      });
    }

    // Verify OTP
    const { verifyOTP } = require("../utils/otp");
    const otpResult = verifyOTP(email, otp);

    if (!otpResult.valid) {
      return res.status(400).json({
        error: "Invalid OTP",
        message: otpResult.message
      });
    }

    // Find user
    const user = await prisma.account.findUnique({
      where: { email }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    // Hash new password
    const hashedPassword = await hashPassword(newPassword);

    // Update password
    await prisma.account.update({
      where: { id: user.id },
      data: { hashedPassword }
    });

    res.status(200).json({
      message: "Password reset successfully. You can now login with your new password.",
      data: {
        email,
        resetAt: new Date().toISOString()
      }
    });
  } catch (error) {
    console.error("Reset password error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Google Sign-In Authentication
 * POST /auth/google
 * Authenticates user with Google ID token
 * Creates or updates user in database
 */
async function googleSignIn(req, res) {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({
        error: "Validation Error",
        message: "ID token is required"
      });
    }

    // Verify token with Firebase Auth
    // Note: Requires Firebase Admin SDK initialization
    // For now, we'll implement a simpler version that works with Google's public API
    
    // In production, use Firebase Admin SDK:
    // const decodedToken = await admin.auth().verifyIdToken(idToken);
    
    // Alternative: Decode JWT manually (without verification for now)
    // In production, always verify the token signature
    let decodedToken;
    try {
      // Simple JWT decode (NOT SECURE - only for development)
      // In production, use Firebase Admin SDK for proper verification
      decodedToken = decodeJwtPayload(idToken);
    } catch (error) {
      console.error("Token decode error:", error);
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid ID token"
      });
    }

    const { email, name, picture, sub } = decodedToken;

    if (!email) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Email not found in token"
      });
    }

    // Check if user exists
    let user = await prisma.account.findFirst({
      where: { email },
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

    if (!user) {
      // Create new user from Google Sign-In
      user = await prisma.account.create({
        data: {
          email,
          phoneNumber: `g${String(sub || Date.now()).slice(0, 19)}`,
          hashedPassword: "", // No password for Google Sign-In users
          fullName: name || email.split('@')[0],
          role: "User",
          avatar: picture || null,
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

      console.log("New user created via Google Sign-In:", email);
    } else {
      // Update existing user with Google profile info if needed
      if (!user.avatar && picture) {
        user = await prisma.account.update({
          where: { id: user.id },
          data: { avatar: picture },
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
      }
    }

    // Generate JWT token
    const token = generateToken({
      id: user.id,
      email: user.email,
      role: user.role
    });

    res.status(200).json({
      message: "Google Sign-In successful",
      token,
      user,
      data: {
        user
      }
    });
  } catch (error) {
    console.error("Google Sign-In error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  register,
  login,
  logout,
  getProfile,
  refreshToken,
  forgotPassword,
  resetPassword,
  googleSignIn
};
