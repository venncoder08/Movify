const { PrismaClient, Prisma } = require("@prisma/client");
const bcrypt = require("bcryptjs");

const mockData = require("./mock-data");

const prisma = new PrismaClient();

async function hashPassword(password) {
  return bcrypt.hash(password, 10);
}

function decimal(value) {
  return new Prisma.Decimal(value);
}

async function upsertAccount(account) {
  const hashedPassword = await hashPassword(account.password);

  return prisma.account.upsert({
    where: { email: account.email },
    update: {
      phoneNumber: account.phoneNumber,
      hashedPassword,
      role: account.role,
      fullName: account.fullName,
      gender: account.gender ?? null,
      avatar: account.avatar ?? null,
      isActive: true,
      idCard: account.idCard ?? null,
      driverLicense: account.driverLicense ?? null,
      vehicleRegistration: account.vehicleRegistration ?? null,
      driverStatus: account.driverStatus ?? null,
      ratePoint: decimal(account.ratePoint ?? "0")
    },
    create: {
      email: account.email,
      phoneNumber: account.phoneNumber,
      hashedPassword,
      role: account.role,
      fullName: account.fullName,
      gender: account.gender ?? null,
      avatar: account.avatar ?? null,
      isActive: true,
      idCard: account.idCard ?? null,
      driverLicense: account.driverLicense ?? null,
      vehicleRegistration: account.vehicleRegistration ?? null,
      driverStatus: account.driverStatus ?? null,
      ratePoint: decimal(account.ratePoint ?? "0")
    }
  });
}

async function seedAccounts() {
  const entries = await Promise.all(
    Object.entries(mockData.accounts).map(async ([key, account]) => [key, await upsertAccount(account)])
  );

  return new Map(entries);
}

async function seedPromotions(accountMap) {
  const entries = await Promise.all(
    mockData.promotions.map(async (promotion) => {
      const admin = accountMap.get(promotion.adminKey);
      const record = await prisma.promotion.upsert({
        where: { id: promotion.id },
        update: {
          name: promotion.name,
          type: promotion.type,
          discountValue: decimal(promotion.discountValue),
          adminId: admin.id,
          deletedAt: null
        },
        create: {
          id: promotion.id,
          name: promotion.name,
          type: promotion.type,
          discountValue: decimal(promotion.discountValue),
          adminId: admin.id
        }
      });

      return [promotion.key, record];
    })
  );

  return new Map(entries);
}

async function seedTrips(accountMap, promotionMap) {
  const entries = await Promise.all(
    mockData.trips.map(async (trip) => {
      const user = accountMap.get(trip.userKey);
      const driver = accountMap.get(trip.driverKey);
      const promotion = trip.promotionKey ? promotionMap.get(trip.promotionKey) : null;

      const record = await prisma.trip.upsert({
        where: { id: trip.id },
        update: {
          distance: decimal(trip.distance),
          totalAmount: decimal(trip.totalAmount),
          pickUpLocation: trip.pickUpLocation,
          destinationLocation: trip.destinationLocation,
          duration: trip.duration ? decimal(trip.duration) : null,
          tripType: trip.tripType,
          status: trip.status,
          pickUpTime: trip.pickUpTime ? new Date(trip.pickUpTime) : null,
          completedTime: trip.completedTime ? new Date(trip.completedTime) : null,
          cancelReason: trip.cancelReason ?? null,
          paymentMethod: trip.paymentMethod,
          paymentStatus: trip.paymentStatus,
          userId: user.id,
          driverId: driver?.id ?? null,
          promotionId: promotion?.id ?? null,
          deletedAt: null
        },
        create: {
          id: trip.id,
          distance: decimal(trip.distance),
          totalAmount: decimal(trip.totalAmount),
          pickUpLocation: trip.pickUpLocation,
          destinationLocation: trip.destinationLocation,
          duration: trip.duration ? decimal(trip.duration) : null,
          tripType: trip.tripType,
          status: trip.status,
          pickUpTime: trip.pickUpTime ? new Date(trip.pickUpTime) : null,
          completedTime: trip.completedTime ? new Date(trip.completedTime) : null,
          cancelReason: trip.cancelReason ?? null,
          paymentMethod: trip.paymentMethod,
          paymentStatus: trip.paymentStatus,
          userId: user.id,
          driverId: driver?.id ?? null,
          promotionId: promotion?.id ?? null
        }
      });

      return [trip.key, record];
    })
  );

  return new Map(entries);
}

