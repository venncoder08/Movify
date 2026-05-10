const RoutePreviewModel = require("../models/routePreviewModel");
const geoapifyRepository = require("../repositories/geoapifyRepository");
const placeService = require("./placeService");
const extractRoutePoints = require("../utils/routeGeometry");
const AppError = require("../utils/httpError");

async function preview({ pickupQuery, destinationQuery }) {
  if (!pickupQuery || !destinationQuery) {
    throw new AppError("Thiếu tham số điểm đón hoặc điểm đến", 400);
  }

  const [pickup, destination] = await Promise.all([
    placeService.findPlaceByQuery(pickupQuery, {
      missingMessage: "Thiếu tham số điểm đón",
      notFoundMessage: "Không tìm thấy điểm đón"
    }),
    placeService.findPlaceByQuery(destinationQuery, {
      missingMessage: "Thiếu tham số điểm đến",
      notFoundMessage: "Không tìm thấy điểm đến"
    })
  ]);

  const payload = await geoapifyRepository.fetchRoutePreview(pickup, destination);
  const feature = payload.features?.[0];
  if (!feature) {
    throw new AppError("Geoapify không trả về tuyến đường", 404);
  }

  const routePoints = extractRoutePoints(feature.geometry);
  if (!routePoints.length) {
    throw new AppError("Không đọc được dữ liệu tuyến đường", 404);
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
