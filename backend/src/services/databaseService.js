const env = require("../config/env");
const HealthStatusModel = require("../models/healthStatusModel");
const databaseRepository = require("../repositories/databaseRepository");

async function getStatus() {
  const databaseStatus = {
    configured: Boolean(env.databaseUrl),
    directUrlConfigured: Boolean(env.directUrl),
    provider: "postgresql",
    connected: false,
    schemaReady: false,
    mockSeeded: false,
    stats: null,
    error: null
  };

  if (!databaseStatus.configured) {
    return new HealthStatusModel(databaseStatus);
  }

  try {
    await databaseRepository.ping();
    databaseStatus.connected = true;
  } catch (error) {
    databaseStatus.error = error.message;
    return new HealthStatusModel(databaseStatus);
  }

  try {
    const counts = await databaseRepository.getCounts();
    databaseStatus.schemaReady = true;
    databaseStatus.stats = counts;
    databaseStatus.mockSeeded = Object.values(counts).some((value) => Number(value) > 0);
  } catch (error) {
    databaseStatus.error = error.message;
  }

  return new HealthStatusModel(databaseStatus);
}

module.exports = {
  getStatus
};
