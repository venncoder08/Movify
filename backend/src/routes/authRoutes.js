const express = require("express");
const {
  register,
  login,
  logout,
  getProfile,
  refreshToken,
  forgotPassword,
  resetPassword,
  googleSignIn
} = require("../controllers/authController");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const {
  registerSchema,
  loginSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  googleSignInSchema
} = require("../validations/authSchemas");

const router = express.Router();

/**
 * Public Routes
 */
router.post("/register", validateRequest(registerSchema), register);
router.post("/login", validateRequest(loginSchema), login);
router.post("/google", validateRequest(googleSignInSchema), googleSignIn);
router.post("/forgot-password", validateRequest(forgotPasswordSchema), forgotPassword);
router.post("/reset-password", validateRequest(resetPasswordSchema), resetPassword);

/**
 * Protected Routes (require authentication)
 */
router.get("/me", authMiddleware, getProfile);
router.post("/refresh", authMiddleware, refreshToken);
router.post("/logout", authMiddleware, logout);

module.exports = router;
