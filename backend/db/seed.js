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
    where: { Email: account.email },
    update: {
      PhoneNumber: account.phoneNumber,
      HashedPassword: hashedPassword,
      Role: account.role,
      FullName: account.fullName,
      Gender: account.gender ?? null,
      Avatar: account.avatar ?? null,
      IsActive: true,
      IDCard: account.idCard ?? null,
      DriverLicense: account.driverLicense ?? null,
      VehicleRegistration: account.vehicleRegistration ?? null,
      VehicleType: account.vehicleType ?? null,
      DriverStatus: account.driverStatus ?? null,
      RatePoint: decimal(account.ratePoint ?? "0"),
      UpdatedAt: new Date()
    },
    create: {
      Email: account.email,
      PhoneNumber: account.phoneNumber,
      HashedPassword: hashedPassword,
      Role: account.role,
      FullName: account.fullName,
      Gender: account.gender ?? null,
      Avatar: account.avatar ?? null,
      IsActive: true,
      IDCard: account.idCard ?? null,
      DriverLicense: account.driverLicense ?? null,
      VehicleRegistration: account.vehicleRegistration ?? null,
      VehicleType: account.vehicleType ?? null,
      DriverStatus: account.driverStatus ?? null,
      RatePoint: decimal(account.ratePoint ?? "0"),
      CreatedAt: new Date(),
      UpdatedAt: new Date()
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
        where: { ID: promotion.id },
        update: {
          Name: promotion.name,
          Type: promotion.type,
          DiscountValue: decimal(promotion.discountValue),
          AdminID: admin.ID,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: promotion.id,
          Name: promotion.name,
          Type: promotion.type,
          DiscountValue: decimal(promotion.discountValue),
          AdminID: admin.ID,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
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
        where: { ID: trip.id },
        update: {
          Distance: decimal(trip.distance),
          TotalAmount: decimal(trip.totalAmount),
          PickUpLocation: trip.pickUpLocation,
          DestinationLocation: trip.destinationLocation,
          PickUpLatitude: trip.pickUpLatitude ? decimal(trip.pickUpLatitude) : null,
          PickUpLongitude: trip.pickUpLongitude ? decimal(trip.pickUpLongitude) : null,
          DestinationLatitude: trip.destinationLatitude ? decimal(trip.destinationLatitude) : null,
          DestinationLongitude: trip.destinationLongitude ? decimal(trip.destinationLongitude) : null,
          Duration: trip.duration ? decimal(trip.duration) : null,
          TripType: trip.tripType,
          Status: trip.status,
          PickUpTime: trip.pickUpTime ? new Date(trip.pickUpTime) : null,
          CompletedTime: trip.completedTime ? new Date(trip.completedTime) : null,
          CancelReason: trip.cancelReason ?? null,
          PaymentMethod: trip.paymentMethod,
          PaymentStatus: trip.paymentStatus,
          UserID: user.ID,
          DriverID: driver?.ID ?? null,
          PromotionID: promotion?.id ?? null,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: trip.id,
          Distance: decimal(trip.distance),
          TotalAmount: decimal(trip.totalAmount),
          PickUpLocation: trip.pickUpLocation,
          DestinationLocation: trip.destinationLocation,
          PickUpLatitude: trip.pickUpLatitude ? decimal(trip.pickUpLatitude) : null,
          PickUpLongitude: trip.pickUpLongitude ? decimal(trip.pickUpLongitude) : null,
          DestinationLatitude: trip.destinationLatitude ? decimal(trip.destinationLatitude) : null,
          DestinationLongitude: trip.destinationLongitude ? decimal(trip.destinationLongitude) : null,
          Duration: trip.duration ? decimal(trip.duration) : null,
          TripType: trip.tripType,
          Status: trip.status,
          PickUpTime: trip.pickUpTime ? new Date(trip.pickUpTime) : null,
          CompletedTime: trip.completedTime ? new Date(trip.completedTime) : null,
          CancelReason: trip.cancelReason ?? null,
          PaymentMethod: trip.paymentMethod,
          PaymentStatus: trip.paymentStatus,
          UserID: user.ID,
          DriverID: driver?.ID ?? null,
          PromotionID: promotion?.ID ?? null,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
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
        where: { ID: rating.id },
        update: {
          TripID: trip.ID,
          RaterID: rater.ID,
          RateeID: ratee.ID,
          Score: decimal(rating.score),
          Comment: rating.comment ?? null,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: rating.id,
          TripID: trip.ID,
          RaterID: rater.ID,
          RateeID: ratee.ID,
          Score: decimal(rating.score),
          Comment: rating.comment ?? null,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
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
        where: { ID: notification.id },
        update: {
          AccountID: account.ID,
          Title: notification.title,
          Body: notification.body,
          IsRead: notification.isRead,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: notification.id,
          AccountID: account.ID,
          Title: notification.title,
          Body: notification.body,
          IsRead: notification.isRead,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
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
        where: { ID: request.id },
        update: {
          IDCard: request.idCard,
          DriverLicense: request.driverLicense,
          VehicleRegistration: request.vehicleRegistration,
          VehicleType: request.vehicleType,
          Status: request.status,
          DriverID: driver.ID,
          AdminID: admin?.ID ?? null,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: request.id,
          IDCard: request.idCard,
          DriverLicense: request.driverLicense,
          VehicleRegistration: request.vehicleRegistration,
          VehicleType: request.vehicleType,
          Status: request.status,
          DriverID: driver.ID,
          AdminID: admin?.ID ?? null,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
        }
      });

      await prisma.account.update({
        where: { ID: driver.ID },
        data: {
          IDCard: request.idCard,
          DriverLicense: request.driverLicense,
          VehicleRegistration: request.vehicleRegistration,
          VehicleType: request.vehicleType,
          DriverStatus: request.status
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
        where: { ID: message.id },
        update: {
          Content: message.content,
          ContentType: message.contentType ?? "Text",
          IsRead: message.isRead,
          SenderID: sender.ID,
          RecipientID: recipient.ID,
          UpdatedAt: new Date(),
          DeletedAt: null
        },
        create: {
          ID: message.id,
          Content: message.content,
          ContentType: message.contentType ?? "Text",
          IsRead: message.isRead,
          SenderID: sender.ID,
          RecipientID: recipient.ID,
          CreatedAt: new Date(),
          UpdatedAt: new Date()
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
