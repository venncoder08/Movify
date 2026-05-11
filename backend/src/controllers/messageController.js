const prisma = require("../config/prisma");

const accountSelect = {
  ID: true,
  FullName: true,
  Role: true,
  Avatar: true,
  PhoneNumber: true,
  DriverStatus: true,
  RatePoint: true
};

function serializeParticipant(account) {
  if (!account) {
    return null;
  }

  return {
    id: account.ID,
    fullName: account.FullName,
    role: account.Role,
    avatar: account.Avatar,
    phoneNumber: account.PhoneNumber,
    driverStatus: account.DriverStatus ?? null,
    ratePoint: account.RatePoint == null ? null : Number(account.RatePoint)
  };
}

function serializeMessage(message, currentUserId) {
  return {
    id: message.ID,
    content: message.Content,
    isRead: message.IsRead,
    createdAt: message.CreatedAt,
    updatedAt: message.UpdatedAt,
    senderId: message.SenderID,
    recipientId: message.RecipientID,
    isMine: message.SenderID === currentUserId,
    sender: serializeParticipant(message.Account_Message_SenderIDToAccount),
    recipient: serializeParticipant(message.Account_Message_RecipientIDToAccount)
  };
}

function buildMessagePreview(content, maxLength = 80) {
  const normalized = content.replace(/\s+/g, " ").trim();
  if (normalized.length <= maxLength) {
    return normalized;
  }

  return `${normalized.slice(0, maxLength - 3)}...`;
}

async function findChatAccount(accountId) {
  return prisma.account.findFirst({
    where: {
      ID: accountId,
      DeletedAt: null,
      IsActive: true
    },
    select: accountSelect
  });
}

async function listConversations(req, res) {
  try {
    const currentUserId = req.user.id;
    const messages = await prisma.message.findMany({
      where: {
        DeletedAt: null,
        OR: [
          { SenderID: currentUserId },
          { RecipientID: currentUserId }
        ]
      },
      orderBy: [
        { CreatedAt: "desc" },
        { ID: "desc" }
      ],
      include: {
        Account_Message_SenderIDToAccount: {
          select: accountSelect
        },
        Account_Message_RecipientIDToAccount: {
          select: accountSelect
        }
      }
    });

    const conversations = new Map();
    let unreadCount = 0;

    for (const message of messages) {
      const participant = message.SenderID === currentUserId
        ? message.Account_Message_RecipientIDToAccount
        : message.Account_Message_SenderIDToAccount;

      if (!participant) {
        continue;
      }

      const participantId = participant.ID;
      const isIncomingUnread = message.RecipientID === currentUserId && !message.IsRead;

      if (!conversations.has(participantId)) {
        conversations.set(participantId, {
          participant: serializeParticipant(participant),
          lastMessage: serializeMessage(message, currentUserId),
          unreadCount: 0,
          updatedAt: message.CreatedAt
        });
      }

      if (isIncomingUnread) {
        unreadCount += 1;
        conversations.get(participantId).unreadCount += 1;
      }
    }

    const items = Array.from(conversations.values()).sort(
      (left, right) => new Date(right.updatedAt) - new Date(left.updatedAt)
    );

    res.status(200).json({
      data: {
        items,
        unreadCount
      }
    });
  } catch (error) {
    console.error("List conversations error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function getConversationMessages(req, res) {
  try {
    const currentUserId = req.user.id;
    const participantId = Number(req.params.accountId);

    if (participantId === currentUserId) {
      return res.status(400).json({
        error: "Validation Error",
        message: "You cannot open a conversation with yourself"
      });
    }

    const participant = await findChatAccount(participantId);
    if (!participant) {
      return res.status(404).json({
        error: "Not Found",
        message: "Chat account not found"
      });
    }

    const messages = await prisma.message.findMany({
      where: {
        DeletedAt: null,
        OR: [
          {
            SenderID: currentUserId,
            RecipientID: participantId
          },
          {
            SenderID: participantId,
            RecipientID: currentUserId
          }
        ]
      },
      orderBy: [
        { CreatedAt: "asc" },
        { ID: "asc" }
      ],
      include: {
        Account_Message_SenderIDToAccount: {
          select: accountSelect
        },
        Account_Message_RecipientIDToAccount: {
          select: accountSelect
        }
      }
    });

    const unreadCount = messages.filter(
      (message) =>
        message.SenderID === participantId &&
        message.RecipientID === currentUserId &&
        !message.IsRead
    ).length;

    res.status(200).json({
      data: {
        participant: serializeParticipant(participant),
        unreadCount,
        items: messages.map((message) => serializeMessage(message, currentUserId))
      }
    });
  } catch (error) {
    console.error("Get conversation messages error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function sendMessage(req, res) {
  try {
    const currentUserId = req.user.id;
    const participantId = Number(req.params.accountId);
    const content = req.body.content.trim();

    if (participantId === currentUserId) {
      return res.status(400).json({
        error: "Validation Error",
        message: "You cannot send a message to yourself"
      });
    }

    const [sender, participant] = await Promise.all([
      findChatAccount(currentUserId),
      findChatAccount(participantId)
    ]);

    if (!sender) {
      return res.status(401).json({
        error: "Unauthorized",
        message: "Authenticated account not found"
      });
    }

    if (!participant) {
      return res.status(404).json({
        error: "Not Found",
        message: "Recipient account not found"
      });
    }

    const createdMessage = await prisma.$transaction(async (transaction) => {
      const message = await transaction.message.create({
        data: {
          SenderID: currentUserId,
          RecipientID: participantId,
          Content: content,
          IsRead: false
        },
        include: {
          Account_Message_SenderIDToAccount: {
            select: accountSelect
          },
          Account_Message_RecipientIDToAccount: {
            select: accountSelect
          }
        }
      });

      await transaction.notification.create({
        data: {
          AccountID: participantId,
          Title: "New message",
          Body: `${sender.FullName || "Someone"}: ${buildMessagePreview(content)}`,
          IsRead: false
        }
      });

      return message;
    });

    res.status(201).json({
      message: "Message sent successfully",
      data: serializeMessage(createdMessage, currentUserId)
    });
  } catch (error) {
    console.error("Send message error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

async function markConversationAsRead(req, res) {
  try {
    const currentUserId = req.user.id;
    const participantId = Number(req.params.accountId);

    if (participantId === currentUserId) {
      return res.status(400).json({
        error: "Validation Error",
        message: "You cannot mark your own messages as read"
      });
    }

    const participant = await findChatAccount(participantId);
    if (!participant) {
      return res.status(404).json({
        error: "Not Found",
        message: "Chat account not found"
      });
    }

    const result = await prisma.message.updateMany({
      where: {
        SenderID: participantId,
        RecipientID: currentUserId,
        IsRead: false,
        DeletedAt: null
      },
      data: {
        IsRead: true
      }
    });

    res.status(200).json({
      message: "Conversation marked as read",
      data: {
        updatedCount: result.count
      }
    });
  } catch (error) {
    console.error("Mark conversation as read error:", error);
    res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

module.exports = {
  listConversations,
  getConversationMessages,
  sendMessage,
  markConversationAsRead
};
