const { PrismaClient } = require("@prisma/client");
const { OAuth2Client } = require("google-auth-library");
const env = require("../config/env");
const { generateToken } = require("../utils/jwt");
const { hashPassword, comparePassword, needsPasswordRehash } = require("../utils/password");
const { generateOTP, storeOTP, verifyOTP } = require("../utils/otp");
const { sendOTPEmail } = require("../services/emailService");

const prisma = new PrismaClient();
const googleClient = new OAuth2Client();

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
    vehicleType: account.VehicleType,
    driverStatus: account.DriverStatus,
    isActive: account.IsActive,
    createdAt: account.CreatedAt,
    updatedAt: account.UpdatedAt
  };
}

function pickFirstDefined(...values) {
  for (const value of values) {
    if (value !== undefined) {
      return value;
    }
  }

  return undefined;
}

async function verifyGoogleIdToken(idToken) {
  if (!env.googleClientId) {
    const error = new Error("Google client ID is not configured");
    error.statusCode = 500;
    throw error;
  }

  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: env.googleClientId
  });

  return ticket.getPayload();
}

/**
 * Register new user
 * POST /auth/register
 */
async function register(req, res) {
  try {
    const { email, phoneNumber, password, fullName, otp } = req.body;

    // Validation
    if (!email || !phoneNumber || !password || !fullName || !otp) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Vui lòng nhập email, mã OTP, số điện thoại, mật khẩu và họ tên"
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
        message: "Email hoặc số điện thoại đã được đăng ký"
      });
    }

    const otpResult = await verifyOTP(email, otp, "verify-email");
    if (!otpResult.valid) {
      return res.status(400).json({
        error: "Invalid OTP",
        message: otpResult.message
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

    // Generate token
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
        message: "Vui lòng nhập email và mật khẩu"
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
        message: "Email hoặc mật khẩu không đúng"
      });
    }

    if (!user.IsActive) {
      return res.status(403).json({
        error: "Forbidden",
        message: "Tài khoản đã bị vô hiệu hóa"
      });
    }

    if (!user.HashedPassword) {
      return res.status(400).json({
        error: "Bad Request",
        message: "Tài khoản này dùng đăng nhập Google. Vui lòng tiếp tục bằng Google."
      });
    }

    // Compare password
    const isPasswordValid = await comparePassword(password, user.HashedPassword);
    if (!isPasswordValid) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Email hoặc mật khẩu không đúng"
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
      message: "Đăng nhập thành công",
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
        VehicleType: true,
        DriverStatus: true,
        IsActive: true,
        CreatedAt: true,
        UpdatedAt: true
      }
    });

    if (!user) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy người dùng"
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
        message: "Không tìm thấy người dùng"
      });
    }

    const avatar = pickFirstDefined(req.body.avatar, req.body.Avatar);
    const fullName = pickFirstDefined(req.body.fullName, req.body.FullName);
    const phoneNumber = pickFirstDefined(req.body.phoneNumber, req.body.PhoneNumber);
    const gender = pickFirstDefined(req.body.gender, req.body.Gender);
    const dateOfBirth = pickFirstDefined(req.body.dateOfBirth, req.body.DateOfBirth);

    if (phoneNumber) {
      const existingPhoneUser = await prisma.account.findFirst({
        where: {
          PhoneNumber: phoneNumber,
          ID: { not: req.user.id }
        }
      });

      if (existingPhoneUser) {
        return res.status(400).json({
          error: "Bad Request",
          message: "Số điện thoại đã được sử dụng"
        });
      }
    }

    const updatedUser = await prisma.account.update({
      where: { ID: req.user.id },
      data: {
        Avatar: avatar,
        FullName: fullName,
        PhoneNumber: phoneNumber,
        Gender: gender,
        DateOfBirth: dateOfBirth
      }
    });

    res.status(200).json({
      message: "Cập nhật hồ sơ thành công",
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
        message: "Không tìm thấy người dùng"
      });
    }

    const newToken = generateToken({
      id: user.ID,
      email: user.Email,
      role: user.Role
    });

    res.status(200).json({
      message: "Làm mới mã phiên thành công",
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
      message: "Đăng xuất thành công. Vui lòng xóa mã phiên khỏi thiết bị.",
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
        message: "Vui lòng nhập email"
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
        message: "Nếu email này đã được đăng ký, mã OTP đặt lại mật khẩu sẽ được gửi đến email"
      });
    }

    // Generate OTP
    const otp = generateOTP();

    // Store OTP
    await storeOTP(email, otp, "reset-password");

    // Send OTP email
    const emailResult = await sendOTPEmail(email, otp, "reset-password");

    if (!emailResult.success) {
      return res.status(500).json({
        error: "Email Service Error",
        message: emailResult.message
      });
    }

    res.status(200).json({
      message: "Đã gửi mã OTP đến email. Vui lòng kiểm tra hộp thư để đặt lại mật khẩu.",
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
        message: "Vui lòng nhập email, mã OTP và mật khẩu mới"
      });
    }

    if (newPassword !== confirmPassword) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Mật khẩu xác nhận không khớp"
      });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Mật khẩu phải có ít nhất 6 ký tự"
      });
    }

    // Verify OTP
    const otpResult = await verifyOTP(email, otp, "reset-password");

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
        message: "Không tìm thấy người dùng"
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
      message: "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.",
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
        message: "Vui lòng cung cấp mã xác thực Google"
      });
    }

    let decodedToken;
    try {
      decodedToken = await verifyGoogleIdToken(idToken);
    } catch (error) {
      console.error("Token verification error:", error);
      const statusCode = error.statusCode || 401;
      return res.status(statusCode).json({
        error: statusCode === 500 ? "Configuration Error" : "Unauthorized",
        message: statusCode === 500 ? error.message : "Mã xác thực Google không hợp lệ"
      });
    }

    const { email, name, picture, sub, email_verified: emailVerified } = decodedToken;

    if (!email) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Không tìm thấy email trong mã xác thực"
      });
    }

    if (emailVerified === false) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Email Google chưa được xác minh"
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
      message: "Đăng nhập Google thành công",
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
