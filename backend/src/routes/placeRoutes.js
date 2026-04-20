const express = require("express");

const placeController = require("../controllers/placeController");
const asyncHandler = require("../utils/asyncHandler");

const router = express.Router();

router.get("/autocomplete", asyncHandler(placeController.autocomplete));
router.get("/geocode", asyncHandler(placeController.geocode));

module.exports = router;
