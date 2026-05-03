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

async function fetchRoutePreview(pickup, destination) {
  return geoapifyService.fetchJson("/v1/routing", {
    waypoints: `${pickup.lat},${pickup.lng}|${destination.lat},${destination.lng}`,
    mode: "drive"
  });
}

module.exports = {
  fetchAutocompleteResults,
  fetchGeocodeResults,
  fetchRoutePreview
};
