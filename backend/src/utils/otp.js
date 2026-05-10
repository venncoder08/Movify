const crypto = require("crypto");
const { PrismaClient } = require("@prisma/client");
const env = require("../config/env");

const prisma = new PrismaClient();
const MAX_OTP_ATTEMPTS = 5;

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function normalizePurpose(purpose) {
  return String(purpose || "generic").trim();
}

function hashOTP(otp) {
  return crypto
    .createHash("sha256")
    .update(String(otp))
    .digest("hex");
}

/**
 * Generate random OTP
 * @returns {string} OTP code
 */
function generateOTP() {
  return Math.floor(Math.pow(10, env.otpLength - 1) + Math.random() * 9 * Math.pow(10, env.otpLength - 1))
    .toString()
    .slice(0, env.otpLength);
}

/**
 * Store OTP with expiration.
 * @param {string} email - User email
 * @param {string} otp - OTP code
 * @param {string} purpose - OTP purpose
 */
async function storeOTP(email, otp, purpose = "generic") {
  const normalizedEmail = normalizeEmail(email);
  const normalizedPurpose = normalizePurpose(purpose);
  const expiresAt = new Date(Date.now() + env.otpExpiration * 1000);

  await prisma.otpCode.upsert({
    where: {
      Email_Purpose: {
        Email: normalizedEmail,
        Purpose: normalizedPurpose
      }
    },
    create: {
      Email: normalizedEmail,
      Purpose: normalizedPurpose,
      CodeHash: hashOTP(otp),
      Attempts: 0,
      ExpiresAt: expiresAt
    },
    update: {
      CodeHash: hashOTP(otp),
      Attempts: 0,
      ExpiresAt: expiresAt
    }
  });
}

/**
 * Verify OTP.
 * @param {string} email - User email
 * @param {string} otp - OTP code to verify
 * @param {string} purpose - OTP purpose
 * @returns {object} { valid: boolean, message: string }
 */
async function verifyOTP(email, otp, purpose = "generic") {
  const normalizedEmail = normalizeEmail(email);
  const normalizedPurpose = normalizePurpose(purpose);

  const stored = await prisma.otpCode.findUnique({
    where: {
      Email_Purpose: {
        Email: normalizedEmail,
        Purpose: normalizedPurpose
      }
    }
  });

  if (!stored) {
    return {
      valid: false,
      message: "Không tìm thấy mã OTP hoặc mã đã hết hạn"
    };
  }

  if (stored.ExpiresAt.getTime() <= Date.now()) {
    await prisma.otpCode.delete({ where: { ID: stored.ID } });
    return {
      valid: false,
      message: "Mã OTP đã hết hạn"
    };
  }

  if (stored.Attempts >= MAX_OTP_ATTEMPTS) {
    await prisma.otpCode.delete({ where: { ID: stored.ID } });
    return {
      valid: false,
      message: "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới."
    };
  }

  if (stored.CodeHash !== hashOTP(otp)) {
    const attempts = stored.Attempts + 1;

    if (attempts >= MAX_OTP_ATTEMPTS) {
      await prisma.otpCode.delete({ where: { ID: stored.ID } });
      return {
        valid: false,
        message: "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu mã mới."
      };
    }

    await prisma.otpCode.update({
      where: { ID: stored.ID },
      data: { Attempts: attempts }
    });

    return {
      valid: false,
      message: `Mã OTP không đúng. Còn ${MAX_OTP_ATTEMPTS - attempts} lần thử.`
    };
  }

  await prisma.otpCode.delete({ where: { ID: stored.ID } });
  return {
    valid: true,
    message: "Xác thực OTP thành công"
  };
}

/**
 * Get OTP info (for testing).
 * @param {string} email - User email
 * @param {string} purpose - OTP purpose
 * @returns {object} OTP info
 */
function getOTPInfo(email, purpose = "generic") {
  return prisma.otpCode.findUnique({
    where: {
      Email_Purpose: {
        Email: normalizeEmail(email),
        Purpose: normalizePurpose(purpose)
      }
    }
  });
}

/**
 * Clear OTP.
 * @param {string} email - User email
 * @param {string} purpose - OTP purpose
 */
async function clearOTP(email, purpose = "generic") {
  const stored = await getOTPInfo(email, purpose);
  if (stored) {
    await prisma.otpCode.delete({ where: { ID: stored.ID } });
  }
}

/**
 * Clear all OTPs (for testing/development).
 */
function clearAllOTPs() {
  return prisma.otpCode.deleteMany({});
}

module.exports = {
  generateOTP,
  storeOTP,
  verifyOTP,
  getOTPInfo,
  clearOTP,
  clearAllOTPs
};
