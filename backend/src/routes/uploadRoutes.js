const express = require("express");

const { authMiddleware } = require("../middlewares/authMiddleware");
const { upload } = require("../middlewares/uploadMiddleware");
const { uploadAvatar } = require("../controllers/uploadController");

const router = express.Router();

router.post("/avatar", authMiddleware, upload.single("avatar"), uploadAvatar);

module.exports = router;
