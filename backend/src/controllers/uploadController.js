const { Readable } = require("stream");

const cloudinary = require("../config/cloudinary");
const env = require("../config/env");

function uploadImageWithMessage(req, res, missingMessage, successMessage) {
  try {
    if (!req.file) {
      return res.status(400).json({
        error: "Validation Error",
        message: missingMessage
      });
    }

    const uploadStream = cloudinary.uploader.upload_stream(
      {
        folder: env.cloudinaryFolder,
        resource_type: "image",
        overwrite: false,
        unique_filename: true
      },
      (error, result) => {
        if (error) {
          return res.status(500).json({
            error: "Upload Error",
            message: error.message
          });
        }

        return res.status(200).json({
          message: successMessage,
          data: {
            url: result.secure_url,
            publicId: result.public_id
          }
        });
      }
    );

    Readable.from(req.file.buffer).pipe(uploadStream);
  } catch (error) {
    return res.status(500).json({
      error: "Internal Server Error",
      message: error.message
    });
  }
}

function uploadAvatar(req, res) {
  return uploadImageWithMessage(req, res, "Vui lòng chọn ảnh đại diện", "Tải ảnh đại diện lên thành công");
}

function uploadImage(req, res) {
  return uploadImageWithMessage(req, res, "Vui lòng chọn ảnh", "Tải ảnh lên thành công");
}

module.exports = {
  uploadAvatar,
  uploadImage
};
