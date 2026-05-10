const PlaceModel = require("../models/placeModel");
const geoapifyRepository = require("../repositories/geoapifyRepository");
const AppError = require("../utils/httpError");

const COORDINATE_PATTERN = /(-?\d{1,2}\.\d{3,})\s*,\s*(-?\d{1,3}\.\d{3,})/;

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
    missingMessage: "Thiếu tham số địa điểm",
    notFoundMessage: "Không tìm thấy địa điểm"
  });

  return { location: place };
}

async function findPlaceByQuery(query, { missingMessage, notFoundMessage }) {
  if (!query) {
    throw new AppError(missingMessage, 400);
  }

  const embeddedCoordinates = parseEmbeddedCoordinates(query);
  if (embeddedCoordinates) {
    return new PlaceModel({
      label: query,
      lat: embeddedCoordinates.lat,
      lng: embeddedCoordinates.lng
    });
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

function parseEmbeddedCoordinates(query) {
  const match = query.match(COORDINATE_PATTERN);
  if (!match) {
    return null;
  }

  const lat = Number(match[1]);
  const lng = Number(match[2]);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }

  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    return null;
  }

  return { lat, lng };
}

module.exports = {
  autocomplete,
  geocode,
  findPlaceByQuery
};
