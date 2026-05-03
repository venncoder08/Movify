const { PrismaClient } = require("@prisma/client");

const env = require("./env");

const globalForPrisma = globalThis;

const prisma =
  globalForPrisma.prisma ||
  new PrismaClient({
    log: env.nodeEnv === "development" ? ["warn", "error"] : ["error"]
  });

if (env.nodeEnv !== "production") {
  globalForPrisma.prisma = prisma;
}

module.exports = prisma;
