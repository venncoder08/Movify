const express = require("express");

const {
  listConversations,
  getConversationMessages,
  sendMessage,
  markConversationAsRead
} = require("../controllers/messageController");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const {
  conversationAccountSchema,
  sendMessageSchema
} = require("../validations");

const router = express.Router();

router.get("/", authMiddleware, listConversations);
router.get("/:accountId", authMiddleware, validateRequest(conversationAccountSchema), getConversationMessages);
router.post("/:accountId", authMiddleware, validateRequest(sendMessageSchema), sendMessage);
router.patch("/:accountId/read", authMiddleware, validateRequest(conversationAccountSchema), markConversationAsRead);

module.exports = router;
