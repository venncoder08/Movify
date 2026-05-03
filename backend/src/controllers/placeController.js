const placeService = require("../services/placeService");

async function autocomplete(req, res) {
  const payload = await placeService.autocomplete(String(req.query.q || "").trim());
  res.json(payload);
}

async function geocode(req, res) {
  const payload = await placeService.geocode(String(req.query.q || "").trim());
  res.json(payload);
}

module.exports = {
  autocomplete,
  geocode
};
