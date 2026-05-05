const { PrismaClient } = require("@prisma/client");
const { generateToken } = require("../utils/jwt");
const { hashPassword, comparePassword, needsPasswordRehash } = require("../utils/password");
const { generateOTP, storeOTP } = require("../utils/otp");
const { sendOTPEmail } = require("../services/emailService");

const prisma = new PrismaClient();

function serializeAccount(account) {
  if (!account) {
    return null;
  }

  return {
    id: account.ID,
    email: account.Email,
    phoneNumber: account.PhoneNumber,
    fullName: account.FullName,
    role: account.Role,
    gender: account.Gender,
    avatar: account.Avatar,
    dateOfBirth: account.DateOfBirth,
    idCard: account.IDCard,
    driverLicense: account.DriverLicense,
    vehicleRegistration: account.VehicleRegistration,
    driverStatus: account.DriverStatus,
    isActive: account.IsActive,
    createdAt: account.CreatedAt,
    updatedAt: account.UpdatedAt
  };
}

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
        OR: [{ Email: email }, { PhoneNumber: phoneNumber }]
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
        Email: email,
        PhoneNumber: phoneNumber,
        HashedPassword: hashedPassword,
        FullName: fullName,
        Role: role || "User",
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

    // Generate token
    const token = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
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
      where: { Email: email },
      select: {
        ID: true,
        Email: true,
        PhoneNumber: true,
        FullName: true,
        Role: true,
        Avatar: true,
        HashedPassword: true,
        IsActive: true
      }
    });

    if (!user) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid email or password"
      });
    }

    if (!user.IsActive) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Account is deactivated"
      });
    }

    if (!user.HashedPassword) {
      return res.status(400).json({
        error: "Bad Request",
        message: "This account uses Google Sign-In. Please continue with Google."
      });
    }

    // Compare password
    const isPasswordValid = await comparePassword(password, user.HashedPassword);
    if (!isPasswordValid) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid email or password"
      });
    }

    if (needsPasswordRehash(user.HashedPassword)) {
      await prisma.account.update({
        where: { ID: user.ID },
        data: {
          HashedPassword: await hashPassword(password)
        }
      });
    }

    // Generate token
    const token = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
    });

    // Remove HashedPassword from response
    const { HashedPassword, ...userWithoutPassword } = user;

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
      where: { ID: req.user.id },
      select: {
        ID: true,
        Email: true,
        PhoneNumber: true,
        FullName: true,
        Role: true,
        Gender: true,
        Avatar: true,
        DateOfBirth: true,
        IDCard: true,
        DriverLicense: true,
        VehicleRegistration: true,
        DriverStatus: true,
        IsActive: true,
        CreatedAt: true,
        UpdatedAt: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    res.status(200).json({
      data: serializeAccount(user)
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
 * Update current user profile
 * PATCH /auth/me (protected)
 */
async function updateProfile(req, res) {
  try {
    const user = await prisma.account.findUnique({
      where: { ID: req.user.id },
      select: {
        ID: true,
        Email: true,
        Role: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    const { 
      Avatar,
      FullName,
      PhoneNumber,
      Gender,
      DateOfBirth,
    } = req.body;

    if (PhoneNumber) {
      const existingPhoneUser = await prisma.account.findFirst({
        where: {
          PhoneNumber,
          ID: { not: req.user.id }
        }
      });

      if (existingPhoneUser) {
        return res.status(400).json({
          error: "Bad Request",
          message: "Phone number is already in use"
        });
      }
    }

    const updatedUser = await prisma.account.update({
      where: { ID: req.user.id },
      data: {
        Avatar,
        FullName,
        PhoneNumber,
        Gender,
        DateOfBirth
      }
    });

    res.status(200).json({
      message: "Profile updated successfully",
      data: serializeAccount(updatedUser)
    });
  } catch (error) {
    console.error("Update profile error:", error);
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
      where: { ID: req.user.id },
      select: {
        ID: true,
        Email: true,
        Role: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "User not found"
      });
    }

    const newToken = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
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
      where: { Email: email },
      select: { ID: true, Email: true, FullName: true }
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
      where: { Email: email }
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
      where: { ID: user.ID },
      data: { HashedPassword: hashedPassword }
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
      where: { Email: email },
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

    if (!user) {
      // Create new user from Google Sign-In
      user = await prisma.account.create({
        data: {
          Email: email,
          PhoneNumber: `g${String(sub || Date.now()).slice(0, 19)}`,
          HashedPassword: "", // No password for Google Sign-In users
          FullName: name || email.split('@')[0],
          Role: "User",
          Avatar: picture || null,
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

      console.log("New user created via Google Sign-In:", email);
    } else {
      // Update existing user with Google profile info if needed
      if (!user.Avatar && picture) {
        user = await prisma.account.update({
          where: { ID: user.ID },
          data: { Avatar: picture },
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
      }
    }

    // Generate JWT token
    const token = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
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
  updateProfile,
  refreshToken,
  forgotPassword,
  resetPassword,
  googleSignIn
};
