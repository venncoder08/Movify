const env = require("../config/env");
const geoapifyService = require("../services/geoapifyService");

async function fetchAutocompleteResults(query) {
  return geoapifyService.fetchJson("/v1/geocode/autocomplete", {
    text: query,
    format: "json",
    lang: "vi",
    limit: env.autocompleteLimit,
    filter: `countrycode:${env.countryCode}`,
    bias: env.bias
  });
}

async function fetchGeocodeResults(query, limit = 1) {
  return geoapifyService.fetchJson("/v1/geocode/search", {
    text: query,
    format: "json",
    lang: "vi",
    limit,
    filter: `countrycode:${env.countryCode}`,
    bias: env.bias
  });
}

async function fetchNearbyGeocodeResults({ query, lat, lng, radiusMeters, limit }) {
  return geoapifyService.fetchJson("/v1/geocode/search", {
    text: query,
    format: "json",
    lang: "vi",
    limit,
    filter: `circle:${lng},${lat},${radiusMeters}`,
    bias: `proximity:${lng},${lat}`
  });
}

async function fetchRoutePreview(pickup, destination) {
  return geoapifyService.fetchJson("/v1/routing", {
    waypoints: `${pickup.lat},${pickup.lng}|${destination.lat},${destination.lng}`,
    mode: "drive"
  });
}

async function fetchNearbyPlaces({ categories, conditions, lat, lng, radiusMeters, limit, name }) {
  return geoapifyService.fetchJson("/v2/places", {
    categories: Array.isArray(categories) ? categories.join(",") : categories,
    conditions: Array.isArray(conditions) ? conditions.join(",") : conditions,
    filter: `circle:${lng},${lat},${radiusMeters}`,
    bias: `proximity:${lng},${lat}`,
    lang: "vi",
    limit,
    name
  });
}

module.exports = {
  fetchAutocompleteResults,
  fetchGeocodeResults,
  fetchNearbyGeocodeResults,
  fetchRoutePreview,
  fetchNearbyPlaces
};
