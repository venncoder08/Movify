function extractRoutePoints(geometry) {
  const points = [];
  if (!geometry || !Array.isArray(geometry.coordinates)) {
    return points;
  }

  if (geometry.type === "LineString") {
    appendLineStringPoints(points, geometry.coordinates);
    return points;
  }

  if (geometry.type === "MultiLineString") {
    for (const lineString of geometry.coordinates) {
      appendLineStringPoints(points, lineString);
    }
  }

  return points;
}

function appendLineStringPoints(target, lineString) {
  if (!Array.isArray(lineString)) {
    return;
  }

  for (const coordinate of lineString) {
    if (!Array.isArray(coordinate) || coordinate.length < 2) {
      continue;
    }

    const [lng, lat] = coordinate;
    if (typeof lat !== "number" || typeof lng !== "number") {
      continue;
    }

    target.push({ lat, lng });
  }
}

module.exports = extractRoutePoints;
