/**
 * Routes Index - Centralized route management
 * All routes are imported and exported from this file
 */

const authRoutes = require("./authRoutes");
const otpRoutes = require("./otpRoutes");
const healthRoutes = require("./healthRoutes");
const placeRoutes = require("./placeRoutes");
const promotionRoutes = require("./promotionRoutes");
const routeRoutes = require("./routeRoutes");
const notificationRoutes = require("./notificationRoutes");
const historyRoutes = require("./historyRoutes");
const driverRequestRoutes = require("./driverRequestRoutes");
const uploadRoutes = require("./uploadRoutes");

/**
 * Mount all routes
 * @param {Express} app - Express application instance
 */
function mountRoutes(app) {
  app.use("/api/auth", authRoutes);
  app.use("/api/otp", otpRoutes);
  app.use("/health", healthRoutes);
  app.use("/api/places", placeRoutes);
  app.use("/api/promotions", promotionRoutes);
  app.use("/api/routes", routeRoutes);
  app.use("/api/notifications", notificationRoutes);
  app.use("/api/history", historyRoutes);
  app.use("/api/driver-requests", driverRequestRoutes);
  app.use("/api/uploads", uploadRoutes);
}

/**
 * Export routes for direct use or mounting
 */
module.exports = {
  authRoutes,
  otpRoutes,
  healthRoutes,
  placeRoutes,
  promotionRoutes,
  routeRoutes,
  notificationRoutes,
  historyRoutes,
  driverRequestRoutes,
  uploadRoutes,
  mountRoutes
};
