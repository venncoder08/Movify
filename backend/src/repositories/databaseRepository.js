const prisma = require("../config/prisma");

async function ping() {
  await prisma.$queryRaw`SELECT 1`;
}

async function getCounts() {
  const [
    accounts,
    promotions,
    trips,
    ratings,
    notifications,
    driverRequests
  ] = await prisma.$transaction([
    prisma.account.count(),
    prisma.promotion.count(),
    prisma.trip.count(),
    prisma.rating.count(),
    prisma.notification.count(),
    prisma.toDriverRequest.count()
  ]);

  return {
    accounts,
    promotions,
    trips,
    ratings,
    notifications,
    driverRequests
  };
}

module.exports = {
  ping,
  getCounts
};
