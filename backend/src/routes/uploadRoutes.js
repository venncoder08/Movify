const express = require("express");

const { authMiddleware } = require("../middlewares/authMiddleware");
const { upload } = require("../middlewares/uploadMiddleware");
const { uploadAvatar, uploadImage } = require("../controllers/uploadController");

const router = express.Router();

router.post("/avatar", authMiddleware, upload.single("avatar"), uploadAvatar);
router.post("/image", authMiddleware, upload.single("image"), uploadImage);

module.exports = router;
