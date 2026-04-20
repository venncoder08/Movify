const express = require("express");

const healthRoutes = require("./routes/healthRoutes");
const placeRoutes = require("./routes/placeRoutes");
const routeRoutes = require("./routes/routeRoutes");
const errorHandler = require("./middlewares/errorHandler");
const notFoundHandler = require("./middlewares/notFoundHandler");

const app = express();

app.use(express.json());

app.use("/health", healthRoutes);
app.use("/api/places", placeRoutes);
app.use("/api/routes", routeRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