async function seedRatings(accountMap, tripMap) {
  await Promise.all(
    mockData.ratings.map(async (rating) => {
      const trip = tripMap.get(rating.tripKey);
      const rater = accountMap.get(rating.raterKey);
      const ratee = accountMap.get(rating.rateeKey);

      await prisma.rating.upsert({
        where: { id: rating.id },
        update: {
          tripId: trip.id,
          raterId: rater.id,
          rateeId: ratee.id,
          score: decimal(rating.score),
          comment: rating.comment ?? null,
          deletedAt: null
        },
        create: {
          id: rating.id,
          tripId: trip.id,
          raterId: rater.id,
          rateeId: ratee.id,
          score: decimal(rating.score),
          comment: rating.comment ?? null
        }
      });
    })
  );
}

async function seedNotifications(accountMap) {
  await Promise.all(
    mockData.notifications.map(async (notification) => {
      const account = accountMap.get(notification.accountKey);

      await prisma.notification.upsert({
        where: { id: notification.id },
        update: {
          accountId: account.id,
          title: notification.title,
          body: notification.body,
          isRead: notification.isRead,
          deletedAt: null
        },
        create: {
          id: notification.id,
          accountId: account.id,
          title: notification.title,
          body: notification.body,
          isRead: notification.isRead
        }
      });
    })
  );
}

async function seedDriverRequests(accountMap) {
  await Promise.all(
    mockData.driverRequests.map(async (request) => {
      const driver = accountMap.get(request.driverKey);
      const admin = request.adminKey ? accountMap.get(request.adminKey) : null;

      await prisma.toDriverRequest.upsert({
        where: { id: request.id },
        update: {
          idCard: request.idCard,
          driverLicense: request.driverLicense,
          vehicleRegistration: request.vehicleRegistration,
          status: request.status,
          driverId: driver.id,
          adminId: admin?.id ?? null,
          deletedAt: null
        },
        create: {
          id: request.id,
          idCard: request.idCard,
          driverLicense: request.driverLicense,
          vehicleRegistration: request.vehicleRegistration,
          status: request.status,
          driverId: driver.id,
          adminId: admin?.id ?? null
        }
      });
    })
  );
}

async function seedMessages(accountMap) {
  await Promise.all(
    mockData.messages.map(async (message) => {
      const sender = accountMap.get(message.senderKey);
      const recipient = accountMap.get(message.recipientKey);

      await prisma.message.upsert({
        where: { id: message.id },
        update: {
          content: message.content,
          isRead: message.isRead,
          senderId: sender.id,
          recipientId: recipient.id,
          deletedAt: null
        },
        create: {
          id: message.id,
          content: message.content,
          isRead: message.isRead,
          senderId: sender.id,
          recipientId: recipient.id
        }
      });
    })
  );
}

async function getCounts() {
  const [
    accounts,
    promotions,
    trips,
    ratings,
    notifications,
    driverRequests,
    messages
  ] = await prisma.$transaction([
    prisma.account.count(),
    prisma.promotion.count(),
    prisma.trip.count(),
    prisma.rating.count(),
    prisma.notification.count(),
    prisma.toDriverRequest.count(),
    prisma.message.count()
  ]);

  return {
    accounts,
    promotions,
    trips,
    ratings,
    notifications,
    driverRequests,
    messages
  };
}

async function main() {
  const accountMap = await seedAccounts();
  const promotionMap = await seedPromotions(accountMap);
  const tripMap = await seedTrips(accountMap, promotionMap);

  await seedRatings(accountMap, tripMap);
  await seedNotifications(accountMap);
  await seedDriverRequests(accountMap);
  await seedMessages(accountMap);

  const counts = await getCounts();
  console.log("Mock seed completed:", counts);
}

main()
  .catch((error) => {
    console.error("Mock seed failed:", error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
