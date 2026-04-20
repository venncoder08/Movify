const express = require("express");

const {
  listNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead
} = require("../controllers/notificationController");
const { authMiddleware } = require("../middlewares/authMiddleware");

const router = express.Router();

router.get("/", authMiddleware, listNotifications);
router.patch("/read-all", authMiddleware, markAllNotificationsAsRead);
router.patch("/:id/read", authMiddleware, markNotificationAsRead);

module.exports = router;
