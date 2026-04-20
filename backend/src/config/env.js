const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../../.env") });

module.exports = {
  nodeEnv: process.env.NODE_ENV || "development",
  port: Number(process.env.PORT || 3000),
  databaseUrl: process.env.DATABASE_URL || "",
  directUrl: process.env.DIRECT_URL || "",
  geoapifyApiKey: process.env.GEOAPIFY_API_KEY || "",
  countryCode: process.env.GEOAPIFY_COUNTRYCODE || "vn",
  bias: process.env.GEOAPIFY_BIAS || "proximity:106.6297,10.8231",
  autocompleteLimit: Number(process.env.GEOAPIFY_AUTOCOMPLETE_LIMIT || 5),
  jwtSecret: process.env.JWT_SECRET || "movify-dev-secret-key-change-in-production",
  jwtExpiration: process.env.JWT_EXPIRATION || "7d",
  // Email configuration for OTP
  emailService: process.env.EMAIL_SERVICE || "gmail",
  emailUser: process.env.EMAIL_USER || "",
  emailPassword: process.env.EMAIL_PASSWORD || "",
  emailFrom: process.env.EMAIL_FROM || process.env.EMAIL_USER || "noreply@movify.app",
  otpExpiration: Number(process.env.OTP_EXPIRATION || 600), // 10 minutes in seconds
  otpLength: Number(process.env.OTP_LENGTH || 6)
};
