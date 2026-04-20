const express = require("express");
const cors = require("cors");

const { mountRoutes } = require("./routes");
const errorHandler = require("./middlewares/errorHandler");
const notFoundHandler = require("./middlewares/notFoundHandler");

const app = express();

// Enable CORS for all origins (adjust in production)
app.use(cors({
  origin: "*",
  methods: ["GET", "POST", "PUT", "DELETE", "PATCH"],
  credentials: true
}));

app.use(express.json());

// Mount all routes
mountRoutes(app);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
