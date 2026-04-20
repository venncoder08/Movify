const RoutePreviewModel = require("../models/routePreviewModel");
const geoapifyRepository = require("../repositories/geoapifyRepository");
const placeService = require("./placeService");
const extractRoutePoints = require("../utils/routeGeometry");
const AppError = require("../utils/httpError");

async function preview({ pickupQuery, destinationQuery }) {
  if (!pickupQuery || !destinationQuery) {
    throw new AppError("Missing pickup or destination query parameter", 400);
  }

  const [pickup, destination] = await Promise.all([
    placeService.findPlaceByQuery(pickupQuery, {
      missingMessage: "Missing pickup query parameter",
      notFoundMessage: "Khong tim thay diem don"
    }),
    placeService.findPlaceByQuery(destinationQuery, {
      missingMessage: "Missing destination query parameter",
      notFoundMessage: "Khong tim thay diem den"
    })
  ]);

  const payload = await geoapifyRepository.fetchRoutePreview(pickup, destination);
  const feature = payload.features?.[0];
  if (!feature) {
    throw new AppError("Geoapify khong tra ve route", 404);
  }

  const routePoints = extractRoutePoints(feature.geometry);
  if (!routePoints.length) {
    throw new AppError("Khong doc duoc route points", 404);
  }

  return new RoutePreviewModel({
    pickup,
    destination,
    distanceMeters: feature.properties?.distance ?? null,
    durationSeconds: feature.properties?.time ?? null,
    routePoints
  });
}

module.exports = {
  preview
};
