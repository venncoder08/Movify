const { z } = require("zod");

const accountIdParam = z.coerce
  .number()
  .int("Account id must be an integer")
  .positive("Account id must be greater than 0");

const conversationAccountSchema = z.object({
  params: z.object({
    accountId: accountIdParam
  })
});

const sendMessageSchema = z.object({
  params: z.object({
    accountId: accountIdParam
  }),
  body: z.object({
    content: z
      .string()
      .trim()
      .min(1, "Message content is required")
      .max(2000, "Message content must be at most 2000 characters")
  })
});

module.exports = {
  conversationAccountSchema,
  sendMessageSchema
};
