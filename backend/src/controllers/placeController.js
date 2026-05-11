const placeService = require("../services/placeService");

async function autocomplete(req, res) {
  const payload = await placeService.autocomplete(String(req.query.q || "").trim());
  res.json(payload);
}

async function geocode(req, res) {
  const payload = await placeService.geocode(String(req.query.q || "").trim());
  res.json(payload);
}

async function recommendNearby(req, res) {
  const payload = await placeService.recommendNearby({
    query: String(req.query.q || "").trim(),
    lat: req.query.lat,
    lng: req.query.lng,
    radiusMeters: req.query.radiusMeters,
    limit: req.query.limit
  });
  res.json(payload);
}

module.exports = {
  autocomplete,
  geocode,
  recommendNearby
};
