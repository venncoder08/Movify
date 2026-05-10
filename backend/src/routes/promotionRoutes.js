const express = require("express");

const {
  listPromotions,
  createPromotion,
  updatePromotion,
  deletePromotion
} = require("../controllers/promotionController");
const asyncHandler = require("../utils/asyncHandler");
const { authMiddleware, roleMiddleware } = require("../middlewares/authMiddleware");

const router = express.Router();

router.get("/", asyncHandler(listPromotions));
router.post("/", authMiddleware, roleMiddleware(["Admin"]), asyncHandler(createPromotion));
router.patch("/:id", authMiddleware, roleMiddleware(["Admin"]), asyncHandler(updatePromotion));
router.put("/:id", authMiddleware, roleMiddleware(["Admin"]), asyncHandler(updatePromotion));
router.delete("/:id", authMiddleware, roleMiddleware(["Admin"]), asyncHandler(deletePromotion));

module.exports = router;
