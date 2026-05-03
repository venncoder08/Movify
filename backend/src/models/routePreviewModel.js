class RoutePreviewModel {
  constructor({ pickup, destination, distanceMeters, durationSeconds, routePoints }) {
    this.pickup = pickup;
    this.destination = destination;
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
    this.routePoints = routePoints;
  }
}

module.exports = RoutePreviewModel;
