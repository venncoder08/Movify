const { z } = require("zod");

const accountIdParam = z.coerce
  .number()
  .int("Account id must be an integer")
  .positive("Account id must be greater than 0");

const messageTypeEnum = z.enum(["Text", "Image"], {
  errorMap: () => ({ message: "Message content type must be Text or Image" })
});

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
    contentType: messageTypeEnum.optional().default("Text"),
    content: z
      .string()
      .trim()
      .max(2000, "Message content must be at most 2000 characters")
      .optional()
      .default(""),
    mediaUrl: z
      .string()
      .trim()
      .max(500, "Media URL must be at most 500 characters")
      .optional()
      .default("")
  }).superRefine((body, context) => {
    if (body.contentType === "Text" && !body.content.trim()) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["content"],
        message: "Message content is required"
      });
    }

    if (body.contentType === "Image" && !body.mediaUrl.trim()) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["mediaUrl"],
        message: "Media URL is required for image messages"
      });
    }
  })
});

module.exports = {
  conversationAccountSchema,
  sendMessageSchema
};
