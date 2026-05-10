const express = require("express");

const {
  getMyDriverRequest,
  createDriverRequest,
  listDriverRequests,
  updateDriverRequestStatus
} = require("../controllers/driverRequestController");
const { authMiddleware, roleMiddleware } = require("../middlewares/authMiddleware");
const { validateRequest } = require("../middlewares/validateRequest");
const { createDriverRequestSchema, updateDriverRequestStatusSchema } = require("../validations");

const router = express.Router();

router.get("/me", authMiddleware, getMyDriverRequest);
router.post("/", authMiddleware, validateRequest(createDriverRequestSchema), createDriverRequest);
router.get("/admin", authMiddleware, roleMiddleware(["Admin"]), listDriverRequests);
router.patch("/:id/status", authMiddleware, roleMiddleware(["Admin"]), validateRequest(updateDriverRequestStatusSchema), updateDriverRequestStatus);

module.exports = router;
