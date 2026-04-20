const { verifyToken, extractToken } = require("../utils/jwt");

/**
 * Middleware to verify JWT token
 * Adds user data to req.user if token is valid
 */
function authMiddleware(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    const token = extractToken(authHeader);

    if (!token) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Missing or invalid authorization token"
      });
    }

    const decoded = verifyToken(token);
    if (!decoded) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Invalid or expired token"
      });
    }

    req.user = decoded;
    next();
  } catch (error) {
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

/**
 * Middleware to check if user has specific role
 */
function roleMiddleware(allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "User not authenticated"
      });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        error: "Forbidden",
        message: `This action requires one of these roles: ${allowedRoles.join(", ")}`
      });
    }

    next();
  };
}

module.exports = {
  authMiddleware,
  roleMiddleware
};
