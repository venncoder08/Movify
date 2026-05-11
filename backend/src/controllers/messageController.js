const prisma = require("../config/prisma");
const { emitToAccounts } = require("../socket");
const {
  getConversationState,
  findSharedActiveTrip,
  serializeActiveTrip
} = require("../services/messageTripService");

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
    tripId: message.TripID ?? null,
    content: message.Content,
    contentType: message.ContentType,
    mediaUrl: message.MediaUrl ?? null,
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
  const normalized = String(content || "").replace(/\s+/g, " ").trim();
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

    const items = await Promise.all(Array.from(conversations.values()).map(async (conversation) => {
      const state = await getConversationState(currentUserId, conversation.participant.id);
      return {
        ...conversation,
        activeTrip: state.activeTrip,
        canSend: state.canSend,
        lastTripId: conversation.lastMessage.tripId
      };
    }));

    items.sort((left, right) => {
      const activeDelta = Number(Boolean(right.activeTrip)) - Number(Boolean(left.activeTrip));
      if (activeDelta !== 0) {
        return activeDelta;
      }

      return new Date(right.updatedAt) - new Date(left.updatedAt);
    });

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

    const [messages, state] = await Promise.all([
      prisma.message.findMany({
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
      }),
      getConversationState(currentUserId, participantId)
    ]);

    const unreadCount = messages.filter(
      (message) =>
        message.SenderID === participantId
        && message.RecipientID === currentUserId
        && !message.IsRead
    ).length;

    res.status(200).json({
      data: {
        participant: serializeParticipant(participant),
        unreadCount,
        canSend: state.canSend,
        activeTrip: state.activeTrip,
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
    const contentType = req.body.contentType || "Text";
    const content = String(req.body.content || "").trim();
    const mediaUrl = String(req.body.mediaUrl || "").trim();

    if (participantId === currentUserId) {
      return res.status(400).json({
        error: "Validation Error",
        message: "You cannot send a message to yourself"
      });
    }

    const [sender, participant, activeTrip] = await Promise.all([
      findChatAccount(currentUserId),
      findChatAccount(participantId),
      findSharedActiveTrip(currentUserId, participantId)
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

    if (!activeTrip) {
      return res.status(409).json({
        error: "Conflict",
        message: "You can only send messages during an accepted or in-progress trip"
      });
    }

    const createdMessage = await prisma.$transaction(async (transaction) => {
      const message = await transaction.message.create({
        data: {
          SenderID: currentUserId,
          RecipientID: participantId,
          TripID: activeTrip.ID,
          Content: content,
          ContentType: contentType,
          MediaUrl: mediaUrl || null,
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
          Body: contentType === "Image"
            ? `${sender.FullName || "Someone"} sent you an image`
            : `${sender.FullName || "Someone"}: ${buildMessagePreview(content)}`,
          IsRead: false
        }
      });

      return message;
    });

    const serializedMessage = serializeMessage(createdMessage, currentUserId);
    const activeTripPayload = serializeActiveTrip(activeTrip);

    emitToAccounts([currentUserId], "message:new", {
      participantId,
      message: serializedMessage,
      activeTrip: activeTripPayload,
      canSend: true
    });

    emitToAccounts([participantId], "message:new", {
      participantId: currentUserId,
      message: serializedMessage,
      activeTrip: activeTripPayload,
      canSend: true
    });

    res.status(201).json({
      message: "Message sent successfully",
      data: {
        message: serializedMessage,
        activeTrip: activeTripPayload,
        canSend: true
      }
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

    if (result.count > 0) {
      emitToAccounts([currentUserId], "conversation:read", {
        participantId,
        updatedCount: result.count
      });

      emitToAccounts([participantId], "conversation:read", {
        participantId: currentUserId,
        updatedCount: result.count
      });
    }

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

async function getTripMessages(req, res) {
  try {
    const currentUserId = req.user.id;
    const tripId = Number(req.params.id);

    const trip = await prisma.trip.findFirst({
      where: {
        ID: tripId,
        DeletedAt: null
      },
      select: {
        ID: true,
        Status: true,
        UserID: true,
        DriverID: true
      }
    });

    if (!trip) {
      return res.status(404).json({
        error: "Not Found",
        message: "Trip not found"
      });
    }

    const isPassenger = trip.UserID === currentUserId;
    const isDriver = trip.DriverID === currentUserId;
    const isAdmin = req.user.role === "Admin";
    if (!isPassenger && !isDriver && !isAdmin) {
      return res.status(403).json({
        error: "Forbidden",
        message: "You do not have access to this trip conversation"
      });
    }

    const messages = await prisma.message.findMany({
      where: {
        DeletedAt: null,
        TripID: tripId
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

    const participantId = isPassenger ? trip.DriverID : trip.UserID;

    res.status(200).json({
      data: {
        trip: {
          id: trip.ID,
          status: trip.Status
        },
        participantId: participantId ?? null,
        canSend: false,
        activeTrip: null,
        items: messages.map((message) => serializeMessage(message, currentUserId))
      }
    });
  } catch (error) {
    console.error("Get trip messages error:", error);
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
  markConversationAsRead,
  getTripMessages
};
