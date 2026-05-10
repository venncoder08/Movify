const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

function inferNotificationMeta(notification) {
  const normalizedText = normalizeNotificationText(`${notification.Title} ${notification.Body}`);

  if (containsNotificationKeyword(normalizedText, ["hủy", "từ chối"])) {
    return {
      category: "Cảnh báo",
      iconKey: "warning",
      isWarning: true
    };
  }

  if (containsNotificationKeyword(normalizedText, ["thanh toán"])) {
    return {
      category: "Thanh toán",
      iconKey: "payment",
      isWarning: false
    };
  }

  if (containsNotificationKeyword(normalizedText, ["tài xế", "chuyến đi"])) {
    return {
      category: "Chuyến đi",
      iconKey: "trip",
      isWarning: false
    };
  }

  if (containsNotificationKeyword(normalizedText, ["khuyến mãi"])) {
    return {
      category: "Khuyến mãi",
      iconKey: "promotion",
      isWarning: false
    };
  }

  return {
    category: "Thông báo",
    iconKey: "info",
    isWarning: false
  };
}

function containsNotificationKeyword(normalizedText, keywords) {
  return keywords.some((keyword) => normalizedText.includes(normalizeNotificationText(keyword)));
}

function normalizeNotificationText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();
}

function serializeNotification(notification) {
  const meta = inferNotificationMeta(notification);

  return {
    id: notification.ID,
    title: notification.Title,
    body: notification.Body,
    isRead: notification.IsRead,
    createdAt: notification.CreatedAt,
    updatedAt: notification.UpdatedAt,
    ...meta
  };
}

async function listNotifications(req, res) {
  try {
    const notifications = await prisma.notification.findMany({
      where: {
        AccountID: req.user.id,
        DeletedAt: null
      },
      orderBy: {
        CreatedAt: "desc"
      }
    });

    const unreadCount = notifications.filter((notification) => !notification.IsRead).length;

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
        message: "Mã thông báo không hợp lệ"
      });
    }

    const notification = await prisma.notification.findFirst({
      where: {
        ID: notificationId,
        AccountID: req.user.id,
        DeletedAt: null
      }
    });

    if (!notification) {
      return res.status(404).json({
        error: "Not Found",
        message: "Không tìm thấy thông báo"
      });
    }

    const updatedNotification = await prisma.notification.update({
      where: {
        ID: notificationId
      },
      data: {
        IsRead: true
      }
    });

    res.status(200).json({
      message: "Đã đánh dấu thông báo là đã đọc",
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
        AccountID: req.user.id,
        IsRead: false,
        DeletedAt: null
      },
      data: {
        IsRead: true
      }
    });

    res.status(200).json({
      message: "Đã đánh dấu tất cả thông báo là đã đọc",
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
