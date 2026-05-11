const prisma = require("../config/prisma");
const { emitToAccounts } = require("../socket");

const ACTIVE_CHAT_TRIP_STATUSES = ["Accepted", "InProgress"];

function serializeActiveTrip(trip) {
  if (!trip) {
    return null;
  }

  return {
    id: trip.ID,
    status: trip.Status
  };
}

async function findSharedActiveTrip(accountId, participantId, prismaClient = prisma) {
  if (!accountId || !participantId || Number(accountId) === Number(participantId)) {
    return null;
  }

  return prismaClient.trip.findFirst({
    where: {
      DeletedAt: null,
      Status: { in: ACTIVE_CHAT_TRIP_STATUSES },
      OR: [
        {
          UserID: Number(accountId),
          DriverID: Number(participantId)
        },
        {
          UserID: Number(participantId),
          DriverID: Number(accountId)
        }
      ]
    },
    orderBy: [
      { UpdatedAt: "desc" },
      { ID: "desc" }
    ],
    select: {
      ID: true,
      Status: true,
      UserID: true,
      DriverID: true
    }
  });
}

async function getConversationState(accountId, participantId, prismaClient = prisma) {
  const activeTrip = await findSharedActiveTrip(accountId, participantId, prismaClient);
  return {
    activeTrip: serializeActiveTrip(activeTrip),
    canSend: Boolean(activeTrip)
  };
}

function emitConversationEligibilityForTrip(trip) {
  if (!trip?.UserID || !trip?.DriverID) {
    return;
  }

  const activeTrip = serializeActiveTrip(trip);
  const canSend = ACTIVE_CHAT_TRIP_STATUSES.includes(trip.Status);

  emitToAccounts([trip.UserID], "conversation:eligibility", {
    participantId: trip.DriverID,
    activeTrip,
    canSend
  });

  emitToAccounts([trip.DriverID], "conversation:eligibility", {
    participantId: trip.UserID,
    activeTrip,
    canSend
  });
}

module.exports = {
  serializeActiveTrip,
  findSharedActiveTrip,
  getConversationState,
  emitConversationEligibilityForTrip
};
