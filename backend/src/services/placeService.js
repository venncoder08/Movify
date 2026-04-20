const PlaceModel = require("../models/placeModel");
const geoapifyRepository = require("../repositories/geoapifyRepository");
const AppError = require("../utils/httpError");

async function autocomplete(query) {
  if (!query) {
    return { suggestions: [] };
  }

  const payload = await geoapifyRepository.fetchAutocompleteResults(query);
  const suggestions = [];
  const seen = new Set();

  for (const result of payload.results || []) {
    const label = result.formatted?.trim();
    if (!label || seen.has(label)) {
      continue;
    }
    seen.add(label);
    suggestions.push(label);
  }

  return { suggestions };
}

async function geocode(query) {
  const place = await findPlaceByQuery(query, {
    missingMessage: "Missing q query parameter",
    notFoundMessage: "Khong tim thay dia diem"
  });

  return { location: place };
}

async function findPlaceByQuery(query, { missingMessage, notFoundMessage }) {
  if (!query) {
    throw new AppError(missingMessage, 400);
  }

  const payload = await geoapifyRepository.fetchGeocodeResults(query, 1);
  const firstResult = payload.results?.[0];
  if (!firstResult) {
    throw new AppError(notFoundMessage, 404);
  }

  return new PlaceModel({
    label: firstResult.formatted || query,
    lat: firstResult.lat,
    lng: firstResult.lon
  });
}

module.exports = {
  autocomplete,
  geocode,
  findPlaceByQuery
};
