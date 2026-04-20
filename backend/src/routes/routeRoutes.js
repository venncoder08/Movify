const express = require("express");

const routeController = require("../controllers/routeController");
const asyncHandler = require("../utils/asyncHandler");

const router = express.Router();

router.get("/preview", asyncHandler(routeController.preview));

module.exports = router;
