const env = require("../config/env");

// In-memory OTP storage (for development)
// In production, use Redis or database
const otpStore = new Map();

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
 * Store OTP with expiration
 * @param {string} email - User email
 * @param {string} otp - OTP code
 */
function storeOTP(email, otp) {
  const expiresAt = Date.now() + env.otpExpiration * 1000;
  otpStore.set(email, {
    otp,
    expiresAt,
    attempts: 0
  });

  // Auto-clean expired OTP after expiration time
  setTimeout(() => {
    if (otpStore.has(email)) {
      const stored = otpStore.get(email);
      if (stored.expiresAt <= Date.now()) {
        otpStore.delete(email);
      }
    }
  }, env.otpExpiration * 1000 + 1000);
}

/**
 * Verify OTP
 * @param {string} email - User email
 * @param {string} otp - OTP code to verify
 * @returns {object} { valid: boolean, message: string }
 */
function verifyOTP(email, otp) {
  if (!otpStore.has(email)) {
    return {
      valid: false,
      message: "OTP not found or expired"
    };
  }

  const stored = otpStore.get(email);

  // Check expiration
  if (stored.expiresAt <= Date.now()) {
    otpStore.delete(email);
    return {
      valid: false,
      message: "OTP has expired"
    };
  }

  // Check attempts (max 5 attempts)
  if (stored.attempts >= 5) {
    otpStore.delete(email);
    return {
      valid: false,
      message: "Maximum OTP attempts exceeded. Request a new OTP."
    };
  }

  // Verify OTP
  if (stored.otp !== otp) {
    stored.attempts += 1;
    return {
      valid: false,
      message: `Invalid OTP. ${5 - stored.attempts} attempts remaining.`
    };
  }

  // Valid OTP - delete from store
  otpStore.delete(email);
  return {
    valid: true,
    message: "OTP verified successfully"
  };
}

/**
 * Get OTP info (for testing)
 * @param {string} email - User email
 * @returns {object} OTP info
 */
function getOTPInfo(email) {
  return otpStore.get(email);
}

/**
 * Clear OTP
 * @param {string} email - User email
 */
function clearOTP(email) {
  otpStore.delete(email);
}

/**
 * Clear all OTPs (for testing/development)
 */
function clearAllOTPs() {
  otpStore.clear();
}

module.exports = {
  generateOTP,
  storeOTP,
  verifyOTP,
  getOTPInfo,
  clearOTP,
  clearAllOTPs
};
