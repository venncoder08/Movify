const routeService = require("../services/routeService");

async function preview(req, res) {
  const payload = await routeService.preview({
    pickupQuery: String(req.query.pickup || "").trim(),
    destinationQuery: String(req.query.destination || "").trim()
  });

  res.json(payload);
}

module.exports = {
  preview
};
