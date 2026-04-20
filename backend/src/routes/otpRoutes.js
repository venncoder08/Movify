const express = require("express");
const {
  requestOTP,
  verifyOTPCode,
  registerWithOTP
} = require("../controllers/otpController");
const { validateRequest } = require("../middlewares/validateRequest");
const {
  requestOTPSchema,
  verifyOTPSchema,
  registerWithOTPSchema
} = require("../validations/otpSchemas");

const router = express.Router();

/**
 * Public Routes
 */

// Request OTP (send to email)
router.post("/request", validateRequest(requestOTPSchema), requestOTP);

// Verify OTP
router.post("/verify", validateRequest(verifyOTPSchema), verifyOTPCode);

// Register with OTP verification
router.post("/register", validateRequest(registerWithOTPSchema), registerWithOTP);

module.exports = router;
