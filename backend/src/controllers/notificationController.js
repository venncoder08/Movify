const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function inferNotificationMeta(notification) {
  const normalizedText = `${notification.title} ${notification.body}`.toLowerCase();

  if (normalizedText.includes("huy") || normalizedText.includes("tu choi")) {
    return {
      category: "Canh bao",
      iconKey: "warning",
      isWarning: true
    };
  }

  if (normalizedText.includes("thanh toan")) {
    return {
      category: "Thanh toan",
      iconKey: "payment",
      isWarning: false
    };
  }

  if (normalizedText.includes("tai xe") || normalizedText.includes("chuyen di")) {
    return {
      category: "Chuyen di",
      iconKey: "trip",
      isWarning: false
    };
  }

  if (normalizedText.includes("khuyen mai")) {
    return {
      category: "Khuyen mai",
      iconKey: "promotion",
      isWarning: false
    };
  }

  return {
    category: "Thong bao",
    iconKey: "info",
    isWarning: false
  };
}

function serializeNotification(notification) {
  const meta = inferNotificationMeta(notification);

  return {
    id: notification.id,
    title: notification.title,
    body: notification.body,
    isRead: notification.isRead,
    createdAt: notification.createdAt,
    updatedAt: notification.updatedAt,
    ...meta
  };
}

async function listNotifications(req, res) {
  try {
    const notifications = await prisma.notification.findMany({
      where: {
        accountId: req.user.id,
        deletedAt: null
      },
      orderBy: {
        createdAt: "desc"
      }
    });

    const unreadCount = notifications.filter((notification) => !notification.isRead).length;

    res.status(200).json({
      data: {
        items: notifications.map(serializeNotification),
        unreadCount
      }
    });
  } catch (error) {
    console.error("List notifications error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function markNotificationAsRead(req, res) {
  try {
    const notificationId = Number(req.params.id);

    if (!Number.isInteger(notificationId) || notificationId <= 0) {
      return res.status(400).json({
        error: "Validation Error",
        message: "Notification id is invalid"
      });
    }

    const notification = await prisma.notification.findFirst({
      where: {
        id: notificationId,
        accountId: req.user.id,
        deletedAt: null
      }
    });

    if (!notification) {
      return res.status(404).json({
        error: "Not Found",
        message: "Notification not found"
      });
    }

    const updatedNotification = await prisma.notification.update({
      where: {
        id: notificationId
      },
      data: {
        isRead: true
      }
    });

    res.status(200).json({
      message: "Notification marked as read",
      data: serializeNotification(updatedNotification)
    });
  } catch (error) {
    console.error("Mark notification as read error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function markAllNotificationsAsRead(req, res) {
  try {
    const result = await prisma.notification.updateMany({
      where: {
        accountId: req.user.id,
        isRead: false,
        deletedAt: null
      },
      data: {
        isRead: true
      }
    });

    res.status(200).json({
      message: "All notifications marked as read",
      data: {
        updatedCount: result.count
      }
    });
  } catch (error) {
    console.error("Mark all notifications as read error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  listNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead
};
