const express = require("express");

const { listPromotions } = require("../controllers/promotionController");
const asyncHandler = require("../utils/asyncHandler");

const router = express.Router();

router.get("/", asyncHandler(listPromotions));

module.exports = router;
