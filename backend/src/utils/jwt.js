const jwt = require("jsonwebtoken");
const env = require("../config/env");

/**
 * Generate JWT token
 * @param {object} payload - Token payload (usually user data)
 * @returns {string} JWT token
 */
function generateToken(payload) {
  return jwt.sign(payload, env.jwtSecret, {
    expiresIn: env.jwtExpiration,
    algorithm: "HS256"
  });
}

/**
 * Verify JWT token
 * @param {string} token - JWT token to verify
 * @returns {object|null} Decoded token or null if invalid
 */
function verifyToken(token) {
  try {
    return jwt.verify(token, env.jwtSecret, {
      algorithms: ["HS256"]
    });
  } catch (error) {
    console.error("Token verification error:", error.message);
    return null;
  }
}

/**
 * Extract token from Authorization header
 * @param {string} authHeader - Authorization header value
 * @returns {string|null} Token or null
 */
function extractToken(authHeader) {
  if (!authHeader) return null;
  const parts = authHeader.split(" ");
  if (parts.length !== 2 || parts[0] !== "Bearer") return null;
  return parts[1];
}

module.exports = {
  generateToken,
  verifyToken,
  extractToken
};
