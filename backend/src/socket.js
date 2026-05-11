const http = require("http");
const { Server } = require("socket.io");

const { verifyToken, extractToken } = require("./utils/jwt");

let ioInstance = null;

function resolveSocketToken(socket) {
  const authToken = typeof socket.handshake.auth?.token === "string"
    ? socket.handshake.auth.token.trim()
    : "";
  if (authToken) {
    return authToken;
  }

  return extractToken(socket.handshake.headers?.authorization) || null;
}

function initializeSocketServer(app) {
  const httpServer = http.createServer(app);
  const io = new Server(httpServer, {
    cors: {
      origin: "*",
      methods: ["GET", "POST", "PATCH"]
    }
  });

  io.use((socket, next) => {
    const token = resolveSocketToken(socket);
    if (!token) {
      return next(new Error("Unauthorized"));
    }

    const decoded = verifyToken(token);
    if (!decoded?.id) {
      return next(new Error("Unauthorized"));
    }

    socket.data.user = decoded;
    return next();
  });

  io.on("connection", (socket) => {
    const userId = socket.data.user?.id;
    if (!userId) {
      socket.disconnect(true);
      return;
    }

    socket.join(`account:${userId}`);
  });

  ioInstance = io;
  return httpServer;
}

function emitToAccounts(accountIds, eventName, payload) {
  if (!ioInstance) {
    return;
  }

  Array.from(new Set((accountIds || []).filter(Boolean))).forEach((accountId) => {
    ioInstance.to(`account:${accountId}`).emit(eventName, payload);
  });
}

module.exports = {
  initializeSocketServer,
  emitToAccounts
};
