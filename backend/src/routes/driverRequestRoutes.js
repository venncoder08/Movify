const express = require("express");

const {
  getMyDriverRequest,
  createDriverRequest
} = require("../controllers/driverRequestController");
const { authMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const { createDriverRequestSchema } = require("../validations");

const router = express.Router();

router.get("/me", authMiddleware, getMyDriverRequest);
router.post("/", authMiddleware, validateRequest(createDriverRequestSchema), createDriverRequest);

module.exports = router;
