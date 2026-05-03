const databaseService = require("../services/databaseService");

async function getHealth(_req, res) {
  const database = await databaseService.getStatus();
  res.json({
    status: "ok",
    database
  });
}

module.exports = {
  getHealth
};
