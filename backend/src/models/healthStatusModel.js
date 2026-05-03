class HealthStatusModel {
  constructor({
    configured,
    directUrlConfigured,
    provider,
    connected,
    schemaReady,
    mockSeeded,
    stats,
    error
  }) {
    this.configured = configured;
    this.directUrlConfigured = directUrlConfigured;
    this.provider = provider;
    this.connected = connected;
    this.schemaReady = schemaReady;
    this.mockSeeded = mockSeeded;
    this.stats = stats;
    this.error = error;
  }
}

module.exports = HealthStatusModel;
